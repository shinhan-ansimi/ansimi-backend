package com.ansimi.common;

// 오류 응답 포맷 (개발 명세 4장): {"code", "message"}.
public record ErrorResponse(String code, String message) {

	public static ErrorResponse of(ErrorCode errorCode, String message) {
		return new ErrorResponse(errorCode.name(), message);
	}

	public static ErrorResponse of(ErrorCode errorCode) {
		return of(errorCode, errorCode.defaultMessage());
	}

}
