package com.ansimi.candidate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;

import com.ansimi.area.ServiceAreaRepository;
import com.ansimi.candidate.DetourWaypointGenerator.Waypoint;
import com.ansimi.tmap.PedestrianRouteRequest;
import com.ansimi.tmap.TmapClient;
import com.ansimi.tmap.TmapClientException;
import com.ansimi.tmap.TmapRoute;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// 후보 경로를 여러 개 만든다 (개발 명세 5장): searchOption 4개 병렬 호출 → 중복 제거 →
// 후보가 2개 미만이면 경유지 대안 추가.
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteCandidateService {

	private static final List<Integer> SEARCH_OPTIONS = List.of(0, 4, 10, 30);
	private static final int MIN_CANDIDATES = 2;
	private static final int RATE_LIMITED_STATUS = 429;

	private final TmapClient tmapClient;
	private final CandidateDeduplicator candidateDeduplicator;
	private final DetourWaypointGenerator detourWaypointGenerator;
	private final ServiceAreaRepository serviceAreaRepository;

	public List<RouteCandidate> findCandidates(RouteRequest request) {
		List<RouteCandidate> candidates = callAllOptions(request);
		if (candidates.isEmpty()) {
			throw new RouteProviderException("Tmap 후보 경로를 하나도 받아오지 못했습니다.");
		}

		List<RouteCandidate> deduplicated = candidateDeduplicator.deduplicate(candidates);
		if (deduplicated.size() < MIN_CANDIDATES) {
			deduplicated = addDetourCandidates(deduplicated, request);
		}
		return deduplicated;
	}

	private List<RouteCandidate> callAllOptions(RouteRequest request) {
		AtomicBoolean rateLimited = new AtomicBoolean(false);

		List<RouteCandidate> candidates = Flux.fromIterable(SEARCH_OPTIONS)
				.flatMap(option -> callOption(request, option)
						.onErrorResume(e -> {
							if (isRateLimited(e)) {
								rateLimited.set(true);
							}
							log.warn("searchOption {} 호출 실패", option, e);
							return Mono.empty();
						}))
				.collectList()
				.block();

		// Tmap이 429(초당 호출 수 초과)를 반환하면 병렬 대신 순서대로 다시 호출한다.
		if (rateLimited.get()) {
			return callOptionsSequentially(request);
		}
		return candidates;
	}

	private List<RouteCandidate> callOptionsSequentially(RouteRequest request) {
		List<RouteCandidate> results = new ArrayList<>();
		for (int option : SEARCH_OPTIONS) {
			try {
				results.add(callOption(request, option).block());
			} catch (TmapClientException e) {
				log.warn("searchOption {} 순차 호출 실패", option, e);
			}
		}
		return results;
	}

	private Mono<RouteCandidate> callOption(RouteRequest request, int searchOption) {
		PedestrianRouteRequest pedestrianRequest = new PedestrianRouteRequest(
				request.startLng(), request.startLat(), request.endLng(), request.endLat(),
				request.startName(), request.endName(), searchOption);
		return tmapClient.getPedestrianRouteAsync(pedestrianRequest)
				.map(route -> new RouteCandidate(String.valueOf(searchOption), route));
	}

	private boolean isRateLimited(Throwable e) {
		return e instanceof TmapClientException tce && Integer.valueOf(RATE_LIMITED_STATUS).equals(tce.statusCode());
	}

	private List<RouteCandidate> addDetourCandidates(List<RouteCandidate> existing, RouteRequest request) {
		List<RouteCandidate> combined = new ArrayList<>(existing);

		Waypoint left = detourWaypointGenerator.left(
				request.startLng(), request.startLat(), request.endLng(), request.endLat());
		Waypoint right = detourWaypointGenerator.right(
				request.startLng(), request.startLat(), request.endLng(), request.endLat());

		callDetour(request, left, "DETOUR_LEFT").ifPresent(combined::add);
		callDetour(request, right, "DETOUR_RIGHT").ifPresent(combined::add);

		return candidateDeduplicator.deduplicate(combined);
	}

	private Optional<RouteCandidate> callDetour(RouteRequest request, Waypoint waypoint, String source) {
		if (!serviceAreaRepository.contains(waypoint.lng(), waypoint.lat())) {
			return Optional.empty();
		}

		String passList = waypoint.lng() + "," + waypoint.lat();
		PedestrianRouteRequest pedestrianRequest = new PedestrianRouteRequest(
				request.startLng(), request.startLat(), request.endLng(), request.endLat(),
				request.startName(), request.endName(), 0, passList);
		try {
			TmapRoute route = tmapClient.getPedestrianRoute(pedestrianRequest);
			return Optional.of(new RouteCandidate(source, route));
		} catch (TmapClientException e) {
			log.warn("{} 경유지 호출 실패", source, e);
			return Optional.empty();
		}
	}

}
