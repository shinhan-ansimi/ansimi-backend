package com.ansimi.log;

import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ansimi.route.dto.CandidateDto;
import com.ansimi.route.dto.Place;
import com.ansimi.route.dto.RecommendRequest;
import com.ansimi.route.dto.RecommendResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

// 추천 요청을 비동기로 기록한다 (개발 명세 7장): 캐시 적중 여부와 관계없이 매 요청마다 호출된다.
// segments(GeoJSON)는 용량이 커서 뺀 요약만 candidates 컬럼에 저장한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteRequestLogService {

	private final RouteRequestLogRepository routeRequestLogRepository;
	private final ObjectMapper objectMapper;

	@Async
	public void log(RecommendRequest request, RecommendResponse response, long elapsedMs) {
		try {
			Place origin = request.origin();
			Place destination = request.destination();
			String candidatesJson = objectMapper.writeValueAsString(summarize(response.candidates()));

			routeRequestLogRepository.insert(
					origin.coord().get(1), origin.coord().get(0),
					destination.coord().get(1), destination.coord().get(0),
					response.candidates().size(),
					candidatesJson,
					response.recommendedIndex(),
					response.modelVersion(),
					elapsedMs);
		} catch (RuntimeException e) {
			// 로그 적재 실패가 추천 응답에 영향을 주면 안 된다 — 서버 로그에만 남긴다.
			log.warn("요청 로그 저장 실패", e);
		}
	}

	private List<Map<String, Object>> summarize(List<CandidateDto> candidates) {
		return candidates.stream()
				.map(c -> Map.<String, Object>of(
						"index", c.index(),
						"distanceM", c.distanceM(),
						"riskScore", c.riskScore(),
						"riskLevel", c.riskLevel(),
						"detourRatio", c.detourRatio(),
						"eligible", c.eligible()))
				.toList();
	}

}
