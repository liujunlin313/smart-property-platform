# Stage H implementation report

## Archaeology conclusion

- The applied baseline was V6. V7 is the only new migration; V1–V6 were not edited.
- `residence.end_date IS NULL` remains the only current-residence meaning. V7 adds nullable `expected_end_date`; it never repurposes `end_date`.
- `property_unit.version` is retained for the write claim. The claim is a conditional update over the version and `VACANT`/`AVAILABLE_FOR_RENT` state.
- `meter_reading` already had `uk_meter_reading_time (meter_id, reading_at)` and cumulative `reading_value`; it was reused.
- No semantic resident-initiated incident table existed. V7 adds `resident_incident` independently from Meter data.

## Implemented paths

### Role UI

- System administrators retain the existing `AppLayout` and all existing entries.
- Property staff retain that layout, see `当前管理范围` from `/api/admin/access/community-scope`, and receive `办理入住` plus `居民异常` entries.
- `PERSON_USER` is redirected after login to the lightweight `/resident` layout: home, current residences, owned properties, bills, notifications and incident reports.
- Resident endpoints derive identity only from JWT `ActorContext.personId`; the browser supplies no person id.

### Residence write loop

`MoveInView → /api/admin/residence-management → ResidenceManagementService → ResidenceManagementMapper.xml → MySQL`

- Staff create a Person without creating a login account.
- Available properties are selected from scoped, active, no-current-residence units only.
- Owner occupancy requires a real current `ownership` row; ownership is never created automatically.
- `moveIn` is transactional. It conditionally claims the property with version + availability, then inserts a `RESPONSIBLE` Residence with `end_date=NULL`, then commits.
- A stale claim returns `409 PROPERTY_STATE_CHANGED`; this prevents two concurrent staff submissions from both succeeding.
- Move-out only accepts an active tenant-occupied residence. It sets `end_date`, retains history, and returns the property to `AVAILABLE_FOR_RENT` when an open listing exists, otherwise `VACANT`.

### Meter simulator

- Normal scheduling semantics: Electricity 15 minutes, Water 1 hour, Gas 02:00 daily.
- Demo delays are configuration properties: 30/60/120 seconds by default.
- Scheduler is disabled by default (`APP_METER_SIMULATOR_ENABLED=false`); the system-admin-only manual endpoint is `POST /api/admin/meter-simulator/simulate-now`.
- The algorithm uses meter id and time bucket plus time-of-day; it writes cumulative readings and uses batch queries/inserts, not per-meter round trips.

### Resident incident reports

- Resident: `GET/POST /api/resident/me/incidents`; submission is restricted to the actor's current residence.
- Staff/admin: `GET /api/admin/incidents`, `PATCH /api/admin/incidents/{id}/status`.
- Staff listing and updates run through existing `DataScopeService`; only `OPEN → PROCESSING → RESOLVED` updates are accepted by API validation.

## Applied migration

`V7__stage_h_residence_meter_and_incident.sql`

- adds `residence.expected_end_date` with date consistency check;
- creates `resident_incident` and its property/reporter indexes;
- adds `RESIDENT_MANAGE`, `INCIDENT_VIEW`, and `INCIDENT_MANAGE` to property staff; system administrators retain all permissions.

## Verification performed

- V7 registration, new column and new table: PASS.
- Backend compile: PASS.
- Vue `npm run build`: PASS.
- Temporary backend on 8081: PASS; it was stopped after validation. User-owned 8080/5173 were not touched.
- `staff_scope_a` scope API: PASS, one community (`上地西里`).
- Existing owner-resident representative portal summary/residences/properties: PASS.
- Meter manual trigger: requested 1584, first inserted 1584, same time bucket second inserted 0 / already-exists 1584: PASS.
- Resident incident verification record `1`: created and transitioned `OPEN → PROCESSING → RESOLVED`: PASS. It is retained as an explicitly labeled Stage H verification record.
- Concurrent move-in verification: property `452` began at version `0`; `staff_scope_a` received `200`, `staff_scope_c` received `409`, then the winning demo tenant was moved out with `200`. Final database state: `AVAILABLE_FOR_RENT`, version `2`, zero active residences, and the historical residence retained with both `end_date` and `expected_end_date`.

## Stage H verification writes

- 4 clearly named Stage H demo Person rows (two are preparatory validation people; two participated in the final race).
- 1 Residence INSERT and 1 Residence UPDATE for the successful concurrent move-in/retirement cycle.
- 2 PropertyUnit updates for claim/release of property `452`.
- 1,584 MeterReading INSERTs from the first manual simulation; the repeated time bucket inserted zero rows.
- 1 Resident Incident INSERT and 2 status UPDATEs.

## Remaining manual acceptance

- Verify a staff not assigned to an incident's community cannot list or update it, using the browser UI or a representative out-of-scope account.
- Perform a browser-assisted owner self-occupancy flow against a deliberately selected owned vacant property. The backend ownership check is implemented; no automatic Ownership creation exists.

## Deferred by design

- automatic lease expiry/renewal;
- Meter anomaly detection and Meter-to-incident automation;
- Meter-reading-driven Bill generation;
- payment concurrency;
- parking, complex dispatch/SLA, and MinIO attachments.
