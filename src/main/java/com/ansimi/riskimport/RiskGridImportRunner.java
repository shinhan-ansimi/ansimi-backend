package com.ansimi.riskimport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.ansimi.config.AppConfigService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// ansimi-ai(develop)의 risk_grid.geojson(2.2 스코어링 완료분, safety_score 포함, EPSG:4326)을
// 읽어 risk_v1·level_v1로 변환한 뒤 risk_grid 테이블에 적재한다 (이슈 #23).
// safety_score는 높을수록 안전이라 risk_v1 = 100 - safety_score로 뒤집는다.
//
// 실행: ./gradlew bootRun --args="--spring.profiles.active=risk-import --risk-grid-path=/path/to/risk_grid.geojson"
@Slf4j
@Component
@Profile("risk-import")
@RequiredArgsConstructor
public class RiskGridImportRunner implements ApplicationRunner {

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
	private final ConfigurableApplicationContext context;

	// web-application-type: none이라 서버가 안 뜨는 대신, 적재 후 JVM이 스스로 안 종료되면
	// (HikariCP 등 비-데몬 스레드 때문에) 셸이 계속 걸려 있는다 — 그래서 끝나면 명시적으로 종료한다.
	@Override
	public void run(ApplicationArguments args) {
		int exitCode = runImportAndReturnExitCode(args);
		System.exit(SpringApplication.exit(context, () -> exitCode));
	}

	private int runImportAndReturnExitCode(ApplicationArguments args) {
		try {
			importRiskGrid(args);
			return 0;
		} catch (Exception e) {
			log.error("risk_grid 적재 실패", e);
			return 1;
		}
	}

	private void importRiskGrid(ApplicationArguments args) throws IOException {
		List<String> paths = args.getOptionValues("risk-grid-path");
		if (paths == null || paths.isEmpty()) {
			throw new IllegalArgumentException("--risk-grid-path=<risk_grid.geojson 경로> 가 필요합니다.");
		}

		double yellowMin = appConfigService.getDouble("level_yellow_min");
		double redMin = appConfigService.getDouble("level_red_min");

		JsonNode root = objectMapper.readTree(Files.readString(Path.of(paths.get(0))));
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
		log.info("risk_grid 적재 완료: 총 {}개, 등급 분포={}", rows.size(), levelCounts);
	}

}
