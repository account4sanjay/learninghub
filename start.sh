#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"

./build.sh

# Bring up postgres first and make sure every database exists. The image's init
# script only creates them on a brand-new volume; this covers volumes created
# before a database was added (or after ./drop-db.sh --no-create).
docker compose up --no-build --detach postgres
DBS=(keycloak userdb scheduledb enrollmentdb learningdb assessmentdb paymentdb)
for i in $(seq 1 30); do
  docker exec learninghub-stub_postgres_1 pg_isready -U pguser >/dev/null 2>&1 && break
  sleep 2
done
for db in "${DBS[@]}"; do
  exists=$(docker exec learninghub-stub_postgres_1 psql -U pguser -d postgres -tAc "select 1 from pg_database where datname='$db'")
  if [ "$exists" != "1" ]; then
    echo "==> creating database $db"
    docker exec learninghub-stub_postgres_1 psql -U pguser -d postgres -v ON_ERROR_STOP=1 -c "CREATE DATABASE $db" -c "GRANT ALL PRIVILEGES ON DATABASE $db TO pguser" >/dev/null
  fi
done

# Each service runs its Flyway migrations on startup: an empty database gets the
# schema (V1) and the seed data (V2); an already-migrated one is left as is.
docker compose up --no-build --detach

# Host port published by gateway-service in docker-compose.yml (host:container)
APP_PORT=$(awk '/^  gateway-service:/{f=1} f && /- "[0-9]+:[0-9]+"/{gsub(/[" -]/,""); split($0,p,":"); print p[1]; exit}' docker-compose.yml)
APP_PORT=${APP_PORT:-8080}

echo
echo "LearningHub is starting. Open the application at:"
echo
echo "    http://localhost:${APP_PORT}"
echo
echo "(Services take a minute to register; the page shows a 'server starting' banner until they are ready.)"
echo "(Reset all data: ./drop-db.sh, then ./start.sh again.)"
