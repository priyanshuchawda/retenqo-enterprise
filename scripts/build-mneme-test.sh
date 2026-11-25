#!/usr/bin/env bash
# Build Mneme model test APK. Pins JDK via local.properties (org.gradle.java.home).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP_DIR="$ROOT/android/mneme-test"
JAVA_HOME="${JAVA_HOME:-$HOME/.local/jdk-21}"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
cd "$APP_DIR"
echo "JDK: $($JAVA_HOME/bin/java -version 2>&1 | head -1)"
./gradlew assembleDebug "$@"
APK="$APP_DIR/app/build/outputs/apk/debug/app-debug.apk"
if [[ -f "$APK" ]]; then
  echo "APK: $APK"
fi
