import { expect, test } from "bun:test";
import { serializeDiscordEmbed } from "../src/lib/discord-embed";
import { pageEmbed, pageHead } from "../src/lib/metadata";

test("download previews name the release", () => {
  const component = pageEmbed(
    { title: "Downloads", description: "Get Trestle" },
    "download",
    { title: "Trestle", url: "https://example.com", description: "A launcher" },
    "9.8.7",
  );
  if (!component) throw new Error("Expected a download preview");
  const content = serializeDiscordEmbed(component);
  expect(content).toContain("Release 9.8.7");
  expect(content).toContain("https://example.com/download/");
});

test("guide previews and canonical URLs use the published paths", () => {
  const head = pageHead({ title: "iOS launcher target", description: "Build for iOS" }, "ios");
  expect(head.links).toContainEqual({ rel: "canonical", href: "https://launcher.6b6t.org/ios/" });
  expect(head.meta).toContainEqual({
    property: "og:url",
    content: "https://launcher.6b6t.org/ios/",
  });
  expect(head.meta).toContainEqual({ title: "iOS launcher target · Trestle" });
  expect(head.scripts[0]?.children).toContain("https://launcher.6b6t.org/ios/");
});

test("the homepage uses the site name as its title", () => {
  const head = pageHead();
  expect(head.meta).toContainEqual({ title: "Trestle" });
  expect(head.links).toContainEqual({ rel: "canonical", href: "https://launcher.6b6t.org/" });
});

test("Discord preview JSON cannot close its containing script element", () => {
  const head = pageHead(
    { title: "</script><script>alert(1)</script>", description: "A guide" },
    "releases",
  );
  expect(head.scripts[0]?.children).not.toContain("</script>");
  expect(() => JSON.parse(head.scripts[0].children)).not.toThrow();
});
