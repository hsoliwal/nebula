"""Preserve original seal, refine only known Maven generated-output admission."""
import hashlib
import json
from pathlib import Path
import shutil

R = Path('S:/m3-nebula-portable-recipe-20261006')
old = R / 'candidate'
new = R / 'candidate-v2'
for line in (old / 'SHA256SUMS.tsv').read_text(encoding='utf-8').splitlines():
    digest, relative = line.split('\t')
    assert hashlib.sha256((old / relative).read_bytes()).hexdigest() == digest
shutil.copytree(old, new, ignore=shutil.ignore_patterns('target', '__pycache__', 'PUBLICATION_SEAL.json', 'SHA256SUMS.tsv', 'FINAL_OWNER_CUSTODY.json', 'ROOT_HANDOFF.md'))
with (new / 'FOSS_REUSE_DECISION.tsv').open('a', encoding='utf-8') as out:
    out.write('source-context-generated-output-boundary\tADAPT\tExisting exact Git/manifest admission and frozen Nebula POM owners\tHEAD74f97f3e7feb29239dbc0332e9206586d31f4722\tSOURCE_MANIFEST.json pom.xml paths and source_context.py\tOwned source and existing Eclipse notices retained\tGit lists unknown inputs; only target directories adjacent to frozen original POMs are generated outputs\tRemove broad target-component exclusion; admit added Java package named target discriminator\tINVENTORIED_BEFORE_SUCCESSOR_GUARD\n')
path = new / 'source_context.py'
raw = path.read_text(encoding='utf-8')
needle = "    # Git owns the source boundary. Include ignored inputs as well, excluding\n    # only the inherited Maven generated-output directories and Git metadata.\n"
replace = """    # Only target directories beside frozen Maven/OSGi project descriptors are
    # generated outputs. Tycho's original reactor includes pomless plugins.
    # A source package named target under src/main/java is still a build input.
    generated_dirs = set()
    for row in manifest['files']:
        parts = row['path'].split('/')
        if parts[-1] in {'pom.xml', 'build.properties'}:
            generated_dirs.add('/'.join(parts[:-1] + ['target']))

    def generated_output(path):
        parts = path.split('/')
        return any('/'.join(parts[:i]) in generated_dirs for i in range(1, len(parts)))

    # Git owns the source boundary. Include ignored inputs as well; Git itself
    # omits its private metadata, and output admission is scoped above.
"""
assert raw.count(needle) == 1
raw = raw.replace(needle, replace)
raw = raw.replace("        command += ['--', '.', ':(exclude)**/target/**', ':(exclude)target/**']", "        command += ['--', '.']")
raw = raw.replace("        added = set(result.stdout.decode('utf-8').split('\\0')) - {''} - allowed_untracked", "        unknown = set(result.stdout.decode('utf-8').split('\\0')) - {''} - allowed_untracked\n        added = {path for path in unknown if not generated_output(path)}")
path.write_text(raw, encoding='utf-8')
(new / 'SUPERSEDES.json').write_text(json.dumps({'immutable_prior_seal_sha256': hashlib.sha256((old / 'PUBLICATION_SEAL.json').read_bytes()).hexdigest(),
    'source_address_images_changed': False, 'reason': 'Known POM-owned target output directories only; added Java target package is source input.',
    'prior_owner_apply': False}, indent=2) + '\n', encoding='utf-8')
for old_name, new_name in [('build_portable_native_recipe.py', 'build_portable_native_recipe_v2.py'),
                           ('qualify_source_admission.py', 'qualify_source_admission_v2.py'),
                           ('qualify_native_bounded.py', 'qualify_native_bounded_v2.py'),
                           ('finalize_source_packet.py', 'finalize_source_packet_v2.py')]:
    raw = (R / old_name).read_text(encoding='utf-8')
    raw = raw.replace("C = R / 'candidate'", "C = R / 'candidate-v2'")
    raw = raw.replace("'source-admission-proof-final'", "'source-admission-proof-v2'")
    raw = raw.replace("'recipe-evidence-corrected-v4'", "'recipe-evidence-final-v2'")
    if old_name == 'qualify_source_admission.py':
        raw = raw.replace("fixture / 'widgets/grid/target/generated/Generated.java'", "fixture / 'releng/org.eclipse.nebula.nebula-parent/target/generated/Generated.java'")
        needle = "added.unlink()\npom = fixture"
        replace = """added.unlink()
target_package = fixture / 'src/main/java/demo/target/UnexpectedAddedInput.java'
target_package.parent.mkdir(parents=True)
target_package.write_text('package demo.target; class UnexpectedAddedInput {}\\n', encoding='utf-8')
check('new-java-target-package-refusal', command, 'ADDED_SOURCE_INPUT_REFUSAL')
target_package.unlink()
pom = fixture"""
        assert needle in raw; raw = raw.replace(needle, replace)
    if old_name == 'finalize_source_packet.py':
        raw = raw.replace("len(admission_proof['checks']) == 18", "len(admission_proof['checks']) == 19")
        raw = raw.replace('CPU/Git/JDK admission18', 'CPU/Git/JDK admission19').replace("'CPU_controls': 18", "'CPU_controls': 19")
        raw = raw.replace("'qualify_source_admission.py', 'qualify_native_bounded.py', 'finalize_source_packet.py'", "'qualify_source_admission_v2.py', 'qualify_native_bounded_v2.py', 'finalize_source_packet_v2.py', 'prepare_successor_v2.py'")
    (R / new_name).write_text(raw, encoding='utf-8')
print('SUCCESSOR_V2_PREPARED_OLD_SEAL_UNCHANGED', flush=True)
