#!/usr/bin/env bash
# 안심이 백엔드: 상위 PRD 이슈 + 슬라이스 이슈 11개를 순서대로 등록합니다.
#   미리보기:  DRY_RUN=1 bash scripts/create_issues.sh
#   실제 등록: bash scripts/create_issues.sh
set -euo pipefail

REPO="shinhan-ansimi/ansimi-backend"
SPEC_URL="https://claude.ai/code/artifact/d4b7df5f-1e57-4af7-9596-95fb1fbf63c8"
DRY_RUN="${DRY_RUN:-0}"

command -v gh >/dev/null || { echo "gh CLI가 필요합니다: https://cli.github.com"; exit 1; }
if [[ "$DRY_RUN" != "1" ]]; then gh auth status >/dev/null || { echo "먼저 gh auth login 을 실행하세요"; exit 1; }; fi

declare -A NUM
FAKE=100

# ---------- 공통 함수 ----------
render() {
  local t="$1" i
  t="${t//@@PARENT@@/#${NUM[PRD]:-?}}"
  t="${t//@@SPEC@@/$SPEC_URL}"
  for i in 11 10 9 8 7 6 5 4 3 2 1; do t="${t//@@S$i@@/#${NUM[S$i]:-?}}"; done
  printf '%s' "$t"
}

create_issue() {   # key, title, labels(콤마), body
  local key="$1" title="$2" labels="$3" body url
  body="$(render "$4")"
  if [[ "$DRY_RUN" == "1" ]]; then
    FAKE=$((FAKE + 1)); NUM[$key]=$FAKE
    printf '\n===== [%s] #%s  labels: %s =====\n%s\n' "$key" "$FAKE" "$labels" "$title"
    printf '%s\n' "$body" | head -n 12; echo "..."
    return
  fi
  url=$(gh issue create --repo "$REPO" --title "$title" --label "$labels" --body "$body")
  NUM[$key]="${url##*/}"
  echo "생성: $key -> #${NUM[$key]}  $url"
}

link_sub_issue() { # 슬라이스를 PRD 이슈의 하위 이슈로 연결 (실패해도 계속 진행)
  [[ "$DRY_RUN" == "1" ]] && return
  local id
  id=$(gh api "repos/$REPO/issues/${NUM[$1]}" --jq .id)
  gh api -X POST "repos/$REPO/issues/${NUM[PRD]}/sub_issues" -F sub_issue_id="$id" >/dev/null 2>&1 \
    || echo "  (하위 이슈 연결 생략: #${NUM[$1]})"
}

# ---------- 라벨 ----------
if [[ "$DRY_RUN" != "1" ]]; then
  gh label create "PRD"             --repo "$REPO" --color "5319e7" --description "상위 요구사항 문서" --force
  gh label create "Feature"         --repo "$REPO" --color "1f6feb" --description "기능 슬라이스"      --force
  gh label create "Test"            --repo "$REPO" --color "8a94a6" --description "검증 슬라이스"      --force
  gh label create "blocked"         --repo "$REPO" --color "e4e669" --description "선행 이슈 대기"     --force
  gh label create "ready-for-agent" --repo "$REPO" --color "0e8a16" --description "바로 착수 가능"     --force
fi

# ---------- 상위 PRD ----------
read -r -d '' BODY_PRD <<'EOF' || true
## 개요
출발·도착 좌표를 받아 Tmap 후보 경로를 만들고, 50m 격자 위험도(위험시설·범죄피해발생구간)로 경로별 위험도를 계산해 가장 덜 위험한 경로를 추천하는 API를 만든다.

## 기준 문서
- 백엔드 개발 명세: @@SPEC@@

## 핵심 결정
- 스택: Java 17, Spring Boot 3.x, PostgreSQL 16 + PostGIS 3, Flyway, Caffeine
- 공간 쿼리는 NamedParameterJdbcTemplate로 직접 작성, Hibernate Spatial 미사용
- 좌표는 모든 API에서 [경도, 위도]. DB는 EPSG:5179, API 입출력만 EPSG:4326
- MVP 통신은 REST만 사용 (WebSocket 없음)
- API: POST /api/routes/recommend, GET /api/service-area, GET /api/health

## 슬라이스
슬라이스 1부터 순서대로 진행한다. 각 슬라이스는 이 이슈의 하위 이슈로 연결된다.
EOF
create_issue PRD "PRD: 안심이 백엔드 — 위험도 가중치 기반 안전 경로 추천 API" "PRD" "$BODY_PRD"

# ---------- 슬라이스 1 ----------
read -r -d '' BODY_S1 <<'EOF' || true
## 상위 (Parent)
@@PARENT@@

## 구현 내용 (What to build)
Spring Boot 프로젝트를 만들고(명세 2장 의존성·패키지 구조), 로컬 PostGIS를 docker-compose로 띄운다. Flyway `V1__init.sql`(개발 명세 4장 SQL)로 테이블 7개를 만들고 `GET /api/health`를 구현한다. `application.yml`과 `.env.example`을 명세 3장대로 둔다.

## 완료 시 보이는 것 (Demo)
`docker compose up` 후 서버를 실행하면 `/api/health`가 `{"status":"UP"}`을 돌려주고, DB에 테이블 7개가 생겨 있다.

## 완료 조건 (Acceptance Criteria)
- [ ] `infra/docker-compose.yml`로 postgis/postgis:16-3.4 기동
- [ ] 서버 첫 실행 시 Flyway가 테이블 7개 생성 (`CREATE EXTENSION postgis` 포함)
- [ ] `ddl-auto: validate` 상태로 서버가 정상 기동
- [ ] `GET /api/health` 200 (DB `SELECT 1` 확인 포함)
- [ ] `.env`는 `.gitignore`에 있고 `.env.example`만 커밋

## 차단 관계 (Blocked by)
- 없음

## 관련 명세
- 백엔드 개발 명세 2·3장, 개발 명세 4장

## 사람 판단이 필요한 것 (HITL)
- 없음
EOF
create_issue S1 "[슬라이스 1][BE] 서버가 뜨고 DB 테이블이 만들어진다" "Feature,ready-for-agent" "$BODY_S1"
link_sub_issue S1

# ---------- 슬라이스 2 ----------
read -r -d '' BODY_S2 <<'EOF' || true
## 상위 (Parent)
@@PARENT@@

## 구현 내용 (What to build)
AI의 실제 데이터 없이 개발할 수 있도록 개발용 시드 `db/seed/dev_seed.sql`을 만든다. 가짜 시범 지역(3km × 3km)과 50m 격자 위험도(사인·코사인 줄무늬), app_config 등급 기준값(`level_yellow_min`=60, `level_red_min`=85)을 넣는다. Flyway 대상이 아니며 수동으로 실행한다.

## 완료 시 보이는 것 (Demo)
시드를 한 번 실행하면 risk_grid에 약 3,600개 격자가 생기고, geojson.io나 QGIS에서 초록·노랑·빨강 덩어리가 보인다.

## 완료 조건 (Acceptance Criteria)
- [ ] 시드 1회 실행으로 service_area 1행, risk_grid 격자 생성
- [ ] GREEN·YELLOW·RED 세 등급이 모두 존재
- [ ] 다시 실행해도 오류가 나지 않음 (ON CONFLICT)
- [ ] 실행 방법과 삭제 방법(`TRUNCATE risk_grid CASCADE; DELETE FROM service_area;`)을 README에 기록

## 차단 관계 (Blocked by)
- @@S1@@ (슬라이스 1)

## 관련 명세
- 백엔드 개발 명세 3장(개발용 가짜 데이터), 6.1장(등급 기준값)

## 사람 판단이 필요한 것 (HITL)
- 가짜 지역 중심 좌표를 실제 시범 자치구 안의 지점으로 정한다
EOF
create_issue S2 "[슬라이스 2][BE] 가짜 시범 지역과 위험도가 DB에 들어간다" "Feature,blocked" "$BODY_S2"
link_sub_issue S2

# ---------- 슬라이스 3 ----------
read -r -d '' BODY_S3 <<'EOF' || true
## 상위 (Parent)
@@PARENT@@

## 구현 내용 (What to build)
Tmap 보행자 경로 API 클라이언트를 만든다. `TmapProperties`, `WebClientConfig`, `TmapClient`(명세 5장 요청 형식, 5초 타임아웃), `TmapResponseParser`(LineString 좌표 이어 붙이기, 전체 거리·시간 추출) → `TmapRoute`.

## 완료 시 보이는 것 (Demo)
저장해 둔 실제 응답으로 테스트를 돌리면 경로의 거리(m)·시간(초)·좌표 개수가 출력된다.

## 완료 조건 (Acceptance Criteria)
- [ ] searchOption 0·4·10·30 실제 응답 JSON 4개를 `src/test/resources/tmap/`에 저장
- [ ] 파서 단위 테스트(거리·시간·좌표 개수, 이음 지점 중복 좌표 제거) 통과
- [ ] MockWebServer로 정상·5초 지연(타임아웃)·429·500 응답 처리 테스트
- [ ] 앱키는 환경변수 `TMAP_APP_KEY`로만 주입, 코드·테스트 파일에 키 없음

## 차단 관계 (Blocked by)
- @@S1@@ (슬라이스 1)

## 관련 명세
- 백엔드 개발 명세 5장(요청·응답 파싱), 8장(테스트)

## 사람 판단이 필요한 것 (HITL)
- Tmap 앱키 발급, 필수 파라미터(startName·endName 등)와 searchOption 값을 실제 문서·응답으로 확인
- 일일 무료 사용량과 이용약관(카카오맵 위에 경로 표시 가능 여부) 확인
EOF
create_issue S3 "[슬라이스 3][BE] Tmap에서 경로 하나를 받아온다" "Feature,blocked" "$BODY_S3"
link_sub_issue S3

# ---------- 슬라이스 4 ----------
read -r -d '' BODY_S4 <<'EOF' || true
## 상위 (Parent)
@@PARENT@@

## 구현 내용 (What to build)
경로 하나의 위험도를 계산한다. `RiskScoringRepository`(명세 6.1 SQL, NamedParameterJdbcTemplate), `SegmentMerger`(같은 등급 조각 합치기), `AppConfigService`(등급 기준값 읽기). riskScore(거리 가중평균), redLengthM, riskLevel, segments(GeoJSON FeatureCollection)를 만든다.

## 완료 시 보이는 것 (Demo)
저장해 둔 Tmap 경로로 계산한 segments를 geojson.io에 붙여 넣으면 경로를 따라 색상 구간이 나온다.

## 완료 조건 (Acceptance Criteria)
- [ ] riskScore = Σ(len_m × risk) ÷ Σ len_m, 정수 반올림
- [ ] redLengthM = RED 조각 길이 합
- [ ] 같은 등급이 이어지는 조각이 하나의 Feature로 합쳐짐 (단위 테스트)
- [ ] v1/v2 SQL은 상수 두 개로 두고, 컬럼 이름을 문자열로 이어 붙이지 않음
- [ ] 시드 DB로 통합 테스트 통과

## 차단 관계 (Blocked by)
- @@S2@@ (슬라이스 2)
- @@S3@@ (슬라이스 3)

## 관련 명세
- 백엔드 개발 명세 6.1장, 7장(설정값)

## 사람 판단이 필요한 것 (HITL)
- 계산된 색상 구간이 지도에서 자연스러워 보이는지 눈으로 확인
EOF
create_issue S4 "[슬라이스 4][BE] 경로 하나의 위험도와 색상 구간이 계산된다" "Feature,blocked" "$BODY_S4"
link_sub_issue S4

# ---------- 슬라이스 5 ----------
read -r -d '' BODY_S5 <<'EOF' || true
## 상위 (Parent)
@@PARENT@@

## 구현 내용 (What to build)
후보 경로를 여러 개 만든다. searchOption 4개 병렬 호출(일부 실패 허용, 전부 실패 시 ROUTE_PROVIDER_ERROR), `CandidateDeduplicator`(겹침 90% 이상이면 하나만, 거리 차 15% 이상이면 쿼리 생략), `DetourWaypointGenerator`(후보 2개 미만이면 중간점 좌·우 150m 경유지 대안, 지역 밖 경유지 제외).

## 완료 시 보이는 것 (Demo)
출발·도착 한 쌍으로 후보 목록(source, 거리)이 출력되고, 서로 겹치는 경로가 없다.

## 완료 조건 (Acceptance Criteria)
- [ ] 일부 옵션 실패 시 성공한 후보만 사용
- [ ] 90% 이상 겹치는 경로는 하나만 남음 (짧은 쪽)
- [ ] 후보가 2개 미만이면 DETOUR_LEFT·DETOUR_RIGHT 호출
- [ ] 경유지가 중간점에서 약 150m 떨어져 있음 (단위 테스트)
- [ ] Tmap 429 응답 시 순차 호출로 전환

## 차단 관계 (Blocked by)
- @@S2@@ (슬라이스 2)
- @@S3@@ (슬라이스 3)

## 관련 명세
- 백엔드 개발 명세 5장(병렬 호출·중복 제거·경유지 대안)

## 사람 판단이 필요한 것 (HITL)
- 실제 시범 지역 경로 몇 개로 후보가 의미 있게 다른지 확인
EOF
create_issue S5 "[슬라이스 5][BE] 서로 다른 후보 경로가 2개 이상 나온다" "Feature,blocked" "$BODY_S5"
link_sub_issue S5

# ---------- 슬라이스 6 ----------
read -r -d '' BODY_S6 <<'EOF' || true
## 상위 (Parent)
@@PARENT@@

## 구현 내용 (What to build)
핵심 API `POST /api/routes/recommend`를 완성한다. DTO(명세 4장 record), `RouteController`, `RouteService`(검증 → Tmap 후보 → 중복 제거·경유지 → 스코어링 → 추천 → 응답), `RecommendPolicy`(우회 1.2배, 2점 미만이면 짧은 경로), 고지 문구, CORS 설정.

## 완료 시 보이는 것 (Demo)
curl로 요청하면 개발 명세 5장과 같은 형식의 응답이 오고, recommendedIndex가 추천 규칙대로 정해진다.

## 완료 조건 (Acceptance Criteria)
- [ ] 응답 JSON이 개발 명세 5장과 필드 이름·순서·타입까지 동일
- [ ] RecommendPolicy 단위 테스트 5가지(명세 6.3) 통과
- [ ] @SpringBootTest + MockWebServer 전체 통합 테스트 통과
- [ ] CORS 허용 도메인만 통과
- [ ] 프론트 담당에게 로컬 API 주소와 예시 응답 공유

## 차단 관계 (Blocked by)
- @@S4@@ (슬라이스 4)
- @@S5@@ (슬라이스 5)

## 관련 명세
- 백엔드 개발 명세 4장, 6.2·6.3장, 7장(CORS)

## 사람 판단이 필요한 것 (HITL)
- 응답 형식을 프론트 담당과 최종 확인
EOF
create_issue S6 "[슬라이스 6][BE] 가장 덜 위험한 경로가 추천된다" "Feature,blocked" "$BODY_S6"
link_sub_issue S6

# ---------- 슬라이스 7 ----------
read -r -d '' BODY_S7 <<'EOF' || true
## 상위 (Parent)
@@PARENT@@

## 구현 내용 (What to build)
입력 검증과 오류 응답을 완성한다. `RequestValidator`(형식 → 하버사인 거리 50m~5km → 시범 지역 ST_Contains), `ErrorCode`, `GlobalExceptionHandler`(@RestControllerAdvice), `GET /api/service-area`(EPSG:4326 GeoJSON).

## 완료 시 보이는 것 (Demo)
범위 밖 좌표는 422 `OUT_OF_AREA`, 너무 가깝거나 먼 요청은 400 `INVALID_DISTANCE`가 같은 형식(`{"code","message"}`)으로 나온다.

## 완료 조건 (Acceptance Criteria)
- [ ] 400 INVALID_REQUEST·INVALID_DISTANCE, 422 OUT_OF_AREA, 502 ROUTE_PROVIDER_ERROR, 500 INTERNAL_ERROR가 명세의 message로 응답
- [ ] 응답에 스택 트레이스가 노출되지 않음 (서버 로그에만)
- [ ] `/api/service-area`가 위경도 GeoJSON Feature 반환
- [ ] 검증 단위 테스트(경도·위도 범위, 거리 경계값) 통과

## 차단 관계 (Blocked by)
- @@S6@@ (슬라이스 6)

## 관련 명세
- 백엔드 개발 명세 4장(입력 검증·오류 처리)

## 사람 판단이 필요한 것 (HITL)
- 없음
EOF
create_issue S7 "[슬라이스 7][BE] 잘못된 요청에 알맞은 오류가 나온다" "Feature,blocked" "$BODY_S7"
link_sub_issue S7

# ---------- 슬라이스 8 ----------
read -r -d '' BODY_S8 <<'EOF' || true
## 상위 (Parent)
@@PARENT@@

## 구현 내용 (What to build)
캐시와 요청 로그를 붙인다. Caffeine `Cache` 빈(최대 1,000개, 24시간, 키에 active_model 포함), `@Async` 요청 로그(좌표 소수점 3자리, 후보 요약, elapsed_ms), `@Scheduled` 매일 4시 30일 경과 로그 삭제.

## 완료 시 보이는 것 (Demo)
같은 요청을 두 번 보내면 두 번째는 0.5초 안에 오고, route_request에 두 건이 모두 남는다.

## 완료 조건 (Acceptance Criteria)
- [ ] 같은 요청 두 번째 응답 0.5초 이내
- [ ] 캐시 적중 때도 로그 저장
- [ ] route_request 좌표가 소수점 3자리로만 저장
- [ ] 30일 삭제 쿼리 동작 확인
- [ ] 서버 로그에 원래 좌표와 Tmap 키가 남지 않음

## 차단 관계 (Blocked by)
- @@S6@@ (슬라이스 6)

## 관련 명세
- 백엔드 개발 명세 7장(캐시·요청 로그)

## 사람 판단이 필요한 것 (HITL)
- 없음
EOF
create_issue S8 "[슬라이스 8][BE] 같은 요청은 바로 응답하고 기록이 남는다" "Feature,blocked" "$BODY_S8"
link_sub_issue S8

# ---------- 슬라이스 9 ----------
read -r -d '' BODY_S9 <<'EOF' || true
## 상위 (Parent)
@@PARENT@@

## 구현 내용 (What to build)
`AppConfigService`가 app_config를 60초마다 다시 읽고, `active_model` 값에 따라 v1/v2 SQL을 고른다. 응답의 modelVersion에 반영한다. risk_v2가 비어 있는데 v2가 선택되면 v1으로 응답하고 경고 로그를 남긴다.

## 완료 시 보이는 것 (Demo)
DB에서 active_model을 v2로 바꾸면 재배포 없이 1분 안에 응답의 modelVersion이 v2로 바뀐다.

## 완료 조건 (Acceptance Criteria)
- [ ] app_config 변경이 1분 안에 반영
- [ ] v2 데이터가 없을 때 v1으로 안전하게 동작
- [ ] 캐시 키가 모델별로 분리되어 이전 결과가 섞이지 않음

## 차단 관계 (Blocked by)
- @@S4@@ (슬라이스 4)

## 관련 명세
- 백엔드 개발 명세 7장(설정값)

## 사람 판단이 필요한 것 (HITL)
- v2 채택 여부는 AI 담당의 모델 평가 결과로 결정
EOF
create_issue S9 "[슬라이스 9][BE] 설정만 바꿔 v1·v2 위험도를 전환한다" "Feature,blocked" "$BODY_S9"
link_sub_issue S9

# ---------- 슬라이스 10 ----------
read -r -d '' BODY_S10 <<'EOF' || true
## 상위 (Parent)
@@PARENT@@

## 구현 내용 (What to build)
운영 배포. `backend/Dockerfile`(멀티 스테이지), `infra/docker-compose.prod.yml`(DB·AI 포트 비공개, 백엔드는 127.0.0.1:8080), VM에 Nginx + certbot(HTTPS·자동 갱신), ufw(22·80·443만), 운영 `.env`(Vercel 도메인 CORS), 매일 pg_dump 백업 cron, 이미지 버전 태그.

## 완료 시 보이는 것 (Demo)
휴대폰에서 Vercel 프론트를 열어 경로를 요청하면 HTTPS API가 응답한다.

## 완료 조건 (Acceptance Criteria)
- [ ] `https://api.<도메인>/api/health` 200
- [ ] `certbot renew --dry-run` 성공
- [ ] 외부에서 5432·8000 포트 접속 불가
- [ ] Vercel 도메인에서만 CORS 통과
- [ ] 매일 백업 파일 생성, 이전 이미지 태그로 롤백 가능

## 차단 관계 (Blocked by)
- @@S6@@ (슬라이스 6)

## 관련 명세
- 백엔드 개발 명세 9장(배포)

## 사람 판단이 필요한 것 (HITL)
- 클라우드 VM·도메인 준비 (비용 확인)
- 프론트 담당에게 운영 API 주소 공유
EOF
create_issue S10 "[슬라이스 10][BE] 휴대폰에서 HTTPS로 API가 호출된다" "Feature,blocked" "$BODY_S10"
link_sub_issue S10

# ---------- 슬라이스 11 ----------
read -r -d '' BODY_S11 <<'EOF' || true
## 상위 (Parent)
@@PARENT@@

## 구현 내용 (What to build)
개발용 시드를 지우고 AI 실제 데이터(service_area, risk_grid, 등급 기준값)를 적재한 뒤, 테스트 경로 20개로 운영 환경을 검증한다. 결과를 `docs/test_report.md`에 기록한다.

## 완료 시 보이는 것 (Demo)
실제 시범 지역에서 20개 경로의 추천 결과와 응답 시간이 표로 정리된다.

## 완료 조건 (Acceptance Criteria)
- [ ] 20개 중 18개 이상에서 서로 다른 후보 2개 이상
- [ ] 평균 응답 3초 이내 (route_request.elapsed_ms)
- [ ] 추천 규칙(우회 1.2배 이하, 최저 위험) 위반 0건
- [ ] 백엔드 개발 명세 10장 완료 체크리스트 전부 체크

## 차단 관계 (Blocked by)
- @@S10@@ (슬라이스 10)

## 관련 명세
- 백엔드 개발 명세 1장(개발 순서 11), 10장(완료 체크리스트)

## 사람 판단이 필요한 것 (HITL)
- 일부 경로를 실제로 걸어 보며 색상 구간이 체감과 맞는지 판단
EOF
create_issue S11 "[슬라이스 11][BE] 실제 데이터로 테스트 경로 20개가 검증된다" "Test,blocked" "$BODY_S11"
link_sub_issue S11

echo
echo "완료. PRD: #${NUM[PRD]}  슬라이스: #${NUM[S1]} ~ #${NUM[S11]}"
echo "선행 슬라이스가 닫히면 다음 슬라이스의 라벨을 blocked → ready-for-agent 로 바꿔 주세요."
