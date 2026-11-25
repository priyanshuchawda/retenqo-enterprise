#!/data/data/com.termux/files/usr/bin/bash
# =============================================================================
# Mneme — ONE-TIME Termux setup (run this INSIDE Termux on Phone 1)
# Author: Priyanshu Chawda
#
# How to run:
#   1. Open Termux on the phone
#   2. bash /sdcard/Mneme/scripts/termux-setup-mneme.sh
#      (or: curl/bash after adb push)
#
# After this, model tests and dev commands work reliably from Termux + Ubuntu.
# =============================================================================
set -euo pipefail

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
log()  { echo -e "${GREEN}[mneme-setup]${NC} $*"; }
warn() { echo -e "${YELLOW}[mneme-setup]${NC} $*"; }

log "=== Mneme Termux one-time setup ==="

# --- 1. Storage (CRITICAL — fixes /sdcard access in Ubuntu) ---
log "Step 1: Storage access"
if [[ ! -d "$HOME/storage/shared" ]]; then
  log "Running termux-setup-storage (accept the Android prompt)..."
  termux-setup-storage
else
  log "Storage already set up: $HOME/storage/shared -> /sdcard"
fi

# Canonical paths (same folder, two views)
MNEME_SDCARD="/sdcard/Mneme"
MNEME_TERMUX="$HOME/storage/shared/Mneme"
mkdir -p "$MNEME_SDCARD"/{models/phone1/{llm,whisper,embed/litert,fallback},bin,scripts,test-assets,logs}
mkdir -p "$MNEME_TERMUX"/{models/phone1/{llm,whisper,embed/litert,fallback},bin,scripts,test-assets,logs}

# Symlink so `ubuntu` and adb push use the same tree
if [[ ! -L "$HOME/Mneme" ]]; then
  ln -sf "$MNEME_TERMUX" "$HOME/Mneme"
  log "Linked ~/Mneme -> storage/shared/Mneme"
fi

# --- 2. Termux packages for testing ---
log "Step 2: Termux packages"
pkg update -y
pkg install -y proot-distro git curl wget

# --- 3. Ubuntu (if missing) ---
log "Step 3: Ubuntu proot"
if ! proot-distro list 2>/dev/null | grep -q "ubuntu"; then
  warn "Installing Ubuntu (one-time, ~5 min)..."
  proot-distro install ubuntu
else
  log "Ubuntu already installed"
fi

# --- 4. Ubuntu dev tools (for building llama.cpp / whisper.cpp) ---
log "Step 4: Ubuntu build tools"
if [[ -f "$HOME/ubuntu-dev-bootstrap.sh" ]]; then
  ubuntu -- bash "$HOME/ubuntu-dev-bootstrap.sh" || warn "Bootstrap had warnings (may be OK if already done)"
else
  ubuntu -- bash -c '
    export DEBIAN_FRONTEND=noninteractive
    apt-get update -y
    apt-get install -y --no-install-recommends \
      python3 python3-pip python3-venv build-essential cmake git curl
  '
fi

# --- 5. Allow external apps (for adb automation — optional) ---
log "Step 5: External app access"
PROPS="$HOME/.termux/termux.properties"
mkdir -p "$HOME/.termux"
if grep -q '^allow-external-apps = true' "$PROPS" 2>/dev/null; then
  log "allow-external-apps already enabled"
else
  echo 'allow-external-apps = true' >> "$PROPS"
  log "Enabled allow-external-apps in termux.properties"
  warn "Run: termux-reload-settings   (or force-stop Termux and reopen)"
fi

# --- 6. SSH for laptop -> phone commands (BEST automation) ---
log "Step 6: SSH (recommended for adb-free testing from laptop)"
if ! command -v sshd >/dev/null 2>&1; then
  pkg install -y openssh
fi
mkdir -p "$HOME/.ssh"
# Set password once if missing
if [[ ! -f "$HOME/.ssh/authorized_keys" ]]; then
  warn "Set a Termux SSH password now (for laptop access):"
  passwd
fi
sshd 2>/dev/null || true
TERMUX_USER=$(whoami)
TERMUX_PORT=$(sshd -T 2>/dev/null | awk '/^port /{print $2}' || echo 8022)
log "SSH user: $TERMUX_USER  port: $TERMUX_PORT"
log "From laptop: adb forward tcp:8022 tcp:8022 && ssh -p 8022 ${TERMUX_USER}@127.0.0.1"

# --- 7. Install test runner ---
log "Step 7: Test runner command"
cat > "$HOME/mneme-test" << 'RUNNER'
#!/data/data/com.termux/files/usr/bin/bash
# Run all P0 model smoke tests inside Ubuntu
exec ubuntu -- bash /sdcard/Mneme/scripts/phone1-test-ubuntu.sh "$@"
RUNNER
chmod +x "$HOME/mneme-test"
log "Installed: ~/mneme-test"

# --- 8. Android permissions checklist ---
log ""
log "=== MANUAL (do once in Android Settings) ==="
warn "1. Settings > Apps > Termux > Permissions > Files and media > ALLOW ALL / Manage all files"
warn "2. Settings > Apps > Termux > Battery > Unrestricted (keeps SSH + long LLM runs alive)"
warn "3. Disable MIUI battery saver for Termux (Security app > Battery > Termux > No restrictions)"
log ""
log "=== DONE ==="
log "Models folder:  /sdcard/Mneme/models/phone1/"
log "Run tests:      mneme-test"
log "Open Ubuntu:    ubuntu"
log "From laptop:    adb push ... /sdcard/Mneme/..."
log "SSH test:       ssh -p 8022 (after adb forward tcp:8022 tcp:8022)"
