![Trestle concept art showing a brass and black bridge across a rocky landscape](docs/assets/trestle-banner.png)

# Trestle

Trestle is a cross-platform Minecraft Java Edition launcher built with Kotlin and Compose Multiplatform.

Trestle provides:

- Isolated, persisted Minecraft instances with atomic registry writes.
- Vanilla, Fabric, NeoForge, Forge, and Quilt metadata resolution and installation.
- Verified Mojang asset, library, native, logging, and client downloads.
- Managed Mojang Java runtimes for desktop launch.
- Modrinth and CurseForge search for mods, modpacks, resource packs, and shaders.
- Installed-content updates, dependency tracking, enable controls, and removal.
- Modrinth, CurseForge, Prism Launcher, and MultiMC pack import.
- Portable instance export with Prism-compatible metadata.
- Instance cloning, grouping, custom icons, launch statistics, and component changes.
- World backups, data-pack controls, server bookmarks, screenshots, and crash reports.
- Microsoft, imported-session, The Altening, and offline account profiles.
- Desktop launch controls, custom Java paths, environment variables, and live console output.
- Android launch support for Vanilla Minecraft 26.2 on compatible 64-bit ARM devices.
- Android touch, keyboard, mouse, and gamepad input.

Android uses a fixed Java 25 and Zink runtime. Android does not support other Minecraft versions or mod loaders yet.

## Targets

- Android 8.0 or newer (API 26)
- Desktop systems that support Java 21

The shared module contains the interface, product logic, network clients, persistence, installer, and domain model. Platform source sets provide runtime adapters and app storage paths.

## Requirements

- JDK 21
- Android SDK 37 for Android builds

Use the included Gradle wrapper. You do not need a separate Gradle installation.

## Run the desktop app

```bash
./gradlew :desktopApp:run
```

## Use the desktop command line

The packaged desktop application also provides command-line operations:

```bash
trestle --list
trestle --install <instance-id>
trestle --launch <instance-id>
trestle --export <instance-id> [archive-path]
```

The launch command writes Minecraft output to the terminal. It returns the game process exit code.

Modrinth works without an access key. CurseForge requires a key that CurseForge issued for Trestle.

If you have an approved Trestle key, set it before you build or run the application:

```bash
export TRESTLE_CURSEFORGE_API_KEY="your-approved-key"
./gradlew :desktopApp:run
```

The Android build reads the same environment variable and adds it to the application manifest. Do not use the key from another launcher.

## Build the Android app

```bash
./gradlew :androidApp:assembleDebug
```

The command creates the debug APK under `androidApp/build/outputs/apk/debug`.

Trestle shows CurseForge as unavailable when the key is not set. Modrinth search and installation remain available.

## Validate changes

Run the shared tests:

```bash
./gradlew :shared:allTests
```

Run Android lint:

```bash
./gradlew :androidApp:lintDebug
```

Compile the desktop app:

```bash
./gradlew :desktopApp:classes
```

GitHub Actions runs these checks for each pull request and each push to `main`.

## Architecture references

- [Android runtime boundary](docs/android-runtime.md)
- [Launcher service endpoints](docs/service-endpoints.md)

## Project structure

```text
androidApp/   Android application and entry point
desktopApp/   Desktop application and entry point
shared/       Shared UI, resources, domain model, and tests
gradle/       Version catalog and Gradle wrapper files
licenses/     Licenses for bundled third-party assets
```

## License

Trestle source code uses the [Apache License 2.0](LICENSE).

The bundled Barlow fonts use the SIL Open Font License 1.1. See [`licenses/OFL-Barlow.txt`](licenses/OFL-Barlow.txt).

The vendored PlatformTools accent readers use the MIT License. See [`licenses/MIT-PlatformTools.txt`](licenses/MIT-PlatformTools.txt).
