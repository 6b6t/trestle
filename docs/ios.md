# iOS launcher target

This guide is for contributors who build Trestle on macOS. It explains the launcher target and the game-runtime boundary.

## Current boundary

The iOS target provides these launcher features:

- The shared Compose interface for iPhone and iPad.
- Instance creation, installation, content management, and file browsing.
- Microsoft device-code authentication and offline profiles.
- Keychain-backed credential storage.
- iOS file, ZIP extraction, clipboard, and long-press adapters.
- An `IosRuntimeBridge` interface for an in-process Java runtime.

The default Xcode app uses `UnavailableIosRuntimeBridge`. The app can manage instances, but it cannot start Minecraft with this bridge.

Game launch needs a separate runtime bundle. This bundle must contain patched Java, LWJGL, GLFW, audio, and graphics libraries.

Trestle does not include this bundle. The projects below show the required native architecture:

- [PojavLauncher for iOS](https://github.com/PojavLauncherTeam/PojavLauncher_iOS)
- [Amethyst for iOS](https://github.com/AngelAuraMC/Amethyst-iOS)

These projects use GPL-licensed source. Do not copy their source into Trestle without a compatible licensing decision.

## Build the launcher

Use a Mac with Xcode 15.4 or newer and JDK 21.

1. Open `iosApp/Trestle.xcodeproj` in Xcode.
2. Select the `Trestle` target.
3. Select your development team.
4. Change the bundle identifier if your team does not own `net.blockhost.trestle`.
5. Select an iPhone or iPad simulator.
6. Build and launch the app.

The Xcode build phase runs this Gradle task:

```bash
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

You can compile the Kotlin target without Xcode:

```bash
./gradlew :shared:compileKotlinIosSimulatorArm64
```

This command compiles Kotlin code. It does not link or launch the iOS application on Linux.

## Build an unsigned release IPA

Build the device application on a Mac:

```bash
xcodebuild \
  -project iosApp/Trestle.xcodeproj \
  -scheme Trestle \
  -configuration Release \
  -sdk iphoneos \
  -destination 'generic/platform=iOS' \
  -derivedDataPath build/ios \
  ARCHS=arm64 \
  CODE_SIGNING_ALLOWED=NO \
  build
```

Then package the application:

```bash
python3 scripts/package-ios.py \
  --app build/ios/Build/Products/Release-iphoneos/Trestle.app \
  --version 0.1.0 \
  --output build/release-artifacts
```

The generated IPA has no signature. Sign it with your Apple account and provisioning profile before installation.

## Connect a game runtime

Implement `IosRuntimeBridge` in an iOS host library. Pass the implementation to `TrestleViewController` from `TrestleApp.swift`.

The bridge must provide these operations:

- Report JIT and runtime availability.
- Select a runtime for the required Java major version.
- Return the patched classpath and native-library directory.
- Start `JLI_Launch` in the application process.
- Send start, output, exit, and error events to `IosJvmLaunchObserver`.
- Stop the active runtime when Trestle cancels the launch.

The bridge owns the game surface, input, audio, renderer, and Java lifecycle. Trestle owns installation, authentication, and launch arguments.

The runtime descriptor must exclude normal desktop LWJGL files. It must provide patched iOS LWJGL files instead.

## Signing and JIT

Minecraft needs JIT for usable Java performance on iOS. Standard App Store signing does not enable unrestricted JIT.

The Xcode target contains these public memory entitlements:

- `com.apple.developer.kernel.extended-virtual-addressing`
- `com.apple.developer.kernel.increased-memory-limit`

Your Apple account and provisioning profile must permit these entitlements. Remove an entitlement if your profile rejects it.

The project does not contain private Apple entitlements, sandbox bypasses, or automatic JIT exploits. A runtime bridge must report a clear error when JIT is unavailable.

For development, Xcode can attach LLDB to the application. Other installation methods need their own legal JIT activation process.

## Platform notes

- The target minimum is iOS 16.
- The device runtime target is ARM64.
- The simulator target needs an Apple silicon Mac. It can validate launcher workflows, but it cannot prove game, JIT, GPU, audio, or input compatibility.
- CurseForge stays unavailable because the iOS project does not embed a Trestle API key.
- ZIP instance export and advanced world edits remain unavailable in the first iOS target.
