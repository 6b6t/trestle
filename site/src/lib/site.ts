export const site = {
  title: "Trestle",
  url: "https://launcher.6b6t.org",
  description:
    "A Minecraft Java Edition launcher for Windows, macOS, Linux, Android, and iOS, with isolated instances and modpack support.",
  github: "https://github.com/6b6t/trestle",
  latestRelease: "https://github.com/6b6t/trestle/releases/latest",
  releases: "https://github.com/6b6t/trestle/releases",
  issues: "https://github.com/6b6t/trestle/issues",
} as const;

export const homePage = { title: "Home", description: site.description };

export interface PageMetadata {
  title: string;
  description: string;
}

export function pageUrl(slug: string) {
  return slug === "index" ? "/" : `/${slug}/`;
}
