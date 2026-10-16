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

COMMIT;