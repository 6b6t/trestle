#!/usr/bin/env python3
"""Apply ad hoc signatures before jpackage creates the macOS installers."""
import argparse
from pathlib import Path
import subprocess
import tempfile
import zipfile


MACH_O_MAGIC = {
    b'\xfe\xed\xfa\xce', b'\xce\xfa\xed\xfe',
    b'\xfe\xed\xfa\xcf', b'\xcf\xfa\xed\xfe',
    b'\xca\xfe\xba\xbe', b'\xbe\xba\xfe\xca',
    b'\xca\xfe\xba\xbf', b'\xbf\xba\xfe\xca',
}


def is_mach_o(header):
    # Java class files share the fat-binary magic, but use this field for
    # their minor/major version rather than a small architecture count.
    if header[:4] == b'\xca\xfe\xba\xbe':
        return 0 < int.from_bytes(header[4:8], 'big') < 32
    return header[:4] in MACH_O_MAGIC


def sign(path, entitlements):
    subprocess.run([
        'codesign', '--force', '--sign', '-', '--timestamp=none',
        '--options', 'runtime', '--entitlements', str(entitlements), str(path),
    ], check=True)


def sign_jar(path, entitlements):
    with zipfile.ZipFile(path) as source:
        native_entries = []
        for entry in source.infolist():
            if not entry.is_dir():
                with source.open(entry) as stream:
                    if is_mach_o(stream.read(8)):
                        native_entries.append(entry.filename)
        if not native_entries:
            return
        # Use generated local paths, never archive paths, for extracted code.
        with tempfile.TemporaryDirectory(prefix='trestle-sign-', dir=path.parent) as temporary:
            temporary = Path(temporary)
            output = temporary / 'signed.jar'
            with zipfile.ZipFile(output, 'w') as target:
                target.comment = source.comment
                for entry in source.infolist():
                    data = source.read(entry)
                    if entry.filename in native_entries:
                        native = temporary / 'native'
                        native.write_bytes(data)
                        sign(native, entitlements)
                        data = native.read_bytes()
                        native.unlink()
                    target.writestr(entry, data)
            output.replace(path)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--app', type=Path, required=True)
    parser.add_argument('--entitlements', type=Path, required=True)
    args = parser.parse_args()
    if not (args.app / 'Contents/MacOS/Trestle').is_file():
        parser.error('The Trestle application bundle is missing.')
    if not args.entitlements.is_file():
        parser.error('The entitlements file is missing.')

    for path in sorted(args.app.rglob('*')):
        if path.is_symlink() or not path.is_file():
            continue
        if path.suffix == '.jar':
            sign_jar(path, args.entitlements)
        else:
            with path.open('rb') as stream:
                native = is_mach_o(stream.read(8))
            if native:
                sign(path, args.entitlements)
    # Seal nested bundles before their parent bundle. Do not use --deep to sign.
    sign(args.app / 'Contents/runtime', args.entitlements)
    sign(args.app, args.entitlements)
    subprocess.run(['codesign', '--verify', '--deep', '--strict', str(args.app)], check=True)
    print('Ad hoc signing and bundle verification passed.')


if __name__ == '__main__':
    main()
