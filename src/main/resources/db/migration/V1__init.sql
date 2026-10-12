CREATE EXTENSION IF NOT EXISTS postgis;

-- 시범 지역 경계 (범위 밖 요청 차단용)
CREATE TABLE service_area (
  id    SMALLINT PRIMARY KEY,
  name  VARCHAR(50) NOT NULL,
  geom  geometry(MultiPolygon, 5179) NOT NULL
);

-- 50m 격자와 위험도 (AI 배치가 쓰고, 백엔드가 읽음)
CREATE TABLE risk_grid (
  grid_id      VARCHAR(20) PRIMARY KEY,          -- 예: r123_c456
  geom         geometry(Polygon, 5179) NOT NULL,
  hazard_score REAL     NOT NULL DEFAULT 0,      -- H, 0~1
  crime_grade  SMALLINT NOT NULL DEFAULT 0,      -- 0 = 범죄구간 아님
  risk_v1      REAL     NOT NULL DEFAULT 0,      -- 0~100
  level_v1     VARCHAR(6) NOT NULL DEFAULT 'GREEN',
  risk_v2      REAL,                             -- v2 적용 전 NULL
  level_v2     VARCHAR(6),
  updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_risk_grid_geom ON risk_grid USING GIST (geom);

-- v2 학습용 피처 (범죄구간 관련 값은 넣지 않음)
CREATE TABLE grid_feature (
  grid_id   VARCHAR(20) PRIMARY KEY REFERENCES risk_grid(grid_id),
  features  JSONB NOT NULL                       -- {"bar_100m": 3, "lamp_100m": 12, ...}
);

CREATE TABLE hazard_facility (
  facility_id BIGSERIAL PRIMARY KEY,
  type        VARCHAR(30) NOT NULL,              -- 유흥주점, 단란주점 등
  name        VARCHAR(100),
  geom        geometry(Point, 5179) NOT NULL,
  source      VARCHAR(50) NOT NULL
);
CREATE INDEX idx_hazard_geom ON hazard_facility USING GIST (geom);

CREATE TABLE crime_zone (
  zone_id    BIGSERIAL PRIMARY KEY,
  grade      SMALLINT NOT NULL,
  geom       geometry(MultiPolygon, 5179) NOT NULL,
  source     VARCHAR(50),
  base_date  DATE
);
CREATE INDEX idx_crime_geom ON crime_zone USING GIST (geom);

-- 추천 요청 로그 (좌표는 소수점 3자리 ≈ 100m로 반올림, 30일 후 삭제)
CREATE TABLE route_request (
  request_id      BIGSERIAL PRIMARY KEY,
  origin_lat      NUMERIC(6,3), origin_lng NUMERIC(7,3),
  dest_lat        NUMERIC(6,3), dest_lng   NUMERIC(7,3),
  candidate_count SMALLINT,
  candidates      JSONB,                         -- 후보별 거리·위험도 요약
  chosen_index    SMALLINT,
  model_ver       VARCHAR(10),
  elapsed_ms      INT,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 설정값 (재배포 없이 v1/v2 전환)
CREATE TABLE app_config (
  key    VARCHAR(50) PRIMARY KEY,
  value  VARCHAR(100) NOT NULL
);
-- level_*_min: 경로 riskScore를 등급으로 나누는 경계 점수 (AI가 위험도 적재 때 갱신)
INSERT INTO app_config VALUES
  ('active_model', 'v1'),
  ('detour_limit', '1.2'),
  ('level_yellow_min', '60'),
  ('level_red_min', '85');
