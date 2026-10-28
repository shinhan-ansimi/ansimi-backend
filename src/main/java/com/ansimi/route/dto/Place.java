package com.ansimi.route.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record Place(
		@NotNull @Size(min = 2, max = 2) List<Double> coord, // [경도, 위도]
		String name) {
}
