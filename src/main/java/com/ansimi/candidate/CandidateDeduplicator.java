package com.ansimi.candidate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.ansimi.tmap.TmapRoute.Coordinate;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

// 두 경로가 90% 이상 겹치면 같은 경로로 보고 짧은 쪽만 남긴다 (개발 명세 5장).
@Component
@RequiredArgsConstructor
public class CandidateDeduplicator {

	private static final double OVERLAP_THRESHOLD = 0.9;
	private static final double DISTANCE_DIFF_SKIP_THRESHOLD = 0.15;

	private static final String OVERLAP_QUERY = """
			SELECT LEAST(
			  ST_Length(ST_Intersection(ST_Buffer(a, 10), b)) / ST_Length(b),
			  ST_Length(ST_Intersection(ST_Buffer(b, 10), a)) / ST_Length(a)) AS overlap
			FROM (SELECT ST_Transform(ST_SetSRID(ST_GeomFromGeoJSON(:a), 4326), 5179) AS a,
			             ST_Transform(ST_SetSRID(ST_GeomFromGeoJSON(:b), 4326), 5179) AS b) t
			""";

	private final NamedParameterJdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	public List<RouteCandidate> deduplicate(List<RouteCandidate> candidates) {
		List<RouteCandidate> remaining = new ArrayList<>(candidates);

		for (int i = 0; i < remaining.size(); i++) {
			for (int j = i + 1; j < remaining.size(); j++) {
				RouteCandidate a = remaining.get(i);
				RouteCandidate b = remaining.get(j);
				if (a == null || b == null || distanceDiffRatio(a, b) >= DISTANCE_DIFF_SKIP_THRESHOLD) {
					continue;
				}
				if (overlapRatio(a, b) >= OVERLAP_THRESHOLD) {
					int longerIndex = a.distanceMeters() >= b.distanceMeters() ? i : j;
					remaining.set(longerIndex, null);
				}
			}
		}

		return remaining.stream().filter(Objects::nonNull).toList();
	}

	private double distanceDiffRatio(RouteCandidate a, RouteCandidate b) {
		int max = Math.max(a.distanceMeters(), b.distanceMeters());
		if (max == 0) {
			return 0;
		}
		return Math.abs(a.distanceMeters() - b.distanceMeters()) / (double) max;
	}

	private double overlapRatio(RouteCandidate a, RouteCandidate b) {
		Map<String, Object> params = Map.of(
				"a", toLineStringGeoJson(a.route().coordinates()),
				"b", toLineStringGeoJson(b.route().coordinates()));
		Double overlap = jdbcTemplate.queryForObject(OVERLAP_QUERY, params, Double.class);
		return overlap == null ? 0 : overlap;
	}

	private String toLineStringGeoJson(List<Coordinate> coordinates) {
		List<double[]> points = coordinates.stream()
				.map(c -> new double[] { c.lng(), c.lat() })
				.toList();
		return objectMapper.writeValueAsString(Map.of("type", "LineString", "coordinates", points));
	}

}
