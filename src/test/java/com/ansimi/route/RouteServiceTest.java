package com.ansimi.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ansimi.candidate.RouteCandidate;
import com.ansimi.candidate.RouteCandidateService;
import com.ansimi.candidate.RouteRequest;
import com.ansimi.config.AppConfigService;
import com.ansimi.risk.RiskScoringService;
import com.ansimi.risk.RouteRiskScore;
import com.ansimi.route.dto.CandidateDto;
import com.ansimi.route.dto.Place;
import com.ansimi.route.dto.RecommendRequest;
import com.ansimi.route.dto.RecommendResponse;
import com.ansimi.tmap.TmapRoute;
import com.ansimi.tmap.TmapRoute.Coordinate;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class RouteServiceTest {

	private RequestValidator requestValidator;
	private RouteCandidateService routeCandidateService;
	private RiskScoringService riskScoringService;
	private AppConfigService appConfigService;
	private RecommendPolicy recommendPolicy;
	private RouteService routeService;

	@BeforeEach
	void setUp() {
		requestValidator = mock(RequestValidator.class);
		routeCandidateService = mock(RouteCandidateService.class);
		riskScoringService = mock(RiskScoringService.class);
		appConfigService = mock(AppConfigService.class);
		recommendPolicy = mock(RecommendPolicy.class);
		routeService = new RouteService(
				requestValidator, routeCandidateService, riskScoringService, recommendPolicy, appConfigService);
	}

	@Test
	void 후보를_스코어링하고_추천_결과를_응답으로_조립한다() {
		RouteCandidate candidateA = candidate("0", 1000, 600);
		RouteCandidate candidateB = candidate("DETOUR_LEFT", 1150, 700);

		given(routeCandidateService.findCandidates(any(RouteRequest.class)))
				.willReturn(List.of(candidateA, candidateB));
		given(riskScoringService.score(candidateA.route().coordinates()))
				.willReturn(riskScore(40, "YELLOW", 0));
		given(riskScoringService.score(candidateB.route().coordinates()))
				.willReturn(riskScore(20, "GREEN", 0));
		given(recommendPolicy.recommend(List.of(new ScoredCandidate(1000, 40), new ScoredCandidate(1150, 20))))
				.willReturn(new RecommendResult(List.of(1.0, 1.15), List.of(true, true), 1));
		given(appConfigService.getString("active_model")).willReturn("v1");

		RecommendRequest request = new RecommendRequest(
				new Place(List.of(127.0473, 37.5172), "출발"),
				new Place(List.of(127.0573, 37.5232), "도착"));

		RecommendResponse response = routeService.recommend(request);

		assertThat(response.modelVersion()).isEqualTo("v1");
		assertThat(response.recommendedIndex()).isEqualTo(1);
		assertThat(response.notice()).isNotBlank();
		assertThat(response.candidates()).hasSize(2);

		CandidateDto first = response.candidates().get(0);
		assertThat(first.index()).isEqualTo(0);
		assertThat(first.source()).isEqualTo("TMAP_OPTION_0");
		assertThat(first.distanceM()).isEqualTo(1000);
		assertThat(first.durationMin()).isEqualTo(10);
		assertThat(first.riskScore()).isEqualTo(40);
		assertThat(first.riskLevel()).isEqualTo("YELLOW");
		assertThat(first.detourRatio()).isEqualTo(1.0);
		assertThat(first.eligible()).isTrue();

		CandidateDto second = response.candidates().get(1);
		assertThat(second.index()).isEqualTo(1);
		assertThat(second.source()).isEqualTo("DETOUR_LEFT");
		assertThat(second.detourRatio()).isEqualTo(1.15);

		verify(requestValidator).validate(request);
	}

	// 후보마다 좌표가 달라야 riskScoringService.score(...) 스텁이 서로 섞이지 않는다.
	private RouteCandidate candidate(String source, int distanceMeters, int timeSeconds) {
		double lng = 127.0 + distanceMeters / 100000.0;
		TmapRoute route = new TmapRoute(distanceMeters, timeSeconds,
				List.of(new Coordinate(127.0, 37.5), new Coordinate(lng, 37.501)));
		return new RouteCandidate(source, route);
	}

	private RouteRiskScore riskScore(int riskScore, String level, double redLengthM) {
		JsonNode segments = new ObjectMapper().valueToTree(Map.of("type", "FeatureCollection"));
		return new RouteRiskScore(riskScore, redLengthM, level, segments);
	}

}
