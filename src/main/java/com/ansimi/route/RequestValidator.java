package com.ansimi.route;

import org.springframework.stereotype.Component;

import com.ansimi.area.ServiceAreaRepository;
import com.ansimi.route.dto.Place;
import com.ansimi.route.dto.RecommendRequest;

import lombok.RequiredArgsConstructor;

// 추천 요청을 검증한다 (개발 명세 4장): 좌표 범위 → 하버사인 거리 → 시범 지역 포함 여부.
@Component
@RequiredArgsConstructor
public class RequestValidator {

	private static final double EARTH_RADIUS_M = 6_371_000;
	private static final double MIN_LNG = -180;
	private static final double MAX_LNG = 180;
	private static final double MIN_LAT = -90;
	private static final double MAX_LAT = 90;

	private final RouteProperties routeProperties;
	private final ServiceAreaRepository serviceAreaRepository;

	public void validate(RecommendRequest request) {
		validateCoordinateRange(request.origin());
		validateCoordinateRange(request.destination());

		// 미터 단위로 반올림해서 비교한다 — 부동소수점 오차로 경계값(50m·5000m)이 어긋나지 않게.
		long distanceM = Math.round(haversineDistanceM(request.origin(), request.destination()));
		if (distanceM < routeProperties.minDistanceM() || distanceM > routeProperties.maxDistanceM()) {
			throw new InvalidDistanceException(
					"출발지-도착지 거리가 %dm로, 허용 범위(%d~%dm)를 벗어났습니다."
							.formatted(distanceM, routeProperties.minDistanceM(), routeProperties.maxDistanceM()));
		}

		if (!isInServiceArea(request.origin()) || !isInServiceArea(request.destination())) {
			throw new OutOfAreaException("출발지 또는 도착지가 시범 지역 밖에 있습니다.");
		}
	}

	private void validateCoordinateRange(Place place) {
		double lng = place.coord().get(0);
		double lat = place.coord().get(1);
		if (lng < MIN_LNG || lng > MAX_LNG || lat < MIN_LAT || lat > MAX_LAT) {
			throw new InvalidRequestException(
					"좌표 범위가 올바르지 않습니다 (경도 %d~%d, 위도 %d~%d).".formatted(
							(int) MIN_LNG, (int) MAX_LNG, (int) MIN_LAT, (int) MAX_LAT));
		}
	}

	private boolean isInServiceArea(Place place) {
		return serviceAreaRepository.contains(place.coord().get(0), place.coord().get(1));
	}

	// 두 좌표 사이 거리(m)를 구면 거리(하버사인 공식)로 계산한다.
	private double haversineDistanceM(Place origin, Place destination) {
		double lat1 = origin.coord().get(1);
		double lng1 = origin.coord().get(0);
		double lat2 = destination.coord().get(1);
		double lng2 = destination.coord().get(0);

		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lng2 - lng1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
						* Math.sin(dLng / 2) * Math.sin(dLng / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return EARTH_RADIUS_M * c;
	}

}
