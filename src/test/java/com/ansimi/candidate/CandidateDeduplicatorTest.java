package com.ansimi.candidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.ansimi.tmap.TmapRoute;
import com.ansimi.tmap.TmapRoute.Coordinate;

import tools.jackson.databind.ObjectMapper;

class CandidateDeduplicatorTest {

	private NamedParameterJdbcTemplate jdbcTemplate;
	private CandidateDeduplicator deduplicator;

	@BeforeEach
	void setUp() {
		jdbcTemplate = org.mockito.Mockito.mock(NamedParameterJdbcTemplate.class);
		deduplicator = new CandidateDeduplicator(jdbcTemplate, new ObjectMapper());
	}

	@Test
	void 거리차이가_15퍼센트_이상이면_겹침_쿼리_없이_둘_다_남는다() {
		RouteCandidate a = candidate("0", 1000);
		RouteCandidate b = candidate("4", 1200);

		List<RouteCandidate> result = deduplicator.deduplicate(List.of(a, b));

		assertThat(result).containsExactly(a, b);
		verify(jdbcTemplate, never()).queryForObject(anyString(), anyMap(), any(Class.class));
	}

	@Test
	void 거리차이가_작고_90퍼센트_이상_겹치면_짧은_쪽만_남는다() {
		RouteCandidate shorter = candidate("0", 1000);
		RouteCandidate longer = candidate("4", 1050);
		given(jdbcTemplate.queryForObject(anyString(), anyMap(), eq(Double.class))).willReturn(0.95);

		List<RouteCandidate> result = deduplicator.deduplicate(List.of(shorter, longer));

		assertThat(result).containsExactly(shorter);
	}

	@Test
	void 거리차이가_작아도_겹침이_90퍼센트_미만이면_둘_다_남는다() {
		RouteCandidate a = candidate("0", 1000);
		RouteCandidate b = candidate("4", 1050);
		given(jdbcTemplate.queryForObject(anyString(), anyMap(), eq(Double.class))).willReturn(0.5);

		List<RouteCandidate> result = deduplicator.deduplicate(List.of(a, b));

		assertThat(result).containsExactly(a, b);
	}

	private RouteCandidate candidate(String source, int distanceMeters) {
		TmapRoute route = new TmapRoute(distanceMeters, distanceMeters,
				List.of(new Coordinate(127.0, 37.5), new Coordinate(127.001, 37.501)));
		return new RouteCandidate(source, route);
	}

}
