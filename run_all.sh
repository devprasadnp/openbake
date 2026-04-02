#!/usr/bin/env zsh
# ─────────────────────────────────────────────────────────────────────────────
# run_all.sh — Start backend, web, ngrok, and Android for OpenBake
#
# Usage:
#   ./run_all.sh              # Full stack (backend + web + ngrok; Android info printed)
#   ./run_all.sh --android    # Also build & install Android APK on emulator/device
#   ./run_all.sh --help
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

# ── Colours ───────────────────────────────────────────────────────────────────
RED="\033[0;31m"; GREEN="\033[0;32m"; YELLOW="\033[1;33m"
CYAN="\033[0;36m"; BOLD="\033[1m"; RESET="\033[0m"

log()  { echo "${BOLD}[$(date +%H:%M:%S)]${RESET} $*"; }
ok()   { echo "${GREEN}[✔]${RESET} $*"; }
warn() { echo "${YELLOW}[!]${RESET} $*"; }
err()  { echo "${RED}[✘]${RESET} $*" >&2; }
sep()  { echo "${CYAN}────────────────────────────────────────────────────${RESET}"; }

# ── Paths ─────────────────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/backend"
WEB_DIR="$SCRIPT_DIR/web"
ANDROID_DIR="$SCRIPT_DIR/android"
ANDROID_GRADLE="$ANDROID_DIR/app/build.gradle.kts"

BACKEND_PORT=8000
WEB_PORT=3000
NGROK_API="http://localhost:4040/api/tunnels"

# Saved PIDs for cleanup
declare -a PIDS=()

# ── Help ──────────────────────────────────────────────────────────────────────
if [[ "${1:-}" == "--help" ]]; then
  echo ""
  echo "${BOLD}Usage:${RESET}"
  echo "  ./run_all.sh              # backend + web + ngrok"
  echo "  ./run_all.sh --android    # + build & install Android APK"
  echo "  ./run_all.sh --help"
  echo ""
  exit 0
fi

BUILD_ANDROID=false
[[ "${1:-}" == "--android" ]] && BUILD_ANDROID=true

# ── Cleanup on exit ───────────────────────────────────────────────────────────
cleanup() {
  echo ""
  warn "Shutting down all services…"
  for pid in "${PIDS[@]}"; do
    kill "$pid" 2>/dev/null || true
  done
  pkill -f "ngrok http $BACKEND_PORT" 2>/dev/null || true
  ok "Done."
}
trap cleanup EXIT INT TERM

# ── Prerequisite checks ───────────────────────────────────────────────────────
sep
log "Checking prerequisites…"

check_cmd() {
  if ! command -v "$1" &>/dev/null; then
    err "$1 not found. Install it and re-run."
    exit 1
  fi
  ok "$1 found"
}

check_cmd python3
check_cmd node
check_cmd npm

# ngrok is optional — warn only
if ! command -v ngrok &>/dev/null; then
  warn "ngrok not found — Android emulator URL (10.0.2.2) will be used instead."
  NGROK_AVAILABLE=false
else
  ok "ngrok found"
  NGROK_AVAILABLE=true
fi

# ── 1. Backend ────────────────────────────────────────────────────────────────
sep
log "Starting FastAPI backend on port $BACKEND_PORT…"

# Prefer root .venv, fall back to backend/venv
if [[ -f "$SCRIPT_DIR/.venv/bin/python" ]]; then
  VENV_PYTHON="$SCRIPT_DIR/.venv/bin/python"
  VENV_UVICORN="$SCRIPT_DIR/.venv/bin/uvicorn"
else
  VENV_PYTHON="$BACKEND_DIR/venv/bin/python"
  VENV_UVICORN="$BACKEND_DIR/venv/bin/uvicorn"
fi

if [[ ! -f "$VENV_PYTHON" ]]; then
  warn "venv not found. Creating at $SCRIPT_DIR/.venv…"
  python3 -m venv "$SCRIPT_DIR/.venv"
  VENV_PYTHON="$SCRIPT_DIR/.venv/bin/python"
  VENV_UVICORN="$SCRIPT_DIR/.venv/bin/uvicorn"
  "$VENV_PYTHON" -m pip install -q -r "$BACKEND_DIR/requirements.txt"
  ok "venv created and packages installed."
fi

# Write .env if it doesn't exist
if [[ ! -f "$BACKEND_DIR/.env" ]]; then
  cat > "$BACKEND_DIR/.env" <<EOF
SECRET_KEY=$(python3 -c "import secrets; print(secrets.token_hex(32))")
ACCESS_TOKEN_EXPIRE_MINUTES=30
REFRESH_TOKEN_EXPIRE_DAYS=7
DATABASE_URL=sqlite:///./openbake.db
RAZORPAY_KEY_ID=rzp_test_placeholder
RAZORPAY_KEY_SECRET=placeholder
BAKERY_LAT=12.9716
BAKERY_LNG=77.5946
EOF
  ok ".env created at $BACKEND_DIR/.env — update credentials before production use."
fi

(
  cd "$BACKEND_DIR"
  "$VENV_UVICORN" app.main:app --host 0.0.0.0 --port "$BACKEND_PORT" --reload \
    >> /tmp/openbake_backend.log 2>&1
) &
PIDS+=($!)
BACKEND_PID=${PIDS[-1]}

# Wait until /health responds
log "Waiting for backend to become ready…"
READY=false
for i in {1..30}; do
  if curl -sf "http://localhost:$BACKEND_PORT/health" > /dev/null 2>&1; then
    READY=true
    break
  fi
  sleep 1
done

if $READY; then
  ok "Backend is up: http://localhost:$BACKEND_PORT"
else
  err "Backend did not start within 30 s. Check /tmp/openbake_backend.log"
  exit 1
fi

# ── 2. ngrok ──────────────────────────────────────────────────────────────────
NGROK_URL=""

if $NGROK_AVAILABLE; then
  sep
  log "Starting ngrok tunnel…"

  ngrok http "$BACKEND_PORT" --log=stdout > /tmp/openbake_ngrok.log 2>&1 &
  PIDS+=($!)

  # Poll ngrok API for the public URL (up to 15 s)
  for i in {1..15}; do
    NGROK_URL=$(
      curl -sf "$NGROK_API" 2>/dev/null |
      python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    tunnels = data.get('tunnels', [])
    https = [t['public_url'] for t in tunnels if t['proto'] == 'https']
    print(https[0] if https else '', end='')
except Exception:
    print('', end='')
" 2>/dev/null
    ) || NGROK_URL=""
    [[ -n "$NGROK_URL" ]] && break
    sleep 1
  done

  if [[ -n "$NGROK_URL" ]]; then
    ok "ngrok tunnel: $NGROK_URL"
  else
    warn "Could not obtain ngrok URL. Inspect /tmp/openbake_ngrok.log"
    warn "Falling back to emulator address for Android."
    NGROK_URL=""
  fi
fi

# ── 3. Web app ────────────────────────────────────────────────────────────────
sep
log "Starting Next.js web app on port $WEB_PORT…"

# Create .env.local for the web app pointing to local backend
cat > "$WEB_DIR/.env.local" <<EOF
NEXT_PUBLIC_API_URL=http://localhost:${BACKEND_PORT}/api
NEXT_PUBLIC_RAZORPAY_KEY_ID=rzp_test_placeholder
EOF
ok ".env.local written to $WEB_DIR/.env.local"

if [[ ! -d "$WEB_DIR/node_modules" ]]; then
  log "Installing web dependencies (npm install)…"
  (cd "$WEB_DIR" && npm install --silent)
  ok "npm install complete."
fi

(
  cd "$WEB_DIR"
  npm run dev -- --port "$WEB_PORT" >> /tmp/openbake_web.log 2>&1
) &
PIDS+=($!)

# Wait until Next.js dev server responds (up to 60 s)
log "Waiting for web app to become ready…"
WEB_READY=false
for i in {1..60}; do
  if curl -sf "http://localhost:$WEB_PORT" > /dev/null 2>&1; then
    WEB_READY=true
    break
  fi
  sleep 1
done

$WEB_READY && ok "Web app is up: http://localhost:$WEB_PORT" || warn "Web app may still be compiling — check /tmp/openbake_web.log"

# ── 4. Android build config ───────────────────────────────────────────────────
sep
log "Updating Android BASE_URL…"

if [[ -n "$NGROK_URL" ]]; then
  ANDROID_API_URL="${NGROK_URL}/api/"
else
  # Physical device on the same network: use machine IP
  LOCAL_IP=$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || echo "10.0.2.2")
  ANDROID_API_URL="http://${LOCAL_IP}:${BACKEND_PORT}/api/"
  warn "ngrok not available. Using local network IP: $ANDROID_API_URL"
  warn "Ensure your Android device/emulator can reach this address."
fi

# Replace buildConfigField BASE_URL in app/build.gradle.kts
# Uses a Python one-liner for safe in-place replacement (avoids sed escaping issues)
python3 - "$ANDROID_GRADLE" "$ANDROID_API_URL" <<'PYEOF'
import sys, re

filepath, new_url = sys.argv[1], sys.argv[2]
with open(filepath, 'r') as f:
    content = f.read()

# Match: buildConfigField("String", "BASE_URL", <any value until closing paren>)
# Handles Kotlin-escaped values like "\"http://...\""
pattern = r'(buildConfigField\("String",\s*"BASE_URL",\s*)([^)]+)(\))'

def make_replacement(m):
    # Write:  buildConfigField("String", "BASE_URL", "\"<url>\"")
    # In Python f-string: "\\" produces one backslash in the output
    return m.group(1) + '"' + '\\"' + new_url + '\\"' + '"' + m.group(3)

new_content, count = re.subn(pattern, make_replacement, content)

if count == 0:
    print(f"WARNING: BASE_URL field not found in {filepath}", file=sys.stderr)
else:
    with open(filepath, 'w') as f:
        f.write(new_content)
    print(f"Updated BASE_URL -> {new_url}")
PYEOF

ok "Android BASE_URL set to: $ANDROID_API_URL"

# ── 5. Android build & install (optional) ────────────────────────────────────
if $BUILD_ANDROID; then
  sep
  log "Building Android debug APK…"

  if [[ ! -f "$ANDROID_DIR/gradlew" ]]; then
    err "gradlew not found at $ANDROID_DIR/gradlew"
    exit 1
  fi

  # Determine Gradle targets based on whether a device is connected
  GRADLE_TARGETS="assembleDebug"
  DEVICE_CONNECTED=false

  if ! command -v adb &>/dev/null; then
    warn "adb not found — APK will be built but not installed. Install Android SDK platform-tools and add to PATH."
  else
    DEVICE_COUNT=$(adb devices 2>/dev/null | tail -n +2 | grep -c "device$" || true)
    if [[ "$DEVICE_COUNT" -lt 1 ]]; then
      warn "No Android device/emulator attached. Starting first available emulator…"

      # Try to start an emulator if avdmanager is available
      if command -v emulator &>/dev/null; then
        FIRST_AVD=$(emulator -list-avds 2>/dev/null | head -n 1)
        if [[ -n "$FIRST_AVD" ]]; then
          log "Launching AVD: $FIRST_AVD"
          emulator -avd "$FIRST_AVD" -no-snapshot-load > /tmp/openbake_emulator.log 2>&1 &
          PIDS+=($!)

          log "Waiting for Android emulator to boot (up to 120 s)…"
          adb wait-for-device
          # Wait for boot animation to finish
          BOOT=""
          for i in {1..60}; do
            BOOT=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r') || BOOT=""
            [[ "$BOOT" == "1" ]] && break
            sleep 2
          done
          if [[ "$BOOT" == "1" ]]; then
            ok "Emulator booted."
            GRADLE_TARGETS="assembleDebug installDebug"
            DEVICE_CONNECTED=true
          else
            warn "Emulator may not be fully booted. Installing APK will be skipped."
          fi
        else
          warn "No AVDs found. Create one in Android Studio and re-run with --android."
        fi
      else
        warn "'emulator' command not in PATH. Start an emulator/device manually and re-run."
      fi
    else
      GRADLE_TARGETS="assembleDebug installDebug"
      DEVICE_CONNECTED=true
    fi
  fi

  log "Running: ./gradlew $GRADLE_TARGETS"
  GRADLE_RC=0
  (
    cd "$ANDROID_DIR"
    # shellcheck disable=SC2086
    ./gradlew $GRADLE_TARGETS 2>&1 | tee /tmp/openbake_gradle.log
  ) || GRADLE_RC=$?

  if [[ $GRADLE_RC -eq 0 ]]; then
    ok "Android APK built${DEVICE_CONNECTED:+ and installed} successfully."
    if $DEVICE_CONNECTED; then
      adb shell am start -n "com.saibabui.openbake/.MainActivity" 2>/dev/null || true
      ok "OpenBake launched on device."
    else
      APK_PATH=$(find "$ANDROID_DIR/app/build/outputs/apk/debug" -name "*.apk" 2>/dev/null | head -n 1 || true)
      [[ -n "$APK_PATH" ]] && ok "APK ready at: $APK_PATH"
    fi
  else
    err "Gradle build failed (exit $GRADLE_RC). See /tmp/openbake_gradle.log"
  fi
fi

# ── Summary ───────────────────────────────────────────────────────────────────
sep
echo ""
echo "${BOLD}${GREEN}  ✅  All services are running  ${RESET}"
echo ""
echo "  ${BOLD}Backend${RESET}    →  http://localhost:${BACKEND_PORT}"
echo "  ${BOLD}API Docs${RESET}   →  http://localhost:${BACKEND_PORT}/docs"
echo "  ${BOLD}Web App${RESET}    →  http://localhost:${WEB_PORT}"
if [[ -n "$NGROK_URL" ]]; then
echo "  ${BOLD}ngrok${RESET}      →  ${NGROK_URL}"
echo "  ${BOLD}ngrok UI${RESET}   →  http://localhost:4040"
fi
echo "  ${BOLD}Android${RESET}    →  BASE_URL = ${ANDROID_API_URL}"
echo ""
echo "  Logs:"
echo "    Backend  →  /tmp/openbake_backend.log"
echo "    Web      →  /tmp/openbake_web.log"
[[ $NGROK_AVAILABLE == true ]] && echo "    ngrok    →  /tmp/openbake_ngrok.log"
$BUILD_ANDROID && echo "    Gradle   →  /tmp/openbake_gradle.log" || true
echo ""
echo "  ${BOLD}Press Ctrl+C to stop all services.${RESET}"
sep
echo ""

# Keep script alive so trap runs on Ctrl+C
wait
