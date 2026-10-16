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