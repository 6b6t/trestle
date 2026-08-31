#!/usr/bin/env bash
set -euo pipefail
: "${TRESTLE_APPLE_ID:?Set the Apple ID secret.}"
: "${TRESTLE_APPLE_PASSWORD:?Set an app-specific Apple password.}"
: "${TRESTLE_APPLE_TEAM_ID:?Set the Apple developer team ID.}"
found=false
while IFS= read -r -d '' package; do
  found=true
  xcrun notarytool submit "$package" --apple-id "$TRESTLE_APPLE_ID" --password "$TRESTLE_APPLE_PASSWORD" --team-id "$TRESTLE_APPLE_TEAM_ID" --wait --timeout 30m
  xcrun stapler staple "$package"
  xcrun stapler validate "$package"
done < <(find desktopApp/build/compose/binaries -type f \( -name '*.dmg' -o -name '*.pkg' \) -print0)
if [[ "$found" != true ]]; then
  echo 'No macOS installers were found.' >&2
  exit 1
fi
