#!/usr/bin/env bash
set -euo pipefail
: "${TRESTLE_MAC_CERTIFICATE_BASE64:?Set the macOS certificate bundle secret.}"
: "${TRESTLE_MAC_CERTIFICATE_PASSWORD:?Set the certificate bundle password.}"
: "${TRESTLE_MAC_SIGNING_IDENTITY:?Set the Developer ID signing identity.}"
umask 077
signing_dir="$RUNNER_TEMP/trestle-signing"
mkdir -p "$signing_dir"
printf '%s' "$TRESTLE_MAC_CERTIFICATE_BASE64" | base64 --decode > "$signing_dir/certificates.p12"
keychain_password="$(openssl rand -hex 32)"
keychain="$signing_dir/release.keychain-db"
security create-keychain -p "$keychain_password" "$keychain"
security set-keychain-settings -lut 21600 "$keychain"
security unlock-keychain -p "$keychain_password" "$keychain"
security import "$signing_dir/certificates.p12" -k "$keychain" -P "$TRESTLE_MAC_CERTIFICATE_PASSWORD" -T /usr/bin/codesign -T /usr/bin/productbuild
security set-key-partition-list -S apple-tool:,apple: -k "$keychain_password" "$keychain" >/dev/null
printf 'TRESTLE_MAC_KEYCHAIN_PATH=%s\n' "$keychain" >> "$GITHUB_ENV"
rm "$signing_dir/certificates.p12"
