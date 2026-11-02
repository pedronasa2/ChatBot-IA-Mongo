#!/usr/bin/env bash
set -euo pipefail
infra_dir="$(cd "$(dirname "$0")" && pwd)"
docker compose -p chatbot-mongo-local -f "$infra_dir/compose.local.yml" up -d
for attempt in $(seq 1 60); do
  if docker compose -p chatbot-mongo-local -f "$infra_dir/compose.local.yml" exec -T mongo mongosh --quiet --eval 'quit(db.hello().isWritablePrimary ? 0 : 1)' >/dev/null 2>&1; then
    echo 'MongoDB local pronto, replica set primário em 127.0.0.1:27027.'
    exit 0
  fi
  sleep 1
done
echo 'MongoDB não elegeu um primário; confira os logs do mongo e mongo-init.' >&2
exit 1
