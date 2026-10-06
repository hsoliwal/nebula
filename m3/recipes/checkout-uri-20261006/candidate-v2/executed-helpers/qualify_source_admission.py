"""Bounded CPU/Git guards; tiny shared metadata fixture, no owner source writes."""
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess

R = Path('S:/m3-nebula-portable-recipe-20261006')
C = R / 'candidate'
OWNER = Path('S:/gm435314v3/owner74f')
OUT = R / 'source-admission-proof-final'
OUT.mkdir(exist_ok=False)
PY = 'C:/Program Files/Python312/python.exe'
rows = []
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
images = json.loads((C / 'manifest.json').read_text())
before = {r['path']: (sha(OWNER / r['path']), (OWNER / r['path']).stat().st_mtime_ns) for r in images['targets']}

def check(label, command, expected, success=False, env=None):
    result = subprocess.run(command, env=env, capture_output=True, text=True, errors='replace', timeout=40)
    message = result.stdout + result.stderr
    (OUT / (label + '.log')).write_text(message, encoding='utf-8')
    assert (result.returncode == 0) == success and expected in message, (label, message)
    rows.append({'name': label, 'exit': result.returncode, 'expected': expected, 'expected_success': success})
    print(label + ' PASS', flush=True)

context = [PY, '-B', str(C / 'source_context.py'), str(OWNER)]
check('owner-complete5398', context, 'SOURCE_CONTEXT_EXACT paths=5398', True)
git_override = os.environ.copy()
git_override.update({'GIT_DIR': 'S:/nonexistent-spoof-git', 'GIT_WORK_TREE': 'S:/spoof-root',
                     'GIT_INDEX_FILE': 'S:/spoof.index', 'GIT_COMMON_DIR': 'S:/spoof-common'})
check('git-head-overrides-dropped', [PY, '-I', '-B', str(C / 'source_context.py'), '--head', str(OWNER)], images['base_commit'], True, git_override)
check('git-source-overrides-dropped', context, 'SOURCE_CONTEXT_EXACT paths=5398', True, git_override)
for helper in ('source_context.py', 'run_portable_default.py'):
    args = [str(OWNER)] if helper == 'source_context.py' else ['--root', str(OWNER), '--output', str(R / 'full-default-replay')]
    for option in ('-O', '-OO'):
        check(helper.split('.')[0] + option, [PY, option, '-B', str(C / helper), *args], 'OPTIMIZED_PYTHON_REFUSAL')
    for value in ('1', '2'):
        env = os.environ.copy(); env['PYTHONOPTIMIZE'] = value
        check(helper.split('.')[0] + '-envopt' + value, [PY, '-B', str(C / helper), *args], 'OPTIMIZED_PYTHON_REFUSAL', env=env)
env = os.environ.copy(); env['PYTHONOPTIMIZE'] = '2'
check('isolated-context-ignores-envopt', [PY, '-I', '-B', str(C / 'source_context.py'), str(OWNER)], 'SOURCE_CONTEXT_EXACT paths=5398', True, env)
check('preimage-launch-refusal', [PY, '-B', str(C / 'run_portable_default.py'), '--root', str(OWNER), '--output', str(R / 'full-default-replay')], 'EXACT_PORTABLE_SOURCE_APPLY_REQUIRED')

fixture = OUT / 'tiny-source-root'
result = subprocess.run(['git', 'clone', '--shared', '--no-checkout', str(OWNER), str(fixture)], capture_output=True, text=True)
assert result.returncode == 0, result.stderr
subprocess.run(['git', '-C', str(fixture), 'read-tree', 'HEAD'], check=True)
for r in json.loads((C / 'INVENTORY.json').read_text())['qualified_metadata']:
    target = fixture / r['path']; target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(OWNER / r['path'], target)
command = [PY, '-I', '-B', str(C / 'source_context.py'), str(fixture)]
added = fixture / 'src/main/java/UnexpectedAddedInput.java'
added.parent.mkdir(parents=True); added.write_text('class UnexpectedAddedInput {}\n', encoding='utf-8')
check('new-java-input-refusal', command, 'ADDED_SOURCE_INPUT_REFUSAL')
(fixture / '.git/info/exclude').write_text('/src/main/java/UnexpectedAddedInput.java\n', encoding='utf-8')
check('ignored-new-java-input-refusal', command, 'ADDED_SOURCE_INPUT_REFUSAL')
added.unlink()
pom = fixture / 'new-child/pom.xml'; pom.parent.mkdir(); pom.write_text('<project/>\n', encoding='utf-8')
check('new-pom-input-refusal', command, 'ADDED_SOURCE_INPUT_REFUSAL'); pom.unlink()
generated = fixture / 'widgets/grid/target/generated/Generated.java'; generated.parent.mkdir(parents=True)
generated.write_text('class Generated {}\n', encoding='utf-8')
check('generated-output-and-four-metadata-accepted-before-absent-known-input', command, 'SOURCE_CONTEXT_ABSENT')

# Exercise the real launcher's environment leaf without pretending that the
# owner's preimage is eligible to run a portable reactor.
spec = importlib.util.spec_from_file_location('runtime_launcher', C / 'run_portable_default.py')
import sys
sys.path.insert(0, str(C))
module = importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
inherited = json.loads((C / 'INHERITED_DEFAULT_BUILD_COMMAND.json').read_text())
overrides = {'_JAVA_OPTIONS': '-Dm3.nebula.checkout.uri=file:///spoof-root',
             'JAVA_TOOL_OPTIONS': '-Dm3.nebula.checkout.uri=file:///spoof-root',
             'JDK_JAVA_OPTIONS': '-Dm3.nebula.checkout.uri=file:///spoof-root',
             'MAVEN_DEBUG_OPTS': '-Dm3.nebula.checkout.uri=file:///spoof-root',
             'MAVEN_ARGS': '-Dm3.nebula.checkout.uri=file:///spoof-root',
             'MAVEN_BASEDIR': 'S:/spoof-root', 'CLASSPATH': 'S:/spoof.jar'}
original = {key: os.environ.get(key) for key in overrides}
try:
    os.environ.update(overrides)
    env, cleared = module.prepare_runtime_environment(OWNER, OUT, inherited)
    assert all(key not in env for key in overrides if key != 'MAVEN_BASEDIR')
    assert env['MAVEN_BASEDIR'] == str(OWNER) and ('-Dm3.nebula.checkout.uri=' + OWNER.as_uri()) in env['MAVEN_OPTS']
    java = str(Path(inherited['env']['JAVA_HOME']) / 'bin/java.exe')
    check('java-option-injection-dropped', [java, '-Xms16m', '-Xmx32m', '-Dm3.nebula.checkout.uri=' + OWNER.as_uri(), '-XshowSettings:properties', '-version'], 'm3.nebula.checkout.uri = ' + OWNER.as_uri(), True, env)
finally:
    for key, value in original.items():
        if value is None: os.environ.pop(key, None)
        else: os.environ[key] = value
assert not (R / 'full-default-replay').exists()
after = {r['path']: (sha(OWNER / r['path']), (OWNER / r['path']).stat().st_mtime_ns) for r in images['targets']}
assert before == after
(OUT / 'SOURCE_ADMISSION_PROOF.json').write_text(json.dumps({'checks': rows, 'owner_images_bytes_and_mtime_unchanged': True,
    'whole_source_original5398_readonly_pass': True, 'full314_executed': False,
    'tiny_fixture_shared_git_index_only_no_source_tree_copy': True}, indent=2) + '\n', encoding='utf-8')
print('SOURCE_ADMISSION_PROOF_PASS checks=' + str(len(rows)), flush=True)
