#!/usr/bin/env bash

set -euo pipefail

project_directory="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_directory"

if [[ "$#" -ne 1 ]]; then
    echo "Usage: $0 <major.minor.patch>" >&2
    exit 2
fi

version="$1"
if [[ ! "$version" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
    echo "Version must use the numeric major.minor.patch format." >&2
    exit 2
fi

major="${BASH_REMATCH[1]}"
minor="${BASH_REMATCH[2]}"
patch="${BASH_REMATCH[3]}"

for component in "$major" "$minor" "$patch"; do
    if [[ "$component" != "0" && "$component" == 0* ]]; then
        echo "Version components must not contain leading zeroes." >&2
        exit 2
    fi
done

if (( ${#major} > 4 || ${#minor} > 3 || ${#patch} > 3 )); then
    echo "Version components exceed the supported Android version code range." >&2
    exit 2
fi

if (( 10#$minor > 999 || 10#$patch > 999 )); then
    echo "Minor and patch version components must not exceed 999." >&2
    exit 2
fi

version_code=$((10#$major * 1000000 + 10#$minor * 1000 + 10#$patch))
if (( version_code < 1 || version_code > 2100000000 )); then
    echo "The calculated Android version code is outside the supported range." >&2
    exit 2
fi

python3 - "$version" "$version_code" <<'PY'
from pathlib import Path
import re
import sys

version, version_code = sys.argv[1:]
properties_path = Path('gradle.properties')
properties = properties_path.read_text()
for name, value in [('trestle.version', version), ('trestle.versionCode', version_code)]:
    pattern = rf'^{re.escape(name)}=.*$'
    if re.search(pattern, properties, flags=re.MULTILINE):
        properties = re.sub(pattern, f'{name}={value}', properties, flags=re.MULTILINE)
    else:
        properties = properties.rstrip('\n') + f'\n{name}={value}\n'

build_info_path = Path('shared/src/commonMain/kotlin/net/blockhost/trestle/app/BuildInfo.kt')
build_info, count = re.subn(
    r'(const val VERSION = ")[0-9]+\.[0-9]+\.[0-9]+(")',
    lambda match: f'{match[1]}{version}{match[2]}',
    build_info_path.read_text(),
)
if count != 1:
    raise SystemExit('Expected exactly one version constant in BuildInfo.kt.')
properties_path.write_text(properties)
build_info_path.write_text(build_info)
PY

printf '%s\n' "$version_code"
