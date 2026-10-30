package com.ansimi.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.ansimi.route.dto.CandidateDto;
import com.ansimi.route.dto.Place;
import com.ansimi.route.dto.RecommendRequest;
import com.ansimi.route.dto.RecommendResponse;

import tools.jackson.databind.ObjectMapper;

class RouteRequestLogServiceTest {

	private final RouteRequestLogRepository repository = mock(RouteRequestLogRepository.class);
	private final RouteRequestLogService service = new RouteRequestLogService(repository, new ObjectMapper());

	@Test
	void 요청과_응답을_요약해서_저장소에_넘긴다() {
		RecommendRequest request = new RecommendRequest(
				new Place(List.of(127.0473, 37.5172), "출발"),
				new Place(List.of(127.0573, 37.5232), "도착"));
		CandidateDto candidate = new CandidateDto(
				0, "TMAP_OPTION_10", 1000, 10, 40, "YELLOW", 0, 1.0, true,
				new ObjectMapper().valueToTree(Map.of("type", "FeatureCollection", "features", List.of())));
		RecommendResponse response = new RecommendResponse("v1", 0, List.of(candidate), "고지 문구");

		service.log(request, response, 123);

		ArgumentCaptor<String> candidatesJsonCaptor = ArgumentCaptor.forClass(String.class);
		verify(repository).insert(
				eq(37.5172), eq(127.0473), eq(37.5232), eq(127.0573),
				eq(1), candidatesJsonCaptor.capture(), eq(0), eq("v1"), eq(123L));

		// 거대한 segments(GeoJSON)는 빼고, 요약 필드만 들어있어야 한다.
		String candidatesJson = candidatesJsonCaptor.getValue();
		assertThat(candidatesJson).contains("\"distanceM\":1000", "\"riskScore\":40", "\"riskLevel\":\"YELLOW\"");
		assertThat(candidatesJson).doesNotContain("segments", "FeatureCollection");
	}

}