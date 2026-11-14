#!/usr/bin/env python3
"""
bump_version_and_build.py
Deterministic version bumper and build automation script for True Scale.
- Updates versionMajor, versionMinor, versionPatch in app/build.gradle.kts
- Computes versionCode = major * 10000 + minor * 100 + patch
- Runs Gradle build (assembleDebug testDebugUnitTest)
- Directs all intermediate logs to .tmp/build_logs/
"""

import sys
import os
import re
import argparse
import subprocess

ROOT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BUILD_GRADLE_KTS = os.path.join(ROOT_DIR, "app", "build.gradle.kts")
TMP_LOG_DIR = os.path.join(ROOT_DIR, ".tmp", "build_logs")
os.makedirs(TMP_LOG_DIR, exist_ok=True)
BUILD_LOG = os.path.join(TMP_LOG_DIR, "gradle_build.log")

def parse_current_version():
    if not os.path.exists(BUILD_GRADLE_KTS):
        return 1, 0, 0
    with open(BUILD_GRADLE_KTS, "r", encoding="utf-8") as f:
        content = f.read()

    major_m = re.search(r'val\s+versionMajor\s*=\s*(\d+)', content)
    minor_m = re.search(r'val\s+versionMinor\s*=\s*(\d+)', content)
    patch_m = re.search(r'val\s+versionPatch\s*=\s*(\d+)', content)

    major = int(major_m.group(1)) if major_m else 1
    minor = int(minor_m.group(1)) if minor_m else 0
    patch = int(patch_m.group(1)) if patch_m else 0
    return major, minor, patch

def update_build_gradle(major, minor, patch):
    if not os.path.exists(BUILD_GRADLE_KTS):
        print(f"File {BUILD_GRADLE_KTS} does not exist yet. Skipping version file write.")
        return

    with open(BUILD_GRADLE_KTS, "r", encoding="utf-8") as f:
        content = f.read()

    content = re.sub(r'val\s+versionMajor\s*=\s*\d+', f'val versionMajor = {major}', content)
    content = re.sub(r'val\s+versionMinor\s*=\s*\d+', f'val versionMinor = {minor}', content)
    content = re.sub(r'val\s+versionPatch\s*=\s*\d+', f'val versionPatch = {patch}', content)

    with open(BUILD_GRADLE_KTS, "w", encoding="utf-8") as f:
        f.write(content)

    version_code = major * 10000 + minor * 100 + patch
    print(f"Updated app/build.gradle.kts -> v{major}.{minor}.{patch} (versionCode = {version_code})")

def run_gradle_build():
    gradle_cmd = os.path.join(ROOT_DIR, "gradlew")
    if not os.path.exists(gradle_cmd):
        # Fallback to cached gradle binary in ~/.gradle/wrapper/dists
        fallback = "/home/marco/.gradle/wrapper/dists/gradle-9.6.0-bin/42k10rwplmzkhuboz9kdazi7s/gradle-9.6.0/bin/gradle"
        if os.path.exists(fallback):
            gradle_cmd = fallback
        else:
            gradle_cmd = "gradle"

    cmd = [gradle_cmd, "assembleDebug", "testDebugUnitTest"]
    print(f"Executing build: {' '.join(cmd)}")
    print(f"Logging output to: {BUILD_LOG}")

    with open(BUILD_LOG, "w", encoding="utf-8") as log_file:
        proc = subprocess.Popen(
            cmd,
            cwd=ROOT_DIR,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True
        )
        for line in proc.stdout:
            sys.stdout.write(line)
            log_file.write(line)
        proc.wait()

    if proc.returncode != 0:
        print(f"\n[X] Gradle build FAILED with code {proc.returncode}. Inspect log: {BUILD_LOG}", file=sys.stderr)
        return proc.returncode

    apk_path = os.path.join(ROOT_DIR, "app", "build", "outputs", "apk", "debug", "app-debug.apk")
    if os.path.exists(apk_path):
        size_bytes = os.path.getsize(apk_path)
        print(f"\n[✓] Build SUCCESS! APK generated at {apk_path} ({size_bytes} bytes)")
    else:
        print(f"\n[✓] Build tasks completed successfully.")
    return 0

def main():
    parser = argparse.ArgumentParser(description="Deterministic SemVer bumper and Gradle builder for True Scale")
    parser.add_argument("--major", action="store_true", help="Bump major version (e.g. 1.0.0 -> 2.0.0)")
    parser.add_argument("--minor", action="store_true", help="Bump minor version (e.g. 1.0.0 -> 1.1.0)")
    parser.add_argument("--patch", action="store_true", help="Bump patch version (e.g. 1.0.0 -> 1.0.1)")
    parser.add_argument("--set", type=str, help="Set explicit version (format: X.Y.Z)")
    parser.add_argument("--check-only", action="store_true", help="Do not bump version; run build and tests only")
    parser.add_argument("--skip-build", action="store_true", help="Bump version without running gradle build")

    args = parser.parse_args()

    major, minor, patch = parse_current_version()
    print(f"Current version: v{major}.{minor}.{patch}")

    if args.set:
        parts = args.set.strip().lstrip("v").split(".")
        if len(parts) != 3:
            print("Error: Version format must be X.Y.Z", file=sys.stderr)
            return 1
        major, minor, patch = int(parts[0]), int(parts[1]), int(parts[2])
        update_build_gradle(major, minor, patch)
    elif args.major:
        major += 1
        minor = 0
        patch = 0
        update_build_gradle(major, minor, patch)
    elif args.minor:
        minor += 1
        patch = 0
        update_build_gradle(major, minor, patch)
    elif args.patch:
        patch += 1
        update_build_gradle(major, minor, patch)
    elif not args.check_only:
        print("No bump option specified. Running in check mode.")

    if not args.skip_build:
        return run_gradle_build()
    return 0

if __name__ == "__main__":
    sys.exit(main())
