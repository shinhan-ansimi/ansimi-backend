package com.ansimi.route;

// 좌표 형식·범위가 올바르지 않을 때 던진다 (개발 명세 4장: INVALID_REQUEST, 400).
public class InvalidRequestException extends RuntimeException {

	public InvalidRequestException(String message) {
		super(message);
	}

}
