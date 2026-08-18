# Android runtime

This reference describes the Android implementation of `MinecraftRuntime`.

## Support boundary

The Android runtime supports Vanilla Minecraft 26.2 on 64-bit ARM devices. The device must support Vulkan 1.2 or newer.

The runtime does not support other Minecraft versions or mod loaders. The launcher hides unsupported choices on Android.

## Runtime preparation

Trestle downloads a Java 25 runtime for Android. It validates the archive with SHA-256 before extraction.

Trestle also downloads the Android LWJGL, OpenAL, Zink, and native bridge components. Each component has a fixed source revision and checksum.

The runtime activates a component set only after all required files exist. A partial download cannot replace a complete component set.

## Game process

`MinecraftGameActivity` owns the game surface and Java virtual machine. It starts the virtual machine through the Android native bridge.

The launcher passes these values to the activity:

- The Java home directory.
- The game working directory.
- The native-library directory.
- JVM and game arguments.
- The main class.
- Environment variables.
- A result receiver for process events.

The activity sends start, log, exit, and error events to the launcher. The launcher shows these events through `LaunchEvent`.

## Rendering and audio

The current renderer uses Mesa Zink and Kopper. It translates desktop OpenGL calls to Vulkan.

The native component set includes the patched LWJGL bridge and OpenAL Soft. The activity loads these libraries before it starts Minecraft.

## Input

The game activity supports these input sources:

- The touch overlay for movement, camera control, inventory, combat, chat, and hotbar selection.
- Android text input for chat and command text.
- Hardware keyboards with GLFW key and modifier translation.
- Mouse movement, buttons, wheel input, and pointer grab.
- Gamepads with movement, camera, action buttons, triggers, and hotbar selection.

The activity releases held input when Android destroys the game activity.

## Process errors

The activity writes JVM errors to the instance crash directory. Minecraft can also write reports to `crash-reports`.

If the process stops without an exit event, Trestle reads Android process-exit information. It reports native errors, JVM errors, low-memory termination, and ANR events.

## Licensing

The runtime uses components from Amethyst and Android OpenJDK builds at fixed revisions. The source repository records the required licenses.

Trestle downloads Minecraft files from official endpoints. It does not redistribute Mojang files or bypass ownership checks.
