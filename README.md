# Trestle

Trestle is an early cross-platform Minecraft launcher built with Kotlin and Compose Multiplatform.

The current prototype provides a responsive library for Minecraft instances. It also includes placeholders for discovery and launcher configuration. Game installation and launch adapters are not connected yet.

## Targets

- Android 8.0 or newer (API 26)
- Desktop systems that support Java 21

The shared module contains the Compose UI and domain model. Small platform modules provide the Android and desktop entry points.

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
