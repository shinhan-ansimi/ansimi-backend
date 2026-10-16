package com.ansimi.tmap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.StreamUtils;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import tools.jackson.databind.ObjectMapper;

class TmapClientTest {

	private static final PedestrianRouteRequest SAMPLE_REQUEST = new PedestrianRouteRequest(
			126.9779451, 37.5663223, 126.9768983, 37.5759880, "서울시청", "광화문", 0);

	private MockWebServer server;
	private TmapClient client;

	@BeforeEach
	void setUp() throws IOException {
		server = new MockWebServer();
		server.start();

		TmapProperties properties = new TmapProperties(server.url("/").toString(), "test-app-key", 300);
		client = new TmapClient(new WebClientConfig(properties).tmapWebClient(), properties,
				new TmapResponseParser(new ObjectMapper()));
	}

	@AfterEach
	void tearDown() throws IOException {
		server.close();
	}

	@Test
	void 정상_응답이면_경로를_반환한다() throws Exception {
		server.enqueue(new MockResponse.Builder()
				.code(200)
				.addHeader("Content-Type", "application/json")
				.body(readFixture("pedestrian_route_searchOption_0.json"))
				.build());

		TmapRoute route = client.getPedestrianRoute(SAMPLE_REQUEST);

		assertThat(route.totalDistanceMeters()).isEqualTo(1200);
		assertThat(route.totalTimeSeconds()).isEqualTo(1014);

		RecordedRequest recorded = server.takeRequest();
		assertThat(recorded.getTarget()).startsWith("/tmap/routes/pedestrian?version=1");
		assertThat(recorded.getHeaders().get("appKey")).isEqualTo("test-app-key");
	}

	@Test
	void 응답이_설정된_타임아웃보다_늦으면_예외가_발생한다() {
		server.enqueue(new MockResponse.Builder()
				.code(200)
				.headersDelay(1, TimeUnit.SECONDS)
				.body("{}")
				.build());

		assertThatThrownBy(() -> client.getPedestrianRoute(SAMPLE_REQUEST))
				.isInstanceOf(TmapClientException.class)
				.extracting(e -> ((TmapClientException) e).statusCode())
				.isNull();
	}

	@Test
	void 상태코드_429_응답이면_예외가_발생한다() {
		server.enqueue(new MockResponse.Builder()
				.code(429)
				.addHeader("Content-Type", "application/json")
				.body("{\"error\":{\"id\":\"429\",\"category\":\"gw\",\"code\":\"TOO_MANY_REQUESTS\",\"message\":\"Too Many Requests\"}}")
				.build());

		assertThatThrownBy(() -> client.getPedestrianRoute(SAMPLE_REQUEST))
				.isInstanceOf(TmapClientException.class)
				.extracting(e -> ((TmapClientException) e).statusCode())
				.isEqualTo(429);
	}

	@Test
	void 상태코드_500_응답이면_예외가_발생한다() {
		server.enqueue(new MockResponse.Builder()
				.code(500)
				.addHeader("Content-Type", "application/json")
				.body("{\"error\":{\"id\":\"500\",\"category\":\"gw\",\"code\":\"INTERNAL_ERROR\",\"message\":\"Internal Server Error\"}}")
				.build());

		assertThatThrownBy(() -> client.getPedestrianRoute(SAMPLE_REQUEST))
				.isInstanceOf(TmapClientException.class)
				.extracting(e -> ((TmapClientException) e).statusCode())
				.isEqualTo(500);
	}

	private String readFixture(String fileName) throws IOException {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("tmap/" + fileName)) {
			return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
		}
	}

}
