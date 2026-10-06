"""Freeze existing Nebula authority before any portable candidate materialization."""
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import xml.etree.ElementTree as ET

S = Path('S:/m3-nebula-portable-recipe-20261006')
P = Path('S:/gm435314v3')
OWNER = P / 'owner74f'
I = S / 'inventory'
I.mkdir(parents=True, exist_ok=True)
sha = lambda path: hashlib.sha256(path.read_bytes()).hexdigest()
ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
material = json.loads((P / 'runs/first/SOURCE_MATERIALIZATION.json').read_text())
result = json.loads((P / 'runs/first/DEFAULT_BUILD_RESULT.json').read_text())
inputs = json.loads((P / 'INPUTS.json').read_text())
head = subprocess.check_output(['git', '-C', str(OWNER), 'rev-parse', 'HEAD'], text=True).strip()
assert head == material['source_base'] == '74f97f3e7feb29239dbc0332e9206586d31f4722'
assert result['exit'] == 0 and result['success'] == result['reactor_summary_rows'] == 314
assert result['failure'] == result['skipped'] == 0
free = shutil.disk_usage(S).free
assert free >= 8 * 1024**3, free
parent_path = material['sole_changed_tracked_path']
sources = []
for row in material['tracked_files']:
    path = OWNER / row['path']
    digest = sha(path)
    expected = material['parent_qualified_sha256'] if row['path'] == parent_path else row['sha256']
    assert digest == expected, row['path']
    sources.append({'path': row['path'], 'sha256': digest, 'bytes': path.stat().st_size,
                    'git_blob_oid_at_74f': row['git_blob_oid']})
assert len(sources) == 5399
(I / 'SOURCE_MANIFEST.json').write_text(json.dumps({
    'base_commit': head, 'files': sources, 'current_parent_sha256': material['parent_qualified_sha256'],
    'all_other_5398_source_paths_exact_original_74f': True,
    'source_authority': 'CURRENT_PROVEN_OWNER_NOT_STOCK_BASELINE',
}, indent=2) + '\n', encoding='utf-8', newline='\n')

target_dir = OWNER / 'm3/qualified-mixed-original435-target'
target = ET.parse(target_dir / 'mixed.target').getroot()
artifacts = ET.parse(target_dir / 'artifacts.xml').getroot()
parent = ET.parse(OWNER / parent_path).getroot()
properties = parent.find('m:properties', ns)
urls = {node.tag.split('}')[-1]: node.text for node in properties
        if node.tag.split('}')[-1].startswith('target-platform-')}
metadata = [{'path': (target_dir / name).relative_to(OWNER).as_posix(),
             'sha256': sha(target_dir / name), 'bytes': (target_dir / name).stat().st_size}
            for name in ('mixed.target', 'content.xml', 'artifacts.xml', 'p2.index')]
dependency_refs = [{'path': row['path'], 'sha256': row['sha256'], 'bytes': row['bytes']}
                   for row in inputs['files']]
for row in dependency_refs:
    assert sha(Path(row['path'])) == row['sha256'], row['path']
(I / 'DEPENDENCY_MANIFEST.json').write_text(json.dumps({
    'files': dependency_refs, 'candidate_runtime_jars': inputs['candidate_runtime_jars'],
    'existing_shared_maven_mirror': 'S:/codex-work/m3-nebula-20261004/nebula-offline-mirror/repository',
    'no_binary_or_cache_copy': True,
}, indent=2) + '\n', encoding='utf-8', newline='\n')
read_packets = [P / 'FOSS_REUSE_DECISION.tsv', P / 'BASE_HYPOTHESIS.json',
                P / 'materialize_and_build.py', P / 'prepare_recipe.py', P / 'pom.xml',
                P / 'runs/first/DEFAULT_BUILD_RESULT.json', P / 'runs/first/DEFAULT_BUILD_COMMAND.json',
                P / 'runs/first/SOURCE_MATERIALIZATION.json']
summary = {
    'state': 'INVENTORY_FROZEN_BEFORE_PORTABLE_CANDIDATE',
    'base_commit': head, 'current_source_files': 5399, 'original_reactor_projects': 314,
    'original_default_build': {'exit': 0, 'success': 314, 'failure': 0, 'skipped': 0,
                               'result_sha256': sha(P / 'runs/first/DEFAULT_BUILD_RESULT.json'),
                               'log_sha256': result['log_sha256']},
    'current_parent': {'path': parent_path, 'sha256': sha(OWNER / parent_path)},
    'parent_url_properties': urls,
    'target_repository_locations': [node.get('location') for node in target.findall('.//repository')],
    'target_units': len(target.findall('.//unit')),
    'p2_artifact_mappings': len(artifacts.findall('./mappings/rule')),
    'artifact_mapping_sample': [node.get('output') for node in artifacts.findall('./mappings/rule')[:2]],
    'qualified_metadata': metadata,
    'source_manifest_sha256': sha(I / 'SOURCE_MANIFEST.json'),
    'dependencies_manifest_sha256': sha(I / 'DEPENDENCY_MANIFEST.json'),
    'read_packets': [{'path': path.as_posix(), 'sha256': sha(path)} for path in read_packets],
    'reserve_bytes_before_candidate': free,
    'no_additional_AGENTS_in_owner_or_S_ancestors': True,
    'preserve': ['314 original project graph', '5398 other original raw source paths',
                 'all existing test and lifecycle configuration', 'qualified129 target units',
                 'three environments', 'JavaSE-21', 'current Tycho4.0.12', 'all qualified artifact hashes'],
    'scope_frontier': 'Root-local POM/target URL portability is separate from relocatability of 129 externally pinned artifact mappings',
}
(I / 'INVENTORY.json').write_text(json.dumps(summary, indent=2) + '\n', encoding='utf-8', newline='\n')
print(json.dumps({key: summary[key] for key in ('state', 'current_source_files', 'original_reactor_projects',
                                              'current_parent', 'parent_url_properties', 'target_repository_locations',
                                              'target_units', 'p2_artifact_mappings', 'reserve_bytes_before_candidate')}, indent=2))
