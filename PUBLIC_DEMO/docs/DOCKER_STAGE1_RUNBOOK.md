# Docker Stage 1 Runbook — Data Infrastructure and Migration

> Scope: PUBLIC_DEMO only. This runbook creates and migrates **only** isolated Docker MySQL, Redis, and MinIO data services. It does not create Spring Boot, Vue, Nginx, or cloudflared containers.

## 1. Architecture

```text
                    Docker bridge: smart_property_public_demo_network

  127.0.0.1:33307 ──> mysql:3306 ──> public_demo_mysql_data
  (debug only)

                         redis:6379
                         (internal only; no persistent business data)

  127.0.0.1:19000 ──> minio:9000 ──> public_demo_minio_data
  127.0.0.1:19001 ──> minio:9001
  (debug only)

  Stage 2 backend will use: mysql:3306, redis:6379, minio:9000
```

The original Windows MySQL/Redis/MinIO services are not mounted, stopped, reset, or otherwise changed by this design. The debug ports are loopback-only and do not use the original 3306, 6379, 9000, or 9001 ports.

## 2. Service and Volume Inventory

| Compose service | Purpose | Internal hostname / port | Host debug port | Persistent storage |
|---|---|---|---|---|
| `mysql` | Independent MySQL 8.0.43 database restored from the full demo snapshot | `mysql:3306` | `127.0.0.1:33307` | `smart_property_public_demo_mysql_data` |
| `redis` | Empty, rebuildable cache/performance layer | `redis:6379` | none | none |
| `minio` | Independent object store for existing floor plans and future uploads | `minio:9000`, console `minio:9001` | `127.0.0.1:19000`, `127.0.0.1:19001` | `smart_property_public_demo_minio_data` |

Compose project resources use the independent bridge network `smart_property_public_demo_network`.

### Executed Stage 1 record (2026-09-01)

The actual Compose container names are `smart-property-public-demo-data-mysql-1`, `smart-property-public-demo-data-redis-1`, and `smart-property-public-demo-data-minio-1`. Docker created the following named-volume locations:

| Named volume | Container mount path | Docker Desktop Linux mountpoint |
|---|---|---|
| `smart_property_public_demo_mysql_data` | `/var/lib/mysql` in `mysql` | `/var/lib/docker/volumes/smart_property_public_demo_mysql_data/_data` |
| `smart_property_public_demo_minio_data` | `/data` in `minio` | `/var/lib/docker/volumes/smart_property_public_demo_minio_data/_data` |

The volumes are Docker-managed Linux named volumes. They are intentionally not Windows bind mounts.

## 3. Configuration and Secrets

1. In `C:\Projects\数据库课程设计\多小区智慧物业管理系统_PUBLIC_DEMO`, copy the template:

   ```powershell
   Copy-Item .env.docker.example .env.docker
   ```

2. Edit only `.env.docker`, replacing both MySQL passwords and both target-MinIO credential placeholders. Do not place source backup credentials in `compose.yaml`, `.env.docker.example`, Git, terminal screenshots, or chat.
3. Keep the default debug ports unless they are occupied. They are deliberately different from the native local services.

The Compose file must always be called with `--env-file .env.docker`; this keeps real credentials out of tracked configuration.

## 4. First Start

After `.env.docker` is filled, start only the three data services:

```powershell
docker compose --env-file .env.docker up -d
docker compose --env-file .env.docker ps
```

Wait until all three services are `running` and healthy before restoring data. Use:

```powershell
docker compose --env-file .env.docker logs mysql
docker compose --env-file .env.docker logs minio
docker compose --env-file .env.docker logs redis
```

Do not start any backend, frontend, Nginx, or cloudflared service in Stage 1.

## 5. MySQL Restore

### Source and assumptions

Source snapshot (already prepared by the user):

`C:\Projects\数据库课程设计\deployment_backups\community_db_before_public_demo_20260901.sql`

It is a full MySQL 8.0.43 logical dump of `community_db`, including data, indexes, FK/CHECK constraints, `flyway_schema_history`, and both views. It contains destructive `DROP TABLE IF EXISTS` statements by design, so use it **only against this fresh, isolated Docker MySQL**.

### Restore command

Run after MySQL health is passing, from the PUBLIC_DEMO directory:

```powershell
docker compose --env-file .env.docker cp `
  'C:\Projects\数据库课程设计\deployment_backups\community_db_before_public_demo_20260901.sql' `
  mysql:/tmp/community_db_before_public_demo_20260901.sql

docker compose --env-file .env.docker exec -T mysql sh -c `
  'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" < /tmp/community_db_before_public_demo_20260901.sql'

docker compose --env-file .env.docker exec -T mysql rm -f `
  /tmp/community_db_before_public_demo_20260901.sql
```

The SQL file is copied byte-for-byte into the Linux container and is read there by the MySQL client. This deliberately avoids PowerShell text-pipeline encoding conversion. Remove the temporary container copy only after the import command succeeds; its removal does not affect the named MySQL volume or the frozen source backup.

Do not import this dump into native Windows MySQL on port 3306.

### Restore verification

Run only after the import exits successfully:

```powershell
docker compose --env-file .env.docker exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SELECT DATABASE(); SHOW TABLES FROM community_db;"'

docker compose --env-file .env.docker exec -T mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" community_db -e "SELECT ''community'' AS name, COUNT(*) AS total FROM community UNION ALL SELECT ''property_unit'', COUNT(*) FROM property_unit UNION ALL SELECT ''person'', COUNT(*) FROM person UNION ALL SELECT ''meter'', COUNT(*) FROM meter UNION ALL SELECT ''meter_reading'', COUNT(*) FROM meter_reading UNION ALL SELECT ''bill'', COUNT(*) FROM bill UNION ALL SELECT ''bill_item'', COUNT(*) FROM bill_item UNION ALL SELECT ''payment'', COUNT(*) FROM payment; SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank; SHOW FULL TABLES WHERE Table_type = ''VIEW'';"'
```

Expected facts from the source snapshot include current demo tables and Flyway V1–V10. The two views `v_bill_financial_summary` and `v_monthly_collection_summary` must appear. If views fail, stop and inspect the restore log/DEFINER compatibility before Stage 2.

#### Executed verification result

The restored target passed this verification: `community=4`, `property_unit=528`, `person=606`, `meter=1584`, `meter_reading=15840`, `bill=3696`, `bill_item=18273`, `payment=3324`, and `file_object=12`. `flyway_schema_history` contains 10 successful records with numeric versions 1 through 10. Both required views are present and queryable; their current row counts are 3696 and 7 respectively. The target has 77 foreign keys, 108 CHECK constraints, and 208 index entries in `information_schema.statistics`.

Chinese verification passed with `utf8mb4` server/database/connection character sets and `utf8mb4_0900_ai_ci` server/database collations. Real Chinese community names, addresses, person names, building names, notification titles, and notification content were selected from the restored database; UTF-8 hex inspection showed valid UTF-8 bytes, with no replacement characters or garbling.

### Flyway rule for Stage 2

The restored database already has `flyway_schema_history` through V10. Stage 2 must **not** treat this as an empty database. Keep Flyway disabled for the first backend boot unless and until validation against the restored history has been deliberately performed; never run an empty-schema seed flow on this Docker volume.

## 6. MinIO Restore — Logical Migration Only

### Chosen method

Use **logical object migration (Option B)**, not raw copying of the Windows MinIO data directory into `public_demo_minio_data`.

The cold backup is MinIO `xl-single` format and includes `.minio.sys`. Directly copying it into a Linux Docker named volume would carry internal metadata, filesystem assumptions, and source-server state across platforms. A logical mirror preserves the bucket/object namespace required by the DB without depending on raw data-directory compatibility.

### Source material

Cold backup root:

`C:\Projects\数据库课程设计\deployment_backups\minio_data_before_public_demo_20260901\minio-data`

The original server release was `RELEASE.2025-07-23T15-54-02Z`; the temporary recovery server must use this same image tag. The source backup must never be mounted as the target's named volume and must never be edited in place.

### Safe migration sequence

1. Create a **separate writable working copy** of the cold backup outside both source backup and `PUBLIC_DEMO`. Do not use the original backup directory as a MinIO data volume.
2. Start a temporary source-recovery MinIO container from the matching release against that working copy, with its original source credentials supplied only through a local untracked environment file. Attach it to `smart_property_public_demo_network` under a temporary, clearly named container.
3. Verify the temporary source exposes `property-assets`, and inventory object count/keys without changing objects.
4. Use a temporary `minio/mc` container attached to the same network. Configure aliases for the source-recovery server and target `http://minio:9000`, then mirror `property-assets` with key preservation.
5. Record/replicate any non-default bucket policy, CORS, lifecycle, versioning, or retention configuration separately; `mc mirror` copies objects, not every bucket-level setting.
6. Verify target object count and sample object keys against `file_object.bucket_name`/`object_key` in restored MySQL.
7. Stop and remove only the temporary source-recovery and temporary `mc` containers. Keep the target `minio` service and its named volume.

The target must contain bucket `property-assets` with exactly the same object keys. No database update should be needed because `file_object` stores bucket plus object key, not endpoint URL. In Stage 2 the backend will use `MINIO_ENDPOINT=http://minio:9000`; browser access remains through the backend file stream API.

## 7. MinIO Restore Verification

Use the local target console only after the target is healthy:

`http://127.0.0.1:19001`

Verify that:

1. `property-assets` exists.
2. Object count and a few object keys match the temporary source inventory.
3. For a sample `file_object` database row, the same bucket/key exists in target MinIO.
4. No target object URL has been placed into the database.

Do not mark migration complete solely because the bucket exists: the backend auto-create behavior creates an empty bucket and cannot restore historical floor-plan objects.

#### Executed verification result

The source and target both contained 12 `property-assets` objects (about 1.38 MiB total). Every restored MySQL `file_object` row is `ACTIVE`, uses bucket `property-assets`, and has an object key found in the target MinIO inventory. This includes the four template plans and eight layout-variant plans. The object keys and recorded byte sizes match between MySQL and MinIO.

## 8. Redis Verification

Redis has no migrated dataset. Code audit confirms it is an optional cache/performance availability layer; business state remains in MySQL.

```powershell
docker compose --env-file .env.docker exec redis redis-cli ping
```

Expected output: `PONG`.

## 9. Persistence Test

After MySQL and MinIO verification is complete:

```powershell
docker compose --env-file .env.docker down
docker compose --env-file .env.docker up -d
docker compose --env-file .env.docker ps
```

Then repeat the MySQL count/view checks and MinIO bucket/object checks. Plain `down` removes containers and the bridge network but preserves the named MySQL/MinIO volumes.

#### Executed verification result

This test was executed with plain `docker compose --env-file .env.docker down` followed by `up -d` (never `-v`). All services returned healthy. The restored MySQL still had 528 property units, 10 successful Flyway versions through V10, and both views queryable; `property-assets` still contained all 12 migrated MinIO objects; Redis returned `PONG`. This demonstrates that container lifecycle and data lifecycle are separated.

## 10. Safe Operations and Dangerous Commands

Safe after migration:

```powershell
docker compose --env-file .env.docker restart mysql minio redis
docker compose --env-file .env.docker down
docker compose --env-file .env.docker up -d
docker compose --env-file .env.docker logs --tail 200 mysql
```

Never run against this project unless the explicit intention is to permanently discard the PUBLIC_DEMO data copy:

```text
docker compose down -v
docker volume rm smart_property_public_demo_mysql_data
docker volume rm smart_property_public_demo_minio_data
docker system prune --volumes
```

Also never mount the native Windows MySQL data directory or the original native MinIO data directory into these containers.

## 11. Rollback

If the Docker data copy is invalid, stop **only** the PUBLIC_DEMO Compose project, retain the original Windows services untouched, and diagnose from container logs and the backup files. Rebuilding the PUBLIC_DEMO copy requires an explicit decision to delete its named volumes; that action is destructive and is not part of normal restart/rebuild operations.

The original `…_MERGED_V1` project, native `community_db`, native Redis, native MinIO, and backup directories are rollback sources and must remain unchanged.

## 12. Stage 2 Connection Contract

When Stage 2 begins, the backend container should be configured with:

```text
DB_URL=jdbc:mysql://mysql:3306/community_db?...serverTimezone=Asia/Shanghai...
REDIS_HOST=redis
REDIS_PORT=6379
MINIO_ENDPOINT=http://minio:9000
APP_DEV_BOOTSTRAP=false
```

This is a future connection contract only. Stage 1 does not create or start a backend container.
