# Stage 2.6 run-classification review

Date: 2026-09-02. Scope is limited to the PUBLIC_DEMO `logs` run-session classifier. No historical run directory was deleted, renamed or moved.

## Root cause

The earlier `Get-ActivePublicDemoRunSession` treated a missing, unreadable or temporarily unavailable active-state file as a new `HOST_BOOT`. Bootstrap restart, Docker-ready recovery and collector reconnect can all pass through that fallback, although they are lifecycle events rather than new Windows login cycles. Older workers also retained PID files inside historical run directories, allowing a stale worker set to survive the newer global control model.

## Historical review

| Historical directory | Classification after review | Reason |
|---|---|---|
| `run-20260902-114422-host-boot` | Adopted historical baseline | It is the first Stage 2.6 host-boot record. The former metadata predates host-session markers, so an actual Windows logon cannot be proven retrospectively; it is retained as the baseline marker, not asserted as independent boot evidence. |
| `run-20260902-114748-host-boot` | Misclassified | Former trigger: `recovery`. |
| `run-20260902-124209-host-boot` through `run-20260902-133307-host-boot` | Misclassified | Each former trigger is `recovery` and each follows deploy, agent reconnect, Docker readiness or collector lifecycle activity inside the same observed login period. |

The review preserves all eleven historical directories. It does not rewrite their metadata.

## New state machine

1. A host-session marker under `logs/_runtime/host-session.json` derives a stable, non-secret ID from host, current user session and boot time, then stores its one host-boot run.
2. `HOST_BOOT` is created only when that marker identifies a genuinely new host/login session. The first migration adopts the earliest historical baseline rather than creating a duplicate.
3. `AGENT_START`, `AGENT_RECONNECT`, `DOCKER_READY`, Docker connection loss/restoration, collector start/restart and health transitions are lifecycle records under the current run's `ops/lifecycle`; they never create a run.
4. Only true runtime lifecycle creators make normal runs: `HOST_BOOT`, `deploy-service.ps1` manual deployments and a verified unexpected container recovery. Current-run state is held in `logs/_runtime/active-run.json`.
5. System and database tests are evidence, not lifecycle events. They must write only below the already active runtime as `tests/system-load-*` or `tests/database-load-*`; they never update the active-run pointer or redirect live backend, nginx or database logs.
6. A container-restart run name is reserved for a future classifier that has verified unexpected die/start evidence outside a manual-deployment window. Normal Docker event collection never guesses this classification.

## Controlled verification

- Reattached scheduled task in the same Windows session: zero new runs and zero new host-boot runs.
- Reconnected both collectors: zero new runs; lifecycle/event records remained in the active run.
- Reused-image backend deploy: exactly one `run-20260902-143619-manual-deploy-backend`, zero host-boot runs.
- The historical `run-20260902-144854-load-test-system` and `run-20260902-145749-load-test-database` are retained unchanged as evidence of the former, incorrect design: tests previously created top-level runs. They are not models for new output.
- The corrected smoke contract is: one existing active runtime remains active before and after each test, while test-only JSON/HTML evidence is written under that runtime's `tests` directory.

## Root layout

New runtime state uses `logs/_runtime`; legacy data remains untouched. Existing historical root directories, including older `agent-control` and database runtime paths, remain in place for traceability and are no longer used for new agent control state.
