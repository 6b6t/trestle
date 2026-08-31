# Android release signing

Trestle signs Android release APKs and app bundles with a dedicated RSA key in a PKCS12 keystore.
Debug builds use the standard Android debug key and do not need release credentials.

## GitHub Actions configuration

The repository uses these Actions secrets:

| Secret | Value |
|---|---|
| `TRESTLE_ANDROID_KEYSTORE_BASE64` | The PKCS12 keystore encoded as base64. |
| `TRESTLE_ANDROID_STORE_PASSWORD` | The keystore password. |
| `TRESTLE_ANDROID_KEY_ALIAS` | The signing key alias. |
| `TRESTLE_ANDROID_KEY_PASSWORD` | The private key password. For this PKCS12 keystore, it matches the keystore password. |

The release workflow restores the keystore into the runner's temporary directory.
It passes the credentials through environment variables and disables the Gradle configuration cache and build cache for the signing build.
It verifies the APK with `apksigner` and the app bundle with `jarsigner` before uploading either artifact.
The workflow removes the temporary keystore even when the build fails.

The signed artifacts use these names:

- `Trestle-<version>-android-arm64.apk`
- `Trestle-<version>-android-x64.apk`
- `Trestle-<version>-android-universal.apk`
- `Trestle-<version>-android-universal.aab`

The universal packages contain ARM64 and x64 libraries. All APK variants use the same application ID, version code, and signing key.
The release collector verifies package ABIs against their native library headers before publication.

## Build a signed release locally

Keep the keystore and credentials outside the repository.
Set these environment variables through a protected local file or a secret manager:

- `TRESTLE_ANDROID_KEYSTORE_PATH`: the absolute path to the PKCS12 keystore.
- `TRESTLE_ANDROID_STORE_PASSWORD`: the keystore password.
- `TRESTLE_ANDROID_KEY_ALIAS`: the signing key alias.
- `TRESTLE_ANDROID_KEY_PASSWORD`: the private key password.

Then run:

```bash
./gradlew :androidApp:assembleRelease :androidApp:bundleRelease \
  --no-configuration-cache --no-build-cache --no-daemon
```

The build fails if a required signing variable is missing.
The APKs are under `androidApp/build/outputs/apk/release`.
The app bundle is under `androidApp/build/outputs/bundle/release`.

## Back up the key

Keep an encrypted backup of the keystore, its password, and its alias outside GitHub and the build machine.
GitHub Actions secrets cannot serve as a downloadable backup.
Never commit the keystore or passwords, print them in logs, or upload them as workflow artifacts.

Use the same signing key for every release.
An APK signed with a different key cannot replace an installed release through a normal update.
The release key also differs from the debug key, so a release APK cannot update a debug installation.
Preserve any needed application data before uninstalling a debug build.

The initial release certificate has this SHA-256 fingerprint:

```text
C4:CD:4C:4B:44:D2:D5:8E:A9:44:FB:F4:BF:21:C9:36:97:61:29:EE:3F:8F:C9:E5:B6:64:CA:08:F5:E1:AF:2A
```

This setup supports direct APK distribution. Google Play distribution also requires Play Console setup and Play App Signing.
See [Android app signing](https://developer.android.com/studio/publish/app-signing) and [GitHub Actions secrets](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets).
