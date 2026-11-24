package com.ansimi.route.dto;

import java.util.List;

import com.ansimi.tmap.TmapRoute;

public record NavigationDto(List<double[]> path, List<TmapRoute.Step> steps) {

	public static NavigationDto from(TmapRoute route) {
		return new NavigationDto(route.coordinates().stream()
				.map(point -> new double[] { point.lng(), point.lat() })
				.toList(), route.steps());
	}
}
