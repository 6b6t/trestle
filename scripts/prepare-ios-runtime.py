#!/usr/bin/env python3
"""Assemble the pinned Amethyst-derived runtime payload for an iOS app."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import tarfile
import tempfile
import urllib.request
import zipfile


AMETHYST_REVISION = "9212a1894865e7ac0466029e25ddb0d895544c76"
AMETHYST_ARCHIVE = {
    "url": f"https://codeload.github.com/AngelAuraMC/Amethyst-iOS/tar.gz/{AMETHYST_REVISION}",
    "sha256": "59556678e66d7fce331d09434ab7f14822665eac475d78a61e6bc326ce1baecd",
}
JRE_ARCHIVE = {
    "url": "https://assets.angelauramc.dev/openjdk/ios-arm64/jre25-ios-aarch64.zip",
    "sha256": "3686cc278ff2d394b13cc449907259596451829bab0187786264c598dd6bd888",
    "member": "jre25-ios-arm64-20260710-release.tar.xz",
    "memberSha256": "ac71e5bc4191afc712bc74d374663207f7f4a9ee327300b623abc07b6f787c42",
}
RUNTIME_FRAMEWORKS = {"libEGL.framework", "libGLESv2.framework"}


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def fetch(source: dict[str, str], cache: Path, suffix: str) -> Path:
    destination = cache / f"{source['sha256']}{suffix}"
    if destination.is_file() and sha256(destination) == source["sha256"]:
        return destination
    destination.unlink(missing_ok=True)
    request = urllib.request.Request(source["url"], headers={"User-Agent": "Trestle runtime assembler"})
    with urllib.request.urlopen(request, timeout=180) as response, tempfile.NamedTemporaryFile(
        dir=cache,
        delete=False,
    ) as staged:
        shutil.copyfileobj(response, staged)
        staged_path = Path(staged.name)
    actual_hash = sha256(staged_path)
    if actual_hash != source["sha256"]:
        staged_path.unlink(missing_ok=True)
        raise ValueError(
            f"Checksum mismatch for {source['url']}: expected {source['sha256']}, received {actual_hash}.",
        )
    staged_path.replace(destination)
    return destination


def extract_tar(archive: Path, destination: Path) -> None:
    destination.mkdir(parents=True, exist_ok=True)
    with tarfile.open(archive) as source:
        root = destination.resolve()
        for member in source.getmembers():
            target = (destination / member.name).resolve()
            if not target.is_relative_to(root):
                raise ValueError(f"The runtime archive contains an unsafe path: {member.name}.")
            if member.issym() or member.islnk():
                link_target = (target.parent / member.linkname).resolve()
                if not link_target.is_relative_to(root):
                    raise ValueError(f"The runtime archive contains an unsafe link: {member.name}.")
        source.extractall(destination)


def build_lwjgl_bridge(amethyst_root: Path, destination: Path) -> None:
    java_app = amethyst_root / "JavaApp"
    lwjgl_sources = java_app / "src" / "lwjgl"
    work = Path(tempfile.mkdtemp(prefix="trestle-lwjgl-"))
    try:
        patched_sources = work / "sources"
        shutil.copytree(lwjgl_sources, patched_sources)
        for source in patched_sources.rglob("*.java"):
            content = source.read_text(encoding="utf-8")
            content = content.replace("import net.kdt.pojavlaunch.Tools;\n", "")
            content = content.replace('MacOSXLibraryDL("AngelAuraAmethyst"', 'MacOSXLibraryDL("Trestle"')
            content = content.replace('/AngelAuraAmethyst"', '/Trestle"')
            source.write_text(content, encoding="utf-8")

        source_files = sorted(patched_sources.rglob("*.java"))
        source_files.extend(sorted((java_app / "src" / "launcher" / "android").rglob("*.java")))
        lwjgl_jars = sorted((java_app / "libs" / "lwjgl").glob("*.jar"))
        input_jars = list(lwjgl_jars)
        input_jars.extend(sorted((java_app / "libs" / "caciocavallo").glob("*.jar")))
        input_jars.append(java_app / "libs" / "others" / "jsr305.jar")
        classes = work / "classes"
        classes.mkdir()
        javac = shutil.which("javac")
        if javac is None:
            raise ValueError("A JDK is required to build the patched iOS LWJGL bridge.")
        subprocess.run(
            [javac, "-cp", ":".join(map(str, input_jars)), "-d", str(classes), *map(str, source_files)],
            check=True,
        )

        merged: dict[str, bytes] = {}
        for input_jar in lwjgl_jars:
            with zipfile.ZipFile(input_jar) as source:
                for info in source.infolist():
                    if info.is_dir() or info.filename.upper().startswith("META-INF/"):
                        continue
                    merged[info.filename] = source.read(info)
        for compiled in classes.rglob("*.class"):
            merged[compiled.relative_to(classes).as_posix()] = compiled.read_bytes()
        with zipfile.ZipFile(destination, "w", compression=zipfile.ZIP_DEFLATED) as output:
            output.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\nCreated-By: Trestle\n\n")
            for name, content in sorted(merged.items()):
                output.writestr(name, content)
    finally:
        shutil.rmtree(work, ignore_errors=True)


def copy_payload(amethyst_root: Path, jre_archive: Path, output: Path) -> None:
    frameworks_source = amethyst_root / "Natives" / "resources" / "Frameworks"
    frameworks_output = output / "Frameworks"
    libraries_output = output / "amethyst-libs"
    java_output = output / "java_runtimes" / "java-25-openjdk"
    frameworks_output.mkdir(parents=True)
    libraries_output.mkdir(parents=True)

    for source in sorted(frameworks_source.iterdir()):
        if source.suffix == ".dylib" or source.name in RUNTIME_FRAMEWORKS:
            destination = frameworks_output / source.name
            if source.is_dir():
                shutil.copytree(source, destination, symlinks=True)
            else:
                shutil.copy2(source, destination)
    build_lwjgl_bridge(amethyst_root, libraries_output / "lwjgl.jar")
    licenses_output = output / "licenses"
    licenses_output.mkdir(parents=True)
    shutil.copy2(amethyst_root / "LICENSE", licenses_output / "Amethyst-iOS-LICENSE.txt")

    with zipfile.ZipFile(jre_archive) as source:
        member = JRE_ARCHIVE["member"]
        with source.open(member) as input_stream, tempfile.NamedTemporaryFile(delete=False) as staged:
            shutil.copyfileobj(input_stream, staged)
            inner_archive = Path(staged.name)
    try:
        actual_hash = sha256(inner_archive)
        if actual_hash != JRE_ARCHIVE["memberSha256"]:
            raise ValueError(
                f"Checksum mismatch for {member}: expected {JRE_ARCHIVE['memberSha256']}, received {actual_hash}.",
            )
        extract_tar(inner_archive, java_output)
    finally:
        inner_archive.unlink(missing_ok=True)


def assemble(output: Path, cache: Path, platform: str) -> None:
    if platform != "iphoneos":
        return
    cache.mkdir(parents=True, exist_ok=True)
    output.parent.mkdir(parents=True, exist_ok=True)
    staged_output = Path(tempfile.mkdtemp(prefix="trestle-ios-runtime-", dir=output.parent))
    source_root = Path(tempfile.mkdtemp(prefix="trestle-amethyst-source-", dir=cache))
    try:
        amethyst_archive = fetch(AMETHYST_ARCHIVE, cache, ".tar.gz")
        jre_archive = fetch(JRE_ARCHIVE, cache, ".zip")
        extract_tar(amethyst_archive, source_root)
        roots = list(source_root.iterdir())
        if len(roots) != 1 or not roots[0].is_dir():
            raise ValueError("The Amethyst source archive has an unexpected layout.")
        copy_payload(roots[0], jre_archive, staged_output)
        files = [
            {
                "path": path.relative_to(staged_output).as_posix(),
                "size": path.stat().st_size,
                "sha256": sha256(path),
            }
            for path in sorted(staged_output.rglob("*"))
            if path.is_file() and not path.is_symlink()
        ]
        manifest = {
            "format": 1,
            "amethystRevision": AMETHYST_REVISION,
            "javaRuntimeSha256": JRE_ARCHIVE["sha256"],
            "files": files,
        }
        (staged_output / "amethyst-runtime.json").write_text(
            json.dumps(manifest, indent=2) + "\n",
            encoding="utf-8",
        )
        if output.exists():
            shutil.rmtree(output)
        staged_output.replace(output)
    except Exception:
        shutil.rmtree(staged_output, ignore_errors=True)
        raise
    finally:
        shutil.rmtree(source_root, ignore_errors=True)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--cache", type=Path, required=True)
    parser.add_argument("--platform", required=True)
    args = parser.parse_args()
    assemble(args.output.resolve(), args.cache.resolve(), args.platform)


if __name__ == "__main__":
    main()
