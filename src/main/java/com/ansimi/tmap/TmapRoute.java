package com.ansimi.tmap;

import java.util.List;

public record TmapRoute(int totalDistanceMeters, int totalTimeSeconds, List<Coordinate> coordinates) {

	public record Coordinate(double lng, double lat) {
	}

}