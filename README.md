# Trestle

Trestle is a cross-platform Minecraft Java Edition launcher built with Kotlin and Compose Multiplatform.

The first functional milestone provides:

- Isolated, persisted Minecraft instances with atomic registry writes.
- Vanilla and Fabric metadata resolution and installation.
- Verified Mojang asset, library, native, logging, and client downloads.
- Modrinth and CurseForge file resolvers in shared Kotlin code.
- Desktop launch preparation with safe diagnostics and native extraction.
- An honest Android runtime boundary that does not claim game launch support.

Microsoft authentication is not implemented. Desktop launch validation stops with a sign-in requirement instead of creating an offline account.

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

## Build the Android app

```bash
./gradlew :androidApp:assembleDebug
```

The command creates the debug APK under `androidApp/build/outputs/apk/debug`.

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
