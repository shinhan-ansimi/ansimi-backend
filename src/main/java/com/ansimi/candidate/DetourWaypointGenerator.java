package com.ansimi.candidate;

import org.springframework.stereotype.Component;

// 중복 제거 후 후보가 2개 미만이면 중간점에서 좌·우로 150m 떨어진 경유지를 만든다 (개발 명세 5장).
// 서울 위도 기준 경도 1도 ≈ 88,300m, 위도 1도 ≈ 111,000m로 환산한다.
@Component
public class DetourWaypointGenerator {

	private static final double DETOUR_DISTANCE_M = 150;
	private static final double LNG_METERS_PER_DEGREE = 88_300;
	private static final double LAT_METERS_PER_DEGREE = 111_000;

	public record Waypoint(double lng, double lat) {
	}

	public Waypoint left(double startLng, double startLat, double endLng, double endLat) {
		return offset(startLng, startLat, endLng, endLat, 1);
	}

	public Waypoint right(double startLng, double startLat, double endLng, double endLat) {
		return offset(startLng, startLat, endLng, endLat, -1);
	}

	private Waypoint offset(double startLng, double startLat, double endLng, double endLat, int sign) {
		double midLng = (startLng + endLng) / 2;
		double midLat = (startLat + endLat) / 2;

		double dxM = (endLng - startLng) * LNG_METERS_PER_DEGREE;
		double dyM = (endLat - startLat) * LAT_METERS_PER_DEGREE;
		double lengthM = Math.sqrt(dxM * dxM + dyM * dyM);

		// (dx, dy)를 90도 회전한 단위벡터에 150m를 곱해 수직 방향 경유지를 구한다.
		double perpXM = sign * -dyM / lengthM;
		double perpYM = sign * dxM / lengthM;

		double offsetLng = midLng + (perpXM * DETOUR_DISTANCE_M) / LNG_METERS_PER_DEGREE;
		double offsetLat = midLat + (perpYM * DETOUR_DISTANCE_M) / LAT_METERS_PER_DEGREE;

		return new Waypoint(offsetLng, offsetLat);
	}

}
