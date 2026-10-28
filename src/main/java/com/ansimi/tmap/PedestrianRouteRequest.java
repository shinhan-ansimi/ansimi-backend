package com.ansimi.tmap;

public record PedestrianRouteRequest(
		double startX, double startY,
		double endX, double endY,
		String startName, String endName,
		int searchOption, String passList) {

	// 경유지가 없는 기본 호출용 (기존 호출부와의 호환).
	public PedestrianRouteRequest(double startX, double startY, double endX, double endY,
			String startName, String endName, int searchOption) {
		this(startX, startY, endX, endY, startName, endName, searchOption, null);
	}

}
