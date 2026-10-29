package com.ansimi.route.dto;

import java.util.List;

public record RecommendResponse(
		String modelVersion, // "v1" | "v2"
		int recommendedIndex,
		List<CandidateDto> candidates,
		String notice) {
}
