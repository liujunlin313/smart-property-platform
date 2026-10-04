# Stage 2.6 Configuration Audit

Audit date: 2026-09-02. This document records configuration names and policies only; no secret values are included.

| Concern | Actual location | Current variable / policy | Why audited | Expected change |
|---|---|---|---|---|
| Runtime log root | `.env.docker`, `compose.yaml` | `PUBLIC_DEMO_LOG_ROOT=./logs`; legacy source copied separately | All new runtime files must remain within PUBLIC_DEMO | Completed; legacy source was retained |
| Backend log path | `compose.yaml`, `backend/src/main/resources/logback-spring.xml` | `APP_LOG_ROOT=/app/logs/${PUBLIC_DEMO_LOG_RUN}` | Every deployment needs a timestamped run directory | Completed |
| Nginx logs | `frontend/nginx.conf.template`, `compose.yaml` | `/var/log/nginx/${NGINX_LOG_RUN}/nginx/{access,error}` | Run-aware JSON logs without credentials | Completed |
| MySQL error / slow logs | `compose.yaml` | `logs/database-runtime/mysql/{error,slow}`; slow threshold stays 0.5s | Preserve data volume while keeping database observable | Completed |
| Observability auto-start | `scripts/observability/*`, Scheduled Task | current-user logon bootstrap, Docker-ready retry, project-wide PID control and active run session | Must retain automatic recovery after log model changes | Completed |
| Deployment ID | `.env.docker`, `scripts/docker/deploy-service.ps1` | `PUBLIC_DEMO_DEPLOYMENT_ID` plus `PUBLIC_DEMO_LOG_RUN` | Deployment needs a distinct log epoch | Completed |
| Listener binding | `compose.yaml` | Web, backend debug, MySQL and MinIO use `127.0.0.1`; Redis internal only | Pre-LAN gate must stay localhost-only | No |
| JWT / database / MinIO / AMap secrets | `.env.docker`, compose environment mapping | variables exist and are injected at runtime | Secrets must never enter audit, logs, tests or client bundle | No value change planned |
| CORS / refresh cookie | `application.yml`, `SecurityConfig` | localhost allow origin; HTTP local refresh cookie policy | Must remain usable on localhost while auditing CSRF/CORS | Keep unless evidence shows a defect |
| Tomcat / Hikari | `application.yml` | application defaults / existing Hikari settings | Baseline required before concurrency tuning | No pre-emptive change |
| MySQL connection limit | MySQL container runtime | image default / current runtime | Baseline required before concurrency tuning | No pre-emptive change |
| Nginx workers / connections | `frontend/nginx.conf`, base image defaults | no application-specific tuning currently | Baseline required before concurrency tuning | No pre-emptive change |
| Flyway / schema | `application.yml`, Compose | existing restored V1-V10 baseline; Docker Flyway disabled | Performance work must not rewrite database | No schema change unless a documented, reproducible defect blocks safety |

## Load and capacity baseline

No Hikari, MySQL or Nginx capacity tuning was applied without evidence. The completed localhost tests are recorded in their run folders:

- System load, 20 virtual users: 100% HTTP success.
- System load, 50 virtual users: 100% HTTP success.
- Database mixed load, 20 virtual users: 100% HTTP success, with protected table counts unchanged.

The frontend production build still emits a chunk-size warning. It is a non-blocking deferred optimization; it did not affect functional or load-test success.

## Change discipline

Every retained Stage 2.6 change will be listed in `STAGE2_6_CHANGE_REPORT.md` with its verification and narrow rollback method. No `down -v`, volume deletion, schema clean, direct final `ALTER`, network exposure, Cloudflare, or WLAN change is in scope.
