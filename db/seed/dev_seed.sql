-- 개발용 가짜 시드 데이터. Flyway 마이그레이션 대상이 아니며 수동으로 실행한다.
-- 실행/삭제 방법: db/seed/README.md 참고.

BEGIN;

-- 가짜 시범 지역: 강남구 내 임의 좌표(위도 37.5172, 경도 127.0473) 중심 3km x 3km
-- 실제 시범 자치구가 정해지면 이 블록의 중심 좌표만 교체하면 된다.
WITH center AS (
  SELECT ST_Transform(ST_SetSRID(ST_MakePoint(127.0473, 37.5172), 4326), 5179) AS pt
)
INSERT INTO service_area (id, name, geom)
SELECT 1,
       '가짜 시범지역(강남구 일부)',
       ST_Multi(ST_MakeEnvelope(ST_X(pt) - 1500, ST_Y(pt) - 1500,
                                 ST_X(pt) + 1500, ST_Y(pt) + 1500, 5179))
FROM center
ON CONFLICT (id) DO UPDATE
  SET name = excluded.name,
      geom = excluded.geom;

-- 등급 기준값 (개발 명세 6.1장): risk >= 60 이면 YELLOW, >= 85 이면 RED
INSERT INTO app_config (key, value) VALUES
  ('level_yellow_min', '60'),
  ('level_red_min', '85')
ON CONFLICT (key) DO UPDATE
  SET value = excluded.value;

-- 50m 격자 3,600개(60 x 60): 사인·코사인 줄무늬로 risk_v1(0~100) 생성
WITH center AS (
  SELECT ST_Transform(ST_SetSRID(ST_MakePoint(127.0473, 37.5172), 4326), 5179) AS pt
),
bounds AS (
  SELECT ST_X(pt) - 1500 AS xmin, ST_Y(pt) - 1500 AS ymin FROM center
),
grid AS (
  SELECT r, c,
         xmin + c * 50 AS cell_xmin,
         ymin + r * 50 AS cell_ymin,
         round((((sin(r * 0.25) + cos(c * 0.25) + 2) / 4) * 100)::numeric, 2) AS risk
  FROM bounds, generate_series(0, 59) AS r, generate_series(0, 59) AS c
)
INSERT INTO risk_grid (grid_id, geom, risk_v1, level_v1)
SELECT
  'r' || r || '_c' || c,
  ST_MakeEnvelope(cell_xmin, cell_ymin, cell_xmin + 50, cell_ymin + 50, 5179),
  risk,
  CASE WHEN risk >= 85 THEN 'RED' WHEN risk >= 60 THEN 'YELLOW' ELSE 'GREEN' END
FROM grid
ON CONFLICT (grid_id) DO UPDATE
  SET geom = excluded.geom,
      risk_v1 = excluded.risk_v1,
      level_v1 = excluded.level_v1,
      updated_at = now();

COMMIT;