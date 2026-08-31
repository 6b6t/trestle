#!/usr/bin/env python3
"""Build DEB/RPM packages and a portable archive from the tested jpackage image."""
import argparse
import datetime
import os
from pathlib import Path
import re
import shutil
import subprocess
import tarfile
import tempfile
import xml.etree.ElementTree as ET


def run(*args):
    subprocess.run(args, check=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--image', type=Path, required=True)
    parser.add_argument('--version', required=True)
    parser.add_argument('--arch', choices=['x64', 'arm64'], required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--broad-associations', action='store_true')
    args = parser.parse_args()
    if not re.fullmatch(r'(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)', args.version):
        parser.error('Version must be major.minor.patch.')
    image = args.image.resolve()
    if not (image / 'bin/Trestle').is_file():
        parser.error('The jpackage image has no Trestle launcher.')
    repo = Path(__file__).resolve().parent.parent
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    stem = f'Trestle-{args.version}-linux-{args.arch}'
    with tempfile.TemporaryDirectory(prefix='trestle-package-') as temporary:
        work = Path(temporary)
        root = work / 'root'
        shutil.copytree(image, root / 'opt/trestle', symlinks=True)
        data = {
            'applications/net.blockhost.trestle.desktop': repo / 'packaging/linux/net.blockhost.trestle.desktop',
            'metainfo/net.blockhost.trestle.metainfo.xml': repo / 'packaging/linux/net.blockhost.trestle.metainfo.xml',
            'mime/packages/net.blockhost.trestle.xml': repo / 'packaging/linux/net.blockhost.trestle.xml',
            'icons/hicolor/512x512/apps/net.blockhost.trestle.png': repo / 'desktopApp/src/main/resources/trestle.png',
            'doc/trestle/copyright': repo / 'LICENSE',
        }
        for destination, source in data.items():
            target = root / 'usr/share' / destination
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, target)
        metadata = root / 'usr/share/metainfo/net.blockhost.trestle.metainfo.xml'
        tree = ET.parse(metadata)
        releases = ET.SubElement(tree.getroot(), 'releases')
        ET.SubElement(releases, 'release', version=args.version, date=datetime.date.today().isoformat())
        tree.write(metadata, encoding='utf-8', xml_declaration=True)
        if args.broad_associations:
            desktop = root / 'usr/share/applications/net.blockhost.trestle.desktop'
            desktop.write_text(desktop.read_text().replace('MimeType=', 'MimeType=application/java-archive;application/zip;'))
        (root / 'usr/bin').mkdir(parents=True)
        (root / 'usr/bin/trestle').symlink_to('/opt/trestle/bin/Trestle')
        # Portable archives include metadata for downstream Flatpak builds.
        with tarfile.open(output / f'{stem}.tar.gz', 'w:gz') as archive:
            archive.add(root / 'opt/trestle', arcname='trestle')
            archive.add(root / 'usr/share', arcname='share')
        control = root / 'DEBIAN'
        control.mkdir()
        arch = 'amd64' if args.arch == 'x64' else 'arm64'
        (control / 'control').write_text(f'''Package: trestle
Version: {args.version}
Section: games
Priority: optional
Architecture: {arch}
Maintainer: Blockhost Network <40795980+AlexProgrammerDE@users.noreply.github.com>
Homepage: https://github.com/6b6t/trestle
Depends: libc6 (>= 2.35), libstdc++6, libx11-6, libxext6, libxi6, libxrender1, libxtst6, libgl1, libegl1, libfontconfig1, libfreetype6, libasound2
Description: Minecraft Java Edition launcher
 Manage isolated instances, mods, modpacks, and worlds.
 The launcher includes Java. Game runtimes are downloaded separately.
''')
        refresh = '''#!/bin/sh
set -e
if command -v update-desktop-database >/dev/null 2>&1; then update-desktop-database -q /usr/share/applications || true; fi
if command -v update-mime-database >/dev/null 2>&1; then update-mime-database /usr/share/mime || true; fi
if command -v gtk-update-icon-cache >/dev/null 2>&1; then gtk-update-icon-cache -q -t -f /usr/share/icons/hicolor || true; fi
'''
        for name in ['postinst', 'postrm']:
            (control / name).write_text(refresh)
            (control / name).chmod(0o755)
        run('dpkg-deb', '--root-owner-group', '--build', str(root), str(output / f'{stem}.deb'))
        shutil.rmtree(control)
        rpm_arch = 'x86_64' if args.arch == 'x64' else 'aarch64'
        top = work / 'rpm'
        for name in ['BUILD', 'RPMS', 'SOURCES', 'SPECS', 'SRPMS', 'BUILDROOT']:
            (top / name).mkdir(parents=True)
        spec = top / 'SPECS/trestle.spec'
        refresh_body = refresh.split('set -e\n', 1)[1]
        spec.write_text(f'''%global __os_install_post %{{nil}}
%global debug_package %{{nil}}
Name: trestle
Version: {args.version}
Release: 1
Summary: Minecraft Java Edition launcher
License: Apache-2.0
URL: https://github.com/6b6t/trestle
BuildArch: {rpm_arch}
AutoReqProv: no
Requires: glibc >= 2.35, libstdc++, libX11, libXext, libXi, libXrender, libXtst, libglvnd-glx, libglvnd-egl, fontconfig, freetype, alsa-lib
%description
Manage isolated instances, mods, modpacks, and worlds.
%install
mkdir -p "%{{buildroot}}"
cp -a "{root}/." "%{{buildroot}}/"
%files
/opt/trestle
/usr/bin/trestle
/usr/share/applications/net.blockhost.trestle.desktop
/usr/share/metainfo/net.blockhost.trestle.metainfo.xml
/usr/share/mime/packages/net.blockhost.trestle.xml
/usr/share/icons/hicolor/512x512/apps/net.blockhost.trestle.png
/usr/share/doc/trestle
%post
{refresh_body}
%postun
{refresh_body}
''')
        run('rpmbuild', '--define', f'_topdir {top}', '--target', rpm_arch, '-bb', str(spec))
        packages = list((top / 'RPMS').rglob('*.rpm'))
        if len(packages) != 1:
            raise RuntimeError('Expected exactly one RPM package.')
        shutil.copyfile(packages[0], output / f'{stem}.rpm')


if __name__ == '__main__':
    main()
