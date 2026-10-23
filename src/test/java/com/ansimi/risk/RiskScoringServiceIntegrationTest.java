package com.ansimi.risk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ansimi.tmap.TmapRoute.Coordinate;

// db/seed/dev_seed.sql로 시드된 가짜 시범지역(강남구 일부, 중심 127.0473, 37.5172)이 로컬에
// 떠 있어야 통과한다 (docker compose up 후 시드 SQL 적용).
@SpringBootTest
class RiskScoringServiceIntegrationTest {

	@Autowired
	private RiskScoringService riskScoringService;

	@Test
	void 시드_데이터로_경로_위험도를_계산한다() {
		List<Coordinate> coordinates = List.of(
				new Coordinate(127.0393, 37.5172),
				new Coordinate(127.0473, 37.5172),
				new Coordinate(127.0553, 37.5172));

		RouteRiskScore result = riskScoringService.score(coordinates);

		assertThat(result.riskScore()).isBetween(0, 100);
		assertThat(result.redLengthM()).isGreaterThanOrEqualTo(0);
		assertThat(result.riskLevel()).isIn("GREEN", "YELLOW", "RED");
		assertThat(result.segments().path("type").asString()).isEqualTo("FeatureCollection");
		assertThat(result.segments().path("features").size()).isGreaterThan(0);
	}

}
