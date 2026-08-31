from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


class SetVersionTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.script = self.root / 'scripts/set-version.sh'
        self.script.parent.mkdir()
        shutil.copyfile(Path(__file__).parents[1] / 'set-version.sh', self.script)
        self.properties = self.root / 'gradle.properties'
        self.properties.write_text('unrelated.value=keep\ntrestle.version=0.1.0\ntrestle.versionCode=1000\n')
        self.build_info = self.root / 'shared/src/commonMain/kotlin/net/blockhost/trestle/app/BuildInfo.kt'
        self.build_info.parent.mkdir(parents=True)
        self.build_info.write_text('object BuildInfo { const val VERSION = "0.1.0" }\n')

    def run_script(self, version):
        return subprocess.run(['bash', str(self.script), version], cwd=self.root.parent, text=True, capture_output=True)

    def test_updates_both_files_and_calculates_android_code_idempotently(self):
        first = self.run_script('2.3.4')
        self.assertEqual(0, first.returncode, first.stderr)
        self.assertEqual(2_003_004, int(first.stdout))
        values = dict(line.split('=', 1) for line in self.properties.read_text().splitlines())
        self.assertEqual('keep', values['unrelated.value'])
        self.assertEqual('2.3.4', values['trestle.version'])
        self.assertEqual(int(first.stdout), int(values['trestle.versionCode']))
        self.assertEqual('2.3.4', self.build_info.read_text().split('"')[1])
        contents = self.properties.read_bytes(), self.build_info.read_bytes()
        second = self.run_script('2.3.4')
        self.assertEqual(0, second.returncode, second.stderr)
        self.assertEqual(contents, (self.properties.read_bytes(), self.build_info.read_bytes()))

    def test_rejects_invalid_versions_without_mutating_sources(self):
        original = self.properties.read_bytes(), self.build_info.read_bytes()
        for version in ['0.0.0', '01.2.3', '1.1000.0', '2101.0.0', '1.2.3-beta', '1.2.3/../x']:
            with self.subTest(version=version):
                self.assertNotEqual(0, self.run_script(version).returncode)
                self.assertEqual(original, (self.properties.read_bytes(), self.build_info.read_bytes()))

    def test_leaves_properties_unchanged_when_version_constant_is_missing(self):
        self.build_info.write_text('object BuildInfo {}\n')
        original = self.properties.read_bytes()
        self.assertNotEqual(0, self.run_script('1.2.3').returncode)
        self.assertEqual(original, self.properties.read_bytes())
