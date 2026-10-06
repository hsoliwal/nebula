"""Adapt the owned AntRun exact replay with complete read-only source-context admission."""
import hashlib
import json
from pathlib import Path
import shutil
import xml.etree.ElementTree as ET

R = Path('S:/m3-nebula-portable-recipe-20261006')
C = R / 'candidate-v2'
OWNER = Path('S:/gm435314v3/owner74f')
sha = lambda path: hashlib.sha256(path.read_bytes()).hexdigest()
manifest = json.loads((C / 'manifest.json').read_text())
inventory = json.loads((C / 'INVENTORY.json').read_text())
context = [row for row in inventory['qualified_metadata'] if row['path'] not in {r['path'] for r in manifest['targets']}]
(C / 'context.tsv').write_text(''.join(row['path'] + '\t' + row['sha256'] + '\n' for row in context), encoding='utf-8')
(C / 'targets.tsv').write_text(''.join(row['path'] + '\t' + row['pre_sha256'] + '\t' + row['post_sha256'] + '\n' for row in manifest['targets']), encoding='utf-8')
shutil.copyfile(Path('S:/m3-donor-tail-20261006/candidate/windows-reparse-preflight.ps1'), C / 'windows-reparse-preflight.ps1')
proofs = C / 'evidence/checkout-url-proofs'
proofs.mkdir(parents=True, exist_ok=True)
for label, source in (
    ('original-directory-property', R / 'property-proofs/MAVEN_PROPERTY_PROOF.json'),
    ('encoded-maven-property', R / 'portable-property-proofs/PORTABLE_MAVEN_PROPERTY_PROOF.json'),
    ('exact-tycho-uri', R / 'uri-proofs/EXACT_TYCHO_URI_PROOF.json'),
):
    data = json.loads(source.read_text())
    shutil.copyfile(source, proofs / (label + '.json'))
    assert all(trial['exit'] == 0 for trial in data['trials'])
for folder in ('property-proofs', 'portable-property-proofs', 'uri-proofs'):
    out = proofs / folder
    out.mkdir(exist_ok=True)
    for path in sorted((R / folder).glob('*.log')):
        shutil.copyfile(path, out / path.name)
    for path in sorted((R / folder).glob('INITIAL*.json')):
        shutil.copyfile(path, out / path.name)
for name in ('TargetUriProbe.java', 'probe_maven_properties.py', 'probe_portable_properties.py',
             'adapt_portable_property_probe.py', 'probe_tycho_uri.py', 'inventory_owner.py',
             'prepare_portable_candidate.py', 'build_portable_native_recipe.py'):
    out = C / 'executed-helpers' / name
    out.parent.mkdir(exist_ok=True)
    shutil.copyfile(R / name, out)
peer = C / 'evidence/independent-property-assessment'
peer.mkdir(exist_ok=True)
for path in sorted((R / 'peer').glob('*')):
    if path.is_file():
        shutil.copyfile(path, peer / path.name)
proof_receipt = {
    'scope': 'checkout URL model/parser/metadata relocation only; no full314 relocated runtime execution',
    'maven_directory_property_trials': 8,
    'encoded_maven_property_trials': 12,
    'exact_tycho_uri_trials': 4,
    'original_raw_windows_uri': 'REJECTED_BY_EXACT_TYCHO_METHOD',
    'unicode_maven_checkout': 'REFUSAL_RETAINED; full replay helper excludes this unqualified context',
    'external129_artifact_mappings': 'UNCHANGED_ABSOLUTE_INHERITED_RUNTIME_FRONTIER',
    'all_other_5398_source_context': 'FROZEN_AND_READ_ONLY_VERIFIED',
    'full314_portable_runtime': 'PENDING_ROOT_EXECUTION',
}
(C / 'CHECKOUT_URL_PROOF.json').write_text(json.dumps(proof_receipt, indent=2) + '\n', encoding='utf-8')

p = ET.Element('project', {'xmlns': 'http://maven.apache.org/POM/4.0.0', 'xmlns:if': 'ant:if'})
for key, value in [('modelVersion', '4.0.0'), ('groupId', 'com.synexia.recipe'),
                   ('artifactId', 'nebula-qualified-checkout-uri'), ('version', '1.0-SNAPSHOT'), ('packaging', 'pom')]:
    ET.SubElement(p, key).text = value
properties = ET.SubElement(p, 'properties')
ET.SubElement(properties, 'm3.operation').text = 'check'
plugin = ET.SubElement(ET.SubElement(ET.SubElement(p, 'build'), 'plugins'), 'plugin')
for key, value in [('groupId', 'org.apache.maven.plugins'), ('artifactId', 'maven-antrun-plugin'), ('version', '3.1.0')]:
    ET.SubElement(plugin, key).text = value
execution = ET.SubElement(ET.SubElement(plugin, 'executions'), 'execution')
ET.SubElement(execution, 'id').text = 'sealed-checkout-uri-source-replay'
ET.SubElement(execution, 'phase').text = 'verify'
ET.SubElement(ET.SubElement(execution, 'goals'), 'goal').text = 'run'
t = ET.SubElement(ET.SubElement(execution, 'configuration'), 'target', {'xmlns:if': 'ant:if'})


def refused(message):
    return ET.SubElement(ET.SubElement(ET.SubElement(t, 'fail', {'message': message}), 'condition'), 'not')


def checksum(parent, path, expected):
    return ET.SubElement(parent, 'checksum', {'file': path, 'algorithm': 'SHA-256', 'property': expected})


injected = ET.SubElement(ET.SubElement(ET.SubElement(t, 'fail', {'message': 'INTERNAL_PROPERTY_INJECTION_REFUSAL'}), 'condition'), 'or')
for prop in ('m3.head', 'm3.pre', 'm3.post', 'm3.write', 'm3.context'):
    ET.SubElement(injected, 'isset', {'property': prop})
ET.SubElement(refused('EXPLICIT_OWNER_ROOT_REQUIRED'), 'isset', {'property': 'm3.root'})
operations = ET.SubElement(refused('OPERATION_REFUSAL'), 'or')
for operation in ('check', 'apply', 'verify', 'fixedpoint'):
    ET.SubElement(operations, 'equals', {'arg1': '${m3.operation}', 'arg2': operation})
# The executable recipe binds its complete source-effect input closure. The
# publication seal separately binds all historical/final proof receipts; those
# are produced after execution and cannot be self-referential execution inputs.
effect_inputs = {
    'manifest.json', 'SOURCE_MANIFEST.json', 'INVENTORY.json', 'DEPENDENCY_MANIFEST.json',
    'TOOL_ORACLE_DEPENDENCIES.json', 'INHERITED_DEFAULT_BUILD_COMMAND.json',
    'source_context.py', 'run_portable_default.py', 'fixture_context.py',
    'windows-reparse-preflight.ps1', 'targets.tsv', 'context.tsv',
    'GENERATED_OUTPUT_OWNERS.json', 'FOSS_REUSE_DECISION.tsv', 'THIRD_PARTY_NOTICES.md',
}
for path in sorted(C.rglob('*')):
    relative = path.relative_to(C).as_posix()
    if path.is_file() and (relative in effect_inputs or relative.startswith(('preimages/', 'postimages/', 'qualified-metadata/'))):
        checksum(refused('RECIPE_INPUT_CUSTODY_REFUSAL ' + relative), '${project.basedir}/' + relative, sha(path))
command = ET.SubElement(t, 'exec', {'executable': 'powershell.exe', 'osfamily': 'windows', 'failonerror': 'true'})
for arg in ('-NoProfile', '-NonInteractive', '-File', '${project.basedir}/windows-reparse-preflight.ps1',
            '-Root', '${m3.root}', '-Source', '${project.basedir}/postimages',
            '-Manifest', '${project.basedir}/targets.tsv', '-Context', '${project.basedir}/context.tsv'):
    ET.SubElement(command, 'arg', {'value': arg})
command = ET.SubElement(t, 'exec', {'executable': 'C:/Program Files/Python312/python.exe', 'failonerror': 'true', 'outputproperty': 'm3.head'})
for arg in ('-I', '-B', '${project.basedir}/source_context.py', '--head', '${m3.root}'):
    ET.SubElement(command, 'arg', {'value': arg})
ET.SubElement(refused('BASE_REVISION_REFUSAL'), 'equals', {'arg1': '${m3.head}', 'arg2': manifest['base_commit']})
for row in context:
    checksum(refused('SOURCE_CONTEXT_DRIFT_REFUSAL ' + row['path']), '${m3.root}/' + row['path'], row['sha256'])
for state in ('pre', 'post'):
    node = ET.SubElement(ET.SubElement(t, 'condition', {'property': 'm3.' + state, 'else': 'false'}), 'and')
    for row in manifest['targets']:
        checksum(node, '${m3.root}/' + row['path'], row[state + '_sha256'])
either = ET.SubElement(refused('UNKNOWN_OR_MIXED_SOURCE_STATE_REFUSAL'), 'or')
for state in ('pre', 'post'):
    ET.SubElement(either, 'equals', {'arg1': '${m3.' + state + '}', 'arg2': 'true'})
ready = ET.SubElement(refused('VERIFY_REQUIRES_POSTIMAGE_REFUSAL'), 'or')
ET.SubElement(ready, 'equals', {'arg1': '${m3.post}', 'arg2': 'true'})
for operation in ('check', 'apply'):
    ET.SubElement(ready, 'equals', {'arg1': '${m3.operation}', 'arg2': operation})
command = ET.SubElement(t, 'exec', {'executable': 'C:/Program Files/Python312/python.exe', 'failonerror': 'true'})
for arg in ('-I', '-B', '${project.basedir}/source_context.py', '${m3.root}'):
    ET.SubElement(command, 'arg', {'value': arg})
node = ET.SubElement(ET.SubElement(t, 'condition', {'property': 'm3.write', 'else': 'false'}), 'and')
ET.SubElement(node, 'equals', {'arg1': '${m3.operation}', 'arg2': 'apply'})
ET.SubElement(node, 'equals', {'arg1': '${m3.pre}', 'arg2': 'true'})
for row in manifest['targets']:
    ET.SubElement(t, 'copy', {'file': '${project.basedir}/postimages/' + row['path'],
                            'tofile': '${m3.root}/' + row['path'], 'overwrite': 'true', 'if:true': '${m3.write}'})
ET.SubElement(t, 'echo', {'message': 'SEALED_CHECKOUT_URI_RECIPE operation=${m3.operation} pre=${m3.pre} post=${m3.post} write=${m3.write}; full314 relocated runtime pending'})
ET.indent(p, space='  ')
ET.ElementTree(p).write(C / 'pom.xml', encoding='utf-8', xml_declaration=True)
print('PORTABLE_SOURCE_RECIPE_CREATED sha256=' + sha(C / 'pom.xml'))
