package com.ansimi.risk;

import java.util.List;

// RiskScoringRepository 쿼리 결과 한 행. lineStrings는 piece(GeoJSON LineString/MultiLineString)를
// 좌표([lng, lat]) 목록들로 정규화한 것 — MultiLineString이면 요소가 둘 이상이다.
public record RiskGridPiece(double risk, String level, double lengthMeters, List<List<double[]>> lineStrings) {
}
