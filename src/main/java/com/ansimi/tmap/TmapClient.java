package com.ansimi.tmap;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TmapClient {

	private static final String PEDESTRIAN_ROUTE_PATH = "/tmap/routes/pedestrian";

	private final WebClient tmapWebClient;
	private final TmapProperties tmapProperties;
	private final TmapResponseParser tmapResponseParser;

	public TmapRoute getPedestrianRoute(PedestrianRouteRequest request) {
		try {
			String responseBody = tmapWebClient.post()
					.uri(uriBuilder -> uriBuilder.path(PEDESTRIAN_ROUTE_PATH).queryParam("version", 1).build())
					.header("appKey", tmapProperties.appKey())
					.contentType(MediaType.APPLICATION_JSON)
					.accept(MediaType.APPLICATION_JSON)
					.bodyValue(toRequestBody(request))
					.retrieve()
					.bodyToMono(String.class)
					.block();

			return tmapResponseParser.parse(responseBody);
		} catch (WebClientResponseException e) {
			throw new TmapClientException("Tmap API가 오류 응답을 반환했습니다: " + e.getStatusCode(),
					e.getStatusCode().value(), e);
		} catch (WebClientException e) {
			throw new TmapClientException("Tmap API 호출에 실패했습니다.", null, e);
		}
	}

	private TmapPedestrianRequestBody toRequestBody(PedestrianRouteRequest request) {
		return new TmapPedestrianRequestBody(
				request.startX(), request.startY(),
				request.endX(), request.endY(),
				encode(request.startName()), encode(request.endName()),
				request.searchOption());
	}

	private String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private record TmapPedestrianRequestBody(
			double startX, double startY,
			double endX, double endY,
			String startName, String endName,
			int searchOption) {
	}

}