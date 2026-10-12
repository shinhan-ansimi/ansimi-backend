package com.ansimi.common;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	JdbcTemplate jdbcTemplate;

	@Test
	void DB가_응답하면_UP() throws Exception {
		given(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).willReturn(1);

		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void DB에_연결할_수_없으면_503_DOWN() throws Exception {
		given(jdbcTemplate.queryForObject(anyString(), eq(Integer.class)))
				.willThrow(new DataAccessResourceFailureException("connection refused"));

		mockMvc.perform(get("/api/health"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.status").value("DOWN"));
	}

}
