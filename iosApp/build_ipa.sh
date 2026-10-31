#!/bin/bash
# App Store Connect distribution only. Never substitutes a development or local signature.
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IOSAPP_DIR="$PROJECT_ROOT/iosApp"
OUTPUT_DIR="$IOSAPP_DIR/build/distribution"
TEAM_ID="${MATCHORA_TEAM_ID:-XV6VPKTQAS}"
ARCHIVE_PATH="$OUTPUT_DIR/Matchora.xcarchive"
EXPORT_PATH="$OUTPUT_DIR/export"
mkdir -p "$OUTPUT_DIR"

xcodebuild -project "$IOSAPP_DIR/Matchora.xcodeproj" -scheme Matchora \
  -configuration Release -destination 'generic/platform=iOS' \
  -archivePath "$ARCHIVE_PATH" -derivedDataPath "$OUTPUT_DIR/DerivedData" \
  DEVELOPMENT_TEAM="$TEAM_ID" -allowProvisioningUpdates archive

# Include Kotlin symbols as well as the Swift symbols captured by Xcode.
FRAMEWORK_DSYM="$PROJECT_ROOT/composeApp/build/bin/iosArm64/releaseFramework/ComposeApp.framework.dSYM"
if [ -d "$FRAMEWORK_DSYM" ]; then
  mkdir -p "$ARCHIVE_PATH/dSYMs"
  ditto "$FRAMEWORK_DSYM" "$ARCHIVE_PATH/dSYMs/ComposeApp.framework.dSYM"
fi

# Use an isolated export plist so choosing another team does not modify source files.
cp "$IOSAPP_DIR/ExportOptions.plist" "$OUTPUT_DIR/ExportOptions.plist"
/usr/libexec/PlistBuddy -c "Set :teamID $TEAM_ID" "$OUTPUT_DIR/ExportOptions.plist"
xcodebuild -exportArchive -archivePath "$ARCHIVE_PATH" \
  -exportPath "$EXPORT_PATH" -exportOptionsPlist "$OUTPUT_DIR/ExportOptions.plist" \
  -allowProvisioningUpdates

if [ ! -f "$EXPORT_PATH/Matchora.ipa" ]; then
  echo 'App Store Connect export did not produce Matchora.ipa.' >&2
  exit 1
fi
printf 'App Store Connect IPA: %s\n' "$EXPORT_PATH/Matchora.ipa"
