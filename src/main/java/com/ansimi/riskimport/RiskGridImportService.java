package com.ansimi.riskimport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.ansimi.config.AppConfigService;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// ansimi-ai(develop)의 risk_grid.geojson(2.2 스코어링 완료분, safety_score 포함, EPSG:4326)을
// risk_v1·level_v1로 변환해 risk_grid 테이블에 적재한다 (이슈 #23).
// safety_score는 높을수록 안전이라 risk_v1 = 100 - safety_score로 뒤집는다.
@Component
@RequiredArgsConstructor
public class RiskGridImportService {

	private static final String UPSERT_SQL = """
			INSERT INTO risk_grid (grid_id, geom, risk_v1, level_v1)
			VALUES (:gridId, ST_Transform(ST_SetSRID(ST_GeomFromGeoJSON(:geomJson), 4326), 5179),
			        :riskV1, :levelV1)
			ON CONFLICT (grid_id) DO UPDATE
			  SET geom = excluded.geom,
			      risk_v1 = excluded.risk_v1,
			      level_v1 = excluded.level_v1,
			      updated_at = now()
			""";

	private final NamedParameterJdbcTemplate jdbcTemplate;
	private final AppConfigService appConfigService;
	private final ObjectMapper objectMapper;

	public record ImportResult(int rowCount, Map<String, Integer> levelCounts) {
	}

	public ImportResult importRiskGrid(Path geojsonPath) throws IOException {
		double yellowMin = appConfigService.getDouble("level_yellow_min");
		double redMin = appConfigService.getDouble("level_red_min");

		JsonNode root = objectMapper.readTree(Files.readString(geojsonPath));
		List<Map<String, ?>> rows = new ArrayList<>();
		Map<String, Integer> levelCounts = new HashMap<>();

		for (JsonNode feature : root.path("features")) {
			JsonNode properties = feature.path("properties");
			if (!properties.has("safety_score")) {
				throw new IllegalStateException(
						"safety_score가 없습니다 — ansimi-ai에서 `python -m data_pipeline.pipeline` 실행 결과(2.2 스코어링 완료분)인지 확인하세요.");
			}

			double safetyScore = properties.path("safety_score").asDouble();
			double riskV1 = Math.round(Math.clamp(100 - safetyScore, 0, 100) * 100) / 100.0;
			String levelV1 = riskV1 >= redMin ? "RED" : riskV1 >= yellowMin ? "YELLOW" : "GREEN";
			levelCounts.merge(levelV1, 1, Integer::sum);

			rows.add(Map.of(
					"gridId", properties.path("grid_id").asString(),
					"geomJson", objectMapper.writeValueAsString(feature.path("geometry")),
					"riskV1", riskV1,
					"levelV1", levelV1));
		}

		if (rows.isEmpty()) {
			throw new IllegalStateException("risk_grid.geojson에 feature가 없습니다.");
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		Map<String, ?>[] batchValues = rows.toArray(new Map[0]);
		jdbcTemplate.batchUpdate(UPSERT_SQL, batchValues);

		return new ImportResult(rows.size(), levelCounts);
	}

}
