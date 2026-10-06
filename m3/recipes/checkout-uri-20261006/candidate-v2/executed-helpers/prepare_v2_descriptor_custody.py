"""Bind the inherited pomless output owners before final native proof."""
import hashlib
import json
from pathlib import Path
import shutil

R = Path('S:/m3-nebula-portable-recipe-20261006')
C = R / 'candidate-v2'
OWNER = Path('S:/gm435314v3/owner74f')
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
source = json.loads((C / 'SOURCE_MANIFEST.json').read_text(encoding='utf-8'))
descriptors = [r for r in source['files'] if r['path'].split('/')[-1] in {'pom.xml', 'build.properties'}]
for row in descriptors: assert sha(OWNER / row['path']) == row['sha256']
directories = sorted({'/'.join(r['path'].split('/')[:-1] + ['target']) for r in descriptors})
(C / 'GENERATED_OUTPUT_OWNERS.json').write_text(json.dumps({'source_manifest_sha256': sha(C / 'SOURCE_MANIFEST.json'),
    'policy': 'Only frozen original Maven POM or Eclipse build.properties project owner target directories',
    'original_reactor_projects': 314, 'descriptor_count': len(descriptors), 'descriptors': descriptors,
    'generated_output_roots': directories, 'runtime_generated_file_bytes_owned_by_recipe': False,
    'new_java_package_target': 'REFUSED_BY_EXECUTED_DISCRIMINATOR'}, indent=2) + '\n', encoding='utf-8')
ledger = C / 'FOSS_REUSE_DECISION.tsv'
raw = ledger.read_text(encoding='utf-8')
raw = raw.replace('Existing exact Git/manifest admission and frozen Nebula POM owners',
                  'Existing exact Git/manifest admission and frozen original Nebula Maven/OSGi descriptor owners')
raw = raw.replace('SOURCE_MANIFEST.json pom.xml paths and source_context.py',
                  'SOURCE_MANIFEST.json original pom.xml/build.properties raw hashes and source_context.py')
raw = raw.replace('only target directories adjacent to frozen original POMs are generated outputs',
                  'only target directories adjacent to frozen original POM or Eclipse build.properties owners are generated outputs; actual314 includes Tycho pomless plugins')
ledger.write_text(raw, encoding='utf-8')
for path in sorted((R / 'source-admission-proof-v2-final').glob('*')):
    if path.is_file():
        destination = C / 'evidence/complete-source-admission-v2'
        destination.mkdir(exist_ok=True); shutil.copyfile(path, destination / path.name)
failed = C / 'evidence/retained-v2-resource-refusal'
failed.mkdir(exist_ok=True)
shutil.copyfile(R / 'recipe-evidence-final-v2/production-complete-owner-check.log', failed / 'maven-native-memory-refusal.log')
shutil.copyfile(R / 'source-admission-proof-v2/owner-complete5398.log', failed / 'initial-POM-only-pomless-output-refusal.log')
(failed / 'SCOPE.json').write_text(json.dumps({'native': 'JVM3MiB mmap Windows1455; before guard completion; not successful gate evidence',
    'incidental_error_file': 'E:/ws2/com.synexia/hs_err_pid21508.log retained by root',
    'initial_output_boundary': 'POM-only was safely too strict for inherited pomless Tycho plugins; corrected by frozen original build.properties owners',
    'owner_writes': False}, indent=2) + '\n', encoding='utf-8')
shutil.copyfile(R / 'prepare_v2_descriptor_custody.py', C / 'executed-helpers/prepare_v2_descriptor_custody.py')
print('FROZEN_GENERATED_OUTPUT_OWNERS descriptors=' + str(len(descriptors)) + ' target_roots=' + str(len(directories)), flush=True)
