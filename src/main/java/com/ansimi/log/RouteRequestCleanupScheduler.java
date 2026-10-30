package com.ansimi.log;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 매일 새벽 4시 30분에 30일보다 오래된 요청 로그를 지운다 (개발 명세 7장).
@Slf4j
@Component
@RequiredArgsConstructor
public class RouteRequestCleanupScheduler {

	private static final int RETENTION_DAYS = 30;

	private final RouteRequestLogRepository routeRequestLogRepository;

	@Scheduled(cron = "0 30 4 * * *")
	public void cleanUp() {
		Instant threshold = Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS);
		int deleted = routeRequestLogRepository.deleteOlderThan(threshold);
		log.info("오래된 요청 로그 {}건 삭제", deleted);
	}

}
