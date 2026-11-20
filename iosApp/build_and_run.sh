#!/bin/bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_MODE="${1:-${MATCHORA_RUN_MODE:-production}}"
case "$RUN_MODE" in preview|local|production) ;; *) echo "Unknown MATCHORA_RUN_MODE: $RUN_MODE" >&2; exit 1 ;; esac

if [ "$RUN_MODE" = "local" ] && [ -z "${MATCHORA_API_URL:-}" ]; then
  MATCHORA_API_URL="http://127.0.0.1:8787"
elif [ "$RUN_MODE" = "production" ] && [ -z "${MATCHORA_API_URL:-}" ]; then
  MATCHORA_API_URL="https://rqtppytkoetwnjyfxulw.supabase.co/functions/v1/matchora-api"
fi

echo "==> Preparing iOS Simulator..."
BOOTED_ID="${MATCHORA_SIMULATOR_UDID:-}"
if [ -z "$BOOTED_ID" ]; then
  BOOTED_ID=$(xcrun simctl list devices | grep "(Booted)" | head -n 1 | sed -E 's/.*\(([0-9A-F-]+)\).*/\1/' || true)
fi
if [ -z "$BOOTED_ID" ]; then
  TARGET_DEVICE=$(xcrun simctl list devices available | grep "iPhone 17 (" | head -n 1 | sed -E 's/.*\(([0-9A-F-]+)\).*/\1/' || true)
  if [ -z "$TARGET_DEVICE" ]; then
    TARGET_DEVICE=$(xcrun simctl list devices available | grep "iPhone" | head -n 1 | sed -E 's/.*\(([0-9A-F-]+)\).*/\1/')
  fi
  echo "==> Booting device $TARGET_DEVICE..."
  xcrun simctl boot "$TARGET_DEVICE"
  BOOTED_ID="$TARGET_DEVICE"
fi

echo "==> Opening Simulator window..."
if [ "${MATCHORA_OPEN_SIMULATOR:-true}" = "true" ]; then open -a Simulator; fi

echo "==> Waiting for simulator to be ready..."
xcrun simctl bootstatus "$BOOTED_ID" -b

echo "==> Building Matchora ($RUN_MODE -> ${MATCHORA_API_URL:-none}) with xcodebuild..."
export MATCHORA_RUN_MODE="$RUN_MODE"
export MATCHORA_API_URL="${MATCHORA_API_URL:-}"
xcodebuild \
  -project "$PROJECT_ROOT/iosApp/Matchora.xcodeproj" \
  -scheme Matchora \
  -configuration Debug \
  -destination "id=$BOOTED_ID" \
  -derivedDataPath "$PROJECT_ROOT/iosApp/DerivedData" \
  build

APP_DIR="$PROJECT_ROOT/iosApp/DerivedData/Build/Products/Debug-iphonesimulator/Matchora.app"

xcrun simctl terminate "$BOOTED_ID" com.ozdentech.matchora 2>/dev/null || true

echo "==> Installing Matchora.app on simulator..."
xcrun simctl install "$BOOTED_ID" "$APP_DIR"

echo "==> Launching Matchora..."
xcrun simctl launch "$BOOTED_ID" com.ozdentech.matchora

echo "==> Done! App is running on iOS Simulator."
