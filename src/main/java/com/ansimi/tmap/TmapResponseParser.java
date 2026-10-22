package com.ansimi.tmap;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.ansimi.tmap.TmapRoute.Coordinate;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// Tmap 보행자 경로 안내 응답(GeoJSON FeatureCollection)을 TmapRoute로 변환한다.
@Component
@RequiredArgsConstructor
public class TmapResponseParser {

	private static final String POINT_TYPE_START = "SP";
	private static final String GEOMETRY_LINE_STRING = "LineString";

	private final ObjectMapper objectMapper;

	public TmapRoute parse(String responseBody) {
		try {
			return parse(objectMapper.readTree(responseBody));
		} catch (JacksonException e) {
			throw new TmapResponseParseException("Tmap 응답을 파싱할 수 없습니다.", e);
		}
	}

	public TmapRoute parse(JsonNode root) {
		int totalDistanceMeters = 0;
		int totalTimeSeconds = 0;
		List<Coordinate> coordinates = new ArrayList<>();

		for (JsonNode feature : root.path("features")) {
			JsonNode properties = feature.path("properties");
			JsonNode geometry = feature.path("geometry");

			if (POINT_TYPE_START.equals(properties.path("pointType").asString())) {
				totalDistanceMeters = properties.path("totalDistance").asInt();
				totalTimeSeconds = properties.path("totalTime").asInt();
			}

			if (GEOMETRY_LINE_STRING.equals(geometry.path("type").asString())) {
				appendLineString(coordinates, geometry.path("coordinates"));
			}
		}

		return new TmapRoute(totalDistanceMeters, totalTimeSeconds, coordinates);
	}

	// 이어지는 두 구간이 접하는 지점의 좌표가 중복으로 들어오므로, 직전 좌표와 같으면 건너뛴다.
	private void appendLineString(List<Coordinate> coordinates, JsonNode lineCoordinates) {
		for (JsonNode point : lineCoordinates) {
			Coordinate coordinate = new Coordinate(point.get(0).asDouble(), point.get(1).asDouble());
			Coordinate last = coordinates.isEmpty() ? null : coordinates.get(coordinates.size() - 1);
			if (!coordinate.equals(last)) {
				coordinates.add(coordinate);
			}
		}
	}

}