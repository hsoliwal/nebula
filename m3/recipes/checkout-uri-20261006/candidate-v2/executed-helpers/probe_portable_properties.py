"""Execute the installed Maven launcher over tiny inherited POM fixtures only."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET

R = Path('S:/m3-nebula-portable-recipe-20261006')
assert (R / 'FOSS_REUSE_DECISION.tsv').is_file()
assert json.loads((R / 'inventory/INVENTORY.json').read_text())['state'] == 'INVENTORY_FROZEN_BEFORE_PORTABLE_CANDIDATE'
P = R / 'portable-property-proofs'
P.mkdir(exist_ok=True)
MAVEN = 'C:/ProgramData/chocolatey/lib/maven/apache-maven-3.9.14/bin/mvn.cmd'
MIRROR = 'S:/codex-work/m3-nebula-20261004/nebula-offline-mirror/repository'
N = 'http://maven.apache.org/POM/4.0.0'
ET.register_namespace('', N)
q = lambda tag: '{' + N + '}' + tag


def pom(path, artifact, parent=None, modules=(), properties=None, probe=False):
    root = ET.Element(q('project'))
    ET.SubElement(root, q('modelVersion')).text = '4.0.0'
    if parent:
        node = ET.SubElement(root, q('parent'))
        for tag, value in [('groupId', 'proof'), ('artifactId', parent[0]), ('version', '1'), ('relativePath', parent[1])]:
            ET.SubElement(node, q(tag)).text = value
    else:
        ET.SubElement(root, q('groupId')).text = 'proof'
        ET.SubElement(root, q('version')).text = '1'
    ET.SubElement(root, q('artifactId')).text = artifact
    ET.SubElement(root, q('packaging')).text = 'pom'
    if properties:
        node = ET.SubElement(root, q('properties'))
        for tag, value in properties.items():
            ET.SubElement(node, q(tag)).text = value
    if modules:
        node = ET.SubElement(root, q('modules'))
        for value in modules:
            ET.SubElement(node, q('module')).text = value
    if probe:
        plugin = ET.SubElement(ET.SubElement(ET.SubElement(root, q('build')), q('plugins')), q('plugin'))
        for tag, value in [('groupId', 'org.apache.maven.plugins'), ('artifactId', 'maven-antrun-plugin'), ('version', '3.1.0')]:
            ET.SubElement(plugin, q(tag)).text = value
        execution = ET.SubElement(ET.SubElement(plugin, q('executions')), q('execution'))
        ET.SubElement(execution, q('phase')).text = 'validate'
        ET.SubElement(ET.SubElement(execution, q('goals')), q('goal')).text = 'run'
        target = ET.SubElement(ET.SubElement(execution, q('configuration')), q('target'))
        ET.SubElement(target, q('echo'), {'file': '${project.basedir}/observed.txt'}).text = '\n'.join([
            'artifact=${project.artifactId}', 'basedir=${project.basedir}', 'baseUri=${project.baseUri}',
            'multi=${maven.multiModuleProjectDirectory}', 'qualified=${qualified.url}',
            'sessionRoot=${session.executionRootDirectory}',
        ])
    path.parent.mkdir(parents=True, exist_ok=True)
    ET.ElementTree(root).write(path, encoding='utf-8', xml_declaration=True)


receipts = []
for label in ('plain-root', 'root with spaces', 'root #percent%'):
    root = P / label
    (root / '.mvn').mkdir(parents=True, exist_ok=True)
    (root / '.mvn/maven.config').write_bytes(Path('S:/gm435314v3/owner74f/.mvn/maven.config').read_bytes())
    pom(root / 'pom.xml', 'aggregate', modules=('releng/parent',))
    pom(root / 'releng/parent/pom.xml', 'parent', modules=('../../widgets/grid',),
        properties={'qualified.url': '${m3.nebula.checkout.uri}/m3/qualified-mixed-original435-target'}, probe=True)
    pom(root / 'widgets/grid/pom.xml', 'grid', parent=('parent', '../../releng/parent/pom.xml'),
        modules=('child',))
    pom(root / 'widgets/grid/child/pom.xml', 'child', parent=('grid', '../pom.xml'))
    unrelated = P / 'unrelated-cwd'
    unrelated.mkdir(exist_ok=True)
    for variant, cwd, args in (
        ('root-cwd', root, []),
        ('unrelated-cwd-root-f', unrelated, ['-f', str(root / 'pom.xml')]),
        ('child-cwd', root / 'widgets/grid/child', []),
        ('unrelated-cwd-child-f', unrelated, ['-f', str(root / 'widgets/grid/child/pom.xml')]),
    ):
        env = os.environ.copy()
        env.update(JAVA_HOME='C:/Program Files/Java/jdk-21', MAVEN_SKIP_RC='1', MAVEN_OPTS='-Xmx512m -Dfile.encoding=UTF-8')
        env.pop('MAVEN_ARGS', None)
        env['MAVEN_BASEDIR'] = str(root)
        env['MAVEN_OPTS'] += ' -Dm3.nebula.checkout.uri=' + root.as_uri()
        command = [MAVEN, '-o', '-B', '-ntp', '-Dmaven.repo.local=' + MIRROR, *args, 'validate']
        result = subprocess.run(command, cwd=cwd, env=env, capture_output=True, text=True, errors='replace', timeout=90)
        log = P / (label.replace(' ', '_') + '-' + variant + '.log')
        log.write_text(result.stdout + result.stderr, encoding='utf-8')
        assert result.returncode == 0, log
        observed_files = list(root.rglob('observed.txt')) if variant in ('root-cwd', 'unrelated-cwd-root-f') else [root / 'widgets/grid/child/observed.txt']
        values = []
        for path in observed_files:
            props = dict(line.split('=', 1) for line in path.read_text().splitlines())
            assert Path(props['multi']).resolve() == root.resolve(), props
            assert props['qualified'] == (root / 'm3/qualified-mixed-original435-target').as_uri(), props
            values.append({'file': path.relative_to(root).as_posix(), 'values': props})
        receipts.append({'root': str(root), 'variant': variant, 'cwd': str(cwd), 'command': command,
                         'exit': result.returncode, 'observed': values,
                         'log_sha256': hashlib.sha256(log.read_bytes()).hexdigest()})
        print(label + ' ' + variant + ' PASS', flush=True)
(P / 'PORTABLE_MAVEN_PROPERTY_PROOF.json').write_text(json.dumps({'schema': 'synexia.nebula-portable-maven-property-proof.v1',
    'scope': 'tiny Maven-only inherited projection; not full Tycho/default reactor', 'trials': receipts}, indent=2) + '\n', encoding='utf-8')
