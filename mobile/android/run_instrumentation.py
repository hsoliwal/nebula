#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal and contributors.
# SPDX-License-Identifier: EPL-2.0
"""Run the real Android fixture on one isolated, KVM-backed API 35 emulator.

This is a product test driver. It does not install SDK packages, accept licenses,
transform sources, or turn an adb exit code into a native test pass.
"""
from __future__ import annotations

import argparse
import binascii
import hashlib
import json
import os
from pathlib import Path
import platform
import re
import shlex
import signal
import socket
import stat
import struct
import subprocess
import sys
import tempfile
import time
import traceback
import zlib

PACKAGE = "org.eclipse.swt.mobile.probe"
COMPONENT = PACKAGE + "/org.eclipse.nebula.tests.mobile.AndroidMobileInstrumentation"
SCOPE = "Android original SWT Canvas/Shell and unchanged Nebula RoundedCheckbox"
SYSTEM_IMAGE = "system-images;android-35;default;x86_64"
SCREENSHOTS = ("unselected.png", "selected.png", "recreated.png")


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def verify_instrumentation(text: str, minimum_checks: int = 30) -> dict:
    """Require the custom harness's complete, unique terminal result."""
    if re.search(r"^INSTRUMENTATION_(?:FAILED|ABORTED):", text, re.MULTILINE):
        raise ValueError("Android reported an instrumentation startup/abort failure")
    if re.search(r"^INSTRUMENTATION_RESULT: failure=", text, re.MULTILINE):
        raise ValueError("Instrumentation returned a failure alongside its terminal status")
    status = re.findall(r"^INSTRUMENTATION_RESULT: status=(.*)$", text, re.MULTILINE)
    codes = re.findall(r"^INSTRUMENTATION_CODE: (-?\d+)\s*$", text, re.MULTILINE)
    counts = re.findall(r"^INSTRUMENTATION_RESULT: checks=(\d+)\s*$", text, re.MULTILINE)
    scopes = re.findall(r"^INSTRUMENTATION_RESULT: scope=(.*)$", text, re.MULTILINE)
    if status != ["PASS"] or codes != ["-1"] or scopes != [SCOPE] or len(counts) != 1:
        raise ValueError("Missing, duplicate or unsuccessful instrumentation terminal result")
    checks = int(counts[0])
    if checks < minimum_checks:
        raise ValueError(f"Only {checks} checks ran; at least {minimum_checks} are required")
    return {"status": "PASSED", "checks": checks, "scope": scopes[0], "instrumentationCode": -1}


def verify_png(data: bytes) -> dict:
    """Validate complete Android-generated PNG data, without a vision dependency."""
    if not data.startswith(b"\x89PNG\r\n\x1a\n") or len(data) > 32 * 1024 * 1024:
        raise ValueError("Missing PNG signature or oversized screenshot")
    offset, width, height, channels = 8, 0, 0, 0
    compressed = bytearray()
    ended = False
    while offset < len(data):
        if offset + 12 > len(data):
            raise ValueError("Truncated PNG chunk")
        length = struct.unpack_from(">I", data, offset)[0]
        kind = data[offset + 4:offset + 8]
        end = offset + 12 + length
        if end > len(data):
            raise ValueError("Truncated PNG payload")
        payload = data[offset + 8:offset + 8 + length]
        crc = struct.unpack_from(">I", data, offset + 8 + length)[0]
        if binascii.crc32(kind + payload) & 0xFFFFFFFF != crc:
            raise ValueError("Corrupt PNG checksum")
        if offset == 8 and kind != b"IHDR":
            raise ValueError("PNG must begin with IHDR")
        if kind == b"IHDR":
            if width or length != 13:
                raise ValueError("Invalid/duplicate PNG dimensions")
            width, height, depth, color, compression, filtering, interlace = struct.unpack(">IIBBBBB", payload)
            if not 0 < width <= 4096 or not 0 < height <= 4096 or depth != 8 or color not in (2, 6) or compression or filtering or interlace:
                raise ValueError("Unsupported or invalid Android screenshot format")
            channels = 3 if color == 2 else 4
        elif kind == b"IDAT":
            compressed.extend(payload)
        elif kind == b"IEND":
            if length or end != len(data):
                raise ValueError("Invalid PNG ending or trailing data")
            ended = True
        offset = end
    if not ended or not compressed or not width:
        raise ValueError("Incomplete PNG")
    expected = (width * channels + 1) * height
    decoder = zlib.decompressobj()
    raw = decoder.decompress(compressed, expected + 1)
    if len(raw) != expected or not decoder.eof or decoder.unused_data or decoder.unconsumed_tail:
        raise ValueError("Corrupt PNG image stream")
    if any(raw[row * (width * channels + 1)] > 4 for row in range(height)):
        raise ValueError("Invalid PNG row filter")
    return {"width": width, "height": height, "sha256": hashlib.sha256(data).hexdigest()}


class Driver:
    def __init__(self, arguments: argparse.Namespace):
        self.args = arguments
        self.sdk = arguments.sdk.resolve()
        self.apk = arguments.apk.resolve()
        self.output = arguments.output.resolve()
        self.output.parent.mkdir(parents=True, exist_ok=True)
        # Never mix a current invocation with old PASS records or screenshots.
        self.output.mkdir(exist_ok=False)
        self.environment = os.environ.copy()
        self.environment["ANDROID_SDK_ROOT"] = str(self.sdk)
        self.environment["ANDROID_HOME"] = str(self.sdk)
        self.adb = self.sdk / "platform-tools/adb"
        self.emulator = self.sdk / "emulator/emulator"
        self.avdmanager = self.sdk / "cmdline-tools/latest/bin/avdmanager"
        self.serial = f"emulator-{arguments.port}"
        self.process: subprocess.Popen | None = None
        self.adb_server_started = False
        self.current_stage = "preflight"
        self.report = {
            "schemaVersion": 1, "status": "RUNNING", "startedUtc": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
            "swtRevision": arguments.swt_revision, "nebulaRevision": arguments.nebula_revision,
            "githubRunId": os.environ.get("GITHUB_RUN_ID"), "githubRunAttempt": os.environ.get("GITHUB_RUN_ATTEMPT"),
            "driverSha256": sha256(Path(__file__)), "systemImage": SYSTEM_IMAGE,
            "vmAcceleration": "KVM_REQUIRED", "graphicsBackend": "swiftshader",
            "stages": {name: "NOT_RUN" for name in ("preflight", "boot", "install", "instrumentation", "screenshots")},
            "diagnosticErrors": [],
        }
        self.save()

    def save(self):
        (self.output / "native-report.json").write_text(json.dumps(self.report, indent=2) + "\n", encoding="utf-8")

    def stage(self, name: str):
        self.current_stage = name
        self.report["stages"][name] = "RUNNING"
        self.save()

    def passed(self):
        self.report["stages"][self.current_stage] = "PASSED"
        self.save()

    def command(self, arguments: list, *, timeout=30, check=True, binary=False, input_data=None):
        command = [str(value) for value in arguments]
        try:
            result = subprocess.run(command, input=input_data, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                    env=self.environment, timeout=timeout, check=False)
        except subprocess.TimeoutExpired as failure:
            with (self.output / "commands.log").open("a", encoding="utf-8") as log:
                log.write("$ " + shlex.join(command) + f"\nTIMED_OUT after {timeout} seconds\n")
                if not binary:
                    log.write((failure.stdout or b"").decode("utf-8", "replace"))
                log.write((failure.stderr or b"").decode("utf-8", "replace") + "\n")
            if "instrument" in command:
                (self.output / "instrumentation-partial.txt").write_bytes(failure.stdout or b"")
            raise
        with (self.output / "commands.log").open("a", encoding="utf-8") as log:
            log.write("$ " + shlex.join(command) + f"\nexit={result.returncode}\n")
            log.write(f"[binary stdout: {len(result.stdout)} bytes]\n" if binary else result.stdout.decode("utf-8", "replace"))
            log.write(result.stderr.decode("utf-8", "replace") + "\n")
        if check and result.returncode:
            raise RuntimeError(f"Command failed ({result.returncode}): {shlex.join(command)}")
        return result.stdout if binary else result.stdout.decode("utf-8", "replace").replace("\r\n", "\n")

    def device(self, *arguments, **options):
        return self.command([self.adb, "-s", self.serial, *arguments], **options)

    def screenshot(self, name: str, *arguments):
        data = self.device("exec-out", *arguments, binary=True)
        details = verify_png(data)
        (self.output / name).write_bytes(data)
        return details

    def run(self) -> bool:
        success = False
        with tempfile.TemporaryDirectory(prefix="swt-mobile-avd-") as temporary:
            self.environment["ANDROID_AVD_HOME"] = str(Path(temporary) / "avd")
            self.environment["ANDROID_USER_HOME"] = str(Path(temporary) / "user")
            Path(self.environment["ANDROID_AVD_HOME"]).mkdir()
            Path(self.environment["ANDROID_USER_HOME"]).mkdir()
            try:
                self.stage("preflight")
                if platform.system() != "Linux" or platform.machine() != "x86_64":
                    raise RuntimeError("This native gate requires a Linux x86_64 KVM host")
                if not Path("/dev/kvm").exists() or not stat.S_ISCHR(Path("/dev/kvm").stat().st_mode) or not os.access("/dev/kvm", os.R_OK | os.W_OK):
                    raise RuntimeError("Usable /dev/kvm is required; software VM fallback is not admitted")
                for path in (self.adb, self.emulator, self.avdmanager, self.apk,
                             self.sdk / "system-images/android-35/default/x86_64/system.img"):
                    if not path.is_file():
                        raise RuntimeError(f"Missing prepared SDK/APK input: {path}; install packages using already accepted licenses")
                acceleration = self.command([self.emulator, "-accel-check"])
                if "KVM" not in acceleration:
                    raise RuntimeError("The emulator did not confirm KVM acceleration")
                self.report["accelerationCheck"] = acceleration.strip()
                self.report["apkSha256"] = sha256(self.apk)
                self.report["emulatorVersion"] = self.command([self.emulator, "-version"]).strip()
                self.report["adbVersion"] = self.command([self.adb, "version"]).strip()
                for port in (self.args.port, self.args.port + 1):
                    with socket.socket() as probe:
                        probe.bind(("127.0.0.1", port))
                # A private server avoids changing another developer/device
                # session's adb credentials or stopping its server on cleanup.
                with socket.socket() as probe:
                    probe.bind(("127.0.0.1", 0))
                    server_port = probe.getsockname()[1]
                self.environment.pop("ADB_SERVER_SOCKET", None)
                self.environment["ANDROID_ADB_SERVER_ADDRESS"] = "127.0.0.1"
                self.environment["ANDROID_ADB_SERVER_PORT"] = str(server_port)
                self.report["adbServerPort"] = server_port
                self.command([self.adb, "start-server"])
                self.adb_server_started = True
                if self.serial in self.command([self.adb, "devices"]):
                    raise RuntimeError("Selected emulator serial is already in use")
                for name, path in (("system-image", self.sdk / "system-images/android-35/default/x86_64/source.properties"),
                                   ("emulator", self.sdk / "emulator/source.properties")):
                    if path.is_file():
                        (self.output / (name + "-source.properties")).write_bytes(path.read_bytes())
                self.command([self.avdmanager, "create", "avd", "--name", "swt-mobile-native", "--package", SYSTEM_IMAGE,
                              "--device", "pixel_2"], timeout=120, input_data=b"no\n")
                self.passed()

                self.stage("boot")
                emulator_command = [str(self.emulator), "-avd", "swt-mobile-native", "-port", str(self.args.port),
                                    "-accel", "on", "-gpu", "swiftshader", "-no-window", "-no-audio", "-no-snapshot",
                                    "-no-boot-anim", "-memory", "2048", "-cores", "2", "-camera-back", "none", "-camera-front", "none"]
                self.report["emulatorCommand"] = emulator_command
                self.save()
                with (self.output / "emulator.log").open("wb") as log:
                    self.process = subprocess.Popen(emulator_command, stdout=log, stderr=subprocess.STDOUT,
                                                    env=self.environment, start_new_session=True)
                deadline = time.monotonic() + self.args.boot_timeout
                while time.monotonic() < deadline:
                    if self.process.poll() is not None:
                        raise RuntimeError("Emulator exited before boot completed; see emulator.log")
                    if self.device("shell", "getprop", "sys.boot_completed", timeout=10, check=False).strip() == "1":
                        break
                    time.sleep(1)
                else:
                    raise TimeoutError("KVM emulator did not boot before the deadline; native tests did not run")
                sdk = self.device("shell", "getprop", "ro.build.version.sdk").strip()
                abi = self.device("shell", "getprop", "ro.product.cpu.abi").strip()
                if sdk != "35" or abi != "x86_64":
                    raise RuntimeError(f"Wrong emulator target: API {sdk}, ABI {abi}")
                self.report["deviceFingerprint"] = self.device("shell", "getprop", "ro.build.fingerprint").strip()
                self.device("shell", "input", "keyevent", "KEYCODE_WAKEUP")
                self.device("shell", "wm", "dismiss-keyguard")
                for setting in ("window_animation_scale", "transition_animation_scale", "animator_duration_scale"):
                    self.device("shell", "settings", "put", "global", setting, "0")
                self.passed()

                self.stage("install")
                self.device("install", "-t", str(self.apk), timeout=120)
                if self.device("shell", "pm", "clear", PACKAGE).strip() != "Success":
                    raise RuntimeError("Could not establish fresh application state")
                self.passed()

                self.stage("instrumentation")
                output = self.device("shell", "am", "instrument", "-w", "-r", COMPONENT, timeout=self.args.test_timeout)
                (self.output / "instrumentation.txt").write_text(output, encoding="utf-8")
                self.report["instrumentation"] = verify_instrumentation(output)
                self.passed()

                self.stage("screenshots")
                screenshots = {}
                for name in SCREENSHOTS:
                    screenshots[name] = self.screenshot(name, "run-as", PACKAGE, "cat", "files/mobile-proof/" + name)
                dimensions = {(entry["width"], entry["height"]) for entry in screenshots.values()}
                if len(dimensions) != 1 or screenshots["unselected.png"]["sha256"] == screenshots["selected.png"]["sha256"]:
                    raise ValueError("Widget screenshots have mismatched dimensions or unchanged selection evidence")
                self.report["swtVisibleScreenCaptures"] = screenshots
                self.passed()
                success = True
            except BaseException as failure:
                self.report["stages"][self.current_stage] = "FAILED"
                self.report["status"] = "FAILED"
                self.report["failure"] = f"{type(failure).__name__}: {failure}"
                (self.output / "failure.txt").write_text(traceback.format_exc(), encoding="utf-8")
                self.save()
            finally:
                if self.process is not None:
                    for name, command in (("logcat.txt", ("logcat", "-b", "all", "-d", "-v", "threadtime")),
                                          ("device-properties.txt", ("shell", "getprop"))):
                        try:
                            (self.output / name).write_text(self.device(*command, timeout=10), encoding="utf-8")
                        except Exception as failure:
                            self.report["diagnosticErrors"].append(f"{name}: {failure}")
                    try:
                        self.screenshot("emulator-at-completion.png", "screencap", "-p")
                    except Exception as failure:
                        self.report["diagnosticErrors"].append(f"completion screenshot: {failure}")
                    try:
                        os.killpg(self.process.pid, signal.SIGTERM)
                    except ProcessLookupError:
                        pass
                    try:
                        self.process.wait(timeout=10)
                    except subprocess.TimeoutExpired:
                        try:
                            os.killpg(self.process.pid, signal.SIGKILL)
                        except ProcessLookupError:
                            pass
                        self.process.wait(timeout=10)
                if self.adb_server_started:
                    try:
                        self.command([self.adb, "kill-server"], timeout=10)
                    except Exception as failure:
                        self.report["diagnosticErrors"].append(f"private adb server cleanup: {failure}")
                self.report["status"] = "PASSED" if success else "FAILED"
                self.report["finishedUtc"] = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
                self.save()
        return success


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sdk", type=Path, required=True)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True, help="New evidence directory; existing directories are refused")
    parser.add_argument("--swt-revision", required=True)
    parser.add_argument("--nebula-revision", required=True)
    parser.add_argument("--port", type=int, default=5554)
    parser.add_argument("--boot-timeout", type=int, default=300)
    parser.add_argument("--test-timeout", type=int, default=300)
    arguments = parser.parse_args()
    if not all(re.fullmatch(r"[0-9a-f]{40}", revision) and revision != "0" * 40 for revision in (arguments.swt_revision, arguments.nebula_revision)):
        parser.error("Both source revisions must be exact lowercase 40-character commit SHAs")
    if arguments.port < 5554 or arguments.port > 5682 or arguments.port % 2:
        parser.error("Choose an even emulator port from 5554 through 5682")
    if not 1 <= arguments.boot_timeout <= 600 or not 1 <= arguments.test_timeout <= 600:
        parser.error("Boot/test timeouts must be within 1..600 seconds")
    def interrupted(_signal, _frame):
        raise KeyboardInterrupt("Native gate interrupted")
    signal.signal(signal.SIGTERM, interrupted)
    try:
        driver = Driver(arguments)
        result = driver.run()
        print(json.dumps({"status": driver.report["status"], "report": str(driver.output / "native-report.json")}))
        return 0 if result else 1
    except Exception as failure:
        print(f"Android native gate refused: {failure}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
