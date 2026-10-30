package com.ansimi.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ansimi.candidate.RouteProviderException;
import com.ansimi.route.InvalidDistanceException;
import com.ansimi.route.InvalidRequestException;
import com.ansimi.route.OutOfAreaException;

import lombok.extern.slf4j.Slf4j;

// 모든 예외를 {"code","message"} 형식으로 통일한다 (개발 명세 4장). 스택 트레이스는
// 로그에만 남기고 응답 본문에는 절대 포함하지 않는다.
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleInvalidRequest(MethodArgumentNotValidException e) {
		return respond(ErrorCode.INVALID_REQUEST, ErrorCode.INVALID_REQUEST.defaultMessage(), e);
	}

	@ExceptionHandler(InvalidRequestException.class)
	public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException e) {
		return respond(ErrorCode.INVALID_REQUEST, e.getMessage(), e);
	}

	@ExceptionHandler(InvalidDistanceException.class)
	public ResponseEntity<ErrorResponse> handleInvalidDistance(InvalidDistanceException e) {
		return respond(ErrorCode.INVALID_DISTANCE, e.getMessage(), e);
	}

	@ExceptionHandler(OutOfAreaException.class)
	public ResponseEntity<ErrorResponse> handleOutOfArea(OutOfAreaException e) {
		return respond(ErrorCode.OUT_OF_AREA, e.getMessage(), e);
	}

	@ExceptionHandler(RouteProviderException.class)
	public ResponseEntity<ErrorResponse> handleRouteProviderError(RouteProviderException e) {
		return respond(ErrorCode.ROUTE_PROVIDER_ERROR, ErrorCode.ROUTE_PROVIDER_ERROR.defaultMessage(), e);
	}

	// 예상 못한 예외는 내부 메시지를 절대 응답에 담지 않는다 (명세 4장: 스택 트레이스 비노출).
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
		return respond(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage(), e);
	}

	private ResponseEntity<ErrorResponse> respond(ErrorCode errorCode, String message, Exception e) {
		log.error("[{}] {}", errorCode, e.getMessage(), e);
		return ResponseEntity.status(errorCode.status()).body(ErrorResponse.of(errorCode, message));
	}

}
