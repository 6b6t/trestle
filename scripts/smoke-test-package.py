#!/usr/bin/env python3
"""Launch the packaged CLI without reading the developer's launcher data."""
import argparse
import os
from pathlib import Path
import subprocess
import tempfile

parser = argparse.ArgumentParser()
parser.add_argument('--root', type=Path, required=True)
parser.add_argument('--platform', choices=['linux', 'macos', 'windows'], required=True)
args = parser.parse_args()
pattern = {'linux': '**/bin/Trestle', 'macos': '**/Contents/MacOS/Trestle', 'windows': '**/Trestle.exe'}[args.platform]
launchers = list(args.root.glob(pattern))
if len(launchers) != 1:
    raise SystemExit(f'Expected one packaged launcher, found {len(launchers)}.')
with tempfile.TemporaryDirectory(prefix='trestle-smoke-') as temporary:
    env = os.environ.copy()
    env.update(JAVA_TOOL_OPTIONS=f'-Duser.home="{temporary}"',
               XDG_DATA_HOME=temporary, XDG_CONFIG_HOME=temporary, APPDATA=temporary, LOCALAPPDATA=temporary)
    result = subprocess.run([str(launchers[0].resolve()), '--help'], env=env, text=True, capture_output=True, timeout=30, check=True)
    if '--launch' not in result.stdout or '--list' not in result.stdout:
        raise SystemExit('The packaged launcher did not produce CLI help.')
    subprocess.run([str(launchers[0].resolve()), '--list'], env=env, timeout=30, check=True)
    print('Packaged CLI startup and empty-library checks passed.')
