#!/usr/bin/env bash
# Source from development/test scripts. JAVA_HOME selects this project's JDK.
if [[ -n "${JAVA_HOME:-}" ]]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
painel_java_major=$(java -version 2>&1 | sed -n '1s/.*version "\([0-9]*\).*/\1/p')
if [[ "$painel_java_major" != 25 ]]; then
  echo 'Este projeto requer JDK 25. Configure JAVA_HOME para seu JDK 25 e repita o comando.' >&2
  exit 1
fi
unset painel_java_major
