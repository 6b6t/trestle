import { createFileRoute, Link, useHydrated } from "@tanstack/react-router";
import {
  ArrowDownToLine,
  Boxes,
  Globe,
  Languages,
  Layers,
  PackageOpen,
  Server,
  ShieldCheck,
  UserRound,
} from "lucide-react";
import release from "virtual:trestle-release";
import { useState } from "react";
import { NextSteps, PlatformStage } from "../components/downloads";
import { Faq } from "../components/faq";
import { Showcase } from "../components/showcase";
import { useDetectedPlatform } from "../components/use-detected-platform";
import { pageHead } from "../lib/metadata";
import { platformGuides, type Platform } from "../lib/platforms";
import { homePage, site } from "../lib/site";

export const Route = createFileRoute("/")({
  head: () => pageHead(homePage, "index"),
  component: Home,
});

const releaseDate = release?.publishedAt
  ? new Intl.DateTimeFormat("en", { dateStyle: "long", timeZone: "UTC" }).format(
      new Date(release.publishedAt),
    )
  : undefined;

const features = [
  {
    icon: Layers,
    title: "Isolated instances",
    text: "Each instance keeps its own version, loader, content, worlds, and settings. Clone, group, and export them.",
  },
  {
    icon: Boxes,
    title: "Every common loader",
    text: "Install Vanilla, Fabric, NeoForge, Forge, or Quilt. Trestle verifies each library and game file it downloads.",
  },
  {
    icon: Globe,
    title: "Six content catalogs",
    text: "Search Modrinth, CurseForge, ATLauncher, FTB, FTB Legacy, and Technic, then install into the instance you pick.",
  },
  {
    icon: PackageOpen,
    title: "Modpacks that update safely",
    text: "Preview pack updates, keep file backups, and roll back. Import Modrinth, CurseForge, Prism Launcher, and MultiMC packs.",
  },
  {
    icon: Server,
    title: "Worlds and servers",
    text: "Back up, copy, and rename worlds. Check server status and player counts, and join a server directly.",
  },
  {
    icon: UserRound,
    title: "Your accounts",
    text: "Sign in with Microsoft, import a session, or play offline. Pick an account per instance.",
  },
];

const questions = [
  {
    question: "Is Trestle free?",
    answer:
      "Yes. Trestle is free software under the GNU GPL version 3. You can read and build all of its source code on GitHub.",
  },
  {
    question: "Do I need to own Minecraft?",
    answer:
      "Yes. To play online, sign in with the Microsoft account that owns Minecraft Java Edition. Trestle can sign in with a device code in your browser, so it never needs to see your password.",
  },
  {
    question: "Why does my system warn me about the installer?",
    answer:
      "The Windows installers are unsigned, and the macOS app is signed ad hoc without notarization. Windows and macOS warn about apps from publishers they do not know. The install guide shows how to continue.",
  },
  {
    question: "What works on Android and iOS?",
    answer:
      "Android runs Vanilla Minecraft 26.2 on devices with Vulkan 1.2. On iOS, the launcher works on iOS 16 and newer, and game launch needs JIT. Mod loaders on mobile are not supported yet.",
  },
  {
    question: "Can I bring my existing modpacks?",
    answer:
      "Yes. Import Modrinth and CurseForge packs, or instances from Prism Launcher and MultiMC. Trestle identifies imported mods by file hash, so updates work for them too.",
  },
  {
    question: "How does Trestle update?",
    answer:
      "Trestle checks for a new release at most once a day and shows the release notes with a download for your system. It never installs an update without you.",
  },
];

function Home() {
  const hydrated = useHydrated();
  const [platform, setPlatform] = useState<Platform>("windows");
  useDetectedPlatform(setPlatform);
  return (
    <main id="main-content" className="home">
      <section className="hero">
        <div className="shell-width">
          <img
            className="hero-art"
            src="/assets/images/banner.webp"
            alt=""
            width={1800}
            height={600}
            fetchPriority="high"
          />
        </div>
        <div className="hero-intro shell-width">
          <h1 id="trestle">One Minecraft launcher for every device you play on</h1>
          <div className="hero-aside">
            <p className="lead">
              Trestle keeps every Minecraft Java Edition setup in its own instance. Install mods and
              modpacks from six catalogs on Windows, macOS, Linux, Android, and iOS.
            </p>
            <ul className="hero-facts">
              <li>
                <ShieldCheck size={18} aria-hidden="true" />
                Free and open source
              </li>
              <li>
                <Languages size={18} aria-hidden="true" />
                10 interface languages
              </li>
            </ul>
          </div>
        </div>
        <div className="shell-width">
          <PlatformStage
            platform={platform}
            onChange={setPlatform}
            release={release}
            hydrated={hydrated}
            heading="Get Trestle"
            headingId="download-trestle"
          />
        </div>
      </section>

      <section className="steps-section shell-width" aria-labelledby="first-launch">
        <div className="section-heading">
          <h2 id="first-launch">From download to first launch</h2>
          <p>
            Steps for {platformGuides[platform].name}. Switch the platform above to see the others.{" "}
            <Link
              to="/$slug/"
              params={{ slug: "getting-started" }}
              hash={platformGuides[platform].guideHash}
            >
              Read the full guide
            </Link>
          </p>
        </div>
        <NextSteps platform={platform} />
      </section>

      <section className="showcase-section" aria-labelledby="take-a-look">
        <div className="shell-width">
          <div className="section-heading">
            <h2 id="take-a-look">Take a look</h2>
            <p>Recorded from the current launcher, on desktop and on a phone.</p>
          </div>
          <Showcase />
        </div>
      </section>

      <section className="features-section shell-width" aria-labelledby="features">
        <div className="section-heading">
          <h2 id="features">Built for players who run more than one setup</h2>
          <p>
            Keep a Vanilla world, a modded survival pack, and a server client apart, and switch
            between them in one click.
          </p>
        </div>
        <ul className="feature-grid">
          {features.map(({ icon: Icon, title, text }) => (
            <li key={title}>
              <Icon className="feature-icon" size={26} strokeWidth={1.7} aria-hidden="true" />
              <h3>{title}</h3>
              <p>{text}</p>
            </li>
          ))}
        </ul>
      </section>

      <section className="faq-section shell-width" aria-labelledby="questions">
        <div className="section-heading">
          <h2 id="questions">Questions before you start</h2>
          <p>
            Something else? <a href={site.issues}>Ask on GitHub</a>.
          </p>
        </div>
        <Faq items={questions} />
      </section>

      <section className="finale" aria-labelledby="latest-build">
        <div className="shell-width finale-inner">
          <h2 id="latest-build">Cross over to your next world</h2>
          <p>
            {release
              ? `Trestle ${release.tag} is ready${releaseDate ? `, released ${releaseDate}` : ""}.`
              : "The latest release is on GitHub."}{" "}
            Every platform downloads from the same release.
          </p>
          <div className="actions">
            <Link className="button brass large" to="/download/" search={{ platform }}>
              <ArrowDownToLine size={20} aria-hidden="true" />
              Download for {platformGuides[platform].name}
            </Link>
            <a className="button ghost large" href={site.github}>
              View on GitHub
            </a>
          </div>
        </div>
      </section>
    </main>
  );
}
