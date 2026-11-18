import type { Root } from "fumadocs-core/page-tree";
import { pageUrl, type PageMetadata } from "./site";

const metadata = import.meta.glob<{ frontmatter: PageMetadata }>("../../content/docs/*.mdx", {
  eager: true,
  query: "?only=frontmatter",
});

export const documents = Object.entries(metadata).map(([path, { frontmatter }]) => ({
  ...frontmatter,
  path,
  slug: path.split("/").at(-1)!.slice(0, -4),
}));

export function getDocument(slug: string) {
  return documents.find((page) => page.slug === slug);
}

/** Short sidebar names. Pages without one use their title. */
const names: Record<string, string> = {
  "getting-started": "Install and play",
  download: "Downloads",
  "command-line": "Command line",
  "content-updates": "Updates and content",
  "build-from-source": "Build from source",
  "android-runtime": "Android runtime",
  ios: "iOS target",
  "service-endpoints": "Service endpoints",
  releases: "Release process",
  "android-signing": "Android signing",
};

const play = ["getting-started", "download", "content-updates", "command-line"];
const develop = [
  "build-from-source",
  "android-runtime",
  "ios",
  "service-endpoints",
  "releases",
  "android-signing",
];

export const pageTree: Root = {
  name: "Documentation",
  children: [
    { type: "separator", name: "Use Trestle" },
    ...play.map(item),
    { type: "separator", name: "Develop" },
    ...develop.map(item),
  ],
};

function item(slug: string) {
  const page = getDocument(slug);
  if (!page) throw new Error(`Missing documentation: ${slug}`);
  return { type: "page" as const, name: names[slug] ?? page.title, url: pageUrl(slug) };
}
