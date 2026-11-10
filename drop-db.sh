#!/bin/bash
# Drop every LearningHub database (the 6 service databases and keycloak) so the next ./start.sh
# re-creates and re-seeds them: each service runs its Flyway migrations (V1 schema, V2 seed) on an
# empty database and Keycloak re-imports the realm (all accounts) into an empty keycloak database.
#
#   ./drop-db.sh               drop + recreate all 7 databases (empty), then stop everything
#   ./drop-db.sh userdb ...    drop + recreate only the named database(s)
#
# Starts the postgres container if it is not running; stops and removes all containers when done
# (the data volume is kept). Then run ./start.sh.
set -euo pipefail
cd "$(dirname "$0")"

CONTAINER=learninghub-stub_postgres_1
PGUSER=pguser
ALL_DBS="userdb scheduledb enrollmentdb learningdb assessmentdb paymentdb keycloak"

NAMED=""
for arg in "$@"; do
  case "$arg" in
    -h|--help) sed -n '2,9p' "$0"; exit 0 ;;
    *)         NAMED="$NAMED $arg" ;;
  esac
done
DBS=${NAMED:-$ALL_DBS}

echo "Stopping the services that hold database connections..."
docker compose stop user-service schedule-service enrollment-service learning-service assessment-service payment-service keycloak >/dev/null 2>&1 || true

if ! docker ps --format '{{.Names}}' | grep -qx "$CONTAINER"; then
  echo "Starting postgres..."
  docker compose up --no-build --detach postgres >/dev/null 2>&1
fi
for i in $(seq 1 30); do
  docker exec "$CONTAINER" pg_isready -U "$PGUSER" >/dev/null 2>&1 && break
  sleep 2
done
docker exec "$CONTAINER" pg_isready -U "$PGUSER" >/dev/null 2>&1 || { echo "postgres did not become ready" >&2; exit 1; }

psql() { docker exec "$CONTAINER" psql -U "$PGUSER" -d postgres -v ON_ERROR_STOP=1 -tAc "$1"; }

for db in $DBS; do
  if [ "$(psql "select 1 from pg_database where datname='$db'")" = "1" ]; then
    psql "DROP DATABASE $db WITH (FORCE)" >/dev/null
    echo "dropped   $db"
  else
    echo "absent    $db"
  fi
  psql "CREATE DATABASE $db" >/dev/null
  psql "GRANT ALL PRIVILEGES ON DATABASE $db TO $PGUSER" >/dev/null
  echo "recreated $db (empty)"
done

# Stop postgres and remove every container (the data volume and network are kept), so ./start.sh
# creates fresh containers from the current images.
echo "Stopping postgres and removing the containers..."
docker compose down >/dev/null 2>&1 || true

echo
echo "Done. Run ./start.sh to start everything; Flyway re-creates and seeds each schema and Keycloak re-imports the realm."
