#!/usr/bin/env python3
"""Generate release/download and downstream package metadata from actual artifacts."""
import argparse
import hashlib
import html
import json
from pathlib import Path
import re

TARGETS = {
    ('android', 'arm64'): ('apk', 'aab'),
    ('linux', 'x64'): ('deb', 'rpm', 'tar.gz'),
    ('linux', 'arm64'): ('deb', 'rpm', 'tar.gz'),
    ('macos', 'x64'): ('dmg', 'pkg'),
    ('macos', 'arm64'): ('dmg', 'pkg'),
    ('windows', 'x64'): ('msi', 'exe'),
    ('windows', 'arm64'): ('msi', 'exe'),
}
MINIMUM = {'android': 'Android 8.1 (API 27)', 'linux': 'glibc 2.35', 'macos': 'macOS 11', 'windows': 'Windows 10'}


def generate(version, artifacts, repository, commit):
    if not re.fullmatch(r'(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)', version):
        raise ValueError('Version must be major.minor.patch.')
    if not re.fullmatch(r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+', repository):
        raise ValueError('Invalid GitHub repository.')
    if not re.fullmatch(r'[a-f0-9]{40}', commit):
        raise ValueError('A full source commit SHA is required.')
    base = f'https://github.com/{repository}/releases/download/{version}'
    downloads = []
    sums = []
    for (platform, arch), formats in TARGETS.items():
        for extension in formats:
            name = f'Trestle-{version}-{platform}-{arch}.{extension}'
            path = artifacts / name
            if not path.is_file() or path.stat().st_size == 0:
                raise ValueError(f'Missing or empty artifact: {name}')
            digest = hashlib.sha256()
            with path.open('rb') as stream:
                for chunk in iter(lambda: stream.read(1024 * 1024), b''):
                    digest.update(chunk)
            sha = digest.hexdigest()
            downloads.append(dict(platform=platform, architecture=arch, format=extension, url=f'{base}/{name}',
                                  sha256=sha, size=path.stat().st_size, minimumOS=MINIMUM[platform]))
            sums.append(f'{sha}  {name}')
    manifest = dict(schemaVersion=1, version=version, commit=commit, artifacts=downloads)
    (artifacts / 'release-manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
    (artifacts / 'SHA256SUMS').write_text('\n'.join(sums) + '\n')
    rows = ''.join(f'<tr><td>{a["platform"]}</td><td>{a["architecture"]}</td><td><a href="{html.escape(a["url"])}">{a["format"]}</a></td><td>{a["minimumOS"]}</td><td><code>{a["sha256"]}</code></td></tr>' for a in downloads)
    (artifacts / 'downloads.html').write_text(f'''<!doctype html>
<html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Trestle {version} downloads</title>
<style>body{{font:16px system-ui;margin:40px auto;padding:0 20px;max-width:1100px;color:#202020}}table{{border-collapse:collapse;width:100%}}th,td{{text-align:left;padding:12px;border-bottom:1px solid #ccc}}code{{overflow-wrap:anywhere;font-size:12px}}a{{color:#1855ad}}.table{{overflow-x:auto}}</style>
<main><h1>Trestle {version}</h1><p><a href="https://github.com/{repository}/releases/tag/{version}">Release notes and installation guidance</a></p>
<p>Select the package for your system and processor. ARM64 includes Apple Silicon and Snapdragon; x64 covers Intel and AMD desktops.</p>
<div class="table"><table><thead><tr><th>System</th><th>Architecture</th><th>Download</th><th>Minimum system</th><th>SHA-256</th></tr></thead><tbody>{rows}</tbody></table></div>
<p>Built from commit <code>{commit}</code>. Game and renderer compatibility can impose additional requirements.</p></main></html>\n''')
    def artifact(platform, arch, extension):
        return next(a for a in downloads if (a['platform'], a['architecture'], a['format']) == (platform, arch, extension))
    arm, intel = artifact('macos', 'arm64', 'dmg'), artifact('macos', 'x64', 'dmg')
    (artifacts / 'trestle.rb').write_text(f'''cask "trestle" do
  arch arm: "arm64", intel: "x64"
  version "{version}"
  sha256 arm: "{arm['sha256']}", intel: "{intel['sha256']}"
  url "https://github.com/{repository}/releases/download/#{{version}}/Trestle-#{{version}}-macos-#{{arch}}.dmg"
  name "Trestle"
  desc "Minecraft Java Edition launcher"
  homepage "https://github.com/{repository}"
  depends_on macos: ">= :big_sur"
  app "Trestle.app"
end
''')
    identifier = 'BlockhostNetwork.Trestle'
    common = f'PackageIdentifier: {identifier}\nPackageVersion: {version}\n'
    installers = common + 'InstallerType: wix\nScope: user\nUpgradeBehavior: install\nFileExtensions:\n- mrpack\nInstallers:\n'
    for arch in ['x64', 'arm64']:
        a = artifact('windows', arch, 'msi')
        installers += f'- Architecture: {arch}\n  InstallerUrl: {a["url"]}\n  InstallerSha256: {a["sha256"].upper()}\n'
    (artifacts / f'{identifier}.installer.yaml').write_text(installers + 'ManifestType: installer\nManifestVersion: 1.9.0\n')
    (artifacts / f'{identifier}.locale.en-US.yaml').write_text(common + f'''PackageLocale: en-US
Publisher: Blockhost Network
PublisherUrl: https://github.com/{repository}
PublisherSupportUrl: https://github.com/{repository}/issues
PackageName: Trestle
PackageUrl: https://github.com/{repository}
License: Apache-2.0
LicenseUrl: https://github.com/{repository}/blob/{commit}/LICENSE
ShortDescription: Minecraft Java Edition launcher
ReleaseNotesUrl: https://github.com/{repository}/releases/tag/{version}
ManifestType: defaultLocale
ManifestVersion: 1.9.0
''')
    (artifacts / f'{identifier}.yaml').write_text(common + 'DefaultLocale: en-US\nManifestType: version\nManifestVersion: 1.9.0\n')
    flatpak = {
        'app-id': 'net.blockhost.trestle', 'runtime': 'org.freedesktop.Platform', 'runtime-version': '25.08',
        'sdk': 'org.freedesktop.Sdk', 'command': 'trestle',
        'finish-args': ['--share=network', '--share=ipc', '--socket=x11', '--socket=pulseaudio', '--device=dri', '--filesystem=xdg-download'],
        'modules': [{'name': 'trestle', 'buildsystem': 'simple', 'build-commands': [
            'cp -a trestle /app/trestle', 'mkdir -p /app/bin', 'ln -s /app/trestle/bin/Trestle /app/bin/trestle',
            'cp -a share/. /app/share/',
        ], 'sources': [dict(type='archive', url=artifact('linux', arch, 'tar.gz')['url'],
                            sha256=artifact('linux', arch, 'tar.gz')['sha256'], **{'strip-components': 0, 'only-arches': [flatarch]})
                       for arch, flatarch in [('x64', 'x86_64'), ('arm64', 'aarch64')]]}],
    }
    (artifacts / 'net.blockhost.trestle.json').write_text(json.dumps(flatpak, indent=2) + '\n')
    return manifest


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--version', required=True)
    parser.add_argument('--artifacts', type=Path, required=True)
    parser.add_argument('--repository', default='6b6t/trestle')
    parser.add_argument('--commit', required=True)
    args = parser.parse_args()
    generate(args.version, args.artifacts, args.repository, args.commit)


if __name__ == '__main__':
    main()
