package com.ansimi.tmap;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import reactor.netty.http.client.HttpClient;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

	private final TmapProperties tmapProperties;

	@Bean
	public WebClient tmapWebClient() {
		HttpClient httpClient = HttpClient.create()
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) tmapProperties.timeoutMs())
				.responseTimeout(Duration.ofMillis(tmapProperties.timeoutMs()));

		return WebClient.builder()
				.baseUrl(tmapProperties.baseUrl())
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.build();
	}

}