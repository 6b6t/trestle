# Prepare a release

This guide is for maintainers who build and publish Trestle packages.

## Release requirements

Android packages use the Trestle release key. Windows installers are unsigned. macOS apps use ad hoc signatures on Intel and Apple Silicon.

This signing policy applies to stable and preview releases. macOS apps are not notarized, and desktop systems can show security warnings.

The release workflow records the version commit. All package jobs check out that commit. The release tag points to the same commit.

Each desktop job runs tests and starts the packaged CLI. These checks do not replace Minecraft launch tests or native installer tests.

Before the first stable release:

- Complete the packaging matrix on GitHub Actions.
- Test clean installation, upgrade, file activation, and removal on each supported operating system.
- Confirm that removal preserves the launcher data directory and Minecraft instances.
- Test Minecraft launch, game exit, and relaunch on the supported architectures.
- Test Android 8.1 or newer on supported ARM64 and x64 devices. Android currently supports Vanilla Minecraft 26.2 only.
- Back up a test world before testing pack updates across Minecraft versions.
- Verify Android release signatures, macOS ad hoc signatures, and package checksums.

## Configure signing

Android signing uses the existing dedicated key. Follow [Android signing](android-signing.md) for backup and recovery instructions.

Desktop builds do not need signing secrets. The macOS build signs native libraries, the bundled JVM, and the app with the anonymous `-` identity.
The build verifies signatures in both the DMG and PKG before upload. Installer containers do not carry publisher certificates.

Ad hoc signing does not establish a trusted publisher or pass Apple notarization.
See [Apple’s instructions for opening an unnotarized app](https://support.apple.com/en-us/102445). Do not disable Gatekeeper globally.

The optional `TRESTLE_CURSEFORGE_API_KEY` repository secret enables CurseForge. Use a key issued for Trestle.

macOS native package versions use the numeric build number because `jpackage` rejects zero-major versions. For release `0.1.0`, that number is `1000`.
The launcher, download filenames, and release tag keep `0.1.0`. The app bundle records that release version in `TrestleVersion`.

## Build without publishing

Run the **Build release artifacts** workflow with a numeric version such as `0.1.0`. This workflow uploads artifacts without creating a release.

Android builds produce ARM64, x64, and universal APKs, plus one universal app bundle.
Update notices select the APK for the running process architecture. The app bundle is for store distribution, not direct installation.

After a signed Android build, collect and verify its packages:

```bash
python3 scripts/collect-android-packages.py --version 0.1.0
```

The collector uses Gradle output metadata and validates native ELF headers. It rejects missing ABIs, mismatched versions, and incomplete universal packages.

For local Linux packages:

```bash
./gradlew :desktopApp:createDistributable
python3 scripts/package-linux.py \
  --image desktopApp/build/compose/binaries/main/app/Trestle \
  --version 0.1.0 --arch x64 --output build/release-artifacts
python3 scripts/smoke-test-package.py \
  --root desktopApp/build/compose/binaries/main/app --platform linux
```

The Linux script requires `dpkg-deb` and `rpmbuild`. It produces DEB, RPM, and portable tar archives with a bundled launcher JVM.

Linux ARM64 game runtimes use Eclipse Temurin because Mojang does not provide that platform. Runtime downloads use publisher-provided SHA-256 checksums.

Runtime availability does not guarantee native game-library or renderer support for every Minecraft version.

## Publish

Run the **Release** workflow from the intended branch. Supply a numeric version and select whether the release is a preview.

Publication stops if package builds, Android signing, macOS ad hoc signature checks, or artifact validation fail. Desktop publisher certificates are not required.

The release includes:

- `release-manifest.json`: version, source commit, platform, architecture, download URL, checksum, size, and minimum operating system.
- `SHA256SUMS`: checksums for the binary packages.
- `downloads.html`: a download table generated from the same package metadata.
- A Homebrew cask, WinGet manifests, and a Flatpak manifest with actual package checksums.

GitHub downloads HTML as an asset. Hosting that page as a website is a separate publishing step.

## Distribution integrations

The generated Homebrew, WinGet, and Flatpak files are submission artifacts. The release workflow does not publish to package catalogs automatically.

Validate each artifact with its package manager before submission. Flatpak also needs a sandbox launch test against its declared runtime.

Homebrew installs the application without deleting user data. The Flatpak manifest permits network access, graphics, audio, and the downloads directory.

Desktop packages register `.mrpack` by default. Broad `.jar` and `.zip` associations are optional:

- Gradle packages: set `-Ptrestle.broadFileAssociations=true`.
- Linux packaging script: add `--broad-associations`.

Linux packages include desktop entries, MIME registration, icons, and AppStream metadata. Installer removal scripts refresh these registries without deleting user data.

The AppStream screenshot renders the actual interface with sample instances. It contains no user account data.
