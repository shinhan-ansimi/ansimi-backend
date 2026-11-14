package com.ansimi.risk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ansimi.config.AppConfigService;
import com.ansimi.tmap.TmapRoute.Coordinate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// 경로 하나의 위험도를 계산한다 (개발 명세 6.1장): riskScore(거리 가중평균), redLengthM,
// riskLevel, segments(GeoJSON FeatureCollection)를 만든다.
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskScoringService {

	private static final String RED_LEVEL = "RED";

	private final RiskScoringRepository riskScoringRepository;
	private final SegmentMerger segmentMerger;
	private final AppConfigService appConfigService;
	private final ObjectMapper objectMapper;

	public RouteRiskScore score(List<Coordinate> coordinates) {
		boolean useV2 = resolveUseV2();
		List<RiskGridPiece> pieces = riskScoringRepository.findPieces(toLineStringGeoJson(coordinates), useV2);

		double weightedSum = 0;
		double totalLength = 0;
		double redLengthM = 0;
		for (RiskGridPiece piece : pieces) {
			weightedSum += piece.lengthMeters() * piece.risk();
			totalLength += piece.lengthMeters();
			if (RED_LEVEL.equals(piece.level())) {
				redLengthM += piece.lengthMeters();
			}
		}
		int riskScore = totalLength == 0 ? 0 : (int) Math.round(weightedSum / totalLength);

		List<RouteSegment> segments = segmentMerger.merge(pieces);

		return new RouteRiskScore(riskScore, redLengthM, resolveRiskLevel(riskScore), toFeatureCollection(segments));
	}

	// active_model이 v2여도 risk_v2 데이터가 아직 없으면 v1으로 안전하게 동작한다 (개발 명세 7장).
	private boolean resolveUseV2() {
		boolean useV2 = "v2".equals(appConfigService.getString("active_model"));
		if (useV2 && !riskScoringRepository.hasV2Data()) {
			log.warn("active_model=v2이지만 risk_v2 데이터가 없어 v1으로 대체합니다.");
			return false;
		}
		return useV2;
	}

	private String resolveRiskLevel(int riskScore) {
		if (riskScore >= appConfigService.getDouble("level_red_min")) {
			return RED_LEVEL;
		}
		if (riskScore >= appConfigService.getDouble("level_yellow_min")) {
			return "YELLOW";
		}
		return "GREEN";
	}

	private String toLineStringGeoJson(List<Coordinate> coordinates) {
		List<double[]> points = coordinates.stream()
				.map(c -> new double[] { c.lng(), c.lat() })
				.toList();
		return objectMapper.writeValueAsString(Map.of("type", "LineString", "coordinates", points));
	}

	private JsonNode toFeatureCollection(List<RouteSegment> segments) {
		List<Map<String, Object>> features = new ArrayList<>();
		for (RouteSegment segment : segments) {
			features.add(Map.of(
					"type", "Feature",
					"properties", Map.of("level", segment.level(), "lengthM", segment.lengthMeters()),
					"geometry", Map.of("type", "MultiLineString", "coordinates", segment.lineStrings())));
		}
		return objectMapper.valueToTree(Map.of("type", "FeatureCollection", "features", features));
	}

}
