import { copyFile, mkdir, readdir, writeFile } from "node:fs/promises";
import { parseRelease } from "../src/lib/releases";
import { pageUrl, site } from "../src/lib/site";
import { legacyPages, redirectPage } from "./legacy-pages";

async function fetchRelease() {
  try {
    const token = process.env.GITHUB_TOKEN;
    const response = await fetch("https://api.github.com/repos/6b6t/trestle/releases/latest", {
      signal: AbortSignal.timeout(10_000),
      headers: {
        Accept: "application/vnd.github+json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    });
    if (!response.ok) {
      console.warn(
        `Release snapshot unavailable (HTTP ${response.status}); using GitHub release links.`,
      );
      return null;
    }
    const release = parseRelease(await response.json());
    if (!release) console.warn("Release metadata is invalid; using GitHub release links.");
    return release;
  } catch {
    console.warn("Release snapshot unavailable; using GitHub release links.");
    return null;
  }
}

/** The README and the site share these recordings. Keep one copy in docs/assets. */
const showcase = [
  "showcase-library.gif",
  "showcase-discover.gif",
  "showcase-themes.gif",
  "showcase-phone.gif",
];

async function prepare() {
  const release = await fetchRelease();
  await mkdir(".generated", { recursive: true });
  await writeFile(".generated/release.json", `${JSON.stringify(release, null, 2)}\n`);

  await mkdir("public/assets/showcase", { recursive: true });
  for (const file of showcase)
    await copyFile(`../docs/assets/${file}`, `public/assets/showcase/${file}`);

  await mkdir("public/docs", { recursive: true });
  for (const [legacy, target] of Object.entries(legacyPages))
    await writeFile(`public/${legacy}`, redirectPage(target));

  const files = await readdir("content/docs");
  const paths = [
    "/",
    ...files.filter((file) => file.endsWith(".mdx")).map((file) => pageUrl(file.slice(0, -4))),
  ];
  const sitemap = `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${paths
    .toSorted()
    .map((path) => `  <url><loc>${new URL(path, site.url).href}</loc></url>`)
    .join("\n")}\n</urlset>\n`;
  await writeFile("public/sitemap.xml", sitemap);
  console.info(
    `Prepared ${paths.length} public pages${release ? ` with release ${release.tag}` : " with release-link fallbacks"}.`,
  );
}

if (import.meta.main) await prepare();
