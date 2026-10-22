package com.ansimi.tmap;

public record PedestrianRouteRequest(
		double startX, double startY,
		double endX, double endY,
		String startName, String endName,
		int searchOption) {

}
