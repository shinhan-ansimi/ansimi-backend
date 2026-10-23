package com.ansimi.candidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ansimi.area.ServiceAreaRepository;
import com.ansimi.candidate.DetourWaypointGenerator.Waypoint;
import com.ansimi.tmap.PedestrianRouteRequest;
import com.ansimi.tmap.TmapClient;
import com.ansimi.tmap.TmapClientException;
import com.ansimi.tmap.TmapRoute;
import com.ansimi.tmap.TmapRoute.Coordinate;

import reactor.core.publisher.Mono;

class RouteCandidateServiceTest {

	private static final RouteRequest REQUEST = new RouteRequest(127.0473, 37.5172, 127.0573, 37.5172, "출발", "도착");

	private TmapClient tmapClient;
	private CandidateDeduplicator candidateDeduplicator;
	private DetourWaypointGenerator detourWaypointGenerator;
	private ServiceAreaRepository serviceAreaRepository;
	private RouteCandidateService service;

	@BeforeEach
	void setUp() {
		tmapClient = mock(TmapClient.class);
		candidateDeduplicator = mock(CandidateDeduplicator.class);
		detourWaypointGenerator = mock(DetourWaypointGenerator.class);
		serviceAreaRepository = mock(ServiceAreaRepository.class);
		service = new RouteCandidateService(tmapClient, candidateDeduplicator, detourWaypointGenerator,
				serviceAreaRepository);

		// 기본값: 중복 제거는 입력을 그대로 돌려준다 (이 테스트 묶음에서는 검증 대상이 아님).
		given(candidateDeduplicator.deduplicate(anyList())).willAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void 일부_옵션이_실패해도_성공한_후보만_사용한다() {
		given(tmapClient.getPedestrianRouteAsync(forOption(0))).willReturn(Mono.just(route(1000)));
		given(tmapClient.getPedestrianRouteAsync(forOption(4)))
				.willReturn(Mono.error(new TmapClientException("실패", 500, null)));
		given(tmapClient.getPedestrianRouteAsync(forOption(10))).willReturn(Mono.just(route(1100)));
		given(tmapClient.getPedestrianRouteAsync(forOption(30))).willReturn(Mono.just(route(1200)));

		List<RouteCandidate> result = service.findCandidates(REQUEST);

		assertThat(result).extracting(RouteCandidate::source).containsExactlyInAnyOrder("0", "10", "30");
	}

	@Test
	void 전부_실패하면_RouteProviderException이_발생한다() {
		given(tmapClient.getPedestrianRouteAsync(any()))
				.willReturn(Mono.error(new TmapClientException("실패", 500, null)));

		assertThatThrownBy(() -> service.findCandidates(REQUEST)).isInstanceOf(RouteProviderException.class);
	}

	@Test
	void 상태코드_429_응답이_있으면_순차_호출로_전환한다() {
		given(tmapClient.getPedestrianRouteAsync(forOption(0))).willReturn(Mono.just(route(1000)));
		given(tmapClient.getPedestrianRouteAsync(forOption(4)))
				.willReturn(Mono.error(new TmapClientException("rate limited", 429, null)));
		given(tmapClient.getPedestrianRouteAsync(forOption(10))).willReturn(Mono.just(route(1100)));
		given(tmapClient.getPedestrianRouteAsync(forOption(30))).willReturn(Mono.just(route(1200)));

		List<RouteCandidate> result = service.findCandidates(REQUEST);

		assertThat(result).extracting(RouteCandidate::source).containsExactlyInAnyOrder("0", "10", "30");
		// 병렬 시도 1회 + 429 감지 후 순차 재시도 1회 = 옵션당 최소 2번 호출된다.
		verify(tmapClient, atLeast(2)).getPedestrianRouteAsync(forOption(0));
	}

	@Test
	void 중복_제거_후_후보가_2개_미만이면_경유지_대안을_호출한다() {
		given(tmapClient.getPedestrianRouteAsync(any())).willReturn(Mono.just(route(1000)));
		given(candidateDeduplicator.deduplicate(anyList())).willAnswer(invocation -> {
			List<RouteCandidate> input = invocation.getArgument(0);
			boolean hasDetour = input.stream().anyMatch(c -> c.source().startsWith("DETOUR"));
			return hasDetour ? input : List.of(input.get(0));
		});
		given(detourWaypointGenerator.left(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
				.willReturn(new Waypoint(127.05, 37.52));
		given(detourWaypointGenerator.right(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
				.willReturn(new Waypoint(127.05, 37.51));
		given(serviceAreaRepository.contains(anyDouble(), anyDouble())).willReturn(true);
		given(tmapClient.getPedestrianRoute(any())).willReturn(route(1300));

		List<RouteCandidate> result = service.findCandidates(REQUEST);

		verify(detourWaypointGenerator).left(anyDouble(), anyDouble(), anyDouble(), anyDouble());
		verify(detourWaypointGenerator).right(anyDouble(), anyDouble(), anyDouble(), anyDouble());
		verify(tmapClient, times(2)).getPedestrianRoute(any());
		assertThat(result).extracting(RouteCandidate::source).contains("DETOUR_LEFT", "DETOUR_RIGHT");
	}

	@Test
	void 경유지가_지역_밖이면_호출을_건너뛴다() {
		given(tmapClient.getPedestrianRouteAsync(any())).willReturn(Mono.just(route(1000)));
		given(candidateDeduplicator.deduplicate(anyList())).willAnswer(invocation -> {
			List<RouteCandidate> input = invocation.getArgument(0);
			boolean hasDetour = input.stream().anyMatch(c -> c.source().startsWith("DETOUR"));
			return hasDetour ? input : List.of(input.get(0));
		});
		given(detourWaypointGenerator.left(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
				.willReturn(new Waypoint(127.05, 37.52));
		given(detourWaypointGenerator.right(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
				.willReturn(new Waypoint(127.05, 37.51));
		given(serviceAreaRepository.contains(anyDouble(), anyDouble())).willReturn(false);

		List<RouteCandidate> result = service.findCandidates(REQUEST);

		verify(tmapClient, times(0)).getPedestrianRoute(any());
		assertThat(result).extracting(RouteCandidate::source).doesNotContain("DETOUR_LEFT", "DETOUR_RIGHT");
	}

	private PedestrianRouteRequest forOption(int option) {
		return argThat(req -> req != null && req.searchOption() == option);
	}

	private TmapRoute route(int distanceMeters) {
		return new TmapRoute(distanceMeters, distanceMeters,
				List.of(new Coordinate(127.0, 37.5), new Coordinate(127.001, 37.501)));
	}

}
