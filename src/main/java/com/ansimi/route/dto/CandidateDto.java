package com.ansimi.route.dto;

import tools.jackson.databind.JsonNode;

public record CandidateDto(
		int index, String source, // 예: "TMAP_OPTION_10", "DETOUR_LEFT"
		int distanceM, int durationMin,
		Integer riskScore, String riskLevel, Integer redLengthM,
		double detourRatio, boolean eligible,
		JsonNode segments, // GeoJSON FeatureCollection
		NavigationDto navigation) {

	public CandidateDto(int index, String source, int distanceM, int durationMin,
			Integer riskScore, String riskLevel, Integer redLengthM, double detourRatio, boolean eligible, JsonNode segments) {
		this(index, source, distanceM, durationMin, riskScore, riskLevel, redLengthM, detourRatio, eligible, segments, null);
	}
}
