#!/usr/bin/env bash
# Smoke: launch Mneme and dump UI hierarchy (uiautomator).
set -euo pipefail
SERIAL="${1:-${ANDROID_SERIAL:-1431df87}}"
PKG="com.mneme.app.debug"
adb -s "$SERIAL" shell "am force-stop $PKG; am start -n $PKG/com.mneme.app.MainActivity"
sleep 2
adb -s "$SERIAL" shell "uiautomator dump /sdcard/mneme-ui.xml >/dev/null"
adb -s "$SERIAL" shell "cat /sdcard/mneme-ui.xml" | tr '>' '\n' | grep -E 'text="(Mneme|Pulse|Search|Ask|Memory|Nothing saved)' || true
echo "UI dump: /sdcard/mneme-ui.xml"
