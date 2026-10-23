package com.ansimi.risk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class SegmentMergerTest {

	private final SegmentMerger merger = new SegmentMerger();

	@Test
	void 같은_등급이_이어지는_조각은_하나의_세그먼트로_합쳐진다() {
		RiskGridPiece green1 = piece("GREEN", 10, 0, 0, 10, 0);
		RiskGridPiece green2 = piece("GREEN", 15, 10, 0, 20, 0);
		RiskGridPiece yellow = piece("YELLOW", 20, 20, 0, 30, 0);

		List<RouteSegment> segments = merger.merge(List.of(green1, green2, yellow));

		assertThat(segments).hasSize(2);

		RouteSegment first = segments.get(0);
		assertThat(first.level()).isEqualTo("GREEN");
		assertThat(first.lengthMeters()).isEqualTo(25);
		assertThat(first.lineStrings()).hasSize(2);

		RouteSegment second = segments.get(1);
		assertThat(second.level()).isEqualTo("YELLOW");
		assertThat(second.lengthMeters()).isEqualTo(20);
		assertThat(second.lineStrings()).hasSize(1);
	}

	@Test
	void 등급이_바뀌면_각각_다른_세그먼트가_된다() {
		RiskGridPiece green = piece("GREEN", 10, 0, 0, 10, 0);
		RiskGridPiece yellow = piece("YELLOW", 20, 10, 0, 20, 0);
		RiskGridPiece green2 = piece("GREEN", 10, 20, 0, 30, 0);

		List<RouteSegment> segments = merger.merge(List.of(green, yellow, green2));

		assertThat(segments).extracting(RouteSegment::level)
				.containsExactly("GREEN", "YELLOW", "GREEN");
	}

	@Test
	void 빈_목록이면_빈_세그먼트_목록을_반환한다() {
		assertThat(merger.merge(List.of())).isEmpty();
	}

	private RiskGridPiece piece(String level, double lengthMeters, double x1, double y1, double x2, double y2) {
		return new RiskGridPiece(0, level, lengthMeters,
				List.of(List.of(new double[] { x1, y1 }, new double[] { x2, y2 })));
	}

}
