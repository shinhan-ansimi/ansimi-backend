-- 실데이터 기준 등급 기준값(level_yellow_min/level_red_min) 재조정. Flyway 마이그레이션
-- 대상이 아니며 수동으로 실행한다. 실행/삭제 방법: db/seed/README.md 참고.
--
-- 이슈 #12: 실제 risk_grid.geojson을 적재해보니 risk_v1(=100-safety_score) 분포가
-- 대부분 높은 쪽(평균 84, 중앙값 85.7)에 몰려 있어서, dev 가짜 데이터용으로 잡아둔
-- 기준값(60/85)으로는 전체의 55.8%가 RED로 표시됐다. 실데이터 분포를 보고
-- level_yellow_min=75, level_red_min=92로 재조정했다(GREEN 17.2% / YELLOW 62.6% /
-- RED 20.2%). 추천 로직(RecommendPolicy)은 연속값 riskScore로만 판단해서 이 값을
-- 바꿔도 추천 결과 자체는 바뀌지 않는다 — 사용자에게 보여주는 색상 라벨만 바뀐다.
--
-- 주의: risk_grid.level_v1 칼럼은 적재 시점 기준값으로 미리 계산해서 저장해두는
-- 값이라(RiskGridImportService), app_config만 바꾸면 세그먼트 색상(segments)과
-- redLengthM이 옛 기준값 그대로 남는다. 그래서 risk_grid.level_v1도 같이
-- 재계산한다. 기준값을 또 바꾸면 이 스크립트의 CASE 조건도 같이 맞춰서 재실행해야
-- 한다.

BEGIN;

UPDATE app_config SET value = '75' WHERE key = 'level_yellow_min';
UPDATE app_config SET value = '92' WHERE key = 'level_red_min';

UPDATE risk_grid
SET level_v1 = CASE
                  WHEN risk_v1 >= 92 THEN 'RED'
                  WHEN risk_v1 >= 75 THEN 'YELLOW'
                  ELSE 'GREEN'
                END,
    updated_at = now();

COMMIT;
