"""Writer rehearsal only; this helper is never called by the production recipe."""
import hashlib
import json
from pathlib import Path
import sys

if not __debug__:
    raise RuntimeError('OPTIMIZED_PYTHON_REFUSAL')
C = Path(__file__).resolve().parent
root = Path(sys.argv[1]).absolute()
inventory = json.loads((C / 'INVENTORY.json').read_text())
manifest = json.loads((C / 'manifest.json').read_text())
targets = {r['path'] for r in manifest['targets']}
for row in inventory['qualified_metadata']:
    if row['path'] in targets:
        continue
    if hashlib.sha256((root / row['path']).read_bytes()).hexdigest() != row['sha256']:
        raise RuntimeError('FIXTURE_METADATA_REFUSAL ' + row['path'])
print('FIXTURE_WRITER_CONTEXT_ONLY_NOT_FULL_OWNER_ADMISSION', flush=True)
