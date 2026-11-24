package com.ansimi.tmap;

import java.util.List;

public record TmapRoute(int totalDistanceMeters, int totalTimeSeconds, List<Coordinate> coordinates, List<Step> steps) {

	public TmapRoute(int totalDistanceMeters, int totalTimeSeconds, List<Coordinate> coordinates) {
		this(totalDistanceMeters, totalTimeSeconds, coordinates, List.of());
	}

	public record Step(int pathIndex, String instruction) {
	}

	public record Coordinate(double lng, double lat) {
	}

}