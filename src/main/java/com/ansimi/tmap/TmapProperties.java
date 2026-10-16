package com.ansimi.tmap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tmap")
public record TmapProperties(String baseUrl, String appKey, long timeoutMs) {

}
