#!/usr/bin/env bash
# Build Retenqo product APK (arm64, JDK 21).
# Usage: ./scripts/build-mneme.sh [assembleDebug|assembleRelease]
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP_DIR="$ROOT/android/mneme"
# Fedora system JDK may be 25; Gradle 8.14 needs ≤21. Prefer Temurin 21 always.
JAVA_HOME="${MNEME_JAVA_HOME:-${JAVA_HOME:-$HOME/.local/jdk-21}}"
if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
  JAVA_BIN="$(command -v java || true)"
  if [[ -n "$JAVA_BIN" ]]; then
    JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$JAVA_BIN")")")"
  fi
fi
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
if [[ -z "${ANDROID_HOME:-}" && -d "$HOME/android-sdk" ]]; then
  export ANDROID_HOME="$HOME/android-sdk"
fi
if [[ -z "${ANDROID_SDK_ROOT:-}" && -n "${ANDROID_HOME:-}" ]]; then
  export ANDROID_SDK_ROOT="$ANDROID_HOME"
fi
JAVA_VER="$("$JAVA_HOME/bin/java" -version 2>&1 | head -1 || true)"
case "$JAVA_VER" in
  *\"17.*|*\"21.*) ;;
  *)
    echo "Need JDK 17/21 at $JAVA_HOME (or set MNEME_JAVA_HOME). Got: $JAVA_VER"
    exit 1
    ;;
esac
TASK="${1:-assembleDebug}"

"$ROOT/scripts/bootstrap-native-deps.sh"

cd "$APP_DIR"
echo "JDK: $JAVA_VER"
./gradlew "$TASK" --no-daemon
APK="$APP_DIR/app/build/outputs/apk/debug/app-debug.apk"
case "$TASK" in
  *Release*) APK="$APP_DIR/app/build/outputs/apk/release/app-release-unsigned.apk" ;;
esac
if [[ -f "$APK" ]]; then
  SIZE="$(du -h "$APK" | awk '{print $1}')"
  echo "APK: $APK ($SIZE)"
fi
