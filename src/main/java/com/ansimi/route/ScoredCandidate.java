package com.ansimi.route;

// RecommendPolicy 입력: 후보 하나의 거리와 위험도만 담는다.
public record ScoredCandidate(int distanceM, int riskScore) {
}
