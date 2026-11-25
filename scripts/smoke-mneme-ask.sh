#!/usr/bin/env bash
# Smoke Ask: seed note (if empty), open Ask, submit question, assert cited answer.
set -euo pipefail
SERIAL="${1:-${ANDROID_SERIAL:-1431df87}}"
PKG="com.mneme.app.debug"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

adb -s "$SERIAL" shell "am force-stop $PKG"
adb -s "$SERIAL" shell "run-as $PKG cat databases/mneme.db" >"$TMP/mneme.db" 2>/dev/null || true
N="$(sqlite3 "$TMP/mneme.db" "SELECT COUNT(*) FROM notes;" 2>/dev/null || echo 0)"
if [[ "$N" == "0" ]]; then
  NOW="$(date +%s%3N)"
  HASH="$(printf 'Chapter 11|Chapter 11 homework due August 25 from mam WhatsApp|seed' | sha256sum | awk '{print $1}')"
  sqlite3 "$TMP/mneme.db" "INSERT INTO notes (title, body, source, createdAt, dueAt, contentHash, embedding)
    VALUES ('Chapter 11', 'Chapter 11 homework due August 25 from mam WhatsApp', 'seed', $NOW, NULL, '$HASH', NULL);"
  adb -s "$SERIAL" shell "run-as $PKG rm -f databases/mneme.db-wal databases/mneme.db-shm"
  cat "$TMP/mneme.db" | adb -s "$SERIAL" shell "run-as $PKG sh -c 'cat > databases/mneme.db'"
fi

adb -s "$SERIAL" shell "am start -n $PKG/com.mneme.app.MainActivity" >/dev/null
sleep 2
adb -s "$SERIAL" shell "input tap 815 2350"
sleep 1
adb -s "$SERIAL" shell "input tap 540 743"
adb -s "$SERIAL" shell "input text 'When%sis%schapter%s11%sdue'"
adb -s "$SERIAL" shell "input tap 255 927"

ok=0
for _ in $(seq 1 36); do
  sleep 5
  adb -s "$SERIAL" shell "uiautomator dump /sdcard/mneme-ui.xml >/dev/null" 2>/dev/null || true
  adb -s "$SERIAL" pull /sdcard/mneme-ui.xml "$TMP/ui.xml" >/dev/null 2>&1 || true
  if grep -q 'August 25' "$TMP/ui.xml" 2>/dev/null && grep -q 'Answer' "$TMP/ui.xml" 2>/dev/null; then
    ok=1
    break
  fi
  if grep -q 'model busy' "$TMP/ui.xml" 2>/dev/null; then
    echo "FAIL: LLM fell back (model busy / permission)"
    exit 1
  fi
done

if [[ "$ok" -eq 1 ]]; then
  echo "PASS: Ask answered with August 25 citation"
else
  echo "FAIL: no Answer/August 25 in UI after wait"
  exit 1
fi
