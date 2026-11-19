import { site } from "./site";

export interface ReleaseAsset {
  name: string;
  url: string;
  size: number;
}

export interface Release {
  tag: string;
  url: string;
  publishedAt: string;
  assets: ReleaseAsset[];
}

const repository = "/6b6t/trestle";

function githubUrl(value: unknown, prefix: string) {
  if (typeof value !== "string") return;
  let url: URL;
  try {
    url = new URL(value);
  } catch {
    return;
  }
  return url.origin === "https://github.com" &&
    url.pathname.startsWith(prefix) &&
    !url.username &&
    !url.password
    ? url.href
    : undefined;
}

/** Reads a GitHub release response. Drafts, previews, and foreign links return null. */
export function parseRelease(value: unknown): Release | null {
  if (!value || typeof value !== "object") return null;
  if (
    !("tag_name" in value) ||
    typeof value.tag_name !== "string" ||
    !("html_url" in value) ||
    !("assets" in value) ||
    !Array.isArray(value.assets) ||
    ("draft" in value && value.draft) ||
    ("prerelease" in value && value.prerelease)
  )
    return null;
  const url = githubUrl(value.html_url, `${repository}/releases/tag/`);
  if (!url) return null;
  const tag = value.tag_name;
  // A release snapshot must never combine assets from different release tags.
  const expected = `${repository}/releases/download/${encodeURIComponent(tag)}/`;
  const assets = value.assets.flatMap((asset: unknown): ReleaseAsset[] => {
    if (!asset || typeof asset !== "object") return [];
    if (
      !("name" in asset) ||
      typeof asset.name !== "string" ||
      !("browser_download_url" in asset) ||
      !("size" in asset) ||
      typeof asset.size !== "number" ||
      asset.size < 0 ||
      ("state" in asset && asset.state !== "uploaded")
    )
      return [];
    const download = githubUrl(asset.browser_download_url, expected);
    return download ? [{ name: asset.name, url: download, size: asset.size }] : [];
  });
  return {
    tag,
    url,
    publishedAt:
      "published_at" in value && typeof value.published_at === "string" ? value.published_at : "",
    assets,
  };
}

export function releaseLink(release: Release | null) {
  return release?.url ?? site.latestRelease;
}

export function formatSize(bytes: number) {
  return `${(bytes / 1024 / 1024).toFixed(bytes >= 10 * 1024 * 1024 ? 0 : 1)} MB`;
}
