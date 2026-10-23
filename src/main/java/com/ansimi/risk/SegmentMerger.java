package com.ansimi.risk;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

// 경로를 따라 순서대로 들어온 조각 중, 같은 등급(level)이 이어지는 조각을 하나의 세그먼트로 합친다.
@Component
public class SegmentMerger {

	public List<RouteSegment> merge(List<RiskGridPiece> pieces) {
		List<RouteSegment> segments = new ArrayList<>();
		String currentLevel = null;
		double currentLength = 0;
		List<List<double[]>> currentLines = new ArrayList<>();

		for (RiskGridPiece piece : pieces) {
			if (!piece.level().equals(currentLevel)) {
				if (currentLevel != null) {
					segments.add(new RouteSegment(currentLevel, currentLength, currentLines));
				}
				currentLevel = piece.level();
				currentLength = 0;
				currentLines = new ArrayList<>();
			}
			currentLength += piece.lengthMeters();
			currentLines.addAll(piece.lineStrings());
		}
		if (currentLevel != null) {
			segments.add(new RouteSegment(currentLevel, currentLength, currentLines));
		}
		return segments;
	}

}
