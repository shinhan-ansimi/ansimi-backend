package com.ansimi.route.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record RecommendRequest(
		@Valid @NotNull Place origin,
		@Valid @NotNull Place destination,
		Boolean allowUnassessed) {

	public RecommendRequest {
		allowUnassessed = Boolean.TRUE.equals(allowUnassessed);
	}

	public RecommendRequest(Place origin, Place destination) {
		this(origin, destination, false);
	}
}
