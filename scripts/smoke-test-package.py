#!/usr/bin/env python3
"""Launch the packaged CLI without reading the developer's launcher data."""
import argparse
import os
from pathlib import Path
import subprocess
import tempfile

WINDOWS_CONSOLE_SUBSYSTEM = 3


def pe_subsystem(path):
    """Reads the Subsystem field of a Windows executable: 2 for windowed programs, 3 for console programs."""
    data = path.read_bytes()
    header = int.from_bytes(data[0x3C:0x40], 'little')
    if data[header:header + 4] != b'PE\0\0':
        raise SystemExit(f'{path.name} is not a Windows executable.')
    return int.from_bytes(data[header + 24 + 68:header + 24 + 70], 'little')


parser = argparse.ArgumentParser()
parser.add_argument('--root', type=Path, required=True)
parser.add_argument('--platform', choices=['linux', 'macos', 'windows'], required=True)
args = parser.parse_args()
# Windows runs commands through the console launcher. Trestle.exe is a windowed program.
pattern = {'linux': '**/bin/Trestle', 'macos': '**/Contents/MacOS/Trestle', 'windows': '**/trestle-cli.exe'}[args.platform]
launchers = list(args.root.glob(pattern))
if len(launchers) != 1:
    raise SystemExit(f'Expected one packaged launcher, found {len(launchers)}.')
if args.platform == 'windows' and pe_subsystem(launchers[0]) != WINDOWS_CONSOLE_SUBSYSTEM:
    # A captured pipe works even for a windowed program, so check the header that decides what a terminal sees.
    raise SystemExit('trestle-cli.exe is not a console program, so terminals would show no output.')
with tempfile.TemporaryDirectory(prefix='trestle-smoke-') as temporary:
    env = os.environ.copy()
    env.update(JAVA_TOOL_OPTIONS=f'-Duser.home="{temporary}"',
               XDG_DATA_HOME=temporary, XDG_CONFIG_HOME=temporary, APPDATA=temporary, LOCALAPPDATA=temporary)
    result = subprocess.run([str(launchers[0].resolve()), '--help'], env=env, text=True, capture_output=True, timeout=30, check=True)
    if '--launch' not in result.stdout or '--list' not in result.stdout:
        raise SystemExit('The packaged launcher did not produce CLI help.')
    subprocess.run([str(launchers[0].resolve()), '--list'], env=env, timeout=30, check=True)
    print('Packaged CLI startup and empty-library checks passed.')
