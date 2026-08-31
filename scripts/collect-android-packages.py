#!/usr/bin/env python3
"""Collect Android release outputs after validating their versions and native ABIs."""
import argparse
import json
from pathlib import Path
import re
import shutil
import struct
import zipfile

ABIS = {'arm64-v8a': ('arm64', 183), 'x86_64': ('x64', 62)}


def verify_native_libraries(package, expected_abis, bundle=False):
    prefix = 'base/lib/' if bundle else 'lib/'
    libraries = {}
    with zipfile.ZipFile(package) as archive:
        for entry in archive.infolist():
            if not entry.filename.startswith(prefix) or not entry.filename.endswith('.so'):
                continue
            abi, name = entry.filename[len(prefix):].split('/', 1)
            if abi not in expected_abis or '/' in name:
                raise ValueError(f'Unexpected native library in {package.name}: {entry.filename}')
            with archive.open(entry) as stream:
                header = stream.read(20)
            if (len(header) != 20 or header[:7] != b'\x7fELF\x02\x01\x01'
                    or struct.unpack_from('<H', header, 18)[0] != ABIS[abi][1]):
                raise ValueError(f'Wrong native architecture in {package.name}: {entry.filename}')
            libraries.setdefault(abi, set()).add(name)
    if set(libraries) != set(expected_abis) or any('libcutils.so' not in names for names in libraries.values()):
        raise ValueError(f'Missing native libraries in {package.name}')
    if len({frozenset(names) for names in libraries.values()}) != 1:
        raise ValueError(f'Native library sets differ between ABIs in {package.name}')


def collect(outputs, destination, version):
    if not re.fullmatch(r'(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)', version):
        raise ValueError('Version must be major.minor.patch.')
    apk_root = outputs / 'apk/release'
    metadata = json.loads((apk_root / 'output-metadata.json').read_text())
    packages = {}
    for element in metadata['elements']:
        if element['versionName'] != version:
            raise ValueError('The APK version does not match the release version.')
        filters = element['filters']
        if not filters:
            arch, expected = 'universal', set(ABIS)
        elif len(filters) == 1 and filters[0]['filterType'] == 'ABI' and filters[0]['value'] in ABIS:
            abi = filters[0]['value']
            arch, expected = ABIS[abi][0], {abi}
        else:
            raise ValueError(f'Unexpected APK filters: {filters}')
        filename = element['outputFile']
        if Path(filename).name != filename or not filename.endswith('.apk') or arch in packages:
            raise ValueError('Invalid or duplicate APK output.')
        package = apk_root / filename
        verify_native_libraries(package, expected)
        packages[arch] = package
    if set(packages) != {'arm64', 'x64', 'universal'}:
        raise ValueError('Expected ARM64, x64, and universal APKs.')
    bundles = list((outputs / 'bundle/release').glob('*.aab'))
    if len(bundles) != 1:
        raise ValueError('Expected one universal app bundle.')
    verify_native_libraries(bundles[0], set(ABIS), bundle=True)
    destination.mkdir(parents=True, exist_ok=True)
    for arch, package in packages.items():
        shutil.copyfile(package, destination / f'Trestle-{version}-android-{arch}.apk')
    shutil.copyfile(bundles[0], destination / f'Trestle-{version}-android-universal.aab')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--outputs', type=Path, default=Path('androidApp/build/outputs'))
    parser.add_argument('--destination', type=Path, default=Path('release-artifacts'))
    parser.add_argument('--version', required=True)
    args = parser.parse_args()
    collect(args.outputs, args.destination, args.version)


if __name__ == '__main__':
    main()
