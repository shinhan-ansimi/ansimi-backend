package com.ansimi.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

// 개발 명세 7장: 매일 새벽 30일보다 오래된 요청 로그를 지운다.
class RouteRequestCleanupSchedulerTest {

	private final RouteRequestLogRepository repository = mock(RouteRequestLogRepository.class);
	private final RouteRequestCleanupScheduler scheduler = new RouteRequestCleanupScheduler(repository);

	@Test
	void 삭제_기준일을_30일_전으로_계산해서_호출한다() {
		given(repository.deleteOlderThan(any(Instant.class))).willReturn(3);

		scheduler.cleanUp();

		ArgumentCaptor<Instant> thresholdCaptor = ArgumentCaptor.forClass(Instant.class);
		verify(repository).deleteOlderThan(thresholdCaptor.capture());

		Instant expected = Instant.now().minus(30, ChronoUnit.DAYS);
		assertThat(thresholdCaptor.getValue()).isCloseTo(expected, within(5, ChronoUnit.SECONDS));
	}

}
