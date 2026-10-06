"""Freeze source-only custody and focused receipts, without owner materialization."""
import ctypes
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys

R = Path('S:/m3-nebula-portable-recipe-20261006')
C = R / 'candidate'
OWNER = Path('S:/gm435314v3/owner74f')
sys.path.insert(0, str(C))
from source_context import read_revision, verify_context
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
assert not (C / 'PUBLICATION_SEAL.json').exists(), 'already sealed'
assert verify_context(OWNER) == 5398
images = json.loads((C / 'manifest.json').read_text())
assert read_revision(OWNER) == images['base_commit']
for row in images['targets']:
    assert sha(OWNER / row['path']) == row['pre_sha256']
    assert sha(C / 'preimages' / row['path']) == row['pre_sha256']
    assert sha(C / 'postimages' / row['path']) == row['post_sha256']
deps = json.loads((C / 'DEPENDENCY_MANIFEST.json').read_text())
for row in deps['files']:
    assert sha(Path(row['path'])) == row['sha256'], row['path']
source = json.loads((C / 'SOURCE_MANIFEST.json').read_text())
assert len(source['files']) == 5399
for row in source['files']:
    assert sha(OWNER / row['path']) == row['sha256'], row['path']
free = shutil.disk_usage(R).free
assert free >= 8 * 1024**3
status = subprocess.check_output(['git', '-C', str(OWNER), 'status', '--short', '--untracked-files=all'], text=True)
assert len(status.splitlines()) == 5
class MemoryStatus(ctypes.Structure):
    _fields_ = [('length', ctypes.c_ulong), ('load', ctypes.c_ulong),
               ('total_phys', ctypes.c_ulonglong), ('available_phys', ctypes.c_ulonglong),
               ('total_page', ctypes.c_ulonglong), ('available_page', ctypes.c_ulonglong),
               ('total_virtual', ctypes.c_ulonglong), ('available_virtual', ctypes.c_ulonglong),
               ('available_extended', ctypes.c_ulonglong)]
memory = MemoryStatus(); memory.length = ctypes.sizeof(memory)
assert ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(memory))
(C / 'FINAL_OWNER_CUSTODY.json').write_text(json.dumps({'base_commit': read_revision(OWNER),
    'current_source_files_all_raw_verified': 5399, 'source_context_retained_paths': 5398,
    'owner_targets': images['targets'], 'owner_state': 'ALL_PREIMAGE_UNMODIFIED_BY_WORKER',
    'status': status, 'runtime_dependency_inputs': len(deps['files']), 'runtime_dependency_hashes': 'ALL_MATCH',
    'S_free_bytes': free, 'S_minimum_reserve_bytes': 8 * 1024**3,
    'available_windows_commit_bytes': memory.available_page,
    'full314_new_replay': 'PENDING_ROOT_HEADROOM_AND_EXECUTION'}, indent=2) + '\n', encoding='utf-8')

native = R / 'recipe-evidence-corrected-v4'
native_proof = json.loads((native / 'NATIVE_GUARD_PROOF.json').read_text())
assert len(native_proof['checks']) == 29 and native_proof['owner_bytes_and_mtime_unchanged']
admission = R / 'source-admission-proof-final'
admission_proof = json.loads((admission / 'SOURCE_ADMISSION_PROOF.json').read_text())
assert len(admission_proof['checks']) == 18
for name, folder in [('complete-source-admission', admission), ('native-guard-proof', native)]:
    destination = C / 'evidence' / name
    destination.mkdir(exist_ok=True)
    for path in sorted(folder.glob('*')):
        if path.is_file() and path.suffix in {'.json', '.log'}:
            shutil.copyfile(path, destination / path.name)
shutil.copyfile(native / 'writer-rehearsal/pom.xml', C / 'evidence/native-guard-proof/WRITER_REHEARSAL_POM.xml')
failures = []
for label, cause in [
    ('recipe-evidence-initial-custody-refusal', 'Prototype excluded nested POM source image from checksum by basename; negative candidate drift test caught copied mutation in tiny fixture only.'),
    ('recipe-evidence-corrected', 'Separate writer harness counted checksum path as execution callback; stopped before native writer proof.'),
    ('recipe-evidence-corrected-v2', 'Separate writer callback string replacement missed actual XML indent; production guards passed, tiny writer refused incomplete context.'),
    ('recipe-evidence-corrected-v3', 'Separate writer XML serializer moved ant:if namespace off target subtree; Maven/Ant refused writer before any copy.'),
]:
    folder = R / label
    files = [{'path': p.relative_to(R).as_posix(), 'sha256': sha(p)} for p in sorted(folder.glob('*.log'))]
    copy = C / 'evidence/retained-initial-refusals' / label
    copy.mkdir(parents=True, exist_ok=True)
    for p in sorted(folder.glob('*.log')): shutil.copyfile(p, copy / p.name)
    if (folder / 'recipe-copy/pom.xml').exists():
        p = folder / 'recipe-copy/pom.xml'; shutil.copyfile(p, copy / 'FAILED_RECIPE_POM.xml')
    elif (folder / 'writer-rehearsal/pom.xml').exists():
        p = folder / 'writer-rehearsal/pom.xml'; shutil.copyfile(p, copy / 'FAILED_WRITER_REHEARSAL_POM.xml')
    failures.append({'scope': label, 'cause': cause, 'retained_logs': files, 'owner_modified': False})
(C / 'FAILED_ITERATIONS.json').write_text(json.dumps({'iterations': failures,
    'shell_startup_refusal': 'PowerShell Starting the CLR failed with HRESULT 80004005 before any Python proof; replayed via cmd.exe and Python; not guard evidence.'}, indent=2) + '\n', encoding='utf-8')
for name in ('qualify_source_admission.py', 'qualify_native_bounded.py', 'finalize_source_packet.py'):
    shutil.copyfile(R / name, C / 'executed-helpers' / name)
(C / 'ROOT_HANDOFF.md').write_text('''# Source-only checkout URL candidate

The two source images are ready for root review. Production owner remains at all
qualified preimages and HEAD74f; all5399 raw source paths and the inherited runtime
dependency inputs were independently rehashed at freeze. No worker owner writes.

Actual native production proof: complete owner check and12 refusal controls.
Separate fixture-writer proof:16 protocol controls, exact apply and byte+mtime
fixedpoint. Total native29; CPU/Git/JDK admission18. Maven property8+12 and exact
installed Tycho URI4 receipts remain frozen. Full production apply/verify/fixedpoint
and complete314 replay have not run for this candidate and belong to root.

The candidate qualifies checkout URL relocation only. The129 inherited absolute
artifact mappings and shared runtime/mirror remain required. Complete portable
output is unproven; Unicode checkout Maven launch is explicitly refused.

Public recipe source/context callback is bound only to mutation root; m3.context
injection is rejected. Parent POM images and both helpers are custody checked;
Python optimization refuses explicitly; isolated context invocation clears caller
optimization. Git metadata selectors and caller Java option sources cannot change
the bound source revision or URI. Added build inputs (including ignored Java and
new POM) refuse; generated target outputs and exactly4 metadata paths are allowed.

Root review is the independent final review role; earlier independent exact
Maven/Tycho assessment is retained. Additional agent slot was unavailable, so no
new independent final peer review is claimed. Failed prototype and writer harness
iterations are retained, distinct from the passing corrected proof.

Use README commands for production check/apply/verify/fixedpoint and the source-
bound original full314 command. Apply only reviewed sealed production pom.xml;
the archived WRITER_REHEARSAL_POM.xml is evidence and cannot admit production.
Full314 launch also requires adequate measured commit headroom, not only8GiB disk.
''', encoding='utf-8')

# Every source, metadata and textual receipt is sealed. Generated target/cache,
# bytecode, native objects and binary tools are outside this source publication.
sealed = []
for path in sorted(C.rglob('*')):
    if not path.is_file(): continue
    relative = path.relative_to(C)
    if 'target' in relative.parts or '__pycache__' in relative.parts: continue
    assert path.suffix.lower() not in {'.class', '.jar', '.pyc', '.dll', '.exe', '.zip', '.so', '.o'}
    if relative.as_posix() in {'SHA256SUMS.tsv', 'PUBLICATION_SEAL.json'}: continue
    sealed.append({'path': relative.as_posix(), 'sha256': sha(path), 'bytes': path.stat().st_size})
(C / 'SHA256SUMS.tsv').write_text(''.join(r['sha256'] + '\t' + r['path'] + '\n' for r in sealed), encoding='utf-8')
seal = {'schema': 'synexia.source-only-candidate-seal.v1', 'candidate': str(C), 'source_targets': 2,
    'sealed_files': len(sealed), 'total_source_only_bytes': sum(r['bytes'] for r in sealed),
    'sha256sums_sha256': sha(C / 'SHA256SUMS.tsv'), 'production_pom_sha256': sha(C / 'pom.xml'),
    'owner_state': 'PREIMAGE_ROOT_REVIEW_REQUIRED', 'full314_runtime': 'PENDING_ROOT',
    'checkout_URL_only': True, 'external129_artifact_mapping_frontier': True,
    'source_images': images['targets'], 'native_controls': 29, 'CPU_controls': 18,
    'writer_rehearsal_is_production_apply_proof': False}
(C / 'PUBLICATION_SEAL.json').write_text(json.dumps(seal, indent=2) + '\n', encoding='utf-8')
assert all(sha(C / r['path']) == r['sha256'] for r in sealed)
print(json.dumps({'seal': str(C / 'PUBLICATION_SEAL.json'), 'seal_sha256': sha(C / 'PUBLICATION_SEAL.json'),
    'files': len(sealed), 'sha256sums_sha256': seal['sha256sums_sha256'],
    'pom_sha256': seal['production_pom_sha256'], 'available_commit_bytes': memory.available_page}, indent=2), flush=True)
