"""Keep inherited native receipts distinct from the pending successor proof."""
import hashlib
import json
from pathlib import Path
import shutil

R = Path('S:/m3-nebula-portable-recipe-20261006')
C = R / 'candidate-v2'
prior = C / 'evidence/prior-candidate-native-guard-proof'
shutil.copytree(C / 'evidence/native-guard-proof', prior)
scope = {'proof_owner': 'IMMUTABLE_PRIOR_CANDIDATE_ONLY', 'prior_native_controls': 29,
    'prior_production_pom_sha256': '3d0d605f4aaf81a473e62111a29763bc748652f345016e9e6bb29218095877f6',
    'prior_source_seal_sha256': '7f62b10a064adbf2ebbdc29b05a68241eed787a629a10796e7bbac4a02d0c139',
    'successor_native_controls': 'PENDING_ROOT_JVM_RELEASE_THEN_EXECUTION',
    'successor_current_native_attempt': 'JVM_NATIVE_MEMORY_REFUSAL_NOT_GATE_EVIDENCE',
    'receipt_sha256': hashlib.sha256((prior / 'NATIVE_GUARD_PROOF.json').read_bytes()).hexdigest()}
for path in (prior / 'SCOPE.json', C / 'evidence/native-guard-proof/SCOPE.json'):
    path.write_text(json.dumps(scope, indent=2) + '\n', encoding='utf-8')
shutil.copyfile(R / 'label_prior_native_scope.py', C / 'executed-helpers/label_prior_native_scope.py')
print('PRIOR_NATIVE_29_EXPLICITLY_SCOPED_NOT_SUCCESSOR_ADMISSION', flush=True)
