#!/usr/bin/env bash
set -euo pipefail
infra_dir="$(cd "$(dirname "$0")" && pwd)"
if ! command -v docker >/dev/null; then
  echo 'Docker não instalado. Execute sudo bash infra/install-docker-local.sh.' >&2
  exit 1
fi
docker_cmd=(docker)
if ! docker info >/dev/null 2>&1; then
  docker_cmd=(sudo docker)
fi
"${docker_cmd[@]}" compose --env-file "$infra_dir/.env" -f "$infra_dir/compose.yml" -f "$infra_dir/compose.local.yml" up -d --wait postgres
set -a
source "$infra_dir/.env"
set +a
# Usa TCP e senha para testar a autenticação, em vez de socket com trust.
"${docker_cmd[@]}" compose --env-file "$infra_dir/.env" -f "$infra_dir/compose.yml" -f "$infra_dir/compose.local.yml" exec -T -e PGPASSWORD="$DATABASE_PASSWORD" postgres psql -h 127.0.0.1 -U "$DATABASE_USER" -d painel -v ON_ERROR_STOP=1 -c 'SELECT current_database(), current_user, version();'
echo "PostgreSQL disponível em 127.0.0.1:${POSTGRES_PORT:-5433}."
