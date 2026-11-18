import { createFileRoute, Link, useHydrated } from "@tanstack/react-router";
import release from "virtual:trestle-release";
import { Suspense, useState } from "react";
import { NextSteps, PlatformStage } from "../components/downloads";
import { useDetectedPlatform } from "../components/use-detected-platform";
import { getDocument } from "../lib/documents";
import { documentLoader } from "../lib/document-content";
import { pageHead } from "../lib/metadata";
import { isPlatform, platformGuides, type Platform } from "../lib/platforms";
import { releaseLink } from "../lib/releases";
import { site } from "../lib/site";

const page = getDocument("download")!;

export const Route = createFileRoute("/download")({
  validateSearch: (search: Record<string, unknown>): { platform?: Platform } =>
    isPlatform(search.platform) ? { platform: search.platform } : {},
  loader: async () => {
    await (await import("../lib/document-content")).documentLoader.preload(page.path);
  },
  head: () => pageHead(page, "download", release?.tag),
  component: Downloads,
});

function Downloads() {
  const search = Route.useSearch();
  const hydrated = useHydrated();
  const [detected, setDetected] = useState<Platform>();
  useDetectedPlatform(setDetected, !search.platform);
  // Static hosts serve the same HTML for every query. Match that HTML during
  // hydration, then apply the platform from the link or the browser.
  const platform = hydrated ? (search.platform ?? detected ?? "windows") : "windows";
  const navigate = Route.useNavigate();
  const Content = documentLoader.getComponent(page.path);
  return (
    <main id="main-content" className="downloads">
      <div className="page-heading shell-width">
        <h1 id="download-trestle">Download Trestle</h1>
        <p>
          Every package comes from the same GitHub release. Pick your platform, then the package
          that fits your system.
        </p>
      </div>
      <noscript>
        <p className="noscript-note shell-width">
          For all packages, <a href={releaseLink(release)}>open the release assets</a> and{" "}
          <a href="/getting-started/">follow the install guide</a>.
        </p>
      </noscript>
      <div className="shell-width">
        <PlatformStage
          platform={platform}
          release={release}
          hydrated={hydrated}
          heading="Pick a platform"
          headingId="selection-heading"
          onChange={(id) => {
            void navigate({ search: { platform: id }, resetScroll: false });
          }}
        />
      </div>
      <section className="steps-section shell-width" aria-labelledby="after-download">
        <div className="section-heading">
          <h2 id="after-download">After the download</h2>
          <p>
            <Link
              to="/$slug/"
              params={{ slug: "getting-started" }}
              hash={platformGuides[platform].guideHash}
            >
              Read the install guide
            </Link>{" "}
            or <a href={site.issues}>report a problem on GitHub</a>.
          </p>
        </div>
        <NextSteps platform={platform} />
      </section>
      <section className="download-details shell-width" aria-label="Package details">
        <Suspense>
          <Content slug="download" details />
        </Suspense>
      </section>
    </main>
  );
}
