package com.ansimi.area;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import tools.jackson.databind.JsonNode;

// db/seed/dev_seed.sql로 시드된 가짜 시범지역(강남구 일부)이 로컬에 떠 있어야 통과한다.
@SpringBootTest
class ServiceAreaRepositoryIntegrationTest {

	@Autowired
	private ServiceAreaRepository serviceAreaRepository;

	@Test
	void 시범_지역_경계를_위경도_GeoJSON_Feature로_반환한다() {
		JsonNode feature = serviceAreaRepository.findAreaGeoJson();

		assertThat(feature.path("type").asString()).isEqualTo("Feature");
		assertThat(feature.path("geometry").path("type").asString()).isEqualTo("MultiPolygon");
		assertThat(feature.path("geometry").path("coordinates").size()).isGreaterThan(0);
		assertThat(feature.path("properties").path("name").asString()).isNotBlank();
	}

	@Test
	void 지역_중심_좌표는_지역_안에_있다() {
		boolean contains = serviceAreaRepository.contains(127.0473, 37.5172);

		assertThat(contains).isTrue();
	}

	@Test
	void 지역에서_멀리_떨어진_좌표는_지역_밖이다() {
		boolean contains = serviceAreaRepository.contains(129.0756, 35.1796);

		assertThat(contains).isFalse();
	}

}
