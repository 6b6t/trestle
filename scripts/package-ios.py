#!/usr/bin/env python3
"""Validate an unsigned iOS application bundle and package it as an IPA."""

import argparse
import os
from pathlib import Path
import plistlib
import re
import shutil
import stat
import subprocess
import tempfile
import zipfile


VERSION_PATTERN = re.compile(r'(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)')
MACHO_64_LITTLE_ENDIAN = b'\xcf\xfa\xed\xfe'
CPU_TYPE_ARM64 = 0x0100000C


def package(app: Path, version: str, output: Path, extension: str = 'ipa') -> Path:
    if not VERSION_PATTERN.fullmatch(version):
        raise ValueError('Version must be major.minor.patch.')
    if not app.is_dir() or app.suffix != '.app':
        raise ValueError('The iOS application bundle does not exist.')

    info_path = app / 'Info.plist'
    if not info_path.is_file():
        raise ValueError('The iOS application bundle has no Info.plist file.')
    with info_path.open('rb') as stream:
        info = plistlib.load(stream)

    if info.get('CFBundleShortVersionString') != version:
        raise ValueError('The iOS application version does not match the release version.')
    build_version = str(info.get('CFBundleVersion', ''))
    if not build_version.isdigit() or int(build_version) <= 0:
        raise ValueError('The iOS application has an invalid build version.')

    executable_name = info.get('CFBundleExecutable')
    if not isinstance(executable_name, str) or Path(executable_name).name != executable_name:
        raise ValueError('The iOS application has an invalid executable name.')
    executable = app / executable_name
    if not executable.is_file() or executable.stat().st_size == 0:
        raise ValueError('The iOS application executable is missing or empty.')
    if executable.stat().st_mode & stat.S_IXUSR == 0:
        raise ValueError('The iOS application executable is not executable.')
    with executable.open('rb') as stream:
        header = stream.read(8)
    if header[:4] != MACHO_64_LITTLE_ENDIAN:
        raise ValueError('The iOS application executable is not a 64-bit Mach-O file.')
    if int.from_bytes(header[4:8], byteorder='little') != CPU_TYPE_ARM64:
        raise ValueError('The iOS application executable is not ARM64.')

    output.mkdir(parents=True, exist_ok=True)
    destination = output / f'Trestle-{version}-ios-arm64.{extension}'
    temporary = destination.with_suffix(f'.{extension}.tmp')
    temporary.unlink(missing_ok=True)
    try:
        with zipfile.ZipFile(temporary, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
            for path in sorted(app.rglob('*')):
                archive_name = Path('Payload') / app.name / path.relative_to(app)
                if path.is_symlink():
                    info = zipfile.ZipInfo(archive_name.as_posix())
                    info.create_system = 3
                    info.external_attr = (stat.S_IFLNK | 0o777) << 16
                    archive.writestr(info, os.readlink(path))
                elif path.is_file():
                    archive.write(path, archive_name.as_posix())
        temporary.replace(destination)
    finally:
        temporary.unlink(missing_ok=True)

    if destination.stat().st_size == 0:
        raise ValueError('The generated IPA is empty.')
    return destination


def package_jailbreak(app: Path, version: str, output: Path, entitlements: Path, ldid: str = 'ldid') -> Path:
    if not entitlements.is_file():
        raise ValueError('The jailbreak entitlement file does not exist.')
    with tempfile.TemporaryDirectory(prefix='trestle-ios-jailbreak-') as temporary:
        signed_app = Path(temporary) / app.name
        shutil.copytree(app, signed_app, symlinks=True)
        with (signed_app / 'Info.plist').open('rb') as stream:
            executable_name = plistlib.load(stream)['CFBundleExecutable']
        subprocess.run([ldid, '-S', str(signed_app)], check=True)
        subprocess.run([ldid, f'-S{entitlements}', str(signed_app / executable_name)], check=True)
        return package(signed_app, version, output, extension='tipa')


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('--app', type=Path, required=True)
    parser.add_argument('--version', required=True)
    parser.add_argument('--output', type=Path, default=Path('release-artifacts'))
    parser.add_argument('--jailbreak-entitlements', type=Path)
    parser.add_argument('--ldid', default='ldid')
    args = parser.parse_args()
    package(args.app, args.version, args.output)
    if args.jailbreak_entitlements is not None:
        package_jailbreak(
            args.app,
            args.version,
            args.output,
            args.jailbreak_entitlements,
            args.ldid,
        )


if __name__ == '__main__':
    main()
