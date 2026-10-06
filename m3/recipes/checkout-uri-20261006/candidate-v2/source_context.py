"""Read-only complete original source context admission for a two-image recipe."""
import hashlib
import json
import os
from pathlib import Path
import stat
import subprocess
import sys

if not __debug__:
    raise RuntimeError('OPTIMIZED_PYTHON_REFUSAL')

C = Path(__file__).resolve().parent


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def git_environment():
    env = os.environ.copy()
    for key in ('GIT_DIR', 'GIT_WORK_TREE', 'GIT_INDEX_FILE', 'GIT_COMMON_DIR'):
        env.pop(key, None)
    return env


def read_revision(source):
    return subprocess.check_output(['git', '-C', str(source), 'rev-parse', 'HEAD'],
                                   text=True, env=git_environment()).strip()


def verify_context(source):
    source = Path(source).absolute()
    manifest = json.loads((C / 'SOURCE_MANIFEST.json').read_text(encoding='utf-8'))
    images = json.loads((C / 'manifest.json').read_text(encoding='utf-8'))
    target_paths = {row['path'] for row in images['targets']}
    allowed_untracked = {row['path'] for row in json.loads(
        (C / 'INVENTORY.json').read_text(encoding='utf-8'))['qualified_metadata']}
    if read_revision(source) != images['base_commit']:
        raise RuntimeError('BASE_REVISION_REFUSAL')
    git_env = git_environment()
    # Only target directories beside frozen Maven/OSGi project descriptors are
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
    for ignored in (False, True):
        command = ['git', '-C', str(source), 'ls-files', '--others', '-z', '--exclude-standard']
        if ignored:
            command.append('--ignored')
        command += ['--', '.']
        result = subprocess.run(command, check=True, capture_output=True, env=git_env)
        unknown = set(result.stdout.decode('utf-8').split('\0')) - {''} - allowed_untracked
        added = {path for path in unknown if not generated_output(path)}
        if added:
            raise RuntimeError('ADDED_SOURCE_INPUT_REFUSAL ' + repr(sorted(added)))
    tracked = subprocess.run(['git', '-C', str(source), 'ls-files', '-z'],
                             check=True, capture_output=True, env=git_env)
    tracked_paths = set(tracked.stdout.decode('utf-8').split('\0')) - {''}
    original_paths = {row['path'] for row in manifest['files']}
    if tracked_paths != original_paths:
        raise RuntimeError('ORIGINAL_SOURCE_SET_REFUSAL added=' + repr(sorted(tracked_paths - original_paths))
                           + ' missing=' + repr(sorted(original_paths - tracked_paths)))
    seen = set()
    for item in (source, *source.parents):
        info = item.lstat()
        assert not stat.S_ISLNK(info.st_mode) and not (
            getattr(info, 'st_file_attributes', 0) & getattr(stat, 'FILE_ATTRIBUTE_REPARSE_POINT', 0)
        ), 'SOURCE_CONTEXT_REPARSE ' + str(item)
    count = 0
    for row in manifest['files']:
        if row['path'] in target_paths:
            continue
        path = source / row['path']
        assert path.is_file(), 'SOURCE_CONTEXT_ABSENT ' + row['path']
        for item in (path, *path.parents):
            if item in seen:
                break
            seen.add(item)
            info = item.lstat()
            assert not stat.S_ISLNK(info.st_mode) and not (
                getattr(info, 'st_file_attributes', 0) & getattr(stat, 'FILE_ATTRIBUTE_REPARSE_POINT', 0)
            ), 'SOURCE_CONTEXT_REPARSE ' + str(item)
            if item == source:
                break
        assert sha(path) == row['sha256'], 'SOURCE_CONTEXT_DRIFT ' + row['path']
        count += 1
    assert count == 5398, count
    inventory = json.loads((C / 'INVENTORY.json').read_text(encoding='utf-8'))
    for row in inventory['qualified_metadata']:
        if row['path'] in target_paths:
            continue
        path = source / row['path']
        for item in (path, *path.parents):
            if item in seen:
                break
            seen.add(item)
            info = item.lstat()
            assert not stat.S_ISLNK(info.st_mode) and not (
                getattr(info, 'st_file_attributes', 0) & getattr(stat, 'FILE_ATTRIBUTE_REPARSE_POINT', 0)
            ), 'QUALIFIED_METADATA_REPARSE ' + str(item)
            if item == source:
                break
        assert sha(path) == row['sha256'], 'QUALIFIED_METADATA_DRIFT ' + row['path']
    return count


if __name__ == '__main__':
    if len(sys.argv) == 3 and sys.argv[1] == '--head':
        print(read_revision(Path(sys.argv[2]).absolute()), flush=True)
    else:
        assert len(sys.argv) == 2, 'exact read-only context root required'
        print('SOURCE_CONTEXT_EXACT paths=' + str(verify_context(sys.argv[1])), flush=True)
