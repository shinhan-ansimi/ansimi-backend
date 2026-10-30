package com.ansimi.common;

import org.springframework.http.HttpStatus;

// 오류 코드와 기본 응답 상태코드·문구 (개발 명세 4장).
public enum ErrorCode {

	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다."),
	INVALID_DISTANCE(HttpStatus.BAD_REQUEST, "출발지와 도착지 사이 거리가 허용 범위를 벗어났습니다."),
	OUT_OF_AREA(HttpStatus.UNPROCESSABLE_CONTENT, "출발지 또는 도착지가 시범 지역 밖에 있습니다."),
	ROUTE_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "경로 조회 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 문제가 발생했습니다.");

	private final HttpStatus status;
	private final String defaultMessage;

	ErrorCode(HttpStatus status, String defaultMessage) {
		this.status = status;
		this.defaultMessage = defaultMessage;
	}

	public HttpStatus status() {
		return status;
	}

	public String defaultMessage() {
		return defaultMessage;
	}

}
