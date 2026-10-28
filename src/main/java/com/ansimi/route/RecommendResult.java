package com.ansimi.route;

import java.util.List;

// 입력 후보 순서와 같은 순서의 detourRatio·eligible, 그리고 최종 추천 인덱스.
public record RecommendResult(List<Double> detourRatios, List<Boolean> eligible, int recommendedIndex) {
}
