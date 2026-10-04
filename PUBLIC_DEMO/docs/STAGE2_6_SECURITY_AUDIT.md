# Stage 2.6 security audit

Audit scope: localhost-only PUBLIC_DEMO runtime, 2026-09-02. This audit does not expose or reproduce credentials, tokens, cookies, or object-store keys.

## Confirmed controls

| Area | Result | Evidence |
|---|---|---|
| Identity trust | PASS | JWT subject and role claims are converted by Spring Security; services obtain the actor through `ActorContextProvider`, not from request body fields. |
| Authentication / JWT | PASS | HMAC key requires at least 32 decoded bytes; issuer and audience validators are active; access token lifetime is short; refresh sessions are server-side. |
| Admin data scope | PASS (sampled) | Admin property, resident, dashboard, notification, map and incident services invoke `AuthorizationService` and `DataScopeService`; property details call `requireProperty`. |
| Resident IDOR | PASS (sampled) | Resident portal derives `personId` from the authenticated actor and checks current residence before incident creation. |
| SQL injection | PASS | Mapper XML audit found no `${...}` substitutions; mapper parameters use MyBatis bindings. |
| XSS sink scan | PASS | No `v-html`, `innerHTML`, `outerHTML`, or `insertAdjacentHTML` use was found under `frontend/src`. |
| Request validation | PASS (sampled) | Command payloads use `@Valid`; global handler returns safe 400 responses; page size is normalized and capped in sampled paged services. |
| Error sanitization | PASS | Global handler emits stable codes/user messages and trace IDs; server configuration does not include message or stack trace in responses. |
| File endpoint boundary | PASS (sampled) | Floor plan endpoints stay under `/api/v1/**`, which requires SYSTEM_ADMIN; upload size is bounded by servlet settings. |
| CORS / CSRF | PASS for stated deployment | CORS uses the explicit configured localhost origin and credentials; bearer-token API is stateless. The refresh cookie is locally scoped and no cross-site public deployment is enabled in this stage. |
| Secrets | PASS | Runtime secrets are read from ignored local environment files; no audit/report/log value includes password, token, cookie or object-store secret. |
| Container network | PASS | Service ports remain loopback-bound; no LAN or public bind was added. |

## Hardening applied in this stage

1. Added local, category-aware rate limiting after request correlation and before the security chain. It uses the transport peer address only; forwarded headers are not trusted as a client identity.
2. Added `X-Load-Test-ID` as a strict-format, correlation-only MDC field. It is never read by authentication, authorization or data-scope code.
3. Added the header to the explicit CORS allow-list so same-origin/local test tooling is not blocked when a browser preflight is involved.
4. Flow SQL logging now redacts sensitive storage-column labels before a compact statement is recorded. A post-deploy localhost smoke run confirmed no password, token or secret field labels in its generated backend/audit/security logs. Immutable legacy logs were retained separately and were not rewritten.

## Deferred to a future public-internet phase

- Distributed rate limits shared across replicas.
- Public TLS, secure refresh cookies and Internet-facing CSRF review.
- Object-store presigned URL policy review under a public hostname.
- Dependency/SBOM and container image vulnerability scanning.
- Full endpoint-by-endpoint authorization test suite for every future controller.
