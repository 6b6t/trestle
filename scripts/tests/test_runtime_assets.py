import importlib.util
from pathlib import Path
import tarfile
import tempfile
import unittest
import zipfile


def load_script(name: str):
    path = Path(__file__).parents[1] / name
    spec = importlib.util.spec_from_file_location(name.removesuffix('.py'), path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


android = load_script('prepare-android-runtime.py')
ios = load_script('prepare-ios-runtime.py')


class RuntimeAssetsTest(unittest.TestCase):
    def test_android_archive_member_is_copied_exactly(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            archive = root / 'runtime.zip'
            with zipfile.ZipFile(archive, 'w') as output:
                output.writestr('lib/arm64-v8a/libbridge.so', b'bridge')

            destination = root / 'output' / 'libbridge.so'
            android.extract_member(archive, 'lib/arm64-v8a/libbridge.so', destination)

            self.assertEqual(b'bridge', destination.read_bytes())

    def test_ios_archive_rejects_a_parent_path(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            archive = root / 'runtime.tar'
            payload = root / 'payload'
            payload.write_bytes(b'unsafe')
            with tarfile.open(archive, 'w') as output:
                output.add(payload, arcname='../payload')

            with self.assertRaises(ValueError):
                ios.extract_tar(archive, root / 'output')

    def test_ios_simulator_does_not_resolve_device_artifacts(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            output = root / 'output'

            ios.assemble(output, root / 'cache', 'iphonesimulator')

            self.assertFalse(output.exists())


if __name__ == '__main__':
    unittest.main()
