-- 안심이 ERD (ERDCloud 붙여넣기용)
-- docs/erd/ansimi_erdcloud.sql: ERD클라우드에 붙여넣어서 ERD 그림을 그리는 용도. 실제 DB에서 실행하면 안 됨.
-- 원본: PRD 개발 명세 4장 V1__init.sql (PostgreSQL 18 + PostGIS)
-- ERDCloud 파서가 읽을 수 있게 PostGIS 전용 문법을 단순화함. 실제 마이그레이션에는 쓰지 말 것.
--   geometry(MultiPolygon, 5179) -> GEOMETRY  (공간 타입·좌표계는 COMMENT에 표기)
--   BIGSERIAL -> BIGINT AUTO_INCREMENT, TIMESTAMPTZ -> TIMESTAMP, JSONB -> JSON
--   CREATE EXTENSION, GIST 인덱스, 초기 INSERT 제외

CREATE TABLE service_area (
  id    SMALLINT    NOT NULL COMMENT '시범 지역 ID',
  name  VARCHAR(50) NOT NULL COMMENT '지역 이름',
  geom  GEOMETRY    NOT NULL COMMENT '경계 MultiPolygon, EPSG:5179',
  PRIMARY KEY (id)
) COMMENT '시범 지역 경계';

CREATE TABLE risk_grid (
  grid_id      VARCHAR(20) NOT NULL COMMENT '격자 ID (r{행}_c{열})',
  geom         GEOMETRY    NOT NULL COMMENT '50m 격자 Polygon, EPSG:5179',
  hazard_score REAL        NOT NULL DEFAULT 0 COMMENT '위험시설 점수 H (0~1)',
  crime_grade  SMALLINT    NOT NULL DEFAULT 0 COMMENT '범죄피해 등급 (0=해당 없음)',
  risk_v1      REAL        NOT NULL DEFAULT 0 COMMENT 'v1 위험도 (0~100)',
  level_v1     VARCHAR(6)  NOT NULL DEFAULT 'GREEN' COMMENT 'v1 등급 GREEN/YELLOW/RED',
  risk_v2      REAL        NULL COMMENT 'v2 위험도 (적용 전 NULL)',
  level_v2     VARCHAR(6)  NULL COMMENT 'v2 등급',
  updated_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '갱신 시각',
  PRIMARY KEY (grid_id)
) COMMENT '50m 격자 위험도';

CREATE TABLE grid_feature (
  grid_id   VARCHAR(20) NOT NULL COMMENT '격자 ID',
  features  JSON        NOT NULL COMMENT 'v2 학습 피처 (범죄구간 값 제외)',
  PRIMARY KEY (grid_id),
  CONSTRAINT fk_grid_feature_risk_grid FOREIGN KEY (grid_id) REFERENCES risk_grid (grid_id)
) COMMENT '격자 피처 (v2 학습용)';

CREATE TABLE hazard_facility (
  facility_id BIGINT       NOT NULL AUTO_INCREMENT COMMENT '시설 ID',
  type        VARCHAR(30)  NOT NULL COMMENT '시설 종류 (유흥주점 등)',
  name        VARCHAR(100) NULL COMMENT '시설 이름',
  geom        GEOMETRY     NOT NULL COMMENT '위치 Point, EPSG:5179',
  source      VARCHAR(50)  NOT NULL COMMENT '데이터 출처',
  PRIMARY KEY (facility_id)
) COMMENT '위험시설';

CREATE TABLE crime_zone (
  zone_id    BIGINT      NOT NULL AUTO_INCREMENT COMMENT '구간 ID',
  grade      SMALLINT    NOT NULL COMMENT '범죄피해 등급',
  geom       GEOMETRY    NOT NULL COMMENT '구간 MultiPolygon, EPSG:5179',
  source     VARCHAR(50) NULL COMMENT '데이터 출처',
  base_date  DATE        NULL COMMENT '기준일',
  PRIMARY KEY (zone_id)
) COMMENT '범죄피해발생구간';

CREATE TABLE route_request (
  request_id      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '요청 ID',
  origin_lat      NUMERIC(6,3) NULL COMMENT '출발 위도 (소수점 3자리)',
  origin_lng      NUMERIC(7,3) NULL COMMENT '출발 경도 (소수점 3자리)',
  dest_lat        NUMERIC(6,3) NULL COMMENT '도착 위도 (소수점 3자리)',
  dest_lng        NUMERIC(7,3) NULL COMMENT '도착 경도 (소수점 3자리)',
  candidate_count SMALLINT     NULL COMMENT '후보 경로 수',
  candidates      JSON         NULL COMMENT '후보별 거리·위험도·eligible 요약',
  chosen_index    SMALLINT     NULL COMMENT '추천 후보 index',
  model_ver       VARCHAR(10)  NULL COMMENT '위험도 모델 (v1/v2)',
  elapsed_ms      INT          NULL COMMENT '처리 시간 (ms)',
  created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '요청 시각 (30일 후 삭제)',
  PRIMARY KEY (request_id)
) COMMENT '경로 추천 요청 로그';

CREATE TABLE app_config (
  `key`  VARCHAR(50)  NOT NULL COMMENT '설정 키',
  value  VARCHAR(100) NOT NULL COMMENT '설정 값',
  PRIMARY KEY (`key`)
) COMMENT '설정값 (active_model, detour_limit 등)';
