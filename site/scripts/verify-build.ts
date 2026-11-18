import { readdir, readFile, stat } from "node:fs/promises";
import { join, resolve } from "node:path";
import { pageUrl, site } from "../src/lib/site";
import { legacyPages } from "./legacy-pages";

interface HtmlPage {
  path: string;
  html: string;
  ids: Set<string>;
  links: string[];
  h1s: number;
  canonical?: string;
  description?: string;
  discord?: string;
}

async function inspect(path: string, html: string): Promise<HtmlPage> {
  const page: HtmlPage = { path, html, ids: new Set(), links: [], h1s: 0 };
  const parser = new HTMLRewriter()
    .on("[id]", {
      element(element) {
        page.ids.add(element.getAttribute("id")!);
      },
    })
    .on("a[href]", {
      element(element) {
        page.links.push(element.getAttribute("href")!);
      },
    })
    .on("script[src], img[src]", {
      element(element) {
        page.links.push(element.getAttribute("src")!);
      },
    })
    .on("link[href]", {
      element(element) {
        const href = element.getAttribute("href")!;
        page.links.push(href);
        if (element.getAttribute("rel") === "canonical") page.canonical = href;
      },
    })
    .on("h1", {
      element() {
        page.h1s++;
      },
    })
    .on('meta[name="description"]', {
      element(element) {
        page.description = element.getAttribute("content") ?? undefined;
      },
    })
    .on('script[id="discord:component-embed"]', {
      text(chunk) {
        page.discord = (page.discord ?? "") + chunk.text;
      },
    });
  await parser.transform(new Response(html)).text();
  return page;
}

function requireAnchors(pages: Map<string, HtmlPage>, path: string, anchors: string[]) {
  for (const anchor of anchors)
    if (!pages.get(path)?.ids.has(anchor))
      throw new Error(`Missing published anchor: ${path}#${anchor}`);
}

async function verify() {
  const root = resolve("dist/client");
  const docs = (await readdir("content/docs")).filter((file) => file.endsWith(".mdx"));
  const paths = ["/", ...docs.map((file) => pageUrl(file.slice(0, -4)))];
  const pages = new Map<string, HtmlPage>();
  for (const path of paths) {
    const html = await readFile(join(root, path, "index.html"), "utf8");
    const page = await inspect(path, html);
    if (page.h1s !== 1) throw new Error(`${path}: expected one H1, got ${page.h1s}`);
    if (page.canonical !== new URL(path, site.url).href)
      throw new Error(`${path}: missing or incorrect canonical URL`);
    if (!page.description) throw new Error(`${path}: missing page description`);
    if (!page.discord) throw new Error(`${path}: missing Discord component preview`);
    JSON.parse(page.discord);
    if (html.includes("{{") || html.includes("{:toc}") || html.includes("/_serverFn/"))
      throw new Error(`${path}: unresolved template or application-server dependency`);
    pages.set(path, page);
  }
  for (const page of pages.values()) {
    for (const href of page.links) {
      const url = new URL(href, new URL(page.path, site.url));
      if (url.origin !== site.url) continue;
      const targetPath = url.pathname.endsWith("/") ? url.pathname : `${url.pathname}/`;
      const targetPage = pages.get(targetPath);
      if (targetPage) {
        if (url.hash && !targetPage.ids.has(decodeURIComponent(url.hash.slice(1))))
          throw new Error(`${page.path}: missing anchor ${href}`);
      } else if (!(await stat(join(root, url.pathname))).isFile()) {
        throw new Error(`${page.path}: missing static asset ${href}`);
      }
    }
  }
  requireAnchors(pages, "/getting-started/", [
    "install-on-windows",
    "install-on-macos",
    "install-on-linux",
    "install-on-android",
    "install-on-ios",
  ]);
  requireAnchors(pages, "/", ["trestle", "download-trestle", "take-a-look", "questions"]);
  requireAnchors(pages, "/download/", ["download-trestle", "selection-heading"]);

  // Jekyll published guides at /docs/<name>.html. Keep those links working.
  for (const [legacy, target] of Object.entries(legacyPages)) {
    const html = await readFile(join(root, legacy), "utf8");
    if (!pages.has(target) || !html.includes(`url=${target}"`))
      throw new Error(`${legacy}: missing redirect to ${target}`);
  }

  const sitemap = await readFile(join(root, "sitemap.xml"), "utf8");
  for (const path of paths)
    if (!sitemap.includes(`<loc>${new URL(path, site.url).href}</loc>`))
      throw new Error(`Sitemap omits ${path}`);
  const search: unknown = JSON.parse(await readFile(join(root, "search-index.json"), "utf8"));
  if (!search || typeof search !== "object" || !("type" in search))
    throw new Error("Missing static search database");
  const notFound = await readFile(join(root, "404.html"), "utf8");
  if (!notFound.includes("Page not found") || !notFound.includes("noindex"))
    throw new Error("Missing static 404 page");
  for (const asset of ["robots.txt", ".nojekyll", "assets/images/social.jpg"])
    await stat(join(root, asset));
  console.info(
    `Verified ${pages.size} prerendered pages, ${Object.keys(legacyPages).length} legacy redirects, internal links and anchors, metadata, search, sitemap, and static assets.`,
  );
}

if (import.meta.main) await verify();
