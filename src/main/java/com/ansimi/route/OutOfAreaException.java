package com.ansimi.route;

// 출발지 또는 도착지가 시범 지역 밖일 때 던진다 (개발 명세 4장: OUT_OF_AREA, 422).
public class OutOfAreaException extends RuntimeException {

	public OutOfAreaException(String message) {
		super(message);
	}

}
