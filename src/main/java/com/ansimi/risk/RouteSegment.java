package com.ansimi.risk;

import java.util.List;

// SegmentMerger가 같은 level의 연속된 조각들을 합친 결과.
public record RouteSegment(String level, double lengthMeters, List<List<double[]>> lineStrings) {
}
