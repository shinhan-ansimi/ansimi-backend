package com.ansimi.candidate;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import com.ansimi.candidate.DetourWaypointGenerator.Waypoint;

class DetourWaypointGeneratorTest {

	private static final double LNG_M = 88_300;
	private static final double LAT_M = 111_000;

	private final DetourWaypointGenerator generator = new DetourWaypointGenerator();

	@Test
	void 좌측_경유지는_중간점에서_약_150m_떨어져_있다() {
		Waypoint left = generator.left(127.0473, 37.5172, 127.0573, 37.5172);

		double midLng = (127.0473 + 127.0573) / 2;
		double midLat = 37.5172;

		assertThat(distanceMeters(midLng, midLat, left.lng(), left.lat())).isCloseTo(150, Offset.offset(1.0));
	}

	@Test
	void 우측_경유지도_중간점에서_약_150m_떨어져_있다() {
		Waypoint right = generator.right(127.0473, 37.5172, 127.0573, 37.5172);

		double midLng = (127.0473 + 127.0573) / 2;
		double midLat = 37.5172;

		assertThat(distanceMeters(midLng, midLat, right.lng(), right.lat())).isCloseTo(150, Offset.offset(1.0));
	}

	@Test
	void 좌측과_우측_경유지는_중간점을_기준으로_반대편에_있다() {
		Waypoint left = generator.left(127.0473, 37.5172, 127.0573, 37.5172);
		Waypoint right = generator.right(127.0473, 37.5172, 127.0573, 37.5172);

		assertThat(left.lat()).isGreaterThan(37.5172);
		assertThat(right.lat()).isLessThan(37.5172);
		assertThat(left.lng()).isCloseTo(right.lng(), Offset.offset(0.000001));
	}

	private double distanceMeters(double lng1, double lat1, double lng2, double lat2) {
		double dx = (lng2 - lng1) * LNG_M;
		double dy = (lat2 - lat1) * LAT_M;
		return Math.sqrt(dx * dx + dy * dy);
	}

}
