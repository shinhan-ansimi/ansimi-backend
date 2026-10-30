package com.ansimi.log;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

// 추천 요청 로그를 적재·정리한다 (개발 명세 7장). 좌표는 테이블 컬럼이 NUMERIC(·,3)이라
// 저장되는 순간 소수점 3자리로 반올림된다 — 애플리케이션 코드에서 따로 자르지 않아도 된다.
@Repository
@RequiredArgsConstructor
public class RouteRequestLogRepository {

	private static final String INSERT_SQL = """
			INSERT INTO route_request
			  (origin_lat, origin_lng, dest_lat, dest_lng, candidate_count, candidates, chosen_index, model_ver, elapsed_ms)
			VALUES
			  (:originLat, :originLng, :destLat, :destLng, :candidateCount, CAST(:candidates AS JSONB), :chosenIndex, :modelVersion, :elapsedMs)
			""";

	private static final String DELETE_OLDER_THAN_SQL = "DELETE FROM route_request WHERE created_at < :threshold";

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public void insert(double originLat, double originLng, double destLat, double destLng,
			int candidateCount, String candidatesJson, int chosenIndex, String modelVersion, long elapsedMs) {
		Map<String, Object> params = Map.ofEntries(
				Map.entry("originLat", originLat),
				Map.entry("originLng", originLng),
				Map.entry("destLat", destLat),
				Map.entry("destLng", destLng),
				Map.entry("candidateCount", candidateCount),
				Map.entry("candidates", candidatesJson),
				Map.entry("chosenIndex", chosenIndex),
				Map.entry("modelVersion", modelVersion),
				Map.entry("elapsedMs", elapsedMs));
		jdbcTemplate.update(INSERT_SQL, params);
	}

	// 30일보다 오래된 로그를 지운다 (매일 새벽 스케줄러가 호출).
	public int deleteOlderThan(Instant threshold) {
		return jdbcTemplate.update(DELETE_OLDER_THAN_SQL, Map.of("threshold", Timestamp.from(threshold)));
	}

}
