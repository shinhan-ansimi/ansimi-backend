package com.ansimi.tmap;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class TmapClient {

	private static final String PEDESTRIAN_ROUTE_PATH = "/tmap/routes/pedestrian";

	private final WebClient tmapWebClient;
	private final TmapProperties tmapProperties;
	private final TmapResponseParser tmapResponseParser;

	public TmapRoute getPedestrianRoute(PedestrianRouteRequest request) {
		return getPedestrianRouteAsync(request).block();
	}

	// 슬라이스5의 searchOption 병렬 호출(Flux.flatMap)에서 쓰기 위한 논블로킹 버전.
	public Mono<TmapRoute> getPedestrianRouteAsync(PedestrianRouteRequest request) {
		return tmapWebClient.post()
				.uri(uriBuilder -> uriBuilder.path(PEDESTRIAN_ROUTE_PATH).queryParam("version", 1).build())
				.header("appKey", tmapProperties.appKey())
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.bodyValue(toRequestBody(request))
				.retrieve()
				.bodyToMono(String.class)
				.map(tmapResponseParser::parse)
				.onErrorMap(WebClientResponseException.class, e -> new TmapClientException(
						"Tmap API가 오류 응답을 반환했습니다: " + e.getStatusCode(), e.getStatusCode().value(), e))
				.onErrorMap(WebClientException.class, e -> new TmapClientException("Tmap API 호출에 실패했습니다.", null, e));
	}

	private Map<String, Object> toRequestBody(PedestrianRouteRequest request) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("startX", request.startX());
		body.put("startY", request.startY());
		body.put("endX", request.endX());
		body.put("endY", request.endY());
		body.put("startName", encode(request.startName()));
		body.put("endName", encode(request.endName()));
		body.put("searchOption", request.searchOption());
		if (request.passList() != null) {
			body.put("passList", request.passList());
		}
		return body;
	}

	private String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

}
