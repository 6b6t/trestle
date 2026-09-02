import importlib.util
from pathlib import Path
import plistlib
import stat
import tempfile
import unittest
import zipfile


spec = importlib.util.spec_from_file_location('ios_package', Path(__file__).parents[1] / 'package-ios.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class IosPackageTest(unittest.TestCase):
    def make_app(self, root: Path, version: str = '1.2.3') -> Path:
        app = root / 'Trestle.app'
        app.mkdir()
        with (app / 'Info.plist').open('wb') as stream:
            plistlib.dump(
                {
                    'CFBundleExecutable': 'Trestle',
                    'CFBundleShortVersionString': version,
                    'CFBundleVersion': '1002003',
                },
                stream,
            )
        executable = app / 'Trestle'
        executable.write_bytes(module.MACHO_64_LITTLE_ENDIAN + module.CPU_TYPE_ARM64.to_bytes(4, 'little'))
        executable.chmod(0o755)
        resources = app / 'compose-resources'
        resources.mkdir()
        (resources / 'resource.txt').write_text('content')
        return app

    def test_packages_payload_and_preserves_executable_mode(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            ipa = module.package(self.make_app(root), '1.2.3', root / 'output')

            self.assertEqual('Trestle-1.2.3-ios-arm64.ipa', ipa.name)
            with zipfile.ZipFile(ipa) as archive:
                self.assertEqual(
                    {
                        'Payload/Trestle.app/Info.plist',
                        'Payload/Trestle.app/Trestle',
                        'Payload/Trestle.app/compose-resources/resource.txt',
                    },
                    set(archive.namelist()),
                )
                mode = archive.getinfo('Payload/Trestle.app/Trestle').external_attr >> 16
                self.assertNotEqual(0, mode & stat.S_IXUSR)

    def test_rejects_a_mismatched_version(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            with self.assertRaises(ValueError):
                module.package(self.make_app(root, version='1.2.4'), '1.2.3', root / 'output')

    def test_rejects_a_missing_executable(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            app = self.make_app(root)
            (app / 'Trestle').unlink()
            with self.assertRaises(ValueError):
                module.package(app, '1.2.3', root / 'output')

    def test_rejects_a_non_arm64_executable(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            app = self.make_app(root)
            (app / 'Trestle').write_bytes(module.MACHO_64_LITTLE_ENDIAN + (0x01000007).to_bytes(4, 'little'))
            with self.assertRaises(ValueError):
                module.package(app, '1.2.3', root / 'output')
