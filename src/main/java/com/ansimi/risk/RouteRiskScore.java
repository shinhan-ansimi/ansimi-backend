package com.ansimi.risk;

import tools.jackson.databind.JsonNode;

// 경로 하나의 위험도 계산 결과 (개발 명세 6.1장). segments는 GeoJSON FeatureCollection.
public record RouteRiskScore(int riskScore, double redLengthM, String riskLevel, JsonNode segments) {
}
