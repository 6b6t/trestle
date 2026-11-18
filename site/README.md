# Trestle website

The source for [trestle.6b6t.org](https://trestle.6b6t.org) lives here.
The site uses TanStack Start, Fumadocs, and Vite. GitHub Pages serves the static build.

## Run the site locally

Install Bun and Node.js 22.12 or later. From this directory, run:

```bash
bun install --frozen-lockfile
bun run dev
```

Open `http://localhost:3000`. The development server reloads when you edit a page or component.

## Edit a guide

Edit the MDX files in `content/docs/`. Each file needs `title` and `description` fields in its front matter.
Fumadocs supplies code highlighting, copy buttons, callouts, the page contents list, and search.
Keep existing filenames and heading anchors when you update published guides.

Guide URLs use the filename at the site root. For example, `getting-started.mdx` publishes at `/getting-started/`.
Add new guides to the sidebar in `src/lib/documents.ts`.
`download.mdx` is the details section of the downloads page, not a separate guide.
TanStack Start generates `src/routeTree.gen.ts` when you run or build the site. Do not edit that file.

Edit the homepage in `src/routes/index.tsx` and the downloads page in `src/routes/download.tsx`.
Shared site details live in `src/lib/site.ts`. Platform packages and their install steps live in `src/lib/platforms.ts`.
Colors, layout, and animations live in `src/styles/app.css`.

The README and the homepage share the recordings in `../docs/assets/`. The build copies them into the site.
The Jekyll site published guides at `/docs/<name>.html`. `scripts/legacy-pages.ts` keeps redirects for those paths.

## Check the static build

```bash
bun run check
bun run test
bun run build
bun run preview
```

The preview serves `dist/client/` at `http://localhost:3000`. It uses only exported files, including the 404 page.
Set `PORT` to use another preview port.

To run the desktop and mobile browser tests, install their browser once, then test the exported site:

```bash
bunx --no-install playwright install chromium --only-shell
bun run test:e2e
```

Run `bun run build` again before these tests if you change the source. Use `bun run format` to format changes.

The build checks published pages, internal links, heading anchors, canonical URLs, Discord previews, legacy redirects, the sitemap, and the search index.
Search runs in the browser from `search-index.json`. Client navigation loads compiled guide modules without a running application server.

## Refresh downloads and publish

Each build fetches the latest stable GitHub release and selects its packages by filename.
Missing packages use a link to the release instead. If GitHub is unavailable, the site builds with links to GitHub releases.
Set `GITHUB_TOKEN` to authenticate the build request. The token stays in the build process and does not enter the exported site.

The **Publish website** workflow checks every pull request that changes the site.
It publishes after website changes on `main` and after each successful **Release** workflow.
You can also run it manually in GitHub Actions to refresh the release snapshot. Download information stays fixed until the next site build.

The build generates `sitemap.xml`. Place files that need stable public URLs in `public/`.
