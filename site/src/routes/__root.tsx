import { createRootRoute, HeadContent, Outlet, Scripts } from "@tanstack/react-router";
import { RootProvider } from "fumadocs-ui/provider/tanstack";
import { SiteFooter, SiteHeader } from "../components/shell";
import { lazy } from "react";
import appCss from "../styles/app.css?url";

const SiteSearch = lazy(() => import("../components/search"));

export const Route = createRootRoute({
  head: () => ({
    meta: [
      // oxlint-disable-next-line unicorn/text-encoding-identifier-case -- HTML requires the charset label `utf-8`.
      { charSet: "utf-8" },
      { name: "viewport", content: "width=device-width, initial-scale=1" },
      { name: "theme-color", content: "#141412" },
    ],
    links: [
      { rel: "stylesheet", href: appCss },
      { rel: "icon", href: "/assets/images/mark.svg", type: "image/svg+xml" },
    ],
  }),
  component: Root,
});

function Root() {
  return (
    <html lang="en" className="dark" suppressHydrationWarning>
      <head>
        <HeadContent />
      </head>
      <body>
        <RootProvider
          theme={{ defaultTheme: "dark", storageKey: "trestle-theme" }}
          search={{ SearchDialog: SiteSearch, preload: false }}
        >
          <SiteHeader />
          <Outlet />
          <SiteFooter />
        </RootProvider>
        <Scripts />
      </body>
    </html>
  );
}
