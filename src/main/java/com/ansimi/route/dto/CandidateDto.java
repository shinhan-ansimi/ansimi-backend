package com.ansimi.route.dto;

import tools.jackson.databind.JsonNode;

public record CandidateDto(
		int index, String source, // 예: "TMAP_OPTION_10", "DETOUR_LEFT"
		int distanceM, int durationMin,
		int riskScore, String riskLevel, int redLengthM,
		double detourRatio, boolean eligible,
		JsonNode segments) { // GeoJSON FeatureCollection
}
