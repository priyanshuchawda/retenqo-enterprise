#!/usr/bin/env bash
# Run Retenqo JVM unit tests (no device required).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP_DIR="$ROOT/android/mneme"
JAVA_HOME="${MNEME_JAVA_HOME:-${JAVA_HOME:-$HOME/.local/jdk-21}}"
if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
  JAVA_BIN="$(command -v java || true)"
  if [[ -n "$JAVA_BIN" ]]; then
    JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$JAVA_BIN")")")"
  fi
fi
if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
  echo "JDK 21 not found. Set MNEME_JAVA_HOME to a JDK 21 installation."
  exit 1
fi
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
if [[ -z "${ANDROID_HOME:-}" && -d "$HOME/android-sdk" ]]; then
  export ANDROID_HOME="$HOME/android-sdk"
fi
if [[ -z "${ANDROID_SDK_ROOT:-}" && -n "${ANDROID_HOME:-}" ]]; then
  export ANDROID_SDK_ROOT="$ANDROID_HOME"
fi
"$ROOT/scripts/bootstrap-native-deps.sh"
cd "$APP_DIR"
./gradlew :app:testDebugUnitTest --no-daemon "$@"
echo "Unit tests OK"
