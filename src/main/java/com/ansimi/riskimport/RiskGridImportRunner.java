package com.ansimi.riskimport;

import java.nio.file.Path;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// risk-import 프로필에서만 동작하는 CLI 진입점. 실제 변환·적재 로직은 RiskGridImportService에
// 있고, 여기서는 CLI 인자 해석과 JVM 종료만 담당한다 (테스트에서 System.exit을 안 타게
// 분리했다).
//
// 실행: ./gradlew bootRun --args="--spring.profiles.active=risk-import --risk-grid-path=/path/to/risk_grid.geojson"
@Slf4j
@Component
@Profile("risk-import")
@RequiredArgsConstructor
public class RiskGridImportRunner implements ApplicationRunner {

	private final RiskGridImportService importService;
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
			List<String> paths = args.getOptionValues("risk-grid-path");
			if (paths == null || paths.isEmpty()) {
				throw new IllegalArgumentException("--risk-grid-path=<risk_grid.geojson 경로> 가 필요합니다.");
			}

			RiskGridImportService.ImportResult result = importService.importRiskGrid(Path.of(paths.get(0)));
			log.info("risk_grid 적재 완료: 총 {}개, 등급 분포={}", result.rowCount(), result.levelCounts());
			return 0;
		} catch (Exception e) {
			log.error("risk_grid 적재 실패", e);
			return 1;
		}
	}

}
