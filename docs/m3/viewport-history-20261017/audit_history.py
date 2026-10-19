#!/usr/bin/env python3
"""Read recorded GitHub PR heads and Git trees without changing branches or source.

SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
Copyright (c) 2026 Synexia, Hitesh Soliwal and others.
"""
import argparse
import collections
import gzip
import hashlib
import json
import os
from pathlib import Path
import subprocess

ZERO = '0' * 40

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repository', type=Path, required=True)
    parser.add_argument('--inventory', type=Path, required=True)
    parser.add_argument('--revision', required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)

    def git(*words, accepted=(0,)):
        result = subprocess.run(['git', '-C', str(args.repository), *words],
                                stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        if result.returncode not in accepted:
            raise RuntimeError('git ' + ' '.join(words) + ': ' + result.stderr.decode())
        return result

    encoded = args.inventory.read_bytes()
    inventory = json.loads(gzip.decompress(encoded) if args.inventory.suffix == '.gz' else encoded)
    revision = git('rev-parse', args.revision + '^{commit}').stdout.decode().strip()
    current = {}
    for entry in git('ls-tree', '-r', '-z', revision).stdout.split(b'\0'):
        if not entry:
            continue
        metadata, path = entry.split(b'\t', 1)
        mode, kind, blob = metadata.decode().split()
        current[path.decode()] = (mode, blob)
    categories = collections.Counter()
    summaries = []
    file_rows = ['pr\tchange\tpath\tpre_mode\tpre_blob\tpost_mode\tpost_blob\tcurrent_mode\tcurrent_blob\tclassification']
    for pr in sorted(inventory, key=lambda value: value['number']):
        number, head, base = pr['number'], pr['head']['sha'], pr['base']['sha']
        ancestor = git('merge-base', base, head).stdout.decode().strip()
        reachable = git('merge-base', '--is-ancestor', head, revision, accepted=(0, 1)).returncode == 0
        same_tree_merge = False
        parents = []
        merge = pr['merge_commit_sha']
        if pr['merged_at'] and merge:
            parts = git('rev-list', '--parents', '-n', '1', merge).stdout.decode().split()
            parents = parts[1:]
            if len(parents) > 1:
                trees = git('rev-parse', merge + '^{tree}', parents[0] + '^{tree}').stdout.decode().splitlines()
                same_tree_merge = trees[0] == trees[1]
        entries = git('diff-tree', '--no-commit-id', '--no-renames', '--raw', '-r', '-z',
                      '--abbrev=40', ancestor, head).stdout.split(b'\0')
        local = collections.Counter()
        for at in range(0, len(entries) - 1, 2):
            header, path = entries[at].decode(), entries[at+1].decode()
            before_mode, after_mode, before, after, change = header[1:].split()
            current_mode, current_blob = current.get(path, ('000000', ZERO))
            if (after_mode, after) == (current_mode, current_blob):
                classification = 'POSTIMAGE_EXACT'
            elif (before_mode, before) == (current_mode, current_blob):
                classification = 'PREIMAGE_EXACT'
            else:
                classification = 'DIVERGED'
            local[classification] += 1
            categories[classification] += 1
            file_rows.append('\t'.join(map(str, (number, change, path, before_mode, before,
                             after_mode, after, current_mode, current_blob, classification))))
        summaries.append({'number': number, 'title': pr['title'], 'state': pr['state'],
                          'merged': bool(pr['merged_at']), 'url': pr['url'],
                          'head': head, 'recorded_base': base, 'comparison_merge_base': ancestor,
                          'head_is_ancestor_of_audit_revision': reachable,
                          'merge_commit': merge, 'merge_parents': parents,
                          'merge_tree_equals_first_parent': same_tree_merge,
                          'changed_files': sum(local.values()), 'current_blob_classification': dict(local)})
    raw = ('\n'.join(file_rows) + '\n').encode()
    packed = gzip.compress(raw, mtime=0)
    (args.output / 'PR_FILES.tsv.gz').write_bytes(packed)
    result = {'schema': 'NEBULA_PR_HISTORY_AUDIT_V1', 'repository': 'hsoliwal/nebula',
              'revision': revision, 'inventory_rows': len(inventory),
              'merged_prs': sum(bool(p['merged_at']) for p in inventory),
              'closed_unmerged_prs': sum(p['state'] == 'closed' and not p['merged_at'] for p in inventory),
              'open_prs': sum(p['state'] == 'open' for p in inventory),
              'source_path': 'GitHub state=all/per_page=100 pages 1,2,3; each recorded base/head compared via local Git merge-base',
              'classification_meaning': {
                'POSTIMAGE_EXACT': 'Current path mode/blob exactly equals this PR postimage (including an absent deletion).',
                'PREIMAGE_EXACT': 'Current path mode/blob exactly equals this PR preimage. Not proof that a later deliberate reversal was wrong.',
                'DIVERGED': 'Current path differs from both images; later changes need semantic review. Not proof that the PR was lost.'},
              'same_tree_merge_meaning': 'Merge records ancestry and leaves its first-parent tree unchanged. May be already-superseded content; does not prove semantic integration.',
              'file_change_rows': len(file_rows)-1, 'classifications': dict(categories),
              'prs_with_no_current_head_ancestry': sum(not row['head_is_ancestor_of_audit_revision'] for row in summaries),
              'merged_prs_with_unchanged_first_parent_tree': sum(row['merged'] and row['merge_tree_equals_first_parent'] for row in summaries),
              'files_receipt': {'path': 'PR_FILES.tsv.gz', 'sha256': hashlib.sha256(packed).hexdigest(),
                                'uncompressed_sha256': hashlib.sha256(raw).hexdigest()},
              'pull_requests': summaries}
    (args.output / 'AUDIT.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps({key: value for key, value in result.items() if key not in ('pull_requests', 'classification_meaning', 'same_tree_merge_meaning')}, indent=2))

if __name__ == '__main__':
    main()
