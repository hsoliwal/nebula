# SPDX-License-Identifier: EPL-2.0
"""Resolve and verify the two fixed Java17 proof SDK artifacts; no SDK binaries ship.

Adapted bounded intake/digest/archive evidence from the existing CPU Maven mirror:
mirror_resolved.py SHA256 8b3e323d730c53c9a14b14c8499e28775fecf4f64c7143f610886c50f9f01139.
Rule22 capability: nebula-java17-widget-proof-sdk-20261004. This test-only lane
does not replace Nebula's current dependency owner or claim all-SDK/UI behavior.
"""
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import struct
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

CENTRAL = "https://repo.maven.apache.org/maven2/org/eclipse/platform/"
ARTIFACTS = (
    ("org.eclipse.swt.win32.win32.x86_64", "3.126.0", 787,
     "72039b5a1552923783770f714f5ff982486ecbb48c95b0325881bce3691d5655",
     "69f996a91ad8a2c0dc599d94c4f6b6fbd6d343fc66efbb4cbc9e66fd27aeb565"),
    ("org.eclipse.jface", "3.33.0", 613,
     "ed854ce69edb931e954ba5a155fe0e8dd61ee5f65b99f33a1c767210e1ee2fb2",
     "6112debbe59dd8732c432ed3e28c9511bfd8776e10790a09aa3d6ee95edcc79c"),
)
LIMIT = 32 * 1024 * 1024


def require(condition, reason):
    if not condition:
        raise ValueError(reason)


def digest(data):
    return hashlib.sha256(data).hexdigest()


def intake(output, artifact, version, extension, expected, download):
    name = f"{artifact}-{version}.{extension}"
    target = output / name
    url = CENTRAL + f"{artifact}/{version}/{name}"
    if target.is_file():
        data = target.read_bytes()
    else:
        require(download, "SDK artifact absent; explicitly use --download: " + name)
        with urllib.request.urlopen(url, timeout=20) as response:
            data = response.read(LIMIT + 1)
        require(len(data) <= LIMIT, "SDK artifact exceeds size limit: " + name)
        require(digest(data) == expected, "Central artifact custody mismatch: " + name)
        target.write_bytes(data)
    require(digest(data) == expected, "Cached SDK artifact custody mismatch: " + name)
    return target, dict(path=name, url=url, sha256=expected, bytes=len(data))


def inspect(output, jar, pom, count):
    namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
    licenses = [value.text for value in ET.fromstring(pom.read_bytes()).findall(
        "m:licenses/m:license/m:name", namespace)]
    require(any("Eclipse Public License" in value and "2.0" in value for value in licenses),
            "SDK POM must preserve its EPL-2.0 declaration")
    metadata = output / (jar.stem + "-metadata")
    notices = []
    classes = 0
    with zipfile.ZipFile(jar) as archive:
        entries = archive.infolist()
        require(len(entries) < 10000, "SDK archive entry limit")
        require(len({entry.filename for entry in entries}) == len(entries), "Duplicate SDK archive entries")
        for entry in entries:
            name = entry.filename
            if name.endswith(".class"):
                header = archive.read(entry)[:8]
                require(len(header) == 8 and header[:4] == b"\xca\xfe\xba\xbe", "Invalid SDK class")
                require(struct.unpack(">H", header[6:8])[0] == 61, "Every SDK class must have Java17 major61")
                classes += 1
            if name in {"META-INF/MANIFEST.MF", "about.html"} or (
                    name.startswith("about_files/") and not entry.is_dir()):
                parts = PurePosixPath(name).parts
                require(not name.startswith("/") and ".." not in parts and ":" not in name,
                        "Unsafe SDK notice path")
                require(entry.file_size <= 1024 * 1024, "SDK notice size limit")
                data = archive.read(entry)
                target = metadata.joinpath(*parts)
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(data)
                notices.append(dict(path=target.relative_to(output).as_posix(), sha256=digest(data)))
    require(classes == count, "SDK class census drift")
    require(any(row["path"].endswith("/about.html") for row in notices), "Embedded SDK notices required")
    return dict(class_major=61, class_count=classes, licenses=licenses, notices=notices)


def resolve(output, download):
    output.mkdir(parents=True, exist_ok=True)
    records, jars = [], []
    for artifact, version, count, jar_hash, pom_hash in ARTIFACTS:
        jar, jar_record = intake(output, artifact, version, "jar", jar_hash, download)
        pom, pom_record = intake(output, artifact, version, "pom", pom_hash, download)
        records.append(dict(artifact=artifact, version=version, jar=jar_record, pom=pom_record,
                            evidence=inspect(output, jar, pom, count)))
        jars.append(jar.resolve())
    import os
    (output / "SDK_CLASSPATH.txt").write_text(os.pathsep.join(map(str, jars)), encoding="utf-8")
    receipt = dict(schema=1, scope="test-only JavaSE-17 real-source runtime proof SDK",
                   artifacts=records)
    (output / "SDK_CUSTODY.json").write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n",
                                            encoding="utf-8")
    print(json.dumps(dict(status="PASS", artifacts=len(records), class_major=61), sort_keys=True))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True, help="Task-owned SDK cache outside Git")
    parser.add_argument("--download", action="store_true", help="Fetch only fixed hash-bound Central artifacts")
    arguments = parser.parse_args()
    resolve(arguments.output, arguments.download)


if __name__ == "__main__":
    main()
