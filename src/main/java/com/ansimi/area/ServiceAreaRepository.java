package com.ansimi.area;

import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// 좌표가 시범 지역(service_area) 안에 있는지 확인하고, 지역 경계를 GeoJSON으로 돌려준다.
@Repository
@RequiredArgsConstructor
public class ServiceAreaRepository {

	private static final String CONTAINS_QUERY = """
			SELECT EXISTS (
			  SELECT 1 FROM service_area
			  WHERE ST_Contains(geom, ST_Transform(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326), 5179))
			)
			""";

	// 시범 지역이 하나뿐이라 가장 먼저 등록된 행만 쓴다 (개발 명세 4장: GeoJSON Feature 하나 반환).
	private static final String AREA_GEOJSON_QUERY = """
			SELECT name, ST_AsGeoJSON(ST_Transform(geom, 4326)) AS geom_json
			FROM service_area
			ORDER BY id
			LIMIT 1
			""";

	private final NamedParameterJdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	public boolean contains(double lng, double lat) {
		Boolean result = jdbcTemplate.queryForObject(CONTAINS_QUERY, Map.of("lng", lng, "lat", lat), Boolean.class);
		return Boolean.TRUE.equals(result);
	}

	public JsonNode findAreaGeoJson() {
		return jdbcTemplate.queryForObject(AREA_GEOJSON_QUERY, Map.of(), (rs, rowNum) -> {
			JsonNode geometry = objectMapper.readTree(rs.getString("geom_json"));
			return objectMapper.valueToTree(Map.of(
					"type", "Feature",
					"geometry", geometry,
					"properties", Map.of("name", rs.getString("name"))));
		});
	}

}
