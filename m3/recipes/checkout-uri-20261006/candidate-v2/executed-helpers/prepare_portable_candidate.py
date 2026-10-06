"""Deterministic exact two-file address transform after frozen FOSS inventory."""
import hashlib
import json
from pathlib import Path
import shutil
import xml.etree.ElementTree as ET

R = Path('S:/m3-nebula-portable-recipe-20261006')
OWNER = Path('S:/gm435314v3/owner74f')
C = R / 'candidate'
sha = lambda data: hashlib.sha256(data).hexdigest()
inventory = json.loads((R / 'inventory/INVENTORY.json').read_text())
assert inventory['state'] == 'INVENTORY_FROZEN_BEFORE_PORTABLE_CANDIDATE'
assert (R / 'FOSS_REUSE_DECISION.tsv').read_text().count('\n') >= 5
assert json.loads((R / 'uri-proofs/EXACT_TYCHO_URI_PROOF.json').read_text())['javac_exit'] == 0
assert len(json.loads((R / 'property-proofs/MAVEN_PROPERTY_PROOF.json').read_text())['trials']) == 8
assert shutil.disk_usage(R).free >= 8 * 1024**3
C.mkdir(exist_ok=True)
admission = {'inventory_sha256': sha((R / 'inventory/INVENTORY.json').read_bytes()),
             'foss_reuse_sha256': sha((R / 'FOSS_REUSE_DECISION.tsv').read_bytes()),
             'admitted_gap': 'Two existing address-owner files only; one early encoded URI-valued launch property; no new build/module architecture'}
(C / 'INVENTORY_ADMISSION.json').write_text(json.dumps(admission, indent=2) + '\n', encoding='utf-8')
parent_path = 'releng/org.eclipse.nebula.nebula-parent/pom.xml'
target_path = 'm3/qualified-mixed-original435-target/mixed.target'
parent = (OWNER / parent_path).read_bytes()
target = (OWNER / target_path).read_bytes()
assert sha(parent) == inventory['current_parent']['sha256']
assert sha(target) == '646e2ca561f5249c63b81f935d7246922abe18dc62f5947f48e6fd144ded8a02'
old_uri = b'file:///S:/gm435314v3/owner74f/m3/qualified-mixed-original435-target'
assert parent.count(old_uri) == 3
parent_post = parent.replace(old_uri, b'${m3.nebula.checkout.uri}/m3/qualified-mixed-original435-target')
batik = b'file:///${maven.multiModuleProjectDirectory}/m3/third_party/p2-batik-20240313/mirror'
assert parent_post.count(batik) == 1
parent_post = parent_post.replace(batik, b'${m3.nebula.checkout.uri}/m3/third_party/p2-batik-20240313/mirror')
assert target.count(old_uri) == 1
target_post = target.replace(old_uri, b'${system_property:m3.nebula.checkout.uri}/m3/qualified-mixed-original435-target')
# XML custody: after replacing only the five address values back, originals are exact.
assert parent_post.replace(b'${m3.nebula.checkout.uri}/m3/qualified-mixed-original435-target', old_uri).replace(
    b'${m3.nebula.checkout.uri}/m3/third_party/p2-batik-20240313/mirror', batik) == parent
assert target_post.replace(b'${system_property:m3.nebula.checkout.uri}/m3/qualified-mixed-original435-target', old_uri) == target
assert len(ET.fromstring(target_post).findall('.//unit')) == 125
rows = []
for path, pre, post in ((parent_path, parent, parent_post), (target_path, target, target_post)):
    for state, data in (('preimages', pre), ('postimages', post)):
        out = C / state / path
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_bytes(data)
    rows.append({'path': path, 'before': 'PRESENT_EXACT_QUALIFIED_OWNER',
                 'pre_sha256': sha(pre), 'post_sha256': sha(post)})
manifest = {'schema': 'synexia.nebula-portable-address-images.v1',
            'base_commit': inventory['base_commit'], 'targets': rows,
            'new_owner_source_paths': [], 'source_file_count': 5399,
            'early_session_property': 'm3.nebula.checkout.uri',
            'uri_authority': 'actual checkout canonical root Path.as_uri(); launcher before Tycho resolution',
            'address_values_changed': 5, 'all_other_source_and_lifecycle_bytes_unchanged': True,
            'full_default_portable_replay_executed': False}
(C / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
for name in ('INVENTORY.json', 'SOURCE_MANIFEST.json', 'DEPENDENCY_MANIFEST.json', 'TOOL_ORACLE_DEPENDENCIES.json'):
    shutil.copyfile(R / 'inventory' / name, C / name)
shutil.copyfile(R / 'FOSS_REUSE_DECISION.tsv', C / 'FOSS_REUSE_DECISION.tsv')
metadata = C / 'qualified-metadata'
metadata.mkdir(exist_ok=True)
for name in ('content.xml', 'artifacts.xml', 'p2.index'):
    shutil.copyfile(OWNER / 'm3/qualified-mixed-original435-target' / name, metadata / name)
shutil.copyfile(Path('S:/gm435314v3/runs/first/DEFAULT_BUILD_COMMAND.json'), C / 'INHERITED_DEFAULT_BUILD_COMMAND.json')
shutil.copyfile(Path('S:/gm435314v3/runs/first/DEFAULT_BUILD_RESULT.json'), C / 'INHERITED_DEFAULT_BUILD_RESULT.json')
print(json.dumps({'targets': rows, 'source_targets': 2, 'changed_addresses': 5}, indent=2))
