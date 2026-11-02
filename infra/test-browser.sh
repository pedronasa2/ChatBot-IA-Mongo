#!/usr/bin/env bash
set -euo pipefail
project_root="$(cd "$(dirname "$0")/.." && pwd)"
test_dir=$(mktemp -d)
export E2E_CREDENTIAL_FILE="$test_dir/credential"
backend_pid=''
backend_port="${E2E_BACKEND_PORT:-8087}"
export E2E_BASE_URL="http://127.0.0.1:$backend_port"
export E2E_BACKEND_PORT="$backend_port"
export E2E_MODULES_TOKEN="${E2E_MODULES_TOKEN:-modules-only-browser-tests}"
export E2E_METRICS_TOKEN="${E2E_METRICS_TOKEN:-metrics-only-browser-tests}"
[[ "$backend_port" =~ ^[0-9]+$ ]] || exit 1
cleanup() {
  if [[ -n "$backend_pid" ]]; then kill "$backend_pid" 2>/dev/null || true; fi
  rm -rf "$test_dir"
}
trap cleanup EXIT
mkdir -p "$project_root/backend/target/classes/static"
cp -a "$project_root/frontend/dist/painel/browser/." "$project_root/backend/target/classes/static/"
cd "$project_root/backend"
mvn -q test-compile org.apache.maven.plugins:maven-dependency-plugin:3.9.0:build-classpath -Dmdep.outputFile=target/test-classpath -Dmdep.includeScope=test
java -cp "target/classes:target/test-classes:$(cat target/test-classpath)" br.com.painel.BrowserTestServer > "$test_dir/backend.log" 2>&1 &
backend_pid=$!
cd "$project_root/frontend"
for attempt in $(seq 1 90); do
  if [[ -s "$E2E_CREDENTIAL_FILE" ]] && curl -fsS "$E2E_BASE_URL/" >/dev/null; then break; fi
  if ! kill -0 "$backend_pid" 2>/dev/null; then
    cat "$test_dir/backend.log"
    exit 1
  fi
  sleep 1
done
[[ -s "$E2E_CREDENTIAL_FILE" ]] || { echo 'O servidor de teste não ficou pronto.'; exit 1; }
npm run test:e2e
