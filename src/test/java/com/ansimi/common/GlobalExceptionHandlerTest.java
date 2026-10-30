package com.ansimi.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.ansimi.candidate.RouteProviderException;
import com.ansimi.route.InvalidDistanceException;
import com.ansimi.route.InvalidRequestException;
import com.ansimi.route.OutOfAreaException;

// 개발 명세 4장: 예외마다 code·message·상태코드가 맞는지, 내부 예외 메시지가 응답에 새지 않는지 검증.
class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void 형식_검증_실패는_400_INVALID_REQUEST로_응답한다() {
		MethodArgumentNotValidException e = org.mockito.Mockito.mock(MethodArgumentNotValidException.class);

		ResponseEntity<ErrorResponse> response = handler.handleInvalidRequest(e);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody().code()).isEqualTo("INVALID_REQUEST");
	}

	@Test
	void 좌표_범위_오류는_400_INVALID_REQUEST로_응답한다() {
		InvalidRequestException e = new InvalidRequestException("좌표 범위가 올바르지 않습니다.");

		ResponseEntity<ErrorResponse> response = handler.handleInvalidRequest(e);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isEqualTo(new ErrorResponse("INVALID_REQUEST", "좌표 범위가 올바르지 않습니다."));
	}

	@Test
	void 거리_오류는_400_INVALID_DISTANCE로_응답한다() {
		InvalidDistanceException e = new InvalidDistanceException("거리가 범위를 벗어났습니다.");

		ResponseEntity<ErrorResponse> response = handler.handleInvalidDistance(e);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isEqualTo(new ErrorResponse("INVALID_DISTANCE", "거리가 범위를 벗어났습니다."));
	}

	@Test
	void 지역_밖_오류는_422_OUT_OF_AREA로_응답한다() {
		OutOfAreaException e = new OutOfAreaException("지역 밖입니다.");

		ResponseEntity<ErrorResponse> response = handler.handleOutOfArea(e);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(response.getBody()).isEqualTo(new ErrorResponse("OUT_OF_AREA", "지역 밖입니다."));
	}

	@Test
	void 경로_조회_실패는_502_ROUTE_PROVIDER_ERROR로_응답한다() {
		RouteProviderException e = new RouteProviderException("Tmap 전부 실패");

		ResponseEntity<ErrorResponse> response = handler.handleRouteProviderError(e);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
		assertThat(response.getBody().code()).isEqualTo("ROUTE_PROVIDER_ERROR");
	}

	@Test
	void 예상치_못한_예외는_500_INTERNAL_ERROR로_응답하고_원본_메시지를_노출하지_않는다() {
		RuntimeException e = new RuntimeException("DB 연결 문자열: jdbc://내부비밀정보");

		ResponseEntity<ErrorResponse> response = handler.handleUnexpected(e);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
		assertThat(response.getBody().message()).doesNotContain("내부비밀정보");
	}

}
