#!/usr/bin/env bash
# Install Retenqo on connected phone, stage models, grant runtime bits.
# Usage: ./scripts/install-mneme.sh [serial]
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SERIAL="${1:-${ANDROID_SERIAL:-1431df87}}"
APK="$ROOT/android/mneme/app/build/outputs/apk/debug/app-debug.apk"
PKG="com.mneme.app.debug"
APP_FILES="/sdcard/Android/data/$PKG/files/models"
WHISPER_SRC="/sdcard/Retenqo/models/phone1/whisper/ggml-base-q5_1.bin"
WHISPER_DST="$APP_FILES/whisper/ggml-base-q5_1.bin"

[[ -f "$APK" ]] || { echo "Missing APK — run ./scripts/build-mneme.sh first"; exit 1; }

adb -s "$SERIAL" wait-for-device
adb -s "$SERIAL" install -r -t "$APK"
adb -s "$SERIAL" shell "appops set $PKG PROJECT_MEDIA allow 2>/dev/null || true"
adb -s "$SERIAL" shell "appops set $PKG MANAGE_EXTERNAL_STORAGE allow 2>/dev/null || true"
adb -s "$SERIAL" shell "pm grant $PKG android.permission.POST_NOTIFICATIONS 2>/dev/null || true"
adb -s "$SERIAL" shell "pm grant $PKG android.permission.RECORD_AUDIO 2>/dev/null || true"
adb -s "$SERIAL" shell "pm grant $PKG android.permission.READ_MEDIA_IMAGES 2>/dev/null || true"
adb -s "$SERIAL" shell "pm grant $PKG android.permission.READ_MEDIA_AUDIO 2>/dev/null || true"

stage_model() {
  local src="$1" dst="$2" label="$3"
  if adb -s "$SERIAL" shell "test -s '$dst'" >/dev/null 2>&1; then
    echo "$label already in app files"
    return
  fi
  if ! adb -s "$SERIAL" shell "test -f '$src'" >/dev/null 2>&1; then
    echo "WARN: $label missing at $src"
    return
  fi
  echo "Copying $label into app files…"
  adb -s "$SERIAL" shell "mkdir -p \"\$(dirname '$dst')\""
  adb -s "$SERIAL" shell "cp '$src' '$dst'"
}

stage_model "$WHISPER_SRC" "$WHISPER_DST" "Whisper"

adb -s "$SERIAL" shell "am start -n $PKG/com.mneme.app.MainActivity"
echo "Installed + launched $PKG on $SERIAL"
