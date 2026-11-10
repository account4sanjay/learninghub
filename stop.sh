#!/bin/bash
# Stop and remove the learninghub-stub containers; the postgres volume (data) is kept.
# Full reset: ./stop.sh && docker volume rm learninghub-stub_postgres_data && ./start.sh
cd "$(dirname "$0")"
docker compose down
