#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GRADLE_BIN="${GRADLE_BIN:-}"

if [[ -z "${GRADLE_BIN}" ]]; then
  if [[ -x "${ROOT_DIR}/gradlew" ]]; then
    GRADLE_BIN="${ROOT_DIR}/gradlew"
  elif [[ -n "${GRADLE_HOME:-}" && -x "${GRADLE_HOME}/bin/gradle" ]]; then
    GRADLE_BIN="${GRADLE_HOME}/bin/gradle"
  else
    GRADLE_BIN="$(find "${HOME}/.gradle/wrapper/dists" -type f -path "*/bin/gradle" 2>/dev/null | sort | tail -n 1 || true)"
  fi
fi

if [[ -z "${GRADLE_BIN}" ]]; then
  echo "Gradle not found. Set GRADLE_BIN or GRADLE_HOME, or add gradlew." >&2
  exit 1
fi

cd "${ROOT_DIR}"
"${GRADLE_BIN}" "${@:-:lib:testReleaseUnitTest}"
