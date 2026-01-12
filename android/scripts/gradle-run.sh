#!/usr/bin/env bash

# MIT License
#
# Copyright (c) 2025 GRIN Project Contributors
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
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
