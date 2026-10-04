#!/usr/bin/env python3
# SPDX-License-Identifier: EPL-2.0
"""Capture tracked Nebula bytes and the actual recipe toolchain; never a completion receipt."""
from __future__ import annotations
import argparse
import gzip
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import tarfile


def digest(path: Path) -> str:
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def command(*args: str, cwd: Path) -> bytes:
    return subprocess.check_output(args, cwd=cwd, stderr=subprocess.STDOUT)


def plain_only(member: tarfile.TarInfo) -> tarfile.TarInfo | None:
    if not member.isfile() and not member.isdir():
        return None
    if Path(member.name).name in {'settings.xml', 'settings-security.xml', 'toolchains.xml'}:
        return None
    return member


def capture(root: Path, output: Path) -> None:
    root = root.resolve(strict=True)
    output = output.resolve()
    if output == root or output.is_relative_to(root):
        raise ValueError('capture destination must be outside the checkout')
    output.mkdir(parents=True, exist_ok=False)
    commit = command('git', 'rev-parse', 'HEAD', cwd=root).decode().strip()
    tree = command('git', 'rev-parse', 'HEAD^{tree}', cwd=root).decode().strip()
    inventory = command('git', 'ls-tree', '-r', '-z', 'HEAD', cwd=root)
    (output / 'tracked-tree.z').write_bytes(inventory)
    # Archive committed input; the separate patch records actual recipe-produced worktree changes.
    tar_path = output / 'source.tar'
    with tar_path.open('wb') as stream:
        subprocess.run(['git', 'archive', '--format=tar', 'HEAD'], cwd=root, stdout=stream, check=True)
    with tar_path.open('rb') as source, gzip.open(output / 'source.tar.gz', 'wb') as compressed:
        shutil.copyfileobj(source, compressed)
    with tarfile.open(tar_path) as source_archive:
        archived = {entry.name for entry in source_archive if entry.isfile() or entry.issym()}
    tracked = set()
    for record in inventory.split(b'\0'):
        if not record:
            continue
        metadata, path = record.split(b'\t', 1)
        if metadata.split()[1] != b'blob':
            raise RuntimeError('Git-linked source requires separate exact custody: ' + path.decode())
        tracked.add(path.decode('utf-8'))
    if archived != tracked:
        raise RuntimeError('git archive does not cover the exact tracked source tree')
    tar_path.unlink()
    (output / 'bootstrap.patch').write_bytes(command('git', 'diff', '--binary', 'HEAD', cwd=root))
    mvn = shutil.which('mvn')
    if mvn is None:
        raise RuntimeError('the actual Maven executable is required')
    maven = Path(mvn).resolve(strict=True).parent.parent
    repository = Path.home() / '.m2' / 'repository'
    if not (maven / 'lib').is_dir() or not repository.is_dir():
        raise RuntimeError('actual Maven distribution and repository cache required')
    # Only the library repository, never ~/.m2 settings/credentials or Git authentication metadata.
    with tarfile.open(output / 'recipe-toolchain.tar.gz', 'w:gz') as archive:
        archive.add(maven, arcname='maven', filter=plain_only)
        archive.add(repository, arcname='repository', filter=plain_only)
    with tarfile.open(output / 'compiled-inputs.tar.gz', 'w:gz') as archive:
        for target in sorted(root.rglob('target')):
            if not target.is_dir() or target.is_symlink():
                continue
            for part in ('classes', 'test-classes', 'compilelogs', 'surefire-reports'):
                directory = target / part
                if directory.is_dir() and not directory.is_symlink():
                    archive.add(directory, arcname=str(directory.relative_to(root)), filter=plain_only)
    proof = root / 'm3/recipe-first/target/compile-bootstrap/surefire-reports'
    if proof.is_dir():
        shutil.copytree(proof, output / 'bootstrap-surefire')
    (output / 'java-version.txt').write_bytes(command('java', '-version', cwd=root))
    (output / 'maven-version.txt').write_bytes(command('mvn', '-version', cwd=root))
    files = [
        {'path': str(path.relative_to(output)), 'bytes': path.stat().st_size, 'sha256': digest(path)}
        for path in sorted(output.rglob('*')) if path.is_file()
    ]
    (output / 'capture.json').write_text(json.dumps({
        'source_commit': commit,
        'source_git_tree': tree,
        'stage_complete': False,
        'purpose': 'Exact source acquisition and real recipe SDK replay; not whole-Nebula proof.',
        'production_dependency_mirror_proven': False,
        'credentials_included': False,
        'files': files,
    }, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    capture(args.root, args.output)
