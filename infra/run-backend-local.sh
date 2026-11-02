#!/usr/bin/env bash
set -euo pipefail
project_root="$(cd "$(dirname "$0")/.." && pwd)"
set -a
if [[ -f "$project_root/infra/.env" ]]; then source "$project_root/infra/.env"; fi
set +a
cd "$project_root/backend"
if [[ "${1:-}" == provision ]]; then
  mvn -DskipTests package
  exec java -jar target/painel-bot-0.1.0.jar --spring.profiles.active=provision --spring.main.web-application-type=none
fi
exec mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8087
