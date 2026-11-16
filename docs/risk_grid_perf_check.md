# risk_grid 15만 행 규모 성능 점검 (이슈 #25)

AI 실데이터(`risk_grid.geojson`)를 아직 못 받은 상태라, `ansimi-ai` 파이프라인을 mock으로
직접 돌려 나온 **실제 서울 전역 규모(100m 격자, 159,253행)** 데이터를 로컬 DB에 적재해두고
측정했다. 값은 가짜지만 row 수·공간 분포는 운영 규모와 동일해서, 쿼리 성능 특성은 그대로
참고할 수 있다.

## 1. GIST 인덱스 사용 확인

`RiskScoringRepository`의 v1 쿼리(개발 명세 6.1장)를 `EXPLAIN (ANALYZE, BUFFERS)`로 떠봤다.

```sql
EXPLAIN (ANALYZE, BUFFERS)
WITH route AS (
  SELECT ST_Transform(ST_SetSRID(ST_GeomFromGeoJSON('{...LineString...}'), 4326), 5179) AS g
)
SELECT rg.risk_v1 AS risk, rg.level_v1 AS level,
       ST_Length(ST_Intersection(rg.geom, r.g)) AS len_m
FROM risk_grid rg, route r
WHERE ST_Intersects(rg.geom, r.g);
```

결과 (일부):
```
Bitmap Heap Scan on risk_grid rg  (actual time=3.010..3.192 rows=12 loops=1)
  ->  Bitmap Index Scan on idx_risk_grid_geom  (actual time=1.009..1.010 rows=120 loops=1)
Execution Time: 3.548 ms
```

159,253행 중에서도 `idx_risk_grid_geom`(GIST) Bitmap Index Scan을 타고, 실행 시간은
**3.5ms**다. Seq Scan으로 안 빠지는 것을 확인했다 — 별도 조치(`ANALYZE` 등) 불필요.

## 2. API 응답 시간 (좌표 10쌍)

가짜 시범 지역(강남, 중심 127.0473/37.5172, ±1.5km) 안에서 거리 700m~1.9km로 다양하게
10쌍을 뽑아 `/api/routes/recommend`를 실제로 호출했다(Tmap 실호출 포함). `route_request`에
기록된 `elapsed_ms`(서버 내부 처리 시간, 네트워크 왕복 제외) 기준:

| 요청 | 거리(대략) | candidate_count | elapsed_ms |
|---|---|---|---|
| 1 | 875m | 2 | 1072 |
| 2 | 1296m | 3 | 379 |
| 3 | 896m | 2 | 144 |
| 4 | 1753m | 2 | 139 |
| 5 | 1439m | 2 | 187 |
| 6 | 702m | 2 | 124 |
| 7 | 1865m | 3 | 529 |
| 8 | 1310m | 3 | 600 |
| 9 | 1323m | 3 | 429 |
| 10 | 1037m | 3 | 146 |

- 평균 **374ms**, 최대 **1072ms**(첫 요청, JIT·커넥션 warmup 포함) — 슬라이스 11 완료 조건
  (평균 응답 3초 이내)에 여유 있게 들어온다.
- 10건 모두 200 OK, candidate 2~3개 정상 생성.

## 결론

15만 행 규모에서 인덱스가 정상적으로 타고, 응답 시간도 기준치의 1/8 수준이라 **추가 조치
없이 실데이터를 받아도 성능 문제는 없을 것으로 판단**된다. 실데이터가 오면 값만 바뀌는
거라 이 결과는 그대로 참고 가능하다.
