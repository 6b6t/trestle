import { site } from "../src/lib/site";

/** Paths that the Jekyll site published, mapped to their current pages. */
export const legacyPages: Record<string, string> = {
  "docs/android-runtime.html": "/android-runtime/",
  "docs/android-signing.html": "/android-signing/",
  "docs/content-updates.html": "/content-updates/",
  "docs/ios.html": "/ios/",
  "docs/releases.html": "/releases/",
  "docs/service-endpoints.html": "/service-endpoints/",
};

export function redirectPage(target: string) {
  const url = new URL(target, site.url).href;
  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>Moved · ${site.title}</title>
<meta name="robots" content="noindex">
<link rel="canonical" href="${url}">
<meta http-equiv="refresh" content="0; url=${target}">
</head>
<body>
<p>This page moved to <a href="${target}">${url}</a>.</p>
</body>
</html>
`;
}
