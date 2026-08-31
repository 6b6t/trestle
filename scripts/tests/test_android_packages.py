import importlib.util
import json
from pathlib import Path
import struct
import tempfile
import unittest
import zipfile

spec = importlib.util.spec_from_file_location('android_packages', Path(__file__).parents[1] / 'collect-android-packages.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


def write_package(path, abis, bundle=False, override_machine=None):
    path.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(path, 'w') as archive:
        for abi in abis:
            header = bytearray(20)
            header[:7] = b'\x7fELF\x02\x01\x01'
            struct.pack_into('<H', header, 18, override_machine or module.ABIS[abi][1])
            prefix = 'base/lib' if bundle else 'lib'
            archive.writestr(f'{prefix}/{abi}/libcutils.so', header)


class AndroidPackagesTest(unittest.TestCase):
    def test_collects_metadata_outputs_and_ignores_stale_apks(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            apk_root = root / 'apk/release'
            elements = []
            for index, abis in enumerate([{'x86_64'}, {'arm64-v8a'}, set(module.ABIS)]):
                name = f'build-output-{index}.apk'
                write_package(apk_root / name, abis)
                filters = [] if len(abis) == 2 else [{'filterType': 'ABI', 'value': next(iter(abis))}]
                elements.append(dict(versionName='1.2.3', filters=filters, outputFile=name))
            (apk_root / 'output-metadata.json').write_text(json.dumps(dict(elements=elements)))
            (apk_root / 'stale.apk').write_bytes(b'invalid old build')
            bundle = root / 'bundle/release/app.aab'
            write_package(bundle, set(module.ABIS), bundle=True)
            destination = root / 'collected'
            module.collect(root, destination, '1.2.3')
            self.assertEqual(4, len(list(destination.iterdir())))
            for abi, (arch, _) in module.ABIS.items():
                module.verify_native_libraries(destination / f'Trestle-1.2.3-android-{arch}.apk', {abi})
            module.verify_native_libraries(destination / 'Trestle-1.2.3-android-universal.apk', set(module.ABIS))
            self.assertEqual(bundle.read_bytes(), (destination / 'Trestle-1.2.3-android-universal.aab').read_bytes())
            elements[0]['versionName'] = '1.2.2'
            (apk_root / 'output-metadata.json').write_text(json.dumps(dict(elements=elements)))
            with self.assertRaises(ValueError):
                module.collect(root, root / 'wrong-version', '1.2.3')
            self.assertFalse((root / 'wrong-version').exists())

    def test_rejects_missing_extra_and_mislabeled_native_abis(self):
        with tempfile.TemporaryDirectory() as temporary:
            package = Path(temporary) / 'app.apk'
            for actual, expected, machine in [({'arm64-v8a'}, set(module.ABIS), None),
                                               (set(module.ABIS), {'x86_64'}, None),
                                               ({'x86_64'}, {'x86_64'}, 183)]:
                with self.subTest(actual=actual, expected=expected, machine=machine):
                    write_package(package, actual, override_machine=machine)
                    with self.assertRaises(ValueError):
                        module.verify_native_libraries(package, expected)

    def test_rejects_incomplete_universal_library_sets(self):
        with tempfile.TemporaryDirectory() as temporary:
            package = Path(temporary) / 'app.apk'
            write_package(package, set(module.ABIS))
            with zipfile.ZipFile(package, 'a') as archive:
                archive.writestr('lib/x86_64/libextra.so', archive.read('lib/x86_64/libcutils.so'))
            with self.assertRaises(ValueError):
                module.verify_native_libraries(package, set(module.ABIS))
