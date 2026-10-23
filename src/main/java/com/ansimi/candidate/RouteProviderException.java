package com.ansimi.candidate;

// Tmap searchOption 4개가 전부 실패했을 때 던진다 (개발 명세 5장: ROUTE_PROVIDER_ERROR, 502).
// 실제 오류 코드 변환은 슬라이스 7(#8)의 GlobalExceptionHandler에서 처리한다.
public class RouteProviderException extends RuntimeException {

	public RouteProviderException(String message) {
		super(message);
	}

}
