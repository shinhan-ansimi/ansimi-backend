package com.ansimi.route;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

import com.ansimi.config.AppConfigService;

import lombok.RequiredArgsConstructor;

// 가장 덜 위험한 경로를 추천한다 (개발 명세 6.2장).
@Component
@RequiredArgsConstructor
public class RecommendPolicy {

	private static final int RISK_TIE_THRESHOLD = 2;

	private final AppConfigService appConfigService;

	public RecommendResult recommend(List<ScoredCandidate> candidates) {
		double detourLimit = appConfigService.getDouble("detour_limit");
		int shortest = candidates.stream().mapToInt(ScoredCandidate::distanceM).min().orElseThrow();

		List<Double> detourRatios = candidates.stream()
				.map(c -> (double) c.distanceM() / shortest)
				.toList();
		List<Boolean> eligible = detourRatios.stream()
				.map(ratio -> ratio <= detourLimit)
				.toList();

		List<Integer> eligibleIndices = eligibleIndices(eligible);
		int bestRiskScore = eligibleIndices.stream()
				.mapToInt(i -> candidates.get(i).riskScore())
				.min().orElseThrow();

		// 최단 경로는 항상 detourRatio 1.0이라 eligible이므로 추천 대상이 비는 경우는 없다.
		int recommendedIndex = eligibleIndices.stream()
				.filter(i -> candidates.get(i).riskScore() - bestRiskScore < RISK_TIE_THRESHOLD)
				.min(Comparator.comparingInt(i -> candidates.get(i).distanceM()))
				.orElseThrow();

		return new RecommendResult(detourRatios, eligible, recommendedIndex);
	}

	private List<Integer> eligibleIndices(List<Boolean> eligible) {
		return IntStream.range(0, eligible.size())
				.filter(eligible::get)
				.boxed()
				.toList();
	}

}
