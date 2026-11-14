#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal and contributors.
# SPDX-License-Identifier: EPL-2.0
"""Build the original Nebula mobile example with a selected SWT checkout.

Maven invokes this product SDK build bridge. It uses JDK21 and Android's actual
D8/aapt2/zipalign/apksigner tools. There is no Gradle, Kotlin, automatic SDK install,
release signing, device installation, source transformation, or simulated OS gate.
"""

import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile


MIN_API = TARGET_API = 35
BUILD_TOOLS = "36.0.0"
NEBULA = Path(__file__).resolve().parents[1]
ANDROID_NS = "{http://schemas.android.com/apk/res/android}"


def boolean(value):
    if value.lower() not in ("true", "false"):
        raise argparse.ArgumentTypeError("expected true or false")
    return value.lower() == "true"


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def executable(directory, name):
    path = Path(directory) / (name + (".exe" if os.name == "nt" else ""))
    if not path.is_file():
        raise ValueError(f"Required installed build tool is missing: {path}")
    return path


def write_jar(path, files):
    names = set()
    with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for name, file in sorted(files, key=lambda item: item[0]):
            if name in names:
                raise ValueError(f"Duplicate JAR entry: {name}")
            names.add(name)
            entry = zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0))
            entry.compress_type = zipfile.ZIP_DEFLATED
            entry.external_attr = 0o100644 << 16
            archive.writestr(entry, Path(file).read_bytes())


def source_argument(path):
    return '"' + str(path).replace("\\", "\\\\").replace('"', '\\"') + '"'


def selector(swt):
    path = swt / "mobile/prepare_sources.py"
    if not path.is_file():
        raise ValueError(f"SWT checkout lacks the admitted mobile source selection: {path}")
    spec = importlib.util.spec_from_file_location("swt_mobile_source_selection", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def read_manifest(nebula):
    path = nebula / "mobile/source-manifest.json"
    manifest = json.loads(path.read_text(encoding="utf-8"))
    if (manifest.get("schema"), manifest.get("javaRelease"), manifest.get("androidMinimumApi"),
        manifest.get("androidTargetApi"), manifest.get("androidBuildTools")) != (1, 21, 35, 35, BUILD_TOOLS):
        raise ValueError("Unsupported Nebula mobile source/toolchain manifest; API35 is the admitted minimum")
    return manifest


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--swt", required=True, type=Path, help="SWT checkout with its mobile platform sources")
    parser.add_argument("--nebula", type=Path, default=NEBULA)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--platform", choices=("android", "ios"), default="android")
    parser.add_argument("--sdk", type=Path, default=os.environ.get("ANDROID_SDK_ROOT") or os.environ.get("ANDROID_HOME"))
    parser.add_argument("--java-home", type=Path, default=os.environ.get("JAVA_HOME"))
    parser.add_argument("--swt-mobile-jar", type=Path, help="Compiled swt-mobile-ios.jar for the iOS application bytecode closure")
    parser.add_argument("--instrumentation", type=boolean, default=False)
    parser.add_argument("--min-api", type=int, default=MIN_API)
    args = parser.parse_args()
    if args.min_api != MIN_API:
        parser.error("This unchanged common-source closure is admitted only with Android minSdkVersion35; lower APIs require a separately proven closure")
    if args.platform != "android" and args.instrumentation:
        parser.error("Android instrumentation cannot be included in the iOS application JAR")
    swt, nebula, output = args.swt.resolve(), args.nebula.resolve(), args.output.resolve()
    if output in (swt, nebula, swt.parent, nebula.parent):
        parser.error("--output must be a dedicated build directory")
    if args.java_home is None:
        javac = shutil.which("javac")
        if not javac:
            parser.error("JDK21 is required; pass --java-home or set JAVA_HOME")
        java_home = Path(javac).resolve().parents[1]
    else:
        java_home = args.java_home.resolve()
    java = executable(java_home / "bin", "java")
    javac = executable(java_home / "bin", "javac")
    output.mkdir(parents=True, exist_ok=True)
    # Failed rebuilds cannot leave a prior report or executable artifact as fresh evidence.
    for name in ("build-report.json", "app-classes.jar", "nebula-mobile-application.jar",
                 "nebula-mobile-sources.jar", "swt-nebula-mobile-debug.apk",
                 "swt-nebula-mobile-instrumentation.apk", "resources.apk", "aligned.apk"):
        (output / name).unlink(missing_ok=True)
    log_path = output / "build.log"
    log_path.write_text("", encoding="utf-8")
    environment = dict(os.environ, JAVA_HOME=str(java_home))
    environment["PATH"] = str(java_home / "bin") + os.pathsep + environment.get("PATH", "")

    def run(command):
        command = [str(part) for part in command]
        print("RUN " + json.dumps(command), flush=True)
        result = subprocess.run(command, env=environment, text=True, stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT, check=False)
        print(result.stdout, end="" if result.stdout.endswith("\n") else "\n", flush=True)
        with log_path.open("a", encoding="utf-8") as log:
            log.write(json.dumps(command) + "\n" + result.stdout + "\n")
        if result.returncode != 0:
            raise subprocess.CalledProcessError(result.returncode, command, result.stdout)
        return result.stdout

    version = run([javac, "--version"]).strip()
    if re.match(r"javac 21(?:\.|\s|$)", version) is None:
        raise ValueError(f"The admitted build uses JDK21; observed {version}")
    select = selector(swt)
    manifest = read_manifest(nebula)
    application_sources = select.nebula_sources(swt, nebula)
    application_sources += select.checked_sources(nebula, manifest["application"], "nebula-app")
    sources = list(application_sources)
    classpath = []
    if args.platform == "android":
        if args.sdk is None:
            parser.error("Installed Android SDK35 is required; pass --sdk or set ANDROID_SDK_ROOT")
        sdk = args.sdk.resolve()
        android_jar = sdk / "platforms/android-35/android.jar"
        build_tools = sdk / "build-tools" / BUILD_TOOLS
        aapt2 = executable(build_tools, "aapt2")
        zipalign = executable(build_tools, "zipalign")
        d8 = build_tools / "lib/d8.jar"
        apksigner = build_tools / "lib/apksigner.jar"
        for required in (android_jar, d8, apksigner):
            if not required.is_file():
                raise ValueError(f"Required installed Android SDK35 file is missing: {required}")
        classpath.append(android_jar)
        sources = select.swt_sources(swt, "android") + sources
        if args.instrumentation:
            sources += select.checked_sources(nebula, manifest["instrumentation"], "nebula-android-test")
    else:
        swt_jar = args.swt_mobile_jar or swt / "mobile/target/swt-mobile-ios.jar"
        if not swt_jar.is_file():
            raise ValueError(f"Build the SWT iOS Java closure with Maven first, then pass --swt-mobile-jar: {swt_jar}")
        with zipfile.ZipFile(swt_jar) as archive:
            for name in ("org/eclipse/swt/widgets/Display.class", "org/eclipse/swt/accessibility/ACC.class",
                         "org/eclipse/swt/internal/ios/IOSPlatform.class", "org/eclipse/swt/internal/ios/OS.class"):
                if name not in archive.namelist():
                    raise ValueError(f"Selected SWT iOS JAR lacks actual required owner {name}")
        classpath.append(swt_jar)
    if any("tests/org.eclipse.swt.tests.mobile" in entry["path"] for entry in sources):
        raise ValueError("Headless Java2D contract transport must never enter a mobile application")
    classes = select.reset_child(output, "classes")
    source_args = output / "sources.args"
    source_args.write_text("\n".join(source_argument(entry["file"]) for entry in sources) + "\n", encoding="utf-8")
    run([javac, "--release", "21", "-encoding", "UTF-8", "-cp", os.pathsep.join(map(str, classpath)),
         "-d", classes, "@" + str(source_args)])
    resources = select.reset_child(output, "resources")
    if args.platform == "android":
        select.stage_resources(swt, resources)
    for source_name, target_name in (("LICENSE", "LICENSE-NEBULA"), ("NOTICES.MD", "NOTICE-NEBULA")):
        source = nebula / source_name
        if not source.is_file():
            raise ValueError(f"Missing source licensing notice: {source}")
        target = resources / "META-INF" / target_name
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, target)
    compiled_jar = output / ("app-classes.jar" if args.platform == "android" else "nebula-mobile-application.jar")
    jar_files = [(str(path.relative_to(classes)).replace(os.sep, "/"), path)
                 for path in classes.rglob("*") if path.is_file()]
    jar_files += [(str(path.relative_to(resources)).replace(os.sep, "/"), path)
                  for path in resources.rglob("*") if path.is_file()]
    write_jar(compiled_jar, jar_files)
    source_jar = output / "nebula-mobile-sources.jar"
    write_jar(source_jar, [(str(select.source_target(entry["path"])).replace(os.sep, "/"), entry["file"])
                          for entry in application_sources])
    report = {"schema": 1, "platform": args.platform, "javac": version,
              "buildStatus": "PASSED_ARTIFACT_BUILD_ONLY",
              "nativeQualification": "NOT_RUN_BY_BUILD", "instrumentationIncluded": args.instrumentation,
              "sources": select.portable_entries(sources),
              "outputs": {compiled_jar.name: digest(compiled_jar), source_jar.name: digest(source_jar)}}
    if args.platform == "android":
        dex = select.reset_child(output, "dex")
        d8_version = run([java, "-cp", d8, "com.android.tools.r8.D8", "--version"]).strip()
        d8_output = run([java, "-cp", d8, "com.android.tools.r8.D8", "--min-api", MIN_API, "--lib", android_jar,
                         "--output", dex, compiled_jar])
        if "not supported by this compiler" in d8_output:
            raise ValueError("Installed D8 does not support the required minimum API; no lower-API fallback is permitted")
        report["androidToolchain"] = {"d8Version": d8_version, "d8Sha256": digest(d8),
                                       "androidJarSha256": digest(android_jar),
                                       "apksignerSha256": digest(apksigner)}
        dex_files = sorted(dex.glob("*.dex"))
        if not dex_files:
            raise RuntimeError("D8 produced no executable DEX output")
        manifest_name = "AndroidManifest.instrumentation.xml" if args.instrumentation else "AndroidManifest.xml"
        android_manifest = nebula / "mobile/android" / manifest_name
        parsed = ET.parse(android_manifest).getroot()
        sdk_node = parsed.find("uses-sdk")
        application = parsed.find("application")
        if (sdk_node is None or sdk_node.get(ANDROID_NS + "minSdkVersion") != "35" or
                sdk_node.get(ANDROID_NS + "targetSdkVersion") != "35" or application is None or
                application.get(ANDROID_NS + "debuggable") != "true"):
            raise ValueError("The selected manifest must be an API35-only debug application")
        if bool(parsed.findall("instrumentation")) != args.instrumentation:
            raise ValueError("Instrumentation manifest and selected source closure disagree")
        package = output / "resources.apk"
        if package.exists():
            package.unlink()
        run([aapt2, "link", "-o", package, "--manifest", android_manifest, "-I", android_jar,
             "--min-sdk-version", MIN_API, "--target-sdk-version", TARGET_API])
        with zipfile.ZipFile(package, "a") as archive:
            for file in dex_files:
                archive.write(file, file.name)
            for file in sorted(resources.rglob("*")):
                if file.is_file():
                    archive.write(file, str(file.relative_to(resources)).replace(os.sep, "/"))
        aligned = output / "aligned.apk"
        run([zipalign, "-f", "-p", "4", package, aligned])
        # This path and public password are reserved exclusively for generated debug keys.
        # There is deliberately no user/release-keystore parameter.
        keytool = executable(java_home / "bin", "keytool")
        keystore = output / "generated-android-debug.p12"
        key_record = output / "generated-android-debug.json"
        if keystore.exists():
            if not key_record.is_file():
                raise ValueError("Existing debug-keystore path has no generated-debug marker; choose a fresh output directory")
            recorded = json.loads(key_record.read_text(encoding="utf-8"))
            if recorded.get("purpose") != "GENERATED_DEBUG_ONLY" or recorded.get("sha256") != digest(keystore):
                raise ValueError("Debug-keystore marker mismatch; distribution/user signing keys are not accepted")
        else:
            run([keytool, "-genkeypair", "-keystore", keystore, "-storetype", "PKCS12", "-storepass", "android",
                 "-keypass", "android", "-alias", "androiddebugkey", "-dname", "CN=Android Debug,O=SWT Mobile Test,C=US",
                 "-keyalg", "RSA", "-keysize", "2048", "-validity", "365"])
            key_record.write_text(json.dumps({"purpose": "GENERATED_DEBUG_ONLY", "sha256": digest(keystore)}, indent=2) + "\n", encoding="utf-8")
        apk = output / ("swt-nebula-mobile-instrumentation.apk" if args.instrumentation else "swt-nebula-mobile-debug.apk")
        run([java, "-jar", apksigner, "sign", "--ks", keystore, "--ks-key-alias", "androiddebugkey",
             "--ks-pass", "pass:android", "--key-pass", "pass:android", "--out", apk, aligned])
        run([java, "-jar", apksigner, "verify", "--verbose", "--print-certs", apk])
        run([zipalign, "-c", "-p", "4", apk])
        report.update({"minimumApi": MIN_API, "targetApi": TARGET_API, "buildTools": BUILD_TOOLS,
                       "manifest": {"path": str(android_manifest.relative_to(nebula)), "sha256": digest(android_manifest)},
                       "signing": "GENERATED_DEBUG_ONLY"})
        report["outputs"][apk.name] = digest(apk)
        print("DEBUG_APK " + str(apk), flush=True)
    else:
        report["swtIosJarSha256"] = digest(classpath[0])
        print("IOS_APPLICATION_BYTECODE " + str(compiled_jar) + " (not an AOT image or iOS runtime qualification)", flush=True)
    (output / "build-report.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, subprocess.CalledProcessError) as failure:
        print("BUILD FAILED: " + str(failure), file=sys.stderr)
        sys.exit(1)
