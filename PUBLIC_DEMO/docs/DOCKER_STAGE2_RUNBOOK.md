# Stage 2: Five-container local system

## Architecture

```text
Browser (127.0.0.1:18080)
  -> web (Nginx + Vue production build)
     -> /api/* -> backend:8080 (Spring Boot)
                    -> mysql:3306 / redis:6379 / minio:9000
```

Services share `smart_property_public_demo_network`. Persistent data remains only in the Stage 1 named volumes:

| Service | Host access | Persistent volume |
| --- | --- | --- |
| web | `127.0.0.1:18080` | none |
| backend | `127.0.0.1:18081` debug only | none |
| mysql | `127.0.0.1:33307` debug only | `smart_property_public_demo_mysql_data` at `/var/lib/mysql` |
| redis | internal only | intentionally rebuildable cache |
| minio | `127.0.0.1:19000` API, `19001` Console | `smart_property_public_demo_minio_data` at `/data` |

## Local configuration

Copy `.env.docker.example` to the ignored `.env.docker`. Keep the existing Stage 1 MySQL and MinIO credentials unchanged. Add a stable `APP_JWT_SECRET_BASE64` (at least 32 random bytes after Base64 decoding) and the existing AMap credentials. Never commit or print this file.

The Docker backend uses `mysql`, `redis`, and `minio` service DNS names, binds `0.0.0.0:8080` only inside Docker, and sets `APP_DEV_BOOTSTRAP=false`. It does not read Windows PowerShell scripts or local runtime data. The application uses `smart_property_app`, not MySQL root.

## Build and run

```powershell
docker compose --env-file .env.docker config
docker compose --env-file .env.docker build backend web
docker compose --env-file .env.docker up -d
docker compose --env-file .env.docker ps
```

Check logs when a service is unhealthy:

```powershell
docker compose --env-file .env.docker logs --tail 200 backend
docker compose --env-file .env.docker logs --tail 200 web
```

The health URLs are `http://127.0.0.1:18081/actuator/health` and `http://127.0.0.1:18080/healthz`. Use the web entry `http://127.0.0.1:18080/`; Nginx sends `/api/` to backend and uses `try_files $uri $uri/ /index.html` for Vue history routes.

## Cookie, CORS, upload, and Flyway

Stage 2 is localhost HTTP, so refresh cookies use `Secure=false`; Stage 3 HTTPS must set `APP_REFRESH_COOKIE_SECURE=true`. `HttpOnly`, SameSite and the existing refresh path remain application-controlled. Browser API calls are same-origin through Nginx; CORS is explicitly restricted to the local web URL and never wildcarded. Nginx permits 16 MB requests while Spring Boot limits multipart requests to 12 MB.

Stage 1 restored a populated database with Flyway V1–V10. Stage 2 therefore sets `APP_FLYWAY_ENABLED=false` for its first runtime verification: no clean, baseline, seed, or migration replay is allowed. Re-evaluate only after an explicit migration-history validation.

## Updating one component

```powershell
# after Java changes
docker compose --env-file .env.docker build backend
docker compose --env-file .env.docker up -d backend

# after Vue/Nginx changes
docker compose --env-file .env.docker build web
docker compose --env-file .env.docker up -d web
```

Source code, images, containers, and named data volumes have separate lifecycles. Rebuilding either application does not recreate MySQL or MinIO data.

## Persistence, rollback, and LAN preparation

For a normal restart only:

```powershell
docker compose --env-file .env.docker down
docker compose --env-file .env.docker up -d
```

Never use `docker compose down -v`, `docker volume rm`, or `docker system prune --volumes`; they can destroy the verified Stage 1 data.

For a temporary LAN test, change only `WEB_BIND_ADDRESS=0.0.0.0`, recreate `web`, and access `http://<Windows-LAN-IPv4>:18080`. Keep backend, MySQL, Redis, and MinIO loopback/internal. Restore `WEB_BIND_ADDRESS=127.0.0.1` afterwards. Stage 3 starts only after this local verification and is the sole future boundary for cloudflared/Quick Tunnel and HTTPS.
