import { expect, test } from "bun:test";
import { detectPlatform, platformDownloads } from "../src/lib/platforms";
import { parseRelease, releaseLink } from "../src/lib/releases";

const tag = "1.2.3";
function asset(name: string, release = tag) {
  return {
    name,
    browser_download_url: `https://github.com/6b6t/trestle/releases/download/${release}/${name}`,
    size: 1024,
    state: "uploaded",
  };
}
function release(assets: ReturnType<typeof asset>[]) {
  return {
    tag_name: tag,
    html_url: `https://github.com/6b6t/trestle/releases/tag/${tag}`,
    published_at: "2026-11-14T12:00:00Z",
    assets,
  };
}

test("each platform lists its packages from the release, grouped by architecture", () => {
  const parsed = parseRelease(
    release([
      asset(`Trestle-${tag}-windows-x64.exe`),
      asset(`Trestle-${tag}-windows-x64.msi`),
      asset(`Trestle-${tag}-windows-arm64.exe`),
      asset(`Trestle-${tag}-macos-arm64.dmg`),
      asset(`Trestle-${tag}-android-universal.apk`),
      asset(`Trestle-${tag}-android-universal.aab`),
      asset(`Trestle-${tag}-linux-x64.tar.gz`),
      asset("SHA256SUMS"),
    ]),
  );
  expect(
    platformDownloads(parsed, "windows").map(({ build, asset }) => [build, asset.name]),
  ).toEqual([
    ["Intel and AMD (x64)", `Trestle-${tag}-windows-x64.exe`],
    ["Intel and AMD (x64)", `Trestle-${tag}-windows-x64.msi`],
    ["ARM64", `Trestle-${tag}-windows-arm64.exe`],
  ]);
  expect(platformDownloads(parsed, "macos").map(({ asset }) => asset.name)).toEqual([
    `Trestle-${tag}-macos-arm64.dmg`,
  ]);
  // The app bundle is for store distribution, so the page offers only the APK.
  expect(platformDownloads(parsed, "android").map(({ asset }) => asset.name)).toEqual([
    `Trestle-${tag}-android-universal.apk`,
  ]);
  expect(platformDownloads(parsed, "linux")[0]?.label).toBe("Portable archive");
  expect(platformDownloads(parsed, "ios")).toEqual([]);
});

test("packages named for another version are not offered", () => {
  const parsed = parseRelease(release([asset("Trestle-1.2.2-windows-x64.exe")]));
  expect(platformDownloads(parsed, "windows")).toEqual([]);
});

test("foreign assets and assets from another tag never enter the snapshot", () => {
  const parsed = parseRelease(
    release([
      asset(`Trestle-${tag}-windows-x64.exe`),
      asset(`Trestle-${tag}-windows-x64.msi`, "1.2.2"),
      {
        ...asset(`Trestle-${tag}-windows-arm64.exe`),
        browser_download_url: "https://example.com/untrusted.exe",
      },
      { ...asset(`Trestle-${tag}-macos-arm64.dmg`), state: "starter" },
    ]),
  );
  expect(parsed?.assets.map(({ name }) => name)).toEqual([`Trestle-${tag}-windows-x64.exe`]);
});

test("missing, draft, preview, or foreign releases leave a GitHub fallback", () => {
  expect(parseRelease(null)).toBeNull();
  expect(parseRelease({ ...release([]), draft: true })).toBeNull();
  expect(parseRelease({ ...release([]), prerelease: true })).toBeNull();
  expect(parseRelease({ ...release([]), html_url: "https://example.com/release" })).toBeNull();
  expect(platformDownloads(null, "linux")).toEqual([]);
  expect(releaseLink(null)).toBe("https://github.com/6b6t/trestle/releases/latest");
  expect(releaseLink(parseRelease(release([])))).toBe(
    `https://github.com/6b6t/trestle/releases/tag/${tag}`,
  );
});

test("browser detection selects the visitor's platform", () => {
  expect(detectPlatform("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")).toBe("windows");
  expect(detectPlatform("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)")).toBe("macos");
  // iPadOS sends a Mac user agent, but reports touch points.
  expect(detectPlatform("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)", 5)).toBe("ios");
  expect(detectPlatform("Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X)")).toBe("ios");
  expect(detectPlatform("Mozilla/5.0 (Linux; Android 14; Pixel 8)")).toBe("android");
  expect(detectPlatform("Mozilla/5.0 (X11; Linux x86_64)")).toBe("linux");
  expect(detectPlatform("curl/8.0")).toBeUndefined();
});
