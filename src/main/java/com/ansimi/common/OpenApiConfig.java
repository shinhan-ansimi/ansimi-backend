package com.ansimi.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI ansimiOpenApi() {
		return new OpenAPI().info(new Info()
				.title("안심이 백엔드 API")
				.description("위험도 가중치 기반 안전 경로 추천 API")
				.version("v0.0.1"));
	}

}
