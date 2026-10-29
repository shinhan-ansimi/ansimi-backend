package com.ansimi.route;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ansimi.config.AppConfigService;

// 개발 명세 6.3장 "꼭 넣을 단위 테스트" 5가지.
class RecommendPolicyTest {

	private AppConfigService appConfigService;
	private RecommendPolicy policy;

	@BeforeEach
	void setUp() {
		appConfigService = mock(AppConfigService.class);
		given(appConfigService.getDouble("detour_limit")).willReturn(1.2);
		policy = new RecommendPolicy(appConfigService);
	}

	@Test
	void 최단_경로가_가장_위험도도_낮으면_최단_경로를_추천한다() {
		ScoredCandidate shortest = new ScoredCandidate(1000, 30);
		ScoredCandidate other = new ScoredCandidate(1200, 50);

		RecommendResult result = policy.recommend(List.of(shortest, other));

		assertThat(result.recommendedIndex()).isEqualTo(0);
	}

	@Test
	void 우회_1_15배_경로가_위험도_20점_낮으면_우회_경로를_추천한다() {
		ScoredCandidate shortest = new ScoredCandidate(1000, 50);
		ScoredCandidate detour = new ScoredCandidate(1150, 30);

		RecommendResult result = policy.recommend(List.of(shortest, detour));

		assertThat(result.eligible()).containsExactly(true, true);
		assertThat(result.recommendedIndex()).isEqualTo(1);
	}

	@Test
	void 우회_1_3배_경로는_eligible_false이고_최단_경로를_추천한다() {
		ScoredCandidate shortest = new ScoredCandidate(1000, 50);
		ScoredCandidate detour = new ScoredCandidate(1300, 20);

		RecommendResult result = policy.recommend(List.of(shortest, detour));

		assertThat(result.eligible()).containsExactly(true, false);
		assertThat(result.recommendedIndex()).isEqualTo(0);
	}

	@Test
	void 두_후보_위험도_차이가_1점이면_더_짧은_경로를_추천한다() {
		// 더 짧은 쪽(index 0)이 위험도는 1점 더 높아도 짧은 쪽이 추천돼야 한다.
		ScoredCandidate shorterSlightlyRiskier = new ScoredCandidate(1000, 52);
		ScoredCandidate longerSlightlyLessRisky = new ScoredCandidate(1100, 51);

		RecommendResult result = policy.recommend(List.of(shorterSlightlyRiskier, longerSlightlyLessRisky));

		assertThat(result.recommendedIndex()).isEqualTo(0);
	}

	@Test
	void 후보가_1개면_그_경로를_추천한다() {
		ScoredCandidate only = new ScoredCandidate(1000, 40);

		RecommendResult result = policy.recommend(List.of(only));

		assertThat(result.eligible()).containsExactly(true);
		assertThat(result.recommendedIndex()).isEqualTo(0);
	}

}
