"""Borrow the executed Maven projection proof and bind the actual source URI key."""
from pathlib import Path
import json

R = Path('S:/m3-nebula-portable-recipe-20261006')
prior = R / 'portable-property-proofs/root #percent% unicode測試/releng/parent/observed.txt'
if prior.is_file() and not (R / 'portable-property-proofs/INITIAL_UNICODE_LAUNCHER_REFUSAL.json').exists():
    (R / 'portable-property-proofs/INITIAL_UNICODE_LAUNCHER_REFUSAL.json').write_text(json.dumps({
        'state': 'MAVEN_ROOT_DIRECTORY_DISCRIMINATOR_REFUSAL',
        'observed': dict(line.split('=', 1) for line in prior.read_text().splitlines()),
        'expected_root': str(prior.parents[2]),
        'candidate_source_unchanged': True,
    }, indent=2) + '\n', encoding='utf-8')
text = (R / 'probe_maven_properties.py').read_text()
text = text.replace("P = R / 'property-proofs'", "P = R / 'portable-property-proofs'")
text = text.replace("for label in ('plain-root', 'root with spaces'):",
                    "for label in ('plain-root', 'root with spaces', 'root #percent%'):")
text = text.replace("'file:///${maven.multiModuleProjectDirectory}/m3/qualified-mixed-original435-target'",
                    "'${m3.nebula.checkout.uri}/m3/qualified-mixed-original435-target'")
text = text.replace("env.pop('MAVEN_BASEDIR', None)",
                    "env['MAVEN_BASEDIR'] = str(root)\n        env['MAVEN_OPTS'] += ' -Dm3.nebula.checkout.uri=' + root.as_uri()")
text = text.replace("assert Path(props['multi']).resolve() == root.resolve(), props",
                    "assert Path(props['multi']).resolve() == root.resolve(), props\n            assert props['qualified'] == (root / 'm3/qualified-mixed-original435-target').as_uri(), props")
text = text.replace("'MAVEN_PROPERTY_PROOF.json'", "'PORTABLE_MAVEN_PROPERTY_PROOF.json'")
text = text.replace("'synexia.nebula-maven-property-proof.v1'", "'synexia.nebula-portable-maven-property-proof.v1'")
(R / 'probe_portable_properties.py').write_text(text, encoding='utf-8', newline='\n')
