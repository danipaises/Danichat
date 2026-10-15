#!/usr/bin/env python3
"""Install upstream-pinned Android SDK archives with SHA-256 verification."""
from __future__ import annotations

import argparse
import hashlib
from pathlib import Path
import shutil
import tempfile
import urllib.request
import zipfile


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sdk-root", type=Path, required=True)
    parser.add_argument("--accept-license", action="store_true")
    args = parser.parse_args()
    if not args.accept_license:
        parser.error("Review the Android SDK license and pass --accept-license to install")
    source = Path(__file__).resolve().parents[1] / "reproducible-builds"
    config = dict(line.split("=", 1) for line in (source / "TOOLCHAIN.env").read_text().splitlines()
                  if line.strip() and not line.startswith("#"))
    root = args.sdk_root.resolve()
    packages = [
        ("ANDROID_PLATFORM", root / "platforms", "android-37.0", "android-37.0", "platform-37.0.xml"),
        ("ANDROID_BUILD_TOOLS", root / "build-tools", "android-37.0", "37.0.0", "build-tools-37.0.0.xml"),
        ("ANDROID_PLATFORM_TOOLS", root, "platform-tools", "platform-tools", "platform-tools-37.0.0.xml"),
    ]
    # An isolated root prevents replacing a developer's existing SDK packages.
    if any((destination / installed).exists() for _, destination, _, installed, _ in packages):
        raise RuntimeError("Use an empty SDK root; existing packages would be overwritten")
    with tempfile.TemporaryDirectory(prefix="danichat-sdk-") as temp:
        for prefix, destination, archive_dir, installed_dir, metadata in packages:
            filename = config[f"{prefix}_ARCHIVE"]
            archive = Path(temp) / filename
            urllib.request.urlretrieve(f"https://dl.google.com/android/repository/{filename}", archive)
            with archive.open("rb") as stream:
                digest = hashlib.file_digest(stream, "sha256").hexdigest()
            if digest != config[f"{prefix}_SHA256"]:
                raise RuntimeError(f"SDK checksum mismatch: {filename}")
            staging = Path(temp) / prefix
            staging.mkdir()
            with zipfile.ZipFile(archive) as zipped:
                for member in zipped.infolist():
                    if not (staging / member.filename).resolve().is_relative_to(staging.resolve()):
                        raise RuntimeError("Unsafe SDK archive member")
                zipped.extractall(staging)
            installed = destination / installed_dir
            destination.mkdir(parents=True, exist_ok=True)
            shutil.move(str(staging / archive_dir), installed)
            shutil.copy(source / "sdk-metadata" / metadata, installed / "package.xml")
            for file in installed.iterdir():
                if file.is_file() and not file.suffix:
                    file.chmod(file.stat().st_mode | 0o111)
            print(f"Verified and installed {filename}")
    (root / "licenses").mkdir(exist_ok=True)
    (root / "licenses" / "android-sdk-license").write_text(config["ANDROID_SDK_LICENSE_SHA1"] + "\n")


if __name__ == "__main__":
    main()
