"""Compile/run tiny exact installed Tycho URI oracle with shared donor jars."""
import hashlib
import json
from pathlib import Path
import subprocess
import shutil
import xml.etree.ElementTree as ET

R = Path('S:/m3-nebula-portable-recipe-20261006')
assert (R / 'FOSS_REUSE_DECISION.tsv').is_file()
P = R / 'uri-proofs'
P.mkdir(exist_ok=True)
tycho = Path('S:/codex-work/m3-nebula-20261004/nebula-offline-mirror/repository/org/eclipse/tycho')
maven = Path('C:/ProgramData/chocolatey/lib/maven/apache-maven-3.9.14')
jdk = Path('C:/Program Files/Java/jdk-21/bin')
inputs = json.loads((R / 'inventory/DEPENDENCY_MANIFEST.json').read_text())
extras = [tycho.parent / 'platform' / name / version / (name + '-' + version + '.jar')
          for name, version in [('org.eclipse.equinox.p2.updatesite', '1.3.500'),
                                ('org.eclipse.equinox.p2.publisher.eclipse', '1.6.300'),
                                ('org.eclipse.equinox.p2.publisher', '1.9.300')]]
if (P / 'plain-root.log').is_file() and not (P / 'INITIAL_MISSING_RUNTIME_DEPENDENCY.log').exists():
    (P / 'INITIAL_MISSING_RUNTIME_DEPENDENCY.log').write_bytes((P / 'plain-root.log').read_bytes())
elif (P / 'plain-root.log').is_file() and not (P / 'INITIAL_ARGUMENT_NORMALIZATION_FALSE_DISCRIMINATOR.log').exists():
    (P / 'INITIAL_ARGUMENT_NORMALIZATION_FALSE_DISCRIMINATOR.log').write_bytes((P / 'plain-root.log').read_bytes())
(R / 'inventory/TOOL_ORACLE_DEPENDENCIES.json').write_text(json.dumps({
    'authority': 'Tycho4.0.12 p2-maven-plugin/parent POM pinned tool dependencies; not new module runtime providers',
    'files': [{'path': str(path), 'sha256': hashlib.sha256(path.read_bytes()).hexdigest()} for path in extras],
    'initial_refusal': 'exact resolver first probe lacked installed tool p2.updatesite class; retained in uri-proofs/INITIAL_MISSING_RUNTIME_DEPENDENCY.log',
}, indent=2) + '\n', encoding='utf-8')
jars = sorted(set([str(path) for path in tycho.rglob('*4.0.12.jar')] +
                  [str(path) for path in (maven / 'lib').glob('*.jar')] + inputs['candidate_runtime_jars'] +
                  [str(path) for path in extras]))
classpath = ';'.join(jars)
quote = lambda value: '"' + str(value).replace('\\', '/') + '"'
compile_args = P / 'javac.args'
compile_args.write_text('\n'.join(['--release', '21', '-Xlint:all', '-Werror', '-classpath',
                                 quote(classpath), '-d', quote(P / 'classes'), quote(R / 'TargetUriProbe.java')]) + '\n', encoding='utf-8')
result = subprocess.run([str(jdk / 'javac.exe'), '@' + str(compile_args)], capture_output=True, text=True, errors='replace', timeout=90)
(P / 'javac.log').write_text(result.stdout + result.stderr, encoding='utf-8')
assert result.returncode == 0, result.stdout + result.stderr
receipts = []
location = ET.parse(R / 'candidate/postimages/m3/qualified-mixed-original435-target/mixed.target').find('.//repository').get('location')
for index, label in enumerate(('plain-root', 'root with spaces', 'root #percent%', 'root #percent% unicode測試')):
    root = R / 'portable-property-proofs' / label
    assert root.is_dir(), root
    target = root / 'm3/qualified-mixed-original435-target'
    target.mkdir(parents=True, exist_ok=True)
    for name in ('content.xml', 'artifacts.xml', 'p2.index'):
        shutil.copyfile(R / 'candidate/qualified-metadata' / name, target / name)
    shutil.copyfile(R / 'candidate/postimages/m3/qualified-mixed-original435-target/mixed.target', target / 'mixed.target')
    args = P / ('trial-' + str(index) + '.args')
    content_sha = hashlib.sha256((target / 'content.xml').read_bytes()).hexdigest()
    args.write_text('\n'.join(['-cp', quote(str(P / 'classes') + ';' + classpath), 'TargetUriProbe', quote(root.as_uri()), quote(location), content_sha]) + '\n', encoding='utf-8')
    result = subprocess.run([str(jdk / 'java.exe'), '@' + str(args)], capture_output=True, text=True, errors='replace', timeout=90)
    log = P / ('trial-' + str(index) + '.log')
    log.write_text(result.stdout + result.stderr, encoding='utf-8')
    assert result.returncode == 0, result.stdout + result.stderr
    assert result.stdout.startswith('EXACT_TYCHO_URI_PASS\t'), result.stdout
    receipts.append({'root': str(root), 'exit': result.returncode, 'output': result.stdout.strip(),
                     'log_sha256': hashlib.sha256(log.read_bytes()).hexdigest()})
    print(result.stdout.strip(), flush=True)
(P / 'EXACT_TYCHO_URI_PROOF.json').write_text(json.dumps({
    'scope': 'exact Tycho4.0.12 repository URI method; no full reactor/native claim',
    'javac': '--release 21 -Xlint:all -Werror', 'javac_exit': 0,
    'oracle_source_sha256': hashlib.sha256((R / 'TargetUriProbe.java').read_bytes()).hexdigest(),
    'shared_jar_count': len(jars), 'no_binary_copy': True, 'trials': receipts,
}, indent=2) + '\n', encoding='utf-8')
