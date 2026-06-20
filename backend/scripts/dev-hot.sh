#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

MVN_CMD="./mvnw"
if [[ ! -x "$MVN_CMD" ]]; then
  MVN_CMD="mvn"
fi

echo "[dev-hot] Starting Spring Boot with DevTools..."
"$MVN_CMD" spring-boot:run &
APP_PID=$!

cleanup() {
  echo
  echo "[dev-hot] Stopping Spring Boot..."
  kill "$APP_PID" >/dev/null 2>&1 || true
}
trap cleanup EXIT INT TERM

last_signature=""

signature() {
  find src/main/java src/main/resources \
    -type f \
    \( -name '*.java' -o -name '*.yml' -o -name '*.yaml' -o -name '*.properties' \) \
    -print0 \
    | xargs -0 stat -f '%m %N' 2>/dev/null \
    | sort \
    | shasum
}

compile_if_changed() {
  local current_signature
  current_signature="$(signature)"
  if [[ "$current_signature" == "$last_signature" ]]; then
    return
  fi

  last_signature="$current_signature"
  echo "[dev-hot] Source changed. Compiling classes for DevTools restart..."
  if "$MVN_CMD" -q -DskipTests compile; then
    echo "[dev-hot] Compile finished. Spring Boot DevTools will restart the app."
  else
    echo "[dev-hot] Compile failed. Fix the error and save again."
  fi
}

last_signature="$(signature)"

while kill -0 "$APP_PID" >/dev/null 2>&1; do
  sleep 2
  compile_if_changed
done
