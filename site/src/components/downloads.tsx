import { Link } from "@tanstack/react-router";
import {
  AppWindow,
  ArrowDownToLine,
  ExternalLink,
  Monitor,
  Smartphone,
  Tablet,
} from "lucide-react";
import type { CSSProperties } from "react";
import { formatSize, releaseLink, type Release } from "../lib/releases";
import {
  platformDownloads,
  platformGuides,
  platformOrder,
  type Platform,
  type PlatformDownload,
} from "../lib/platforms";

const icons = {
  windows: AppWindow,
  macos: Monitor,
  linux: Monitor,
  android: Smartphone,
  ios: Tablet,
} as const;

export function PlatformSwitch({
  platform,
  onChange,
  disabled,
  name,
}: {
  platform: Platform;
  onChange: (platform: Platform) => void;
  disabled?: boolean;
  name: string;
}) {
  return (
    <fieldset
      className="platform-switch"
      disabled={disabled}
      style={{ "--index": platformOrder.indexOf(platform) } as CSSProperties}
    >
      <legend className="sr-only">Platform</legend>
      <span className="switch-thumb" aria-hidden="true" />
      {platformOrder.map((id) => {
        const Icon = icons[id];
        return (
          <label key={id} className="switch-option" data-selected={platform === id}>
            <input
              type="radio"
              name={name}
              value={id}
              checked={platform === id}
              onChange={() => onChange(id)}
            />
            <Icon size={19} aria-hidden="true" />
            <strong>{platformGuides[id].name}</strong>
          </label>
        );
      })}
    </fieldset>
  );
}

export function DownloadFiles({
  release,
  platform,
}: {
  release: Release | null;
  platform: Platform;
}) {
  const downloads = platformDownloads(release, platform);
  if (downloads.length === 0)
    return (
      <div className="download-fallback">
        <p>
          {release
            ? "Direct downloads for this platform are missing from the latest release. Its asset list has the files and notes."
            : "This site build has no release snapshot. GitHub has the latest files and notes."}
        </p>
        <a className="button brass" href={releaseLink(release)}>
          Open the latest release
          <ExternalLink size={16} aria-hidden="true" />
        </a>
      </div>
    );
  const builds = Map.groupBy(downloads, (download) => download.build);
  return (
    <div className="download-builds">
      {[...builds].map(([build, files]) => (
        <section key={build} aria-label={build}>
          <h4>{build}</h4>
          <ul className="download-files">
            {files.map((download) => (
              <DownloadChip key={download.asset.name} download={download} />
            ))}
          </ul>
        </section>
      ))}
    </div>
  );
}

function DownloadChip({ download: { build, label, asset } }: { download: PlatformDownload }) {
  return (
    <li>
      <a className="download-chip" href={asset.url} aria-label={`Download ${label}, ${build}`}>
        <span className="chip-icon">
          <ArrowDownToLine size={19} aria-hidden="true" />
        </span>
        <span className="chip-text">
          <strong>{label}</strong>
          <span className="mono">{asset.name}</span>
        </span>
        <span className="chip-size">{formatSize(asset.size)}</span>
      </a>
    </li>
  );
}

export function NextSteps({ platform }: { platform: Platform }) {
  return (
    <ol className="next-steps" key={platform}>
      {platformGuides[platform].steps.map((step, index) => (
        <li key={step} style={{ "--step": index } as CSSProperties}>
          <span className="step-number" aria-hidden="true">
            {index + 1}
          </span>
          <p>{step}</p>
        </li>
      ))}
    </ol>
  );
}

/** The platform chooser: pick a platform and download its packages. */
export function PlatformStage({
  platform,
  onChange,
  release,
  hydrated,
  heading,
  headingId,
}: {
  platform: Platform;
  onChange: (platform: Platform) => void;
  release: Release | null;
  hydrated: boolean;
  heading: string;
  headingId: string;
}) {
  const guide = platformGuides[platform];
  return (
    <section className="stage" aria-labelledby={headingId}>
      <div className="stage-top">
        <h2 id={headingId}>{heading}</h2>
        <PlatformSwitch
          name={`${headingId}-platform`}
          platform={platform}
          onChange={onChange}
          disabled={!hydrated}
        />
      </div>
      <div className="stage-bottom">
        <div className="stage-copy" key={platform}>
          <p className="stage-requirement">{guide.requirement}</p>
          <h3>{guide.title}</h3>
          <p>{guide.description}</p>
          <p className="stage-links">
            <Link to="/$slug/" params={{ slug: "getting-started" }} hash={guide.guideHash}>
              Installation guide
            </Link>
            {release && (
              <a className="mono" href={release.url}>
                Trestle {release.tag}
              </a>
            )}
          </p>
        </div>
        <div className="stage-downloads" key={`${platform}-files`}>
          <DownloadFiles release={release} platform={platform} />
        </div>
      </div>
    </section>
  );
}
