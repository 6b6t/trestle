import importlib.util
import hashlib
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('release_metadata', Path(__file__).parents[1] / 'generate-release-metadata.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class ReleaseMetadataTest(unittest.TestCase):
    def test_requires_complete_matrix_and_hashes_actual_bytes(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            with self.assertRaises(ValueError):
                module.generate('1.2.3', root, '6b6t/trestle', 'a' * 40)
            for (platform, arch), extensions in module.TARGETS.items():
                for extension in extensions:
                    (root / f'Trestle-1.2.3-{platform}-{arch}.{extension}').write_bytes(bytes(range(256)))
            manifest = module.generate('1.2.3', root, '6b6t/trestle', 'a' * 40)
            self.assertEqual(16, len(manifest['artifacts']))
            self.assertEqual({hashlib.sha256(bytes(range(256))).hexdigest()}, {a['sha256'] for a in manifest['artifacts']})
            self.assertEqual(manifest, json.loads((root / 'release-manifest.json').read_text()))
            flatpak = json.loads((root / 'net.blockhost.trestle.json').read_text())
            self.assertEqual(2, len(flatpak['modules'][0]['sources']))
            self.assertEqual({256}, {a['size'] for a in manifest['artifacts']})
            # Regeneration must not mistake generated metadata for binary artifacts.
            self.assertEqual(manifest, module.generate('1.2.3', root, '6b6t/trestle', 'a' * 40))

    def test_rejects_untrusted_version_repository_and_commit(self):
        with tempfile.TemporaryDirectory() as temporary:
            for version, repo, commit in [('1.2.3/../x', '6b6t/trestle', 'a' * 40), ('1.2.3', 'evil/x?y', 'a' * 40), ('1.2.3', '6b6t/trestle', 'main')]:
                with self.assertRaises(ValueError):
                    module.generate(version, Path(temporary), repo, commit)
