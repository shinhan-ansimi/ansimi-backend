package com.ansimi.tmap;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.util.StreamUtils;

import tools.jackson.databind.ObjectMapper;

class TmapResponseParserTest {

	private final TmapResponseParser parser = new TmapResponseParser(new ObjectMapper());

	// searchOption별 실제 응답(src/test/resources/tmap)에서 확인한 거리(m)·시간(초)·중복 제거 후 좌표 개수
	static Stream<Arguments> fixtures() {
		return Stream.of(
				Arguments.of("pedestrian_route_searchOption_0.json", 1200, 1014, 58),
				Arguments.of("pedestrian_route_searchOption_4.json", 1200, 1014, 58),
				Arguments.of("pedestrian_route_searchOption_10.json", 1196, 1018, 57),
				Arguments.of("pedestrian_route_searchOption_30.json", 1200, 1014, 58));
	}

	@ParameterizedTest
	@MethodSource("fixtures")
	void 실제_응답을_거리_시간_좌표로_파싱한다(String fixtureFile, int expectedDistance, int expectedTime, int expectedCoordinateCount)
			throws IOException {
		String responseBody = readFixture(fixtureFile);

		TmapRoute route = parser.parse(responseBody);

		assertThat(route.totalDistanceMeters()).isEqualTo(expectedDistance);
		assertThat(route.totalTimeSeconds()).isEqualTo(expectedTime);
		assertThat(route.coordinates()).hasSize(expectedCoordinateCount);
	}

	@Test
	void 구간이_이어지는_지점의_중복_좌표는_하나만_남는다() throws IOException {
		String responseBody = readFixture("pedestrian_route_searchOption_0.json");

		TmapRoute route = parser.parse(responseBody);

		for (int i = 1; i < route.coordinates().size(); i++) {
			assertThat(route.coordinates().get(i)).isNotEqualTo(route.coordinates().get(i - 1));
		}
	}

	private String readFixture(String fileName) throws IOException {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("tmap/" + fileName)) {
			return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
		}
	}

}
