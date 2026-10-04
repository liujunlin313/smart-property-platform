# Stage 2.6 change report

## Scope completed

- All newly generated runtime logs now root at `PUBLIC_DEMO/logs`.
- Every observed run has a `run-YYYYMMDD-HHMMSS-reason` directory and `meta.json`.
- Legacy Stage 2.5 logs were copied—not moved or deleted—to `logs/legacy-stage2_5-20260902`.
- Backend, Nginx, audit/security streams and database-operation telemetry now bind directly to one active runtime directory; Docker event/metric collectors follow that same active runtime. Test reports are children of that runtime rather than separate runs.
- The Windows observability agent uses project-wide process-control files, while each worker writes records to the active run. Manual deploy starts a distinct run and reattaches the agent to it.
- Added correlation-only `X-Load-Test-ID`, local rate limiting, two localhost-only real HTTP workload runners and self-contained offline HTML/JSON reports. Human mode pauses before closing; `-NoPause` supports automation.
- Corrected the run-session classifier: a Windows host-session marker now owns the one HOST_BOOT record; bootstrap/reconnect/Docker-ready/collector lifecycle events write to the current run instead of creating another host-boot directory. Agent control state now lives under `logs/_runtime` and stale project-owned legacy workers are reclaimed precisely.

## Explicit non-changes

- No named volume was removed, reinitialized or copied.
- No Docker service, database or frontend port was changed.
- No LAN/WLAN/0.0.0.0 binding, tunnel or cloud deployment was added.
- No application credential, JWT, database password or MinIO secret is written to tracked source or this report.

## Verification record

- Backend image compilation: PASS.
- Frontend production build: PASS (non-blocking chunk-size warning retained as deferred work).
- All five containers: healthy.
- Nginx configuration syntax: PASS.
- `GET /actuator/health` through localhost web entry: 200.
- Unauthenticated admin request: 401.
- Corrected one-user system smoke: 58/58 real localhost HTTP requests succeeded. Its result is under `run-20260902-160451-manual-deploy-web/tests/system-load-20260902-160554`; no run session was created by the test.
- Corrected one-user database smoke: 61/61 real localhost HTTP requests succeeded. Its result is under `run-20260902-160451-manual-deploy-web/tests/database-load-20260902-161054`, with eight telemetry-derived database operation groups, selected MySQL deltas and unchanged protected table counts.
- Final post-deploy redaction smoke: 1 virtual user, 55 real localhost requests, 100% HTTP success. The generated run contained zero literal occurrences of password, password-hash, refresh-token, token-hash, access-token or secret-key field names.
- No named volume deletion, database reset, schema mutation, port or network exposure occurred.
- Run-classification verification: tests did not create a top-level run or change `active-run.json`; actual backend/Nginx/database logs remained beneath the same `manual-deploy-web` runtime. The former top-level load-test directories are retained as historical design-error evidence.
