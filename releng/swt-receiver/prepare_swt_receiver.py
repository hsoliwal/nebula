# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Bind a Tycho-built SWT receiver to its source, GC bytecode and native payloads."""
from pathlib import Path
import hashlib
import json
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile

PIN = 'f87d0b656a20135bdc54c954bf7e6e5242eb2248'
GC_SOURCE = 'bundles/org.eclipse.swt/Eclipse SWT/gtk/org/eclipse/swt/graphics/GC.java'
GC_SHA256 = '51ccf2acd0c803258f1c9cee09f42f6806bd05eddae8cd176e9178414768c8c9'
GC_CLASS = 'org/eclipse/swt/graphics/GC.class'
CANVAS_SOURCE = 'bundles/org.eclipse.swt/Eclipse SWT/gtk/org/eclipse/swt/widgets/Canvas.java'
CANVAS_GIT_BLOB = 'e53a8bf0273b88093b3ea5c049bd9bf595432280'
REQUIRED_CANVAS_DESCRIPTORS = (
    'int addLayer(org.eclipse.swt.graphics.ImageData);',
    'int[] getLayerIds();',
    'void removeLayer(int);',
    'void setLayerLocation(int, int, int);',
    'void setLayerTransform(int, org.eclipse.swt.graphics.Transform);',
    'void setLayerClip(int, org.eclipse.swt.graphics.Rectangle);',
    'void setLayerOpacity(int, int);',
    'int getLayerAtPoint(int, int);',
    'void setLayerZOrder(int, int);',
)


def digest(data):
    return hashlib.sha256(data).hexdigest()


def prepare(source, output, repository):
    # Refuse a different source revision, even if a partial path diff is empty.
    if subprocess.check_output(['git', '-C', str(source), 'rev-parse', 'HEAD'],
                               text=True).strip() != PIN:
        raise ValueError('SWT checkout does not match pinned source revision')
    # Inspect the whole source owner and fragment build configuration. Native .so files
    # are build outputs, so their byte identities are recorded below instead.
    subprocess.run(['git', '-C', str(source), 'diff', '--quiet', PIN, '--',
                    'pom.xml', 'bundles/org.eclipse.swt', 'binaries/pom.xml',
                    'binaries/org.eclipse.swt.gtk.linux.x86_64/META-INF/MANIFEST.MF',
                    'binaries/org.eclipse.swt.gtk.linux.x86_64/build.properties'], check=True)
    if subprocess.check_output(['git', '-C', str(source), 'ls-files', '--others',
                                '--exclude-standard', '--', 'bundles/org.eclipse.swt']).strip():
        raise ValueError('Untracked SWT source inputs')
    if digest((source / GC_SOURCE).read_bytes()) != GC_SHA256:
        raise ValueError('Unqualified GTK GC source')
    actual_canvas = subprocess.check_output(['git', '-C', str(source),
                    'hash-object', CANVAS_SOURCE], text=True).strip()
    if actual_canvas != CANVAS_GIT_BLOB:
        raise ValueError('Unqualified GTK Canvas native layer API source')
    jars = [source / 'bundles/org.eclipse.swt/target/org.eclipse.swt-3.138.0-SNAPSHOT.jar',
            source / 'binaries/org.eclipse.swt.gtk.linux.x86_64/target/org.eclipse.swt.gtk.linux.x86_64-3.138.0-SNAPSHOT.jar']
    gtk_jar = jars[1]
    # Tycho resolves the parent's three environments even for Linux tests. Include
    # the other Java fragments as resolution inputs; their native execution is unqualified.
    for platform in ('cocoa.macosx.aarch64', 'cocoa.macosx.x86_64', 'gtk.linux.aarch64',
                     'gtk.linux.ppc64le', 'gtk.linux.riscv64', 'win32.win32.aarch64', 'win32.win32.x86_64'):
        jars.append(source / ('binaries/org.eclipse.swt.' + platform + '/target/org.eclipse.swt.' + platform + '-3.138.0-SNAPSHOT.jar'))
    source_jar = gtk_jar.with_name(gtk_jar.stem + '-sources.jar')
    with zipfile.ZipFile(source_jar) as archive:
        if digest(archive.read('org/eclipse/swt/graphics/GC.java')) != GC_SHA256:
            raise ValueError('Packaged GTK GC source differs')
        if archive.read('org/eclipse/swt/widgets/Canvas.java') != (source / CANVAS_SOURCE).read_bytes():
            raise ValueError('Packaged GTK Canvas source differs from source-validated owner')
    with zipfile.ZipFile(gtk_jar) as archive:
        gc_hash = digest(archive.read(GC_CLASS))
        if 'org/eclipse/swt/widgets/Canvas.class' not in archive.namelist():
            raise ValueError('Missing compiled GTK Canvas class')
        natives = {name: digest(archive.read(name)) for name in archive.namelist() if name.endswith('.so')}
        if not all(any(name.startswith(prefix) and archive.read(name).startswith(b'\x7fELF') for name in natives) for prefix in
                   ('libswt-gtk-', 'libswt-pi3-gtk-', 'libswt-cairo-gtk-', 'libswt-atk-gtk-')):
            raise ValueError('Missing source-built GTK3 JNI payloads')
    api = subprocess.check_output(['javap', '-classpath', str(gtk_jar),
                                   'org.eclipse.swt.widgets.Canvas'], text=True)
    for descriptor in REQUIRED_CANVAS_DESCRIPTORS:
        if descriptor not in api:
            raise ValueError('Pinned SWT Canvas layer API missing: ' + descriptor)
    plugins = output / 'input/plugins'
    plugins.mkdir(parents=True, exist_ok=False)
    for jar in jars:
        shutil.copyfile(jar, plugins / jar.name)
    composite = output / 'composite'
    composite.mkdir()
    for kind, family in [('Content', 'metadata'), ('Artifacts', 'artifact')]:
        typename = 'CompositeMetadataRepository' if family == 'metadata' else 'CompositeArtifactRepository'
        tree = ET.Element('repository', {'name': 'Pinned SWT receiving proof with Eclipse platform',
            'type': 'org.eclipse.equinox.internal.p2.' + family + '.repository.' + typename, 'version': '1.0.0'})
        children = ET.SubElement(tree, 'children', {'size': '3'})
        for uri in [repository.as_uri(),
                    'https://download.eclipse.org/eclipse/updates/4.42-I-builds/I20261112-2300/',
                    'https://download.eclipse.org/eclipse/updates/latest']:
            ET.SubElement(children, 'child', {'location': uri})
        header = '<?xml version="1.0" encoding="UTF-8"?>\n<?composite' + ('Metadata' if family == 'metadata' else 'Artifact') + 'Repository version="1.0.0"?>\n'
        (composite / ('composite' + kind + '.xml')).write_text(header + ET.tostring(tree, encoding='unicode') + '\n')
    receipt = {'sourceCommit': PIN, 'sourceWorktreeHead': subprocess.check_output(
        ['git', '-C', str(source), 'rev-parse', 'HEAD'], text=True).strip(),
        'gcSourceSha256': GC_SHA256, 'gcClassSha256': gc_hash,
        'bundleSha256': {jar.name: digest(jar.read_bytes()) for jar in jars},
        'nativeSha256': natives, 'qualifiedBackend': 'GTK3/X11 only; GTK4 payload is not qualified', 'runtimeQualification': 'PENDING'}
    (output / 'expected.json').write_text(json.dumps(receipt, indent=2) + '\n')
    (output / 'receiver.env').write_text('M3_SWT_GC_SHA256=' + gc_hash + '\nM3_SWT_PLATFORM=' + composite.as_uri() + '\n')
    print(json.dumps(receipt, sort_keys=True))


if __name__ == '__main__':
    if len(sys.argv) != 4:
        raise SystemExit('Usage: prepare_swt_receiver.py SWT_CHECKOUT NEW_OUTPUT_DIR P2_REPOSITORY')
    prepare(*(Path(arg).resolve() for arg in sys.argv[1:]))
