#!/usr/bin/env bash
# Mneme — laptop helper for Phone 1 (Redmi 13 5G, serial 1431df87)
# Usage:
#   ./scripts/laptop-mneme-phone.sh setup    # permissions + adb forward + SSH check
#   ./scripts/laptop-mneme-phone.sh test     # run model smoke tests on phone
#   ./scripts/laptop-mneme-phone.sh log      # tail test log
#   ./scripts/laptop-mneme-phone.sh ssh      # interactive Termux SSH
set -euo pipefail

SERIAL="${MNEME_SERIAL:-1431df87}"
SSH_PORT="${MNEME_SSH_PORT:-8022}"
SSH_USER="${MNEME_SSH_USER:-u0_a349}"
KEY="${HOME}/.ssh/id_ed25519"

cmd="${1:-setup}"

adb_ensure() {
  adb -s "$SERIAL" get-state >/dev/null 2>&1 || { echo "Phone $SERIAL not connected"; exit 1; }
}

grant_permissions() {
  adb -s "$SERIAL" shell "
    appops set com.termux MANAGE_EXTERNAL_STORAGE allow
    appops set com.termux READ_EXTERNAL_STORAGE allow
    appops set com.termux WRITE_EXTERNAL_STORAGE allow
    pm grant com.termux android.permission.READ_EXTERNAL_STORAGE 2>/dev/null
    pm grant com.termux android.permission.WRITE_EXTERNAL_STORAGE 2>/dev/null
    dumpsys deviceidle whitelist +com.termux
  " >/dev/null
  echo "Granted: storage appops + battery whitelist"
}

forward_ssh() {
  adb -s "$SERIAL" forward "tcp:${SSH_PORT}" tcp:8022
  echo "ADB forward: localhost:${SSH_PORT} -> phone:8022"
}

ssh_phone() {
  ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -i "$KEY" -p "$SSH_PORT" "${SSH_USER}@127.0.0.1" "$@"
}

case "$cmd" in
  setup)
    adb_ensure
    grant_permissions
    forward_ssh
    ssh_phone 'echo SSH_OK; ls ~/mneme-test/llm/*.gguf 2>/dev/null | head -1 || echo "models in ~/mneme-test"'
    echo ""
    echo "If /sdcard still blocked: open Termux settings on phone -> Allow all files"
    adb -s "$SERIAL" shell "am start -a android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION -d package:com.termux" 2>/dev/null || true
    ;;
  test)
    adb_ensure
    forward_ssh
    echo "Running mneme-test on phone (may take 15-30 min first time)..."
    ssh_phone 'export PATH=$HOME/bin:$PREFIX/bin:$PATH; mneme-test'
    ;;
  log)
    adb_ensure
    forward_ssh
    ssh_phone 'cat ~/mneme-test/test-log.txt 2>/dev/null | tail -50'
    ;;
  ssh)
    adb_ensure
    forward_ssh
    exec ssh -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -i "$KEY" -p "$SSH_PORT" "${SSH_USER}@127.0.0.1"
    ;;
  *)
    echo "Usage: $0 {setup|test|log|ssh}"
    exit 1
    ;;
esac
