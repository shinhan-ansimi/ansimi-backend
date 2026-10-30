package com.ansimi.area;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

@RestController
@RequiredArgsConstructor
public class ServiceAreaController {

	private final ServiceAreaRepository serviceAreaRepository;

	@GetMapping("/api/service-area")
	public ResponseEntity<JsonNode> serviceArea() {
		return ResponseEntity.ok(serviceAreaRepository.findAreaGeoJson());
	}

}
