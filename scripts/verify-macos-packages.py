#!/usr/bin/env python3
"""Verify the ad hoc app signatures in the image, DMG, and PKG."""
import argparse
from pathlib import Path
import subprocess
import tempfile


def one(paths, description):
    paths = list(paths)
    if len(paths) != 1:
        raise ValueError(f'Expected one {description}, found {len(paths)}.')
    return paths[0]


def verify(app, expected_hash=None):
    subprocess.run(['codesign', '--verify', '--deep', '--strict', str(app)], check=True)
    details = subprocess.run(
        ['codesign', '--display', '--verbose=4', str(app)],
        check=True, capture_output=True, text=True,
    ).stderr
    fields = dict(line.split('=', 1) for line in details.splitlines() if '=' in line)
    if fields.get('Signature') != 'adhoc':
        raise ValueError(f'{app} does not have an ad hoc signature.')
    digest = fields.get('CDHash')
    if not digest or (expected_hash is not None and digest != expected_hash):
        raise ValueError(f'{app} does not match the signed application image.')
    return digest


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, required=True)
    args = parser.parse_args()
    digest = verify(args.root / 'app/Trestle.app')
    dmg = one((args.root / 'dmg').glob('*.dmg'), 'DMG')
    pkg = one((args.root / 'pkg').glob('*.pkg'), 'PKG')

    with tempfile.TemporaryDirectory(prefix='trestle-verify-') as temporary:
        temporary = Path(temporary)
        mount = temporary / 'mounted'
        subprocess.run([
            'hdiutil', 'attach', '-readonly', '-nobrowse', '-mountpoint', str(mount), str(dmg),
        ], check=True)
        try:
            verify(one(mount.glob('*.app'), 'application in the DMG'), digest)
        finally:
            subprocess.run(['hdiutil', 'detach', str(mount)], check=True)

        expanded = temporary / 'expanded'
        subprocess.run(['pkgutil', '--expand-full', str(pkg), str(expanded)], check=True)
        verify(one(expanded.rglob('Trestle.app'), 'application in the PKG'), digest)
    print('Both installers contain the verified ad hoc application image.')


if __name__ == '__main__':
    main()
