import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile


spec = importlib.util.spec_from_file_location('macos_signing', Path(__file__).parents[1] / 'sign-macos.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class MacosSigningTest(unittest.TestCase):
    def test_distinguishes_universal_binaries_from_java_classes(self):
        self.assertTrue(module.is_mach_o(bytes.fromhex('cafebabe00000002')))
        self.assertTrue(module.is_mach_o(bytes.fromhex('cffaedfe0c000001')))
        self.assertFalse(module.is_mach_o(bytes.fromhex('cafebabe00000041')))
        self.assertFalse(module.is_mach_o(bytes.fromhex('7f454c4602010100')))

    def test_signs_jar_natives_without_changing_resources_or_entry_metadata(self):
        native = bytes.fromhex('cffaedfe0c000001')
        resource = bytes.fromhex('cafebabe00000041')
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            jar = root / 'library.jar'
            entry = zipfile.ZipInfo('macos/lib.dylib', (2026, 1, 2, 3, 4, 6))
            entry.external_attr = 0o100755 << 16
            entry.compress_type = zipfile.ZIP_DEFLATED
            with zipfile.ZipFile(jar, 'w') as archive:
                archive.writestr(entry, native)
                archive.writestr('Main.class', resource)
                archive.comment = b'library metadata'

            def sign(path, entitlements):
                path.write_bytes(path.read_bytes() + b'\x01\x02\x03\x04')

            with patch.object(module, 'sign', side_effect=sign) as signer:
                module.sign_jar(jar, root / 'entitlements.plist')
            self.assertEqual(signer.call_count, 1)
            with zipfile.ZipFile(jar) as archive:
                self.assertEqual(archive.read(entry.filename), native + b'\x01\x02\x03\x04')
                self.assertEqual(archive.read('Main.class'), resource)
                self.assertEqual(archive.comment, b'library metadata')
                actual = archive.getinfo(entry.filename)
                self.assertEqual(actual.date_time, entry.date_time)
                self.assertEqual(actual.external_attr, entry.external_attr)
                self.assertEqual(actual.compress_type, entry.compress_type)
            self.assertEqual(list(root.iterdir()), [jar])
