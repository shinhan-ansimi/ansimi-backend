package com.ansimi.area;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ServiceAreaController.class)
class ServiceAreaControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ServiceAreaRepository serviceAreaRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void 시범_지역_GeoJSON_Feature를_반환한다() throws Exception {
		given(serviceAreaRepository.findAreaGeoJson()).willReturn(objectMapper.valueToTree(Map.of(
				"type", "Feature",
				"geometry", Map.of("type", "MultiPolygon", "coordinates", java.util.List.of()),
				"properties", Map.of("name", "가짜 시범지역(강남구 일부)"))));

		mockMvc.perform(get("/api/service-area"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.type").value("Feature"))
				.andExpect(jsonPath("$.geometry.type").value("MultiPolygon"))
				.andExpect(jsonPath("$.properties.name").value("가짜 시범지역(강남구 일부)"));
	}

}
