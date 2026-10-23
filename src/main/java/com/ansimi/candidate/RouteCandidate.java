package com.ansimi.candidate;

import com.ansimi.tmap.TmapRoute;

// source: searchOption 번호("0"·"4"·"10"·"30") 또는 "DETOUR_LEFT"·"DETOUR_RIGHT".
public record RouteCandidate(String source, TmapRoute route) {

	public int distanceMeters() {
		return route.totalDistanceMeters();
	}

}
