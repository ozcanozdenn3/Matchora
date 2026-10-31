#!/bin/bash
set -e

SDK_PATH=$(xcrun --sdk iphonesimulator --show-sdk-path)
FRAMEWORK_DIR="/Users/md/Desktop/Matchora/composeApp/build/bin/iosSimulatorArm64/debugFramework"
OUTPUT_DIR="/Users/md/Desktop/Matchora/iosApp/build"
APP_DIR="$OUTPUT_DIR/Matchora.app"

echo "==> Cleaning output..."
rm -rf "$OUTPUT_DIR"
mkdir -p "$APP_DIR/Frameworks"

echo "==> Copying ComposeApp.framework..."
cp -R "$FRAMEWORK_DIR/ComposeApp.framework" "$APP_DIR/Frameworks/"

echo "==> Compiling Swift sources for iOS Simulator..."
xcrun -sdk iphonesimulator swiftc \
  -target arm64-apple-ios16.0-simulator \
  -sdk "$SDK_PATH" \
  -F "$FRAMEWORK_DIR" \
  -framework ComposeApp \
  -Xlinker -rpath -Xlinker @executable_path/Frameworks \
  -o "$APP_DIR/Matchora" \
  /Users/md/Desktop/Matchora/iosApp/iOSApp.swift \
  /Users/md/Desktop/Matchora/iosApp/ContentView.swift

echo "==> Compiling Asset Catalog with actool..."
xcrun actool /Users/md/Desktop/Matchora/iosApp/Assets.xcassets \
  --compile "$APP_DIR" \
  --platform iphonesimulator \
  --minimum-deployment-target 16.0 \
  --app-icon AppIcon \
  --output-partial-info-plist "$OUTPUT_DIR/partial.plist"

echo "==> Copying Info.plist and App Icons..."
cp /Users/md/Desktop/Matchora/iosApp/Info.plist "$APP_DIR/Info.plist"
cp /Users/md/Desktop/Matchora/iosApp/AppIcon*.png "$APP_DIR/" 2>/dev/null || true

echo "==> Copying Compose Multiplatform resources..."
mkdir -p "$APP_DIR/compose-resources"
RES_DIR="/Users/md/Desktop/Matchora/composeApp/build/generated/compose/resourceGenerator/assembledResources/iosSimulatorArm64Main/composeResources"
if [ -d "$RES_DIR" ]; then
  cp -R "$RES_DIR" "$APP_DIR/compose-resources/"
  cp -R "$RES_DIR" "$APP_DIR/"
fi

echo "==> Code signing..."
codesign --force --sign - --timestamp=none "$APP_DIR/Frameworks/ComposeApp.framework"
codesign --force --sign - --timestamp=none "$APP_DIR"

echo "==> Build successful: $APP_DIR"

echo "==> Preparing iOS Simulator..."
BOOTED_ID=$(xcrun simctl list devices | grep "(Booted)" | head -n 1 | sed -E 's/.*\(([0-9A-F-]+)\).*/\1/')
if [ -z "$BOOTED_ID" ]; then
  TARGET_DEVICE=$(xcrun simctl list devices available | grep "iPhone 17 (" | head -n 1 | sed -E 's/.*\(([0-9A-F-]+)\).*/\1/')
  if [ -z "$TARGET_DEVICE" ]; then
    TARGET_DEVICE=$(xcrun simctl list devices available | grep "iPhone" | head -n 1 | sed -E 's/.*\(([0-9A-F-]+)\).*/\1/')
  fi
  echo "==> Booting device $TARGET_DEVICE..."
  xcrun simctl boot "$TARGET_DEVICE"
  BOOTED_ID="$TARGET_DEVICE"
fi

echo "==> Opening Simulator window..."
open -a Simulator

echo "==> Waiting for simulator to be ready..."
xcrun simctl bootstatus "$BOOTED_ID" -b

echo "==> Uninstalling older version (to clear icon cache)..."
xcrun simctl uninstall "$BOOTED_ID" com.matchora.app 2>/dev/null || true

echo "==> Installing Matchora.app on simulator..."
xcrun simctl install "$BOOTED_ID" "$APP_DIR"

echo "==> Launching Matchora..."
xcrun simctl launch "$BOOTED_ID" com.matchora.app

echo "==> Done! App is running on iOS Simulator."
