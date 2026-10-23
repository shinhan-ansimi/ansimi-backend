package com.ansimi.candidate;

public record RouteRequest(
		double startLng, double startLat,
		double endLng, double endLat,
		String startName, String endName) {
}
