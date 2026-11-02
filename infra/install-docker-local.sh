#!/usr/bin/env bash
# Instala Docker pelo repositório oficial stable e sobe apenas o MongoDB.
set -euo pipefail
if [[ "$EUID" -ne 0 ]]; then
  echo 'Execute: sudo bash infra/install-docker-local.sh' >&2
  exit 1
fi
infra_dir="$(cd "$(dirname "$0")" && pwd)"
if ! command -v docker >/dev/null; then
  source /etc/os-release
  docker_codename="${UBUNTU_CODENAME:-${VERSION_CODENAME:-}}"
  case "$docker_codename" in
    noble|jammy|resolute) ;;
    *) echo "Base Ubuntu não reconhecida: $docker_codename" >&2; exit 1 ;;
  esac
  apt-get update
  apt-get install -y ca-certificates curl
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc
  cat > /etc/apt/sources.list.d/docker.sources <<SOURCES
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $docker_codename
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
SOURCES
  apt-get update
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
fi
systemctl start docker
docker version
docker compose version
bash "$infra_dir/start-mongo-local.sh"
