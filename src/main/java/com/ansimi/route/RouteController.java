package com.ansimi.route;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ansimi.route.dto.RecommendRequest;
import com.ansimi.route.dto.RecommendResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class RouteController {

	private final RouteService routeService;

	@PostMapping("/api/routes/recommend")
	public ResponseEntity<RecommendResponse> recommend(@Valid @RequestBody RecommendRequest request) {
		return ResponseEntity.ok(routeService.recommend(request));
	}

}
