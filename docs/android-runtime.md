# Android runtime boundary

This note explains the future Android implementation of `MinecraftRuntime`. It is for contributors who add the native game runtime.

The current adapter reports that launch preparation and game launch are unavailable. It does not use `ProcessBuilder` or claim launch support.

## Responsibilities

The adapter will own these platform functions:

- Select and start a managed JRE that matches the device architecture.
- Load Minecraft and mod-loader native libraries in an isolated runtime.
- Connect LWJGL calls to an Android-compatible rendering layer.
- Translate touch, keyboard, mouse, controller, and pointer-lock input.
- Connect OpenAL calls to the Android audio system.
- Stream process state, progress, logs, exit status, and cancellation through `LaunchEvent`.
- Keep each instance game directory separate from shared immutable assets and libraries.

Shared code will continue to own metadata, downloads, checksums, instance records, arguments, and launch policy. The adapter must not duplicate that logic.

## Rendering and native libraries

The rendering bridge can use proven PojavLauncher or Amethyst concepts. No source from those projects can enter Trestle without a license review.

The adapter must select libraries by Android ABI. It must reject archives that write outside the native staging directory.

## Managed Java runtimes

Each runtime package needs a version, Java major, ABI, source URL, checksum, and license record. Activation must occur after checksum validation.

The runtime manager must keep the previous valid runtime during a failed update. It must not execute a desktop JRE on Android.

## Lifecycle

Android can stop background work or destroy an activity. The adapter must keep process ownership outside Compose and expose observable lifecycle state.

Cancellation must stop the game process and native threads. A later launch must not reuse stale rendering, input, or audio state.

## Licensing review

Before integration, review the licenses for the runtime, LWJGL bridge, renderers, audio bridge, native libraries, and copied patches. Record each source and license.

Trestle must download Minecraft files from official endpoints. It must not redistribute Mojang files or bypass Microsoft account and ownership checks.
