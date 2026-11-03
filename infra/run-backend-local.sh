#!/usr/bin/env bash
set -euo pipefail
project_root="$(cd "$(dirname "$0")/.." && pwd)"
source "$project_root/infra/java25.sh"
set -a
if [[ -f "$project_root/infra/.env" ]]; then source "$project_root/infra/.env"; fi
set +a
cd "$project_root/backend"
mvn package
jar=chatbot-boot/target/painel-bot-0.1.0.jar
if [[ "${1:-}" == provision ]]; then
  exec java -jar "$jar" --spring.profiles.active=provision --spring.main.web-application-type=none
fi
exec java -jar "$jar" --server.port=8087
