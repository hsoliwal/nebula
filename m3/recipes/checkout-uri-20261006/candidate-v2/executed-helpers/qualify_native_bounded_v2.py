"""Separate real complete-owner read-only admission from tiny writer rehearsal."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import xml.etree.ElementTree as ET

R = Path('S:/m3-nebula-portable-recipe-20261006')
C = R / 'candidate-v2'
OWNER = Path('S:/gm435314v3/owner74f')
OUT = R / 'recipe-evidence-final-v2-qualified'
OUT.mkdir(exist_ok=False)
RC = OUT / 'writer-rehearsal'
shutil.copytree(C, RC, ignore=shutil.ignore_patterns('target', '__pycache__'))
FIX = OUT / 'tiny-source-root'
subprocess.run(['git', 'clone', '--shared', '--no-checkout', str(OWNER), str(FIX)], check=True, capture_output=True)
subprocess.run(['git', '-C', str(FIX), 'read-tree', 'HEAD'], check=True)
images = json.loads((C / 'manifest.json').read_text())
inventory = json.loads((C / 'INVENTORY.json').read_text())
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
for row in images['targets']:
    dst = FIX / row['path']; dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(C / 'preimages' / row['path'], dst)
for row in inventory['qualified_metadata']:
    if row['path'] in {r['path'] for r in images['targets']}: continue
    dst = FIX / row['path']; dst.parent.mkdir(parents=True, exist_ok=True); shutil.copyfile(OWNER / row['path'], dst)

# This is a separate declared rehearsal POM, never the production admission.
ns = 'http://maven.apache.org/POM/4.0.0'
ET.register_namespace('', ns); ET.register_namespace('if', 'ant:if')
rehearsal = ET.parse(C / 'pom.xml')
rehearsal.getroot().find('{' + ns + '}artifactId').text = 'nebula-fixture-writer-rehearsal'
# Maven hands only target's DOM subtree to Ant, so the local namespace binding
# must survive even though ElementTree also declares it on the document root.
rehearsal.find('.//{' + ns + '}target').attrib['xmlns:if'] = 'ant:if'
changed = 0
for execute in rehearsal.findall('.//{' + ns + '}exec'):
    arguments = execute.findall('{' + ns + '}arg')
    if any(a.attrib.get('value') == '--head' for a in arguments): continue
    for arg in arguments:
        if arg.attrib.get('value') == '${project.basedir}/source_context.py':
            arg.attrib['value'] = '${project.basedir}/fixture_context.py'; changed += 1
assert changed == 1
for echo in rehearsal.findall('.//{' + ns + '}echo'):
    echo.attrib['message'] = echo.attrib['message'].replace('SEALED_CHECKOUT_URI_RECIPE operation=', 'FIXTURE_WRITER_ONLY_NOT_PRODUCTION_ADMISSION operation=')
rehearsal.write(RC / 'pom.xml', encoding='utf-8', xml_declaration=True)
(OUT / 'WRITER_REHEARSAL_DELTA.json').write_text(json.dumps({'production_pom_sha256': sha(C / 'pom.xml'),
    'rehearsal_pom_sha256': sha(RC / 'pom.xml'), 'delta': ['artifactId label', 'full-source callback replaced with fixture-only helper', 'echo scope label'],
    'production_context_override': False, 'production_apply_proof': False}, indent=2) + '\n')

env = os.environ.copy()
for key in ('MAVEN_ARGS', 'MAVEN_DEBUG_OPTS', '_JAVA_OPTIONS', 'JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS'):
    env.pop(key, None)
env.update({'JAVA_HOME': 'C:/Program Files/Java/jdk-21', 'MAVEN_SKIP_RC': '1', 'PYTHONDONTWRITEBYTECODE': '1',
    'MAVEN_OPTS': '-Xms8m -Xmx64m -XX:+UseSerialGC -XX:MaxMetaspaceSize=96m -XX:ReservedCodeCacheSize=16m -Xss256k -XX:ErrorFile=S:/m3-nebula-portable-recipe-20261006/jvm-refusals/hs_err_pid%p.log'})
(R / 'jvm-refusals').mkdir(exist_ok=True)
MAVEN = 'C:/ProgramData/chocolatey/lib/maven/apache-maven-3.9.14/bin/mvn.cmd'
rows = []
def snapshot(root):
    return {r['path']: [sha(root / r['path']), (root / r['path']).stat().st_mtime_ns] if (root / r['path']).exists() else None for r in images['targets']}
owner_before = snapshot(OWNER)
def run(name, operation, success, expected='', recipe=RC, root=FIX, extra=()):
    before = snapshot(root)
    command = [MAVEN, '-o', '-B', '-ntp', '-Dmaven.repo.local=S:/codex-work/m3-nebula-20261004/nebula-offline-mirror/repository',
               '-f', str(recipe / 'pom.xml'), '-Dm3.root=' + str(root), '-Dm3.operation=' + operation, *extra, 'verify']
    process = subprocess.run(command, env=env, cwd=OUT, capture_output=True, text=True, errors='replace', timeout=90)
    message = process.stdout + process.stderr
    (OUT / (name + '.log')).write_text(message, encoding='utf-8')
    assert (process.returncode == 0) == success and expected in message, (name, process.returncode, message[-2200:])
    after = snapshot(root)
    if operation != 'apply' or not success:
        assert before == after, name + ' changed source bytes or mtime'
    rows.append({'name': name, 'scope': 'production' if recipe == C else 'fixture-writer-only', 'exit': process.returncode,
                 'expected_success': success, 'expected_marker': expected, 'before': before, 'after': after})
    print(name + ' PASS', flush=True)

run('production-complete-owner-check', 'check', True, 'SOURCE_CONTEXT_EXACT paths=5398', C, OWNER)
run('production-incomplete-mutation-root-refusal', 'apply', False, 'SOURCE_CONTEXT_ABSENT', C)
for prop in ('m3.head', 'm3.pre', 'm3.post', 'm3.write', 'm3.context'):
    for value in ('false', 'true'):
        run('production-injected-' + prop.replace('.', '-') + '-' + value, 'check', False,
            'INTERNAL_PROPERTY_INJECTION_REFUSAL', C, FIX, ['-D' + prop + '=' + value])
run('writer-check-preimage', 'check', True, 'FIXTURE_WRITER_CONTEXT_ONLY_NOT_FULL_OWNER_ADMISSION')
run('writer-verify-preimage-refusal', 'verify', False, 'VERIFY_REQUIRES_POSTIMAGE_REFUSAL')
run('writer-fixedpoint-preimage-refusal', 'fixedpoint', False, 'VERIFY_REQUIRES_POSTIMAGE_REFUSAL')
run('writer-operation-refusal', 'replace', False, 'OPERATION_REFUSAL')
file = FIX / images['targets'][0]['path']; original = file.read_bytes(); file.write_bytes(original + b'#drift\n')
run('writer-source-drift-refusal', 'apply', False, 'UNKNOWN_OR_MIXED_SOURCE_STATE_REFUSAL'); file.write_bytes(original)
file.write_bytes((C / 'postimages' / images['targets'][0]['path']).read_bytes())
run('writer-mixed-state-refusal', 'apply', False, 'UNKNOWN_OR_MIXED_SOURCE_STATE_REFUSAL'); file.write_bytes(original)
file.unlink(); run('writer-absent-source-refusal', 'apply', False); file.write_bytes(original)
file = RC / 'postimages' / images['targets'][0]['path']; original = file.read_bytes(); file.write_bytes(original + b'#drift\n')
run('writer-nested-pom-image-custody-refusal', 'apply', False, 'RECIPE_INPUT_CUSTODY_REFUSAL'); file.write_bytes(original)
file = RC / 'source_context.py'; original = file.read_bytes(); file.write_bytes(original + b'\n')
run('writer-source-helper-custody-refusal', 'apply', False, 'RECIPE_INPUT_CUSTODY_REFUSAL'); file.write_bytes(original)
parent = subprocess.check_output(['git', '-C', str(FIX), 'rev-parse', 'HEAD^'], text=True).strip()
subprocess.run(['git', '-C', str(FIX), 'update-ref', 'HEAD', parent], check=True)
try: run('writer-wrong-head-refusal', 'apply', False, 'BASE_REVISION_REFUSAL')
finally: subprocess.run(['git', '-C', str(FIX), 'update-ref', 'HEAD', images['base_commit']], check=True)
metadata = FIX / 'm3/qualified-mixed-original435-target/content.xml'; original = metadata.read_bytes(); metadata.write_bytes(original + b'\n')
try: run('writer-metadata-drift-refusal', 'apply', False, 'SOURCE_CONTEXT_DRIFT_REFUSAL')
finally: metadata.write_bytes(original)
junction = OUT / 'junction-root'
result = subprocess.run(['cmd.exe', '/c', 'mklink', '/J', str(junction), str(FIX)], capture_output=True, text=True)
assert result.returncode == 0, result.stdout + result.stderr
run('writer-reparse-root-refusal', 'apply', False, 'WINDOWS_REPARSE_CUSTODY_REFUSAL', root=junction)
added = FIX / 'src/main/java/UnexpectedAddedInput.java'; added.parent.mkdir(parents=True, exist_ok=True)
added.write_text('class UnexpectedAddedInput {}\n', encoding='utf-8')
try: run('production-added-java-input-refusal', 'apply', False, 'ADDED_SOURCE_INPUT_REFUSAL', recipe=C)
finally: added.unlink()
run('writer-exact-apply', 'apply', True, 'FIXTURE_WRITER_ONLY_NOT_PRODUCTION_ADMISSION')
assert all(sha(FIX / r['path']) == r['post_sha256'] for r in images['targets'])
run('writer-postimage-verify', 'verify', True)
fixed = snapshot(FIX)
run('writer-postimage-fixedpoint', 'fixedpoint', True)
run('writer-second-apply-noop', 'apply', True)
assert fixed == snapshot(FIX) and owner_before == snapshot(OWNER)
(OUT / 'NATIVE_GUARD_PROOF.json').write_text(json.dumps({'production_complete_owner_readonly_check': True,
    'production_pom_sha256': sha(C / 'pom.xml'), 'source_context_sha256': sha(C / 'source_context.py'),
    'run_portable_default_sha256': sha(C / 'run_portable_default.py'),
    'production_apply_verify_fixedpoint': 'PENDING_ROOT', 'fixture_positive_protocol': 'SEPARATE_REHEARSAL_ONLY',
    'owner_bytes_and_mtime_unchanged': True, 'fixture_second_apply_bytes_and_mtime_unchanged': True,
    'resource_bound_vm_options': env['MAVEN_OPTS'], 'checks': rows}, indent=2) + '\n', encoding='utf-8')
print('QUALIFIED_BOUNDED_NATIVE_GUARDS count=' + str(len(rows)), flush=True)
