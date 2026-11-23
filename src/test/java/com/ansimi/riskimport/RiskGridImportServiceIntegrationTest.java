package com.ansimi.riskimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.ansimi.config.AppConfigService;

// 로컬 PostGIS가 떠 있어야 통과한다. grid_id 900001~900099는 이 테스트 전용 마커라 매번
// 먼저 지우고 다시 넣어서 재실행에도 누적되지 않게 한다.
@SpringBootTest
class RiskGridImportServiceIntegrationTest {

	@Autowired
	private RiskGridImportService importService;

	@Autowired
	private NamedParameterJdbcTemplate jdbcTemplate;

	@Autowired
	private AppConfigService appConfigService;

	@Test
	void safety_score를_뒤집어서_risk_v1과_level_v1을_적재한다() throws Exception {
		deleteTestRows();
		double yellowMin = appConfigService.getDouble("level_yellow_min");
		double redMin = appConfigService.getDouble("level_red_min");

		RiskGridImportService.ImportResult result = importService.importRiskGrid(fixture("risk_grid_sample.geojson"));

		assertThat(result.rowCount()).isEqualTo(3);

		Map<String, Object> safe = jdbcTemplate.queryForMap(
				"SELECT risk_v1, level_v1, ST_SRID(geom) AS srid FROM risk_grid WHERE grid_id = '900001'", Map.of());
		assertThat(((Number) safe.get("risk_v1")).doubleValue()).isEqualTo(7.5); // 100 - 92.5
		assertThat(safe.get("level_v1")).isEqualTo(expectedLevel(7.5, yellowMin, redMin));
		assertThat(((Number) safe.get("srid")).intValue()).isEqualTo(5179);

		Map<String, Object> dangerous = jdbcTemplate.queryForMap(
				"SELECT risk_v1, level_v1 FROM risk_grid WHERE grid_id = '900002'", Map.of());
		assertThat(((Number) dangerous.get("risk_v1")).doubleValue()).isEqualTo(90.0); // 100 - 10.0
		assertThat(dangerous.get("level_v1")).isEqualTo(expectedLevel(90.0, yellowMin, redMin));
	}

	// level_yellow_min/level_red_min은 환경마다(dev 기본값, 실데이터 재조정 등) 다를 수 있어서
	// RiskGridImportService와 같은 규칙으로 직접 계산해 비교한다 — 특정 환경의 값을 하드코딩하지 않는다.
	private String expectedLevel(double riskV1, double yellowMin, double redMin) {
		if (riskV1 >= redMin) {
			return "RED";
		}
		if (riskV1 >= yellowMin) {
			return "YELLOW";
		}
		return "GREEN";
	}

	@Test
	void 같은_grid_id는_다시_적재하면_덮어쓴다() throws Exception {
		deleteTestRows();
		importService.importRiskGrid(fixture("risk_grid_sample.geojson"));

		// safety_score가 그대로인 파일을 다시 적재해도(=ON CONFLICT UPDATE), 행 개수는 안 늘어난다.
		RiskGridImportService.ImportResult result = importService.importRiskGrid(fixture("risk_grid_sample.geojson"));

		assertThat(result.rowCount()).isEqualTo(3);
		Long count = jdbcTemplate.queryForObject(
				"SELECT count(*) FROM risk_grid WHERE grid_id IN ('900001','900002','900003')", Map.of(), Long.class);
		assertThat(count).isEqualTo(3);
	}

	@Test
	void safety_score가_없으면_예외를_던진다() {
		assertThatThrownBy(() -> importService.importRiskGrid(fixture("risk_grid_missing_safety_score.geojson")))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("safety_score");
	}

	private void deleteTestRows() {
		jdbcTemplate.update("DELETE FROM risk_grid WHERE grid_id LIKE '9000%'", Map.of());
	}

	private Path fixture(String fileName) throws URISyntaxException {
		return Path.of(getClass().getClassLoader().getResource("riskimport/" + fileName).toURI());
	}

}