#!/bin/bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_MODE="${1:-${MATCHORA_RUN_MODE:-local}}"
case "$RUN_MODE" in preview|local|production) ;; *) echo "Unknown MATCHORA_RUN_MODE: $RUN_MODE" >&2; exit 1 ;; esac
if [ "$RUN_MODE" != "preview" ] && [ -z "${MATCHORA_SIMULATOR_UDID:-}" ]; then
  MATCHORA_SIMULATOR_UDID=$(xcrun simctl list devices | grep "(Booted)" | head -n 1 | sed -E 's/.*\(([0-9A-F-]+)\).*/\1/' || true)
fi
if [ "$RUN_MODE" = "local" ] && [ -z "${MATCHORA_API_URL:-}" ]; then
  MATCHORA_API_URL="http://127.0.0.1:8787"
fi
echo "==> Preparing $RUN_MODE framework with API ${MATCHORA_API_URL:-none}..."
"$PROJECT_ROOT/gradlew" -p "$PROJECT_ROOT" :composeApp:linkDebugFrameworkIosSimulatorArm64 \
  -Pmatchora.mode="$RUN_MODE" -Pmatchora.apiUrl="${MATCHORA_API_URL:-}" --console=plain

SDK_PATH=$(xcrun --sdk iphonesimulator --show-sdk-path)
FRAMEWORK_DIR="$PROJECT_ROOT/composeApp/build/bin/iosSimulatorArm64/debugFramework"
OUTPUT_DIR="$PROJECT_ROOT/iosApp/build"
APP_DIR="$OUTPUT_DIR/Matchora.app"

echo "==> Cleaning output..."
rm -rf "$OUTPUT_DIR"
mkdir -p "$APP_DIR/Frameworks"

echo "==> Copying ComposeApp.framework..."
cp -R "$FRAMEWORK_DIR/ComposeApp.framework" "$APP_DIR/Frameworks/"

echo "==> Compiling Swift sources for iOS Simulator..."
# Simulator capabilities are embedded in the Mach-O, as Xcode does. Putting
# device entitlements in the host code signature incorrectly requires a device profile.
SIMULATED_ENTITLEMENTS="$OUTPUT_DIR/Simulator.xcent"
cp "$PROJECT_ROOT/iosApp/Simulator.entitlements" "$SIMULATED_ENTITLEMENTS"
/usr/bin/derq query -f xml -i "$SIMULATED_ENTITLEMENTS" -o "$SIMULATED_ENTITLEMENTS.der" --raw
xcrun -sdk iphonesimulator swiftc \
  -target arm64-apple-ios16.0-simulator \
  -sdk "$SDK_PATH" \
  -F "$FRAMEWORK_DIR" \
  -framework ComposeApp \
  -Xlinker -sectcreate -Xlinker __TEXT -Xlinker __entitlements -Xlinker "$SIMULATED_ENTITLEMENTS" \
  -Xlinker -sectcreate -Xlinker __TEXT -Xlinker __ents_der -Xlinker "$SIMULATED_ENTITLEMENTS.der" \
  -Xlinker -rpath -Xlinker @executable_path/Frameworks \
  -o "$APP_DIR/Matchora" \
  "$PROJECT_ROOT/iosApp/iOSApp.swift" \
  "$PROJECT_ROOT/iosApp/ContentView.swift"

echo "==> Compiling Asset Catalog with actool..."
xcrun actool "$PROJECT_ROOT/iosApp/Assets.xcassets" \
  --compile "$APP_DIR" \
  --platform iphonesimulator \
  --minimum-deployment-target 16.0 \
  --app-icon AppIcon \
  --output-partial-info-plist "$OUTPUT_DIR/partial.plist"

echo "==> Copying Info.plist and App Icons..."
cp "$PROJECT_ROOT/iosApp/Info.plist" "$APP_DIR/Info.plist"
cp "$PROJECT_ROOT"/iosApp/AppIcon*.png "$APP_DIR/" 2>/dev/null || true

cp "$PROJECT_ROOT/iosApp/PrivacyInfo.xcprivacy" "$APP_DIR/PrivacyInfo.xcprivacy"

echo "==> Copying Compose Multiplatform resources..."
mkdir -p "$APP_DIR/compose-resources"
RES_DIR="$PROJECT_ROOT/composeApp/build/generated/compose/resourceGenerator/assembledResources/iosSimulatorArm64Main/composeResources"
if [ -d "$RES_DIR" ]; then
  cp -R "$RES_DIR" "$APP_DIR/compose-resources/"
  cp -R "$RES_DIR" "$APP_DIR/"
fi

echo "==> Code signing..."
SIMULATOR_SIGN_IDENTITY="${MATCHORA_SIMULATOR_SIGN_IDENTITY:--}"
codesign --force --sign "$SIMULATOR_SIGN_IDENTITY" --timestamp=none "$APP_DIR/Frameworks/ComposeApp.framework"
codesign --force --sign "$SIMULATOR_SIGN_IDENTITY" --timestamp=none "$APP_DIR"

echo "==> Build successful: $APP_DIR"

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

xcrun simctl terminate "$BOOTED_ID" com.matchora.app 2>/dev/null || true

echo "==> Installing Matchora.app on simulator..."
xcrun simctl install "$BOOTED_ID" "$APP_DIR"

echo "==> Launching Matchora..."
xcrun simctl launch "$BOOTED_ID" com.matchora.app

echo "==> Done! App is running on iOS Simulator."
