package com.ansimi.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.RequiredArgsConstructor;

// app_config 테이블의 설정값을 읽는다. 재배포 없이 바꿀 수 있는 임계값들이다 (개발 명세 6.1·7장).
// 60초 TTL 캐시를 둬서 매 요청마다 DB를 조회하지 않는다 — DB에서 값을 바꾸면 최대 60초 안에 반영된다.
@Component
@RequiredArgsConstructor
public class AppConfigService {

	private static final String SELECT_VALUE_SQL = "SELECT value FROM app_config WHERE key = :key";
	private static final Duration REFRESH_INTERVAL = Duration.ofSeconds(60);

	private final NamedParameterJdbcTemplate jdbcTemplate;
	private final Cache<String, String> cache = Caffeine.newBuilder()
			.expireAfterWrite(REFRESH_INTERVAL)
			.build();

	public String getString(String key) {
		return cache.get(key, this::readFromDb);
	}

	public double getDouble(String key) {
		return Double.parseDouble(getString(key));
	}

	private String readFromDb(String key) {
		List<String> values = jdbcTemplate.queryForList(SELECT_VALUE_SQL, Map.of("key", key), String.class);
		if (values.isEmpty()) {
			throw new IllegalStateException("app_config에 키가 없습니다: " + key);
		}
		return values.get(0);
	}

}
