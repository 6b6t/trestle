import { createDiscordEmbed, serializeDiscordEmbed } from "./discord-embed";
import { homePage, pageUrl, site, type PageMetadata } from "./site";

export function pageEmbed(
  page: { title: string; description?: string },
  slug: string,
  website: { title: string; url: string; description: string } = site,
  release?: string,
) {
  return createDiscordEmbed({
    title: `${page.title} · ${website.title}`,
    description: [
      page.description ?? website.description,
      ...(slug === "download" && release ? [`Release ${release}`] : []),
    ].join("\n"),
    url: new URL(pageUrl(slug), website.url).href,
    label: slug === "download" ? "View downloads" : slug === "index" ? "Visit site" : "Read guide",
    links: [
      { label: "Downloads", url: new URL("/download/", website.url).href },
      { label: "GitHub", url: site.github },
    ],
  });
}

export function pageHead(page: PageMetadata = homePage, slug = "index", release?: string) {
  const title = page.title === "Home" ? site.title : `${page.title} · ${site.title}`;
  const url = new URL(pageUrl(slug), site.url).href;
  const image = new URL("/assets/images/social.jpg", site.url).href;
  const component = pageEmbed(page, slug, site, release);
  return {
    meta: [
      { title },
      { name: "description", content: page.description },
      { property: "og:type", content: "website" },
      { property: "og:site_name", content: site.title },
      { property: "og:title", content: title },
      { property: "og:description", content: page.description },
      { property: "og:url", content: url },
      { property: "og:image", content: image },
      { name: "twitter:card", content: "summary_large_image" },
      { name: "twitter:title", content: title },
      { name: "twitter:description", content: page.description },
      { name: "twitter:image", content: image },
    ],
    links: [{ rel: "canonical", href: url }],
    scripts: component
      ? [
          {
            id: "discord:component-embed",
            type: "application/json",
            children: serializeDiscordEmbed(component),
          },
        ]
      : [],
  };
}
