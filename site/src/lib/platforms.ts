import type { Release, ReleaseAsset } from "./releases";

export type Platform = "windows" | "macos" | "linux" | "android" | "ios";

interface PackageFormat {
  extension: string;
  label: string;
}

interface Build {
  /** The architecture in release filenames, such as `x64`. */
  arch: string;
  label: string;
  formats: PackageFormat[];
}

export interface PlatformGuide {
  id: Platform;
  name: string;
  requirement: string;
  title: string;
  description: string;
  guideHash: string;
  builds: Build[];
  steps: string[];
}

const desktopSteps = {
  windows: [
    "Run the EXE setup program. Use the MSI package when you deploy Trestle with management tools.",
    "The installers are unsigned, so SmartScreen can warn you. Select More info, then Run anyway.",
    "Open Trestle, add your Microsoft account, and create your first instance.",
  ],
  macos: [
    "Open the disk image and drag Trestle to Applications, or run the installer package.",
    "Trestle is not notarized. If macOS blocks it, open Privacy & Security in System Settings and select Open Anyway.",
    "Open Trestle, add your Microsoft account, and create your first instance.",
  ],
  linux: [
    "Install the DEB or RPM with your package manager, or extract the portable archive.",
    "Start Trestle from your application menu or run trestle in a terminal.",
    "Add your Microsoft account and create your first instance.",
  ],
};

export const platformGuides: Record<Platform, PlatformGuide> = {
  windows: {
    id: "windows",
    name: "Windows",
    requirement: "Windows 10 or newer",
    title: "Installers for Windows 10 and 11.",
    description:
      "Trestle bundles its own Java runtime for the launcher and downloads the right Java version for each Minecraft release.",
    guideHash: "install-on-windows",
    builds: [
      {
        arch: "x64",
        label: "Intel and AMD (x64)",
        formats: [
          { extension: "exe", label: "Setup program" },
          { extension: "msi", label: "MSI package" },
        ],
      },
      {
        arch: "arm64",
        label: "ARM64",
        formats: [
          { extension: "exe", label: "Setup program" },
          { extension: "msi", label: "MSI package" },
        ],
      },
    ],
    steps: desktopSteps.windows,
  },
  macos: {
    id: "macos",
    name: "macOS",
    requirement: "macOS 11 or newer",
    title: "Native builds for Apple silicon and Intel Macs.",
    description:
      "The app is signed ad hoc and not notarized. macOS asks you to confirm the first launch.",
    guideHash: "install-on-macos",
    builds: [
      {
        arch: "arm64",
        label: "Apple silicon",
        formats: [
          { extension: "dmg", label: "Disk image" },
          { extension: "pkg", label: "Installer package" },
        ],
      },
      {
        arch: "x64",
        label: "Intel",
        formats: [
          { extension: "dmg", label: "Disk image" },
          { extension: "pkg", label: "Installer package" },
        ],
      },
    ],
    steps: desktopSteps.macos,
  },
  linux: {
    id: "linux",
    name: "Linux",
    requirement: "glibc 2.35 or newer",
    title: "DEB, RPM, or a portable archive.",
    description:
      "Packages add a desktop entry, an icon, and .mrpack file handling. The portable archive runs from any folder.",
    guideHash: "install-on-linux",
    builds: [
      {
        arch: "x64",
        label: "Intel and AMD (x64)",
        formats: [
          { extension: "deb", label: "Debian and Ubuntu" },
          { extension: "rpm", label: "Fedora and openSUSE" },
          { extension: "tar.gz", label: "Portable archive" },
        ],
      },
      {
        arch: "arm64",
        label: "ARM64",
        formats: [
          { extension: "deb", label: "Debian and Ubuntu" },
          { extension: "rpm", label: "Fedora and openSUSE" },
          { extension: "tar.gz", label: "Portable archive" },
        ],
      },
    ],
    steps: desktopSteps.linux,
  },
  android: {
    id: "android",
    name: "Android",
    requirement: "Android 8.1 or newer",
    title: "Vanilla Minecraft on your phone.",
    description:
      "Android runs Vanilla Minecraft 26.2 on ARM64 and x64 devices with Vulkan 1.2. Other versions and mod loaders are not supported yet.",
    guideHash: "install-on-android",
    builds: [
      {
        arch: "universal",
        label: "Any supported device",
        formats: [{ extension: "apk", label: "Universal APK" }],
      },
      { arch: "arm64", label: "ARM64 only", formats: [{ extension: "apk", label: "APK" }] },
      { arch: "x64", label: "x64 only", formats: [{ extension: "apk", label: "APK" }] },
    ],
    steps: [
      "Download the universal APK on your device. Choose a smaller APK if you know its architecture.",
      "Open the file and allow your browser to install apps when Android asks.",
      "Open Trestle, add your Microsoft account, and create a Minecraft 26.2 instance.",
    ],
  },
  ios: {
    id: "ios",
    name: "iOS",
    requirement: "iOS 16 or newer",
    title: "For iPhone and iPad, signed by you.",
    description:
      "The IPA is unsigned, so sign it before you install it. The TIPA is for TrollStore and jailbroken devices. Game launch needs JIT.",
    guideHash: "install-on-ios",
    builds: [
      {
        arch: "arm64",
        label: "iPhone and iPad (ARM64)",
        formats: [
          { extension: "ipa", label: "Unsigned IPA" },
          { extension: "tipa", label: "TrollStore or jailbreak" },
        ],
      },
    ],
    steps: [
      "Sign the IPA with your own certificate, or install the TIPA with TrollStore.",
      "Open Trestle and add your Microsoft account.",
      "Enable JIT for Trestle before you launch the game.",
    ],
  },
};

export const platformOrder = [
  "windows",
  "macos",
  "linux",
  "android",
  "ios",
] as const satisfies readonly Platform[];

export function isPlatform(value: unknown): value is Platform {
  return typeof value === "string" && (platformOrder as readonly string[]).includes(value);
}

export interface PlatformDownload {
  build: string;
  label: string;
  asset: ReleaseAsset;
}

/** Selects the release files for one platform. Missing files are left out. */
export function platformDownloads(release: Release | null, platform: Platform) {
  if (!release) return [];
  return platformGuides[platform].builds.flatMap(({ arch, label: build, formats }) =>
    formats.flatMap(({ extension, label }): PlatformDownload[] => {
      const name = `Trestle-${release.tag}-${platform}-${arch}.${extension}`;
      const asset = release.assets.find((candidate) => candidate.name === name);
      return asset ? [{ build, label, asset }] : [];
    }),
  );
}

/** Guesses the visitor's platform from the browser. Returns undefined when unsure. */
export function detectPlatform(userAgent: string, touchPoints = 0): Platform | undefined {
  if (/iPhone|iPad|iPod/.test(userAgent)) return "ios";
  // iPadOS reports a Mac user agent, but Macs have no touch screen.
  if (/Macintosh/.test(userAgent)) return touchPoints > 1 ? "ios" : "macos";
  if (/Android/.test(userAgent)) return "android";
  if (/Windows/.test(userAgent)) return "windows";
  if (/Linux|X11|CrOS/.test(userAgent)) return "linux";
  return undefined;
}
