package com.ansimi.log;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

// 로컬 PostGIS(db/seed 적용 전이라도 route_request 테이블은 Flyway가 만듦)가 떠 있어야 통과한다.
@SpringBootTest
class RouteRequestLogRepositoryIntegrationTest {

	@Autowired
	private RouteRequestLogRepository repository;

	@Autowired
	private NamedParameterJdbcTemplate jdbcTemplate;

	@Test
	void 저장한_좌표는_소수점_3자리로_반올림된다() {
		repository.insert(37.5172345, 127.0473456, 37.5232987, 127.0573111,
				2, "[{\"index\":0,\"distanceM\":1000}]", 0, "v1", 123);

		Map<String, Object> row = jdbcTemplate.queryForMap(
				"SELECT origin_lat, origin_lng, dest_lat, dest_lng FROM route_request ORDER BY request_id DESC LIMIT 1",
				Map.of());

		assertThat(((Number) row.get("origin_lat")).doubleValue()).isEqualTo(37.517);
		assertThat(((Number) row.get("origin_lng")).doubleValue()).isEqualTo(127.047);
		assertThat(((Number) row.get("dest_lat")).doubleValue()).isEqualTo(37.523);
		assertThat(((Number) row.get("dest_lng")).doubleValue()).isEqualTo(127.057);
	}

	@Test
	void 삭제_기준일보다_오래된_로그만_지워진다() {
		// 다른 테스트가 남긴 행과 섞이지 않게, 이 테스트만 쓰는 좌표(1.111/2.222)로 표시해둔다.
		insertWithCreatedAt(1.111, Instant.now().minus(40, ChronoUnit.DAYS));
		insertWithCreatedAt(2.222, Instant.now().minus(10, ChronoUnit.DAYS));

		repository.deleteOlderThan(Instant.now().minus(30, ChronoUnit.DAYS));

		List<Double> remainingMarkers = jdbcTemplate.query(
				"SELECT origin_lat FROM route_request WHERE origin_lat IN (1.111, 2.222)",
				Map.of(),
				(rs, rowNum) -> rs.getDouble("origin_lat"));
		assertThat(remainingMarkers).containsExactly(2.222);
	}

	private void insertWithCreatedAt(double marker, Instant createdAt) {
		jdbcTemplate.update(
				"INSERT INTO route_request (origin_lat, origin_lng, dest_lat, dest_lng, created_at) "
						+ "VALUES (:marker, :marker, :marker, :marker, :createdAt)",
				Map.of("marker", marker, "createdAt", java.sql.Timestamp.from(createdAt)));
	}

}
