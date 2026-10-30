package com.ansimi.route;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.anyDouble;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ansimi.area.ServiceAreaRepository;
import com.ansimi.route.dto.Place;
import com.ansimi.route.dto.RecommendRequest;

// 개발 명세 4장: 좌표 범위 → 하버사인 거리(50m~5km) → 시범 지역 포함 여부 검증.
class RequestValidatorTest {

	private static final double EARTH_RADIUS_M = 6_371_000;
	private static final double BASE_LNG = 127.0;
	private static final double BASE_LAT = 37.5;

	private ServiceAreaRepository serviceAreaRepository;
	private RequestValidator validator;

	@BeforeEach
	void setUp() {
		serviceAreaRepository = mock(ServiceAreaRepository.class);
		given(serviceAreaRepository.contains(anyDouble(), anyDouble())).willReturn(true);
		validator = new RequestValidator(new RouteProperties(50, 5000), serviceAreaRepository);
	}

	@Test
	void 정상_요청이면_예외가_발생하지_않는다() {
		RecommendRequest request = requestWithDistance(1000);

		assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
	}

	@Test
	void 경도가_범위를_벗어나면_InvalidRequestException이_발생한다() {
		RecommendRequest request = new RecommendRequest(
				new Place(List.of(181.0, BASE_LAT), "출발"),
				new Place(List.of(BASE_LNG, BASE_LAT), "도착"));

		assertThatThrownBy(() -> validator.validate(request)).isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void 위도가_범위를_벗어나면_InvalidRequestException이_발생한다() {
		RecommendRequest request = new RecommendRequest(
				new Place(List.of(BASE_LNG, 91.0), "출발"),
				new Place(List.of(BASE_LNG, BASE_LAT), "도착"));

		assertThatThrownBy(() -> validator.validate(request)).isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void 거리가_최소값_50m이면_통과한다() {
		RecommendRequest request = requestWithDistance(50);

		assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
	}

	@Test
	void 거리가_최소값보다_작으면_InvalidDistanceException이_발생한다() {
		RecommendRequest request = requestWithDistance(49);

		assertThatThrownBy(() -> validator.validate(request)).isInstanceOf(InvalidDistanceException.class);
	}

	@Test
	void 거리가_최대값_5000m이면_통과한다() {
		RecommendRequest request = requestWithDistance(5000);

		assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
	}

	@Test
	void 거리가_최대값보다_크면_InvalidDistanceException이_발생한다() {
		RecommendRequest request = requestWithDistance(5001);

		assertThatThrownBy(() -> validator.validate(request)).isInstanceOf(InvalidDistanceException.class);
	}

	@Test
	void 출발지가_시범_지역_밖이면_OutOfAreaException이_발생한다() {
		RecommendRequest request = requestWithDistance(1000);
		given(serviceAreaRepository.contains(request.origin().coord().get(0), request.origin().coord().get(1)))
				.willReturn(false);

		assertThatThrownBy(() -> validator.validate(request)).isInstanceOf(OutOfAreaException.class);
	}

	@Test
	void 도착지가_시범_지역_밖이면_OutOfAreaException이_발생한다() {
		RecommendRequest request = requestWithDistance(1000);
		given(serviceAreaRepository.contains(request.destination().coord().get(0), request.destination().coord().get(1)))
				.willReturn(false);

		assertThatThrownBy(() -> validator.validate(request)).isInstanceOf(OutOfAreaException.class);
	}

	// 같은 경도에서 위도만 바꾸면 하버사인 거리 = 위도차(라디안) * 지구 반지름이 정확히 맞아떨어진다.
	private RecommendRequest requestWithDistance(double distanceM) {
		double deltaLatDeg = Math.toDegrees(distanceM / EARTH_RADIUS_M);
		return new RecommendRequest(
				new Place(List.of(BASE_LNG, BASE_LAT), "출발"),
				new Place(List.of(BASE_LNG, BASE_LAT + deltaLatDeg), "도착"));
	}

}
