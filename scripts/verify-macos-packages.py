#!/usr/bin/env python3
"""Verify the ad hoc app signatures in the image, DMG, and PKG."""
import argparse
import hashlib
from pathlib import Path
import subprocess
import sys
import tempfile


def one(paths, description):
    paths = list(paths)
    if len(paths) != 1:
        raise ValueError(f'Expected one {description}, found {len(paths)}.')
    return paths[0]


def verify(app, expected_payload=None):
    subprocess.run(['codesign', '--verify', '--deep', '--strict', str(app)], check=True)
    details = subprocess.run(
        ['codesign', '--display', '--verbose=4', str(app)],
        check=True, capture_output=True, text=True,
    ).stderr
    fields = dict(line.split('=', 1) for line in details.splitlines() if '=' in line)
    if fields.get('Signature') != 'adhoc':
        raise ValueError(f'{app} does not have an ad hoc signature.')
    # jpackage adds .package metadata and re-signs installer bundles. Their
    # CDHash can differ, so compare the classpath, launch config, and Info.plist.
    files = [app / 'Contents/Info.plist']
    files.extend((app / 'Contents/app').glob('*.jar'))
    files.extend((app / 'Contents/app').glob('*.cfg'))
    payload = {str(path.relative_to(app)): hashlib.sha256(path.read_bytes()).hexdigest() for path in files}
    if expected_payload is not None and payload != expected_payload:
        raise ValueError(f'{app} does not contain the expected application payload.')
    return payload


def verify_installer_app(app, expected_payload):
    verify(app, expected_payload)
    subprocess.run([
        sys.executable, str(Path(__file__).with_name('smoke-test-package.py')),
        '--root', str(app), '--platform', 'macos',
    ], check=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, required=True)
    args = parser.parse_args()
    payload = verify(args.root / 'app/Trestle.app')
    dmg = one((args.root / 'dmg').glob('*.dmg'), 'DMG')
    pkg = one((args.root / 'pkg').glob('*.pkg'), 'PKG')

    with tempfile.TemporaryDirectory(prefix='trestle-verify-') as temporary:
        temporary = Path(temporary)
        mount = temporary / 'mounted'
        subprocess.run([
            'hdiutil', 'attach', '-readonly', '-nobrowse', '-mountpoint', str(mount), str(dmg),
        ], input=b'Y\n', check=True)  # Accept the bundled Apache license without a terminal.
        try:
            verify_installer_app(one(mount.glob('*.app'), 'application in the DMG'), payload)
        finally:
            subprocess.run(['hdiutil', 'detach', str(mount)], check=True)

        expanded = temporary / 'expanded'
        subprocess.run(['pkgutil', '--expand-full', str(pkg), str(expanded)], check=True)
        verify_installer_app(one(expanded.rglob('Trestle.app'), 'application in the PKG'), payload)
    print('Both installers passed signature, application payload, and startup checks.')


if __name__ == '__main__':
    main()
