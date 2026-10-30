package com.ansimi.route;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ansimi.route.dto.RecommendResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

// 추천 응답 캐시 (개발 명세 7장): 최대 1,000개, 24시간 보관. 캐시 키에 active_model을
// 포함시켜(RouteService) v1/v2 전환 시 서로 다른 엔트리로 분리되게 한다.
@Configuration
public class RouteCacheConfig {

	private static final int MAX_ENTRIES = 1_000;
	private static final Duration TTL = Duration.ofHours(24);

	@Bean
	public Cache<String, RecommendResponse> recommendCache() {
		return Caffeine.newBuilder()
				.maximumSize(MAX_ENTRIES)
				.expireAfterWrite(TTL)
				.build();
	}

}
