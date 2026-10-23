package com.ansimi.config;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

// app_config 테이블의 설정값을 읽는다. 재배포 없이 바꿀 수 있는 임계값들이다 (개발 명세 6.1·7장).
@Component
@RequiredArgsConstructor
public class AppConfigService {

	private static final String SELECT_VALUE_SQL = "SELECT value FROM app_config WHERE key = :key";

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public String getString(String key) {
		List<String> values = jdbcTemplate.queryForList(SELECT_VALUE_SQL, Map.of("key", key), String.class);
		if (values.isEmpty()) {
			throw new IllegalStateException("app_config에 키가 없습니다: " + key);
		}
		return values.get(0);
	}

	public double getDouble(String key) {
		return Double.parseDouble(getString(key));
	}

}
