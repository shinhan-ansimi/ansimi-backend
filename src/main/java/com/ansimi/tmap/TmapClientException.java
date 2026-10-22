package com.ansimi.tmap;

public class TmapClientException extends RuntimeException {

	// Tmap이 오류 상태코드로 응답한 경우에만 채워진다. 타임아웃 등 네트워크 수준 실패는 null.
	private final Integer statusCode;

	public TmapClientException(String message, Integer statusCode, Throwable cause) {
		super(message, cause);
		this.statusCode = statusCode;
	}

	public Integer statusCode() {
		return statusCode;
	}

}
