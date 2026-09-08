#!/usr/bin/env bash

set -euo pipefail

cd /home/ubuntu/backend
compose_file="/home/ubuntu/backend/docker-compose.production.yaml"
if [[ ! -f "$compose_file" ]]; then
  compose_file="/home/ubuntu/backend/docker-compose.yaml"
fi

docker compose -f docker-compose.cert.renew.yaml run --rm certbot
docker compose -f "$compose_file" exec -T nginx nginx -s reload
