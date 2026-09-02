#!/usr/bin/env python3
"""Assemble pinned Amethyst runtime assets for Android packages."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import shutil
import tempfile
import urllib.parse
import urllib.request
import zipfile


AMETHYST_REVISION = "d8a195640a7e0929f2ee532d7784de2b980c6c48"
AMETHYST_VERSION = "1.1.6"
RAW_ROOT = f"https://raw.githubusercontent.com/AngelAuraMC/Amethyst-Android/{AMETHYST_REVISION}"
MAVEN_ROOT = "https://repo1.maven.org/maven2"
APK = {
    "url": f"https://github.com/AngelAuraMC/Amethyst-Android/releases/download/{AMETHYST_VERSION}/Amethyst.apk",
    "sha256": "ac8d3aa0b1955c003a3f26f359b13bc6abf6c19ce29e82d4a0d542fbd4b4edc0",
}
AMETHYST_LICENSE = {
    "url": f"{RAW_ROOT}/LICENSE",
    "sha256": "e3a994d82e644b03a792a930f574002658412f62407f5fee083f2555c5f23118",
}
JRES = {
    "arm64": {
        "url": "https://github.com/AngelAuraMC/angelauramc-openjdk-build/releases/download/download_jre25/jre25-android-arm64.tar.xz",
        "sha256": "d3eb7afe2240c26728a1bb440502c5f18ac3883e932d202dd7f0c9bcbbce4c37",
    },
    "x64": {
        "url": "https://github.com/AngelAuraMC/angelauramc-openjdk-build/releases/download/download_jre25/jre25-android-x86_64.tar.xz",
        "sha256": "7fca862ee1b2d5fe23cd9c9c3d9b7ad3c241947ad1a6cc9464ef2e674867105d",
    },
}
JARS = {
    "lwjgl.jar": ("app_pojavlauncher/src/main/assets/components/lwjgl3/3.4.1/lwjgl.jar", "a436d01be183cd77887c2eb8ed3ebf6a031dc6a70fc7d68ec1662f50e3ea54f6"),
    "lwjgl-3.4.1-merged-modules.jar": ("app_pojavlauncher/src/main/assets/components/lwjgl3/3.4.1/lwjgl-3.4.1-merged-modules.jar", "08584aeadb90fec11e0a2d96077dfafb609cd7b735327fe1153e0575add464c9"),
    "lwjgl-freetype.jar": ("app_pojavlauncher/src/main/assets/components/lwjgl3/3.4.1/lwjgl-freetype.jar", "a1993bc7d6f9f72715a4b457715911bd9268d04869d494f20c19884e7c8dbe05"),
    "lwjgl-nanovg.jar": ("app_pojavlauncher/src/main/assets/components/lwjgl3/3.4.1/lwjgl-nanovg.jar", "28dede1a39356bbd731d0a18110cadb9041e32dd7adc633bcbde944bb8201ff5"),
    "lwjgl-openal.jar": ("app_pojavlauncher/src/main/assets/components/lwjgl3/3.4.1/lwjgl-openal.jar", "10467797e14b478eb06dd51259eeceafec4a317fdaff0d713a848abe28fd0c78"),
    "lwjgl-shaderc.jar": ("app_pojavlauncher/src/main/assets/components/lwjgl3/3.4.1/lwjgl-shaderc.jar", "5dc22d389927e58eed04704d200cc091a9b5fb211daa31e8be1d07d880ffb7f8"),
    "lwjgl-spng.jar": ("app_pojavlauncher/src/main/assets/components/lwjgl3/3.4.1/lwjgl-spng.jar", "6b9f99dde99376efc045d47b2c39afe2a7155375e29e3785da38d1c9879b4ac8"),
    "lwjgl-spvc.jar": ("app_pojavlauncher/src/main/assets/components/lwjgl3/3.4.1/lwjgl-spvc.jar", "d5a00514d1d20ffbe1eaf120b3a2c6c6d8115ea23c2d757e6fa25b684242bcbd"),
    "lwjgl-stb.jar": ("app_pojavlauncher/src/main/assets/components/lwjgl3/3.4.1/lwjgl-stb.jar", "e2656fcb59554ec518a8ecf5d5b0cbb544a69a2eab58b427ee52c006e35a737b"),
    "lwjgl-tinyfd.jar": ("app_pojavlauncher/src/main/assets/components/lwjgl3/3.4.1/lwjgl-tinyfd.jar", "fa4a421127c062ac51789f0f69be57fa68958d17309bde9a370e2862a1efc6dc"),
    "lwjgl-vma.jar": ("app_pojavlauncher/src/main/assets/components/lwjgl3/3.4.1/lwjgl-vma.jar", "e4b550cf500996fa48abd54d36226e5b5f4ceb157ee284681b5a00d202fd9e4f"),
    "lwjgl-vulkan.jar": ("app_pojavlauncher/src/main/assets/components/lwjgl3/3.4.1/lwjgl-vulkan.jar", "997c1d80d0e5f0698c66f3644c901ac95923b3bfeb539f2ff53d6b47cb237776"),
}
AARS = {
    "lwjgl": (f"{RAW_ROOT}/app_pojavlauncher/libs/lwjgl-3.4.1-natives-release.aar", "ccb9c7abe942cd40a0490637ca70756a259a40ec1257a515d3343c2f536503c0"),
    "openal": (f"{RAW_ROOT}/app_pojavlauncher/libs/openal-soft-release.aar", "45e630695b6b4c6506704330bf4da80a605b445ea5d187d7b71a370aab5494ea"),
    "kopper": (f"{RAW_ROOT}/app_pojavlauncher/libs/kopper-zink-release.aar", "bf816fc9dc2047edff0284369b6433260ec462b7b26a3e3b544550c721ca26fe"),
    "jna": (f"{MAVEN_ROOT}/net/java/dev/jna/jna/5.17.0/jna-5.17.0.aar", "4dbeffffa665d97ad5aa7eee297531d3c841a86716ab7f774fd6956422b3cf38"),
}
ABIS = {"arm64": "arm64-v8a", "x64": "x86_64"}
APK_LIBRARIES = ("libc++_shared.so", "libpojavexec.so", "libspirv-cross-c-shared.so")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def fetch(url: str, expected_hash: str, cache: Path) -> Path:
    suffix = Path(urllib.parse.urlparse(url).path).suffix
    destination = cache / f"{expected_hash}{suffix}"
    if destination.is_file() and sha256(destination) == expected_hash:
        return destination
    destination.unlink(missing_ok=True)
    request = urllib.request.Request(url, headers={"User-Agent": "Trestle runtime assembler"})
    with urllib.request.urlopen(request, timeout=180) as response, tempfile.NamedTemporaryFile(dir=cache, delete=False) as staged:
        shutil.copyfileobj(response, staged)
        staged_path = Path(staged.name)
    actual_hash = sha256(staged_path)
    if actual_hash != expected_hash:
        staged_path.unlink(missing_ok=True)
        raise ValueError(f"Checksum mismatch for {url}: expected {expected_hash}, received {actual_hash}.")
    staged_path.replace(destination)
    return destination


def extract_member(archive: Path, member: str, destination: Path) -> None:
    with zipfile.ZipFile(archive) as source:
        try:
            info = source.getinfo(member)
        except KeyError as error:
            raise ValueError(f"The runtime archive has no {member} entry.") from error
        if info.is_dir() or info.file_size <= 0:
            raise ValueError(f"The runtime archive has an invalid {member} entry.")
        destination.parent.mkdir(parents=True, exist_ok=True)
        with source.open(info) as input_stream, destination.open("wb") as output_stream:
            shutil.copyfileobj(input_stream, output_stream)


def assemble(output: Path, cache: Path) -> None:
    cache.mkdir(parents=True, exist_ok=True)
    output.parent.mkdir(parents=True, exist_ok=True)
    staged_root = Path(tempfile.mkdtemp(prefix="trestle-amethyst-", dir=output.parent))
    asset_root = staged_root / "amethyst" / "android"
    entries: list[dict[str, object]] = []
    try:
        apk = fetch(APK["url"], APK["sha256"], cache)
        aar_files = {name: fetch(url, digest, cache) for name, (url, digest) in AARS.items()}
        license_source = fetch(AMETHYST_LICENSE["url"], AMETHYST_LICENSE["sha256"], cache)
        license_destination = asset_root / "common" / "licenses" / "Amethyst-Android-LICENSE.txt"
        license_destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(license_source, license_destination)
        entries.append(file_entry(asset_root, license_destination, "common", "license"))

        jars_root = asset_root / "common" / "jars"
        for name, (relative_url, digest) in JARS.items():
            source = fetch(f"{RAW_ROOT}/{relative_url}", digest, cache)
            destination = jars_root / name
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, destination)
            entries.append(file_entry(asset_root, destination, "common", "classpath"))

        for release_name, android_abi in ABIS.items():
            platform_root = asset_root / release_name
            runtime = fetch(JRES[release_name]["url"], JRES[release_name]["sha256"], cache)
            runtime_destination = platform_root / "runtime" / "java-25.tar.xz"
            runtime_destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(runtime, runtime_destination)
            entries.append(file_entry(asset_root, runtime_destination, release_name, "runtime"))

            natives_root = platform_root / "natives"
            for library in APK_LIBRARIES:
                destination = natives_root / library
                extract_member(apk, f"lib/{android_abi}/{library}", destination)
                entries.append(file_entry(asset_root, destination, release_name, "native"))
            for aar in aar_files.values():
                with zipfile.ZipFile(aar) as source:
                    for info in source.infolist():
                        is_native = info.filename.startswith(f"jni/{android_abi}/")
                        is_component = f"/{android_abi}/" in info.filename and info.filename.startswith("assets/components/")
                        if info.is_dir() or not (is_native or is_component) or not info.filename.endswith(".so"):
                            continue
                        destination = natives_root / Path(info.filename).name
                        with source.open(info) as input_stream, destination.open("wb") as output_stream:
                            shutil.copyfileobj(input_stream, output_stream)
                        entries.append(file_entry(asset_root, destination, release_name, "native"))

        manifest = {
            "format": 1,
            "amethystRevision": AMETHYST_REVISION,
            "amethystVersion": AMETHYST_VERSION,
            "files": sorted(entries, key=lambda item: str(item["path"])),
        }
        (asset_root / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
        if output.exists():
            shutil.rmtree(output)
        staged_root.replace(output)
    except Exception:
        shutil.rmtree(staged_root, ignore_errors=True)
        raise


def file_entry(root: Path, path: Path, platform: str, kind: str) -> dict[str, object]:
    return {
        "path": path.relative_to(root).as_posix(),
        "platform": platform,
        "kind": kind,
        "size": path.stat().st_size,
        "sha256": sha256(path),
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--cache", type=Path, required=True)
    args = parser.parse_args()
    assemble(args.output.resolve(), args.cache.resolve())


if __name__ == "__main__":
    main()
