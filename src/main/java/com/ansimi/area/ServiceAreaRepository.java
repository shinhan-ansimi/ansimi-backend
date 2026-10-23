package com.ansimi.area;

import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

// 좌표가 시범 지역(service_area) 안에 있는지 확인한다.
@Repository
@RequiredArgsConstructor
public class ServiceAreaRepository {

	private static final String CONTAINS_QUERY = """
			SELECT EXISTS (
			  SELECT 1 FROM service_area
			  WHERE ST_Contains(geom, ST_Transform(ST_SetSRID(ST_MakePoint(:lng, :lat), 4326), 5179))
			)
			""";

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public boolean contains(double lng, double lat) {
		Boolean result = jdbcTemplate.queryForObject(CONTAINS_QUERY, Map.of("lng", lng, "lat", lat), Boolean.class);
		return Boolean.TRUE.equals(result);
	}

}
