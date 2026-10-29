package com.ansimi.route.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record RecommendRequest(
		@Valid @NotNull Place origin,
		@Valid @NotNull Place destination) {
}
