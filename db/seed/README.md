# 개발용 시드 데이터 (dev_seed.sql)

AI의 실제 위험도 데이터 없이 백엔드 로직(경로 추천, 등급 계산 등)을 로컬에서 검증하기 위한 가짜 데이터다.
`Flyway` 마이그레이션 대상이 아니므로 서버를 띄워도 자동으로 실행되지 않고, 아래 방법으로 수동 실행한다.

내용:
- `service_area` 1행: 강남구 내 임의 좌표 중심 3km x 3km 가짜 시범 지역
- `app_config`의 등급 기준값(`level_yellow_min=60`, `level_red_min=85`)
- `risk_grid` 3,600개(60 x 60, 50m 격자): 사인·코사인 패턴으로 만든 가짜 위험도(GREEN/YELLOW/RED 모두 포함)

여러 번 실행해도 안전하다(`ON CONFLICT`로 덮어씀).

## 실행 방법

저장소 루트에서, 로컬 DB(`infra/docker-compose.yml`)가 떠 있는 상태로 실행한다.

```bash
# macOS/Linux, Git Bash
docker compose --env-file .env -f infra/docker-compose.yml exec -T db psql -U "$DB_USER" -d ansimi < db/seed/dev_seed.sql
```

```powershell
# Windows PowerShell
Get-Content db/seed/dev_seed.sql | docker compose --env-file .env -f infra/docker-compose.yml exec -T db psql -U $env:DB_USER -d ansimi
```

## 삭제 방법

```sql
TRUNCATE risk_grid CASCADE;
DELETE FROM service_area;
```

위 SQL을 같은 방식으로 DB에 실행하면 된다(예: `psql` 접속 후 직접 입력, 또는 `-c` 옵션으로 전달).

# 실제 시범 지역 데이터 (real_service_area.sql)

이슈 #27: `dev_seed.sql`의 가짜 `service_area`(강남구 내 임의 좌표 3km x 3km)를 실제
시범 지역(강남구)의 경계로 교체한다. 위 `dev_seed.sql`과 실행/삭제 방법은 동일하고
(`db/seed/real_service_area.sql`로 경로만 바꿔서), `service_area.id=1` 행만 덮어쓴다.

`risk_grid`는 그대로 두고(이슈 #12에서 `RiskGridImportRunner`로 AI 실데이터를 적재),
`service_area` 경계만 이 스크립트로 먼저 갱신한 뒤 OUT_OF_AREA 경계 안/밖 좌표로
422 응답을 재확인한다.

# 실제 등급 기준값 재조정 (real_level_thresholds.sql)

이슈 #12: 실제 `risk_grid.geojson`을 적재해보니 `risk_v1` 분포가 dev 가짜 데이터보다
훨씬 높은 쪽에 몰려 있어서(평균 84), dev 기준값(60/85)으로는 전체의 55.8%가 RED로
표시됐다. `level_yellow_min=75`, `level_red_min=92`로 재조정하고, 이미 적재된
`risk_grid.level_v1`도 새 기준값으로 다시 계산한다. 실행/삭제 방법은 위와 동일
(`db/seed/real_level_thresholds.sql`).

**주의**: `risk_grid.level_v1`은 적재 시점 기준값으로 미리 계산해서 저장해두는 값이라,
`app_config`만 바꾸면 전체 경로 단위 `riskLevel`(요청마다 새 기준값으로 재계산됨)은
맞지만 세그먼트별 색상(`segments`)과 `redLengthM`은 옛 기준값 그대로 남는다. 기준값을
또 바꾸게 되면 이 스크립트의 `CASE` 조건도 새 값으로 맞춰서 다시 실행해야 한다.