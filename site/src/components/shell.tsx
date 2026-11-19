import { Link } from "@tanstack/react-router";
import { useSearchContext } from "fumadocs-ui/contexts/search";
import { useTheme } from "fumadocs-ui/provider/base";
import { ArrowDownToLine, Menu, Moon, Search, Sun, X } from "lucide-react";
import { useState } from "react";
import { site } from "../lib/site";

export function Brand() {
  return (
    <span className="brand">
      <svg className="brand-mark" viewBox="0 0 64 64" width="28" height="28" aria-hidden="true">
        <path className="mark-deck" d="M4 10h56v8H4z" />
        <path className="mark-truss" d="M6 18h52v5H6zM8 23h12l12 12-5 5zM44 23h12L37 40l-5-5z" />
        <path className="mark-pier" d="M27 35h10v19H27z" />
      </svg>
      Trestle
    </span>
  );
}

function NavLinks() {
  return (
    <>
      <Link to="/$slug/" params={{ slug: "getting-started" }}>
        Install guide
      </Link>
      <Link to="/download/">Downloads</Link>
      <Link to="/$slug/" params={{ slug: "build-from-source" }}>
        Developers
      </Link>
    </>
  );
}

export function SiteHeader() {
  const { setOpenSearch } = useSearchContext();
  const { resolvedTheme, setTheme } = useTheme();
  const [menuOpen, setMenuOpen] = useState(false);
  return (
    <header className="site-header">
      <a href="#main-content" className="skip-link">
        Skip to content
      </a>
      <div className="header-inner">
        <Link
          to="/"
          aria-label="Trestle home"
          onClick={() => {
            setMenuOpen(false);
          }}
        >
          <Brand />
        </Link>
        <nav className="desktop-nav" aria-label="Main navigation">
          <NavLinks />
        </nav>
        <div className="header-tools">
          <button
            className="search-trigger"
            onClick={() => {
              setOpenSearch(true);
            }}
            aria-label="Search documentation"
          >
            <Search size={17} aria-hidden="true" />
            <span>Search docs</span>
            <kbd>Ctrl K</kbd>
          </button>
          <button
            className="icon-button theme-toggle"
            aria-label="Toggle color theme"
            onClick={() => {
              setTheme(resolvedTheme === "dark" ? "light" : "dark");
            }}
          >
            <Sun className="theme-sun" size={19} aria-hidden="true" />
            <Moon className="theme-moon" size={19} aria-hidden="true" />
          </button>
          <button
            className="icon-button mobile-menu-toggle"
            aria-label={menuOpen ? "Close navigation" : "Open navigation"}
            aria-expanded={menuOpen}
            aria-controls="mobile-navigation"
            onClick={() => {
              setMenuOpen(!menuOpen);
            }}
          >
            {menuOpen ? <X size={21} /> : <Menu size={21} />}
          </button>
          <Link
            className="button brass header-download"
            to="/download/"
            onClick={() => {
              setMenuOpen(false);
            }}
          >
            <ArrowDownToLine size={17} aria-hidden="true" />
            <span className="header-download-label">Download</span>
          </Link>
        </div>
      </div>
      {menuOpen && (
        <nav
          id="mobile-navigation"
          className="mobile-nav"
          aria-label="Mobile navigation"
          onClick={() => {
            setMenuOpen(false);
          }}
        >
          <NavLinks />
          <a href={site.github}>GitHub</a>
        </nav>
      )}
    </header>
  );
}

export function SiteFooter() {
  return (
    <footer className="site-footer">
      <div className="footer-inner shell-width">
        <div className="footer-brand">
          <Brand />
          <p>A Minecraft Java Edition launcher for desktop and mobile.</p>
        </div>
        <nav aria-label="Players">
          <h2>Play</h2>
          <Link to="/download/">Downloads</Link>
          <Link to="/$slug/" params={{ slug: "getting-started" }}>
            Install guide
          </Link>
          <Link to="/$slug/" params={{ slug: "content-updates" }}>
            Updates and content
          </Link>
        </nav>
        <nav aria-label="Developers">
          <h2>Develop</h2>
          <Link to="/$slug/" params={{ slug: "build-from-source" }}>
            Build from source
          </Link>
          <Link to="/$slug/" params={{ slug: "service-endpoints" }}>
            Service endpoints
          </Link>
          <Link to="/$slug/" params={{ slug: "releases" }}>
            Release process
          </Link>
        </nav>
        <nav aria-label="Project links">
          <h2>Project</h2>
          <a href={site.github}>GitHub</a>
          <a href={site.releases}>All releases</a>
          <a href={site.issues}>Report a problem</a>
        </nav>
        <p className="footer-note">
          Trestle is free software under the GNU GPL version 3. It is not affiliated with Mojang or
          Microsoft. Minecraft is a trademark of Mojang Synergies AB.
        </p>
      </div>
    </footer>
  );
}
