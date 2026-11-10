#!/bin/bash
# Build all backend jars (requires Java 21 + Maven), then all docker images.
set -euo pipefail
cd "$(dirname "$0")"

for service in services/*/; do
    echo "==> Building ${service}"
    (cd "$service" && mvn -q clean package -DskipTests)
done

docker compose build
