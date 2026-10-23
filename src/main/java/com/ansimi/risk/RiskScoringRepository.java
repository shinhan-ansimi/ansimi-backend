package com.ansimi.risk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// 후보 경로 하나당 쿼리 한 번 (개발 명세 6.1장). v1/v2는 컬럼만 다르므로 SQL 문자열 두 개를
// 상수로 두고 호출하는 쪽(active_model)이 고른다 — 컬럼 이름을 문자열로 이어 붙이지 않는다.
@Repository
@RequiredArgsConstructor
public class RiskScoringRepository {

	private static final String SCORE_QUERY_V1 = """
			WITH route AS (
			  SELECT ST_Transform(ST_SetSRID(ST_GeomFromGeoJSON(:line), 4326), 5179) AS g
			)
			SELECT rg.risk_v1  AS risk,
			       rg.level_v1 AS level,
			       ST_Length(ST_Intersection(rg.geom, r.g))                         AS len_m,
			       ST_AsGeoJSON(ST_Transform(ST_Intersection(rg.geom, r.g), 4326)) AS piece
			FROM risk_grid rg, route r
			WHERE ST_Intersects(rg.geom, r.g)
			ORDER BY ST_LineLocatePoint(r.g, ST_Centroid(ST_Intersection(rg.geom, r.g)))
			""";

	private static final String SCORE_QUERY_V2 = """
			WITH route AS (
			  SELECT ST_Transform(ST_SetSRID(ST_GeomFromGeoJSON(:line), 4326), 5179) AS g
			)
			SELECT rg.risk_v2  AS risk,
			       rg.level_v2 AS level,
			       ST_Length(ST_Intersection(rg.geom, r.g))                         AS len_m,
			       ST_AsGeoJSON(ST_Transform(ST_Intersection(rg.geom, r.g), 4326)) AS piece
			FROM risk_grid rg, route r
			WHERE ST_Intersects(rg.geom, r.g)
			ORDER BY ST_LineLocatePoint(r.g, ST_Centroid(ST_Intersection(rg.geom, r.g)))
			""";

	private final NamedParameterJdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	public List<RiskGridPiece> findPieces(String routeLineStringGeoJson, boolean useV2) {
		String sql = useV2 ? SCORE_QUERY_V2 : SCORE_QUERY_V1;
		return jdbcTemplate.query(sql, Map.of("line", routeLineStringGeoJson),
				(rs, rowNum) -> new RiskGridPiece(
						rs.getDouble("risk"),
						rs.getString("level"),
						rs.getDouble("len_m"),
						parseLineStrings(rs.getString("piece"))));
	}

	// piece는 격자와 경로 교차 결과다. 대부분 LineString/MultiLineString이지만, 경로가 격자
	// 경계에 점으로만 닿는 경우 Point/MultiPoint/GeometryCollection도 나올 수 있다 — 길이가
	// 0이라 riskScore·redLengthM 계산에는 영향이 없으므로 선 geometry가 아니면 건너뛴다.
	private List<List<double[]>> parseLineStrings(String geoJson) {
		JsonNode geometry = objectMapper.readTree(geoJson);
		String type = geometry.path("type").asString();
		JsonNode coordinates = geometry.path("coordinates");

		List<List<double[]>> lines = new ArrayList<>();
		if ("MultiLineString".equals(type)) {
			for (JsonNode line : coordinates) {
				lines.add(toPoints(line));
			}
		} else if ("LineString".equals(type)) {
			lines.add(toPoints(coordinates));
		}
		return lines;
	}

	private List<double[]> toPoints(JsonNode lineCoordinates) {
		List<double[]> points = new ArrayList<>();
		for (JsonNode point : lineCoordinates) {
			points.add(new double[] { point.get(0).asDouble(), point.get(1).asDouble() });
		}
		return points;
	}

}
