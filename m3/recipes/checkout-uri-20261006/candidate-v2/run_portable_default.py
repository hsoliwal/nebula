"""Reuse the inherited complete default reactor command with one encoded URI binding."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import time
if not __debug__:
    raise RuntimeError('OPTIMIZED_PYTHON_REFUSAL')
from source_context import read_revision, sha, verify_context

C = Path(__file__).resolve().parent


def prepare_runtime_environment(root, output, inherited):
    uri = root.as_uri()
    env = os.environ.copy()
    cleared_options = ('MAVEN_ARGS', 'MAVEN_BASEDIR', 'MAVEN_DEBUG_OPTS',
                       '_JAVA_OPTIONS', 'JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', 'CLASSPATH')
    for key in cleared_options:
        env.pop(key, None)
    env['MAVEN_BASEDIR'] = str(root)
    env['MAVEN_SKIP_RC'] = '1'
    env['JAVA_HOME'] = inherited['env']['JAVA_HOME']
    tmp = output / 'tmp'
    env['TMP'] = env['TEMP'] = str(tmp)
    env['MAVEN_OPTS'] = ('-Djava.io.tmpdir="' + str(tmp) + '" -Dfile.encoding=UTF-8 -Xmx2g '
                         '-Dm3.nebula.checkout.uri=' + uri)
    return env, cleared_options


def build_command(root, output):
    # Preserve the caller's physical path until context admission has refused
    # junction/symlink ancestors; resolve() would hide that boundary.
    root = Path(root).absolute()
    assert str(root).isascii(), 'UNQUALIFIED_UNICODE_MAVEN_LAUNCHER_ROOT_REFUSAL'
    output = Path(output).resolve()
    manifest = json.loads((C / 'manifest.json').read_text(encoding='utf-8'))
    inherited = json.loads((C / 'INHERITED_DEFAULT_BUILD_COMMAND.json').read_text(encoding='utf-8'))
    assert read_revision(root) == manifest['base_commit'], 'BASE_REVISION_REFUSAL'
    for row in manifest['targets']:
        assert sha(root / row['path']) == row['post_sha256'], 'EXACT_PORTABLE_SOURCE_APPLY_REQUIRED ' + row['path']
    verify_context(root)
    assert not output.is_relative_to(root), 'runtime output must be outside source checkout'
    assert shutil.disk_usage(output.parent if output.parent.exists() else C).free >= 8 * 1024**3, 'RESERVE_8GIB_REFUSAL'
    for row in json.loads((C / 'DEPENDENCY_MANIFEST.json').read_text(encoding='utf-8'))['files']:
        assert sha(Path(row['path'])) == row['sha256'], 'QUALIFIED_RUNTIME_INPUT_DRIFT ' + row['path']
    uri = root.as_uri()
    # Existing source-bound .mvn config must not add a competing user-property URI.
    config = (root / '.mvn/maven.config').read_text(encoding='utf-8')
    assert 'm3.nebula.checkout.uri' not in config, 'URI_AUTHORITY_OVERRIDE_REFUSAL'
    command = inherited['argv']
    assert command[-5:] == ['-V', '-B', 'clean', 'verify', '-Dtycho.localArtifacts=ignore']
    env, cleared_options = prepare_runtime_environment(root, output, inherited)
    receipt = {'argv': command, 'cwd': str(root), 'base_commit': manifest['base_commit'],
               'original_314_default_argv_preserved': True, 'encoded_checkout_uri': uri,
               'cleared_caller_option_sources': list(cleared_options),
               'env': {key: env[key] for key in ('JAVA_HOME', 'TMP', 'TEMP', 'MAVEN_OPTS', 'MAVEN_SKIP_RC', 'MAVEN_BASEDIR')},
               'source_targets': manifest['targets'], 'full_portable_reactor_executed': False}
    return command, env, receipt


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', required=True)
    parser.add_argument('--output', required=True)
    parser.add_argument('--run', action='store_true', help='execute the inherited complete clean verify reactor')
    args = parser.parse_args()
    command, env, receipt = build_command(args.root, args.output)
    if not args.run:
        print(json.dumps(receipt, indent=2), flush=True)
        return
    output = Path(args.output).resolve()
    assert not output.exists(), 'FRESH_RUNTIME_OUTPUT_REQUIRED'
    (output / 'tmp').mkdir(parents=True)
    (output / 'PORTABLE_BUILD_COMMAND.json').write_text(json.dumps(receipt, indent=2) + '\n', encoding='utf-8')
    log = output / 'portable314-maven.log'
    start = time.monotonic()
    with log.open('wb') as handle:
        process = subprocess.run(command, cwd=args.root, env=env, stdout=handle, stderr=subprocess.STDOUT, timeout=1800)
    verify_context(args.root)
    for row in receipt['source_targets']:
        assert sha(Path(args.root) / row['path']) == row['post_sha256'], 'SOURCE_DRIFT_AFTER_REACTOR'
    text = log.read_text(encoding='utf-8', errors='replace')
    section = text.split('[INFO] Reactor Summary:', 1)[1].split('[INFO] ------------------------------------------------------------------------', 1)[0] if '[INFO] Reactor Summary:' in text else ''
    rows = [line for line in section.splitlines() if re.match(r'^\[INFO\]\s+.+\s+(SUCCESS|FAILURE|SKIPPED)(?:\s+\[.*\])?$', line)]
    result = {'state': 'PORTABLE_DEFAULT314_PASS' if process.returncode == 0 and len(rows) == 314 and all('SUCCESS' in row for row in rows) else 'PORTABLE_DEFAULT314_REFUSAL',
              'exit': process.returncode, 'elapsed_seconds': time.monotonic() - start,
              'reactor_summary_rows': len(rows), 'success': sum('SUCCESS' in row for row in rows),
              'failure': sum('FAILURE' in row for row in rows), 'skipped': sum('SKIPPED' in row for row in rows),
              'log_sha256': sha(log), 'source_bytes_unchanged_after_runtime': True,
              'native_UI_effect_admission': False}
    (output / 'PORTABLE_BUILD_RESULT.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(result, indent=2), flush=True)
    assert result['state'] == 'PORTABLE_DEFAULT314_PASS', result['state']


if __name__ == '__main__':
    main()
