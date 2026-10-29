package com.ansimi.route;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;

// Tmap은 MockWebServer로 대신하고(사용량 제한 때문에 실제 호출 안 함), DB는 로컬 PostGIS +
// db/seed/dev_seed.sql 시드가 떠 있어야 통과한다 (개발 명세 8장).
@SpringBootTest
class RouteControllerIntegrationTest {

	private static final MockWebServer TMAP_SERVER = new MockWebServer();
	private static final String ALLOWED_ORIGIN = "http://localhost:5173";

	@Autowired
	private WebApplicationContext webApplicationContext;

	private MockMvc mockMvc;

	@BeforeAll
	static void startServer() throws IOException {
		TMAP_SERVER.start();
	}

	@AfterAll
	static void stopServer() throws IOException {
		TMAP_SERVER.close();
	}

	@DynamicPropertySource
	static void overrideProperties(DynamicPropertyRegistry registry) {
		registry.add("app.tmap.base-url", () -> TMAP_SERVER.url("/").toString());
		registry.add("app.cors.allowed-origins", () -> ALLOWED_ORIGIN);
	}

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@Test
	void 추천_API가_명세_형식의_응답을_반환한다() throws Exception {
		enqueueCandidateResponses();

		mockMvc.perform(post("/api/routes/recommend")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.modelVersion").value("v1"))
				.andExpect(jsonPath("$.recommendedIndex").isNumber())
				.andExpect(jsonPath("$.candidates").isArray())
				.andExpect(jsonPath("$.candidates.length()").value(2))
				.andExpect(jsonPath("$.candidates[0].index").value(0))
				.andExpect(jsonPath("$.candidates[0].source").isNotEmpty())
				.andExpect(jsonPath("$.candidates[0].distanceM").isNumber())
				.andExpect(jsonPath("$.candidates[0].durationMin").isNumber())
				.andExpect(jsonPath("$.candidates[0].riskScore").isNumber())
				.andExpect(jsonPath("$.candidates[0].riskLevel").isNotEmpty())
				.andExpect(jsonPath("$.candidates[0].redLengthM").isNumber())
				.andExpect(jsonPath("$.candidates[0].detourRatio").isNumber())
				.andExpect(jsonPath("$.candidates[0].eligible").isBoolean())
				.andExpect(jsonPath("$.candidates[0].segments.type").value("FeatureCollection"))
				.andExpect(jsonPath("$.notice").isNotEmpty());
	}

	@Test
	void 허용된_도메인의_요청에는_CORS_헤더가_붙는다() throws Exception {
		enqueueCandidateResponses();

		mockMvc.perform(post("/api/routes/recommend")
						.header("Origin", ALLOWED_ORIGIN)
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody()))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
	}

	@Test
	void 허용되지_않은_도메인의_요청은_거부된다() throws Exception {
		mockMvc.perform(post("/api/routes/recommend")
						.header("Origin", "http://evil.example.com")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody()))
				.andExpect(status().isForbidden());
	}

	private String requestBody() {
		return """
				{
				  "origin": {"coord": [127.0373, 37.5112], "name": "출발"},
				  "destination": {"coord": [127.0573, 37.5232], "name": "도착"}
				}
				""";
	}

	// searchOption 4개 중 3개는 같은 경로(겹침 100%→중복 제거로 1개만 남음), 1개는 거리 차이가
	// 15% 이상인 다른 경로(쿼리 없이 별개로 유지)라서 최종 후보는 항상 2개로 고정된다.
	private void enqueueCandidateResponses() {
		String routeA = """
				{"type":"FeatureCollection","features":[
				  {"type":"Feature","geometry":{"type":"Point","coordinates":[127.0373,37.5112]},"properties":{"pointType":"SP","totalDistance":2700,"totalTime":2000}},
				  {"type":"Feature","geometry":{"type":"LineString","coordinates":[[127.0373,37.5112],[127.0573,37.5232]]},"properties":{}}
				]}
				""";
		String routeB = """
				{"type":"FeatureCollection","features":[
				  {"type":"Feature","geometry":{"type":"Point","coordinates":[127.0373,37.5112]},"properties":{"pointType":"SP","totalDistance":3200,"totalTime":2400}},
				  {"type":"Feature","geometry":{"type":"LineString","coordinates":[[127.0373,37.5112],[127.0373,37.5232]]},"properties":{}},
				  {"type":"Feature","geometry":{"type":"LineString","coordinates":[[127.0373,37.5232],[127.0573,37.5232]]},"properties":{}}
				]}
				""";

		for (int i = 0; i < 3; i++) {
			TMAP_SERVER.enqueue(new MockResponse.Builder()
					.code(200)
					.addHeader("Content-Type", "application/json")
					.body(routeA)
					.build());
		}
		TMAP_SERVER.enqueue(new MockResponse.Builder()
				.code(200)
				.addHeader("Content-Type", "application/json")
				.body(routeB)
				.build());
	}

}
