-- 실제 시범 지역(강남구) service_area 반영. Flyway 마이그레이션 대상이 아니며 수동으로 실행한다.
-- 실행/삭제 방법: db/seed/README.md 참고.
--
-- 이슈 #27: risk_grid.geojson의 gu 라벨이 좌표와 어긋나 있어(예: 강남구로 표시된 행의
-- bbox가 서울 전역을 덮음) 신뢰할 수 없다. 대신 강남구의 공개된 대략적인 행정 경계
-- bbox(위경도)로 사각형(envelope)을 만들었다 — dev_seed.sql과 같은 방식이고, 정확한
-- 행정동 경계 polygon은 아니다. 더 정밀한 경계가 필요해지면 이 블록만 교체하면 된다.

BEGIN;

WITH bbox AS (
  SELECT ST_Transform(
           ST_SetSRID(ST_MakeEnvelope(127.0176, 37.4647, 127.1066, 37.5349, 4326), 4326),
           5179
         ) AS geom
)
INSERT INTO service_area (id, name, geom)
SELECT 1,
       '실제 시범지역(강남구)',
       ST_Multi(geom)
FROM bbox
ON CONFLICT (id) DO UPDATE
  SET name = excluded.name,
      geom = excluded.geom;

COMMIT;
