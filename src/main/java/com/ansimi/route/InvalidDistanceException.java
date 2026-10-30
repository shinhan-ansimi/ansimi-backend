package com.ansimi.route;

// 출발지-도착지 거리가 허용 범위(기본 50m~5km) 밖일 때 던진다 (개발 명세 4장: INVALID_DISTANCE, 400).
public class InvalidDistanceException extends RuntimeException {

	public InvalidDistanceException(String message) {
		super(message);
	}

}
