package com.ansimi.route;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.route")
public record RouteProperties(int minDistanceM, int maxDistanceM) {
}
