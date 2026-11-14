# 운영 배포 가이드 (VM)

개발 명세 9장 기준. `backend`(이 저장소)·`ai`·`infra`가 VM에서 형제 디렉터리로 체크아웃돼 있다고 가정한다
(예: `/srv/ansimi/{backend,ai,infra}`). `infra/docker-compose.prod.yml`·`infra/nginx/ansimi.conf`를 VM의
`infra/` 디렉터리에 둔다.

## 1. VM 준비

```bash
# Docker, Nginx, certbot 설치 후
sudo ufw allow 22,80,443/tcp
sudo ufw enable
```

22(SSH)·80(HTTP)·443(HTTPS)만 열어, DB(5432)·AI(8000) 포트는 외부에 노출하지 않는다
(`docker-compose.prod.yml`에 두 서비스 모두 `ports`가 없어 컨테이너 자체도 내부망 전용이다).

## 2. 도메인 연결

도메인의 `api.` 서브도메인을 VM IP로 연결한다(A 레코드).

## 3. Nginx 설정

```bash
sudo cp infra/nginx/ansimi.conf /etc/nginx/sites-available/ansimi
# server_name의 api.<도메인>을 실제 도메인으로 바꾼 뒤
sudo ln -s /etc/nginx/sites-available/ansimi /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

## 4. HTTPS 발급

```bash
sudo certbot --nginx -d api.<도메인>
```

HTTPS 설정과 자동 갱신 타이머까지 한 번에 끝난다. 아래로 갱신이 정상 동작하는지 확인한다.

```bash
sudo certbot renew --dry-run
```

## 5. 운영 .env 작성 및 기동

`infra/.env`를 작성한다(`docker-compose.prod.yml`이 같은 디렉터리의 `.env`를 읽는다). 로컬 `.env.example`과
같은 키에, `CORS_ALLOWED_ORIGINS`은 Vercel 배포 도메인으로 설정한다.

```bash
cd infra
docker compose -f docker-compose.prod.yml up -d --build
```

## 6. 확인

- `https://api.<도메인>/api/health` → 200
- 외부에서 5432(DB)·8000(AI) 포트 접속 실패
- Vercel 도메인에서만 CORS 통과, 다른 도메인은 차단

## 7. 업데이트·롤백

배포할 때 이미지에 버전 태그를 붙인다(예: `ansimi-backend:v0.3`). 문제가 생기면 이전 태그로 다시 띄운다.

## 8. 백업

매일 새벽 3시 cron으로 DB를 백업한다. `crontab -e`로 추가:

```cron
0 3 * * * docker compose -f /srv/ansimi/infra/docker-compose.prod.yml exec -T db pg_dump -U ansimi ansimi | gzip > /srv/backup/ansimi_$(date +%F).sql.gz
```

`/srv/backup` 디렉터리는 미리 만들어 둔다(`mkdir -p /srv/backup`).
