package com.ansimi.route;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.ansimi.candidate.RouteCandidate;
import com.ansimi.candidate.RouteCandidateService;
import com.ansimi.candidate.RouteRequest;
import com.ansimi.config.AppConfigService;
import com.ansimi.log.RouteRequestLogService;
import com.ansimi.risk.RiskScoringService;
import com.ansimi.risk.RouteRiskScore;
import com.ansimi.route.dto.CandidateDto;
import com.ansimi.route.dto.Place;
import com.ansimi.route.dto.RecommendRequest;
import com.ansimi.route.dto.RecommendResponse;
import com.github.benmanes.caffeine.cache.Cache;

import lombok.RequiredArgsConstructor;

// 전체 흐름을 조립한다: 후보 경로 생성 → 후보별 스코어링 → 추천 결정 → 응답 (개발 명세 3·6장).
// 캐시·요청 로그는 명세 7장.
@Service
@RequiredArgsConstructor
public class RouteService {

	private static final String NOTICE = "추천 경로는 참고용이며, 실제 보행 시 주변 상황을 직접 확인하세요.";
	private static final String TMAP_OPTION_PREFIX = "TMAP_OPTION_";
	private static final String ACTIVE_MODEL_KEY = "active_model";

	private final RequestValidator requestValidator;
	private final RouteCandidateService routeCandidateService;
	private final RiskScoringService riskScoringService;
	private final RecommendPolicy recommendPolicy;
	private final AppConfigService appConfigService;
	private final Cache<String, RecommendResponse> recommendCache;
	private final RouteRequestLogService routeRequestLogService;

	public RecommendResponse recommend(RecommendRequest request) {
		requestValidator.validate(request);
		long startedAt = System.currentTimeMillis();

		String activeModel = appConfigService.getString(ACTIVE_MODEL_KEY);
		String cacheKey = cacheKey(request, activeModel);

		RecommendResponse cached = recommendCache.getIfPresent(cacheKey);
		RecommendResponse response = cached != null ? cached : buildResponse(request, activeModel);
		if (cached == null) {
			recommendCache.put(cacheKey, response);
		}

		routeRequestLogService.log(request, response, System.currentTimeMillis() - startedAt);
		return response;
	}

	// 캐시 키에 active_model을 포함해서 v1/v2 전환 시 서로 다른 엔트리로 분리되게 한다.
	private String cacheKey(RecommendRequest request, String activeModel) {
		Place origin = request.origin();
		Place destination = request.destination();
		return "%s:%s:%s:%s:%s".formatted(
				activeModel, origin.coord().get(0), origin.coord().get(1),
				destination.coord().get(0), destination.coord().get(1));
	}

	private RecommendResponse buildResponse(RecommendRequest request, String activeModel) {
		List<RouteCandidate> candidates = routeCandidateService.findCandidates(toRouteRequest(request));

		List<RouteRiskScore> riskScores = candidates.stream()
				.map(c -> riskScoringService.score(c.route().coordinates()))
				.toList();

		List<ScoredCandidate> scoredCandidates = new ArrayList<>();
		for (int i = 0; i < candidates.size(); i++) {
			scoredCandidates.add(new ScoredCandidate(candidates.get(i).distanceMeters(), riskScores.get(i).riskScore()));
		}
		RecommendResult result = recommendPolicy.recommend(scoredCandidates);

		List<CandidateDto> candidateDtos = new ArrayList<>();
		for (int i = 0; i < candidates.size(); i++) {
			candidateDtos.add(toCandidateDto(i, candidates.get(i), riskScores.get(i), result));
		}

		return new RecommendResponse(activeModel, result.recommendedIndex(), candidateDtos, NOTICE);
	}

	private RouteRequest toRouteRequest(RecommendRequest request) {
		Place origin = request.origin();
		Place destination = request.destination();
		return new RouteRequest(
				origin.coord().get(0), origin.coord().get(1),
				destination.coord().get(0), destination.coord().get(1),
				origin.name(), destination.name());
	}

	private CandidateDto toCandidateDto(int index, RouteCandidate candidate, RouteRiskScore riskScore,
			RecommendResult result) {
		return new CandidateDto(
				index,
				toSource(candidate.source()),
				candidate.distanceMeters(),
				durationMinutes(candidate.route().totalTimeSeconds()),
				riskScore.riskScore(),
				riskScore.riskLevel(),
				(int) Math.round(riskScore.redLengthM()),
				result.detourRatios().get(index),
				result.eligible().get(index),
				riskScore.segments());
	}

	// RouteCandidate.source()는 searchOption 숫자("0"·"4"·"10"·"30") 또는 DETOUR_LEFT·
	// DETOUR_RIGHT를 담고 있다. 숫자인 경우만 응답 포맷(명세 4장 예시 "TMAP_OPTION_10")으로 바꾼다.
	private String toSource(String source) {
		return source.matches("\\d+") ? TMAP_OPTION_PREFIX + source : source;
	}

	private int durationMinutes(int totalTimeSeconds) {
		return (int) Math.round(totalTimeSeconds / 60.0);
	}

}
