# Nebula pull-request and source-history audit

This is a read-only snapshot of all 238 `hsoliwal/nebula` pull requests visible at the start of the 2026-10-17 review, compared with master `91c474935eb4a06e46716a0188b9082f0af0eda9`. GitHub returned pages of 100, 100 and 38 records with `state=all`. The inventory contains 214 merged PRs, 24 closed unmerged PRs and zero open PRs. Later pull requests and commits require a new snapshot.

Every recorded PR base/head pair was resolved through its Git merge base, and every changed path was compared by exact mode and Git blob against the audited master. There are 6,290 file-change records: 4,732 match the current postimage, 509 match the preimage and 1,049 differ from both. These are byte-level classifications. A different current image can contain later improvements; a retained preimage can reflect a deliberate reversal. Neither classification alone proves that a change was lost or that merging it is safe.

The audit also records head reachability and each reported merge's first-parent tree. Thirty PR heads are not ancestors of the audit revision, including nine PRs that GitHub marks merged: #107, #209, #210, #213, #219, #220, #226, #231 and #237. Twenty-two recorded merges leave their first-parent tree unchanged. Branches merged into other development branches and duplicate/superseded work must be distinguished from missing integration.

The latest explicit multi-parent ancestry merge is `7cf7a59f`, which retains #183, #188, #191, #197, #216 and #227 while keeping its first-parent tree. Those PRs require individual content reconciliation. Gallery #177 and #184 have GitHub closure comments explicitly directing reviewers to the more complete #176; reopening their duplicate patches would be inappropriate.

## Coverage

All 238 PR titles, bodies, states, dates, refs and merge commit identities were inventoried. All 238 base/head file-change sets and current blob classifications were generated. The relevant viewport lineage, current Grid selection/row geometry owners, Gallery supersession discussions and recent #234/#235 review collections were inspected; the latter were empty when retrieved. Full narrative review of every historical diff and native execution of every past PR are not claimed. Native runtime qualification in this round covers the separate selection correction and its recorded compatibility checks, not the whole historical inventory.

## Files

- [AUDIT.json](AUDIT.json): readable per-PR summaries, comparison commit IDs, reachability, unchanged-tree merge flags, exact classification definitions and file counts.
- `PR_CATALOGUE.json.gz`: complete captured PR metadata and bodies, compressed once without repeated repository/user payloads.
- `PR_FILES.tsv.gz`: every changed path, change kind, preimage, postimage and current mode/blob. The compressed and uncompressed SHA-256 receipts are in `AUDIT.json`.
- [audit_history.py](audit_history.py): deterministic read-only comparison. It never edits product source, merges branches, rebases or pushes.

To reproduce from a clone that includes historical PR heads, decompress neither input manually nor product source. Run:

```sh
python docs/m3/viewport-history-20261017/audit_history.py \
  --repository . \
  --inventory docs/m3/viewport-history-20261017/PR_CATALOGUE.json.gz \
  --revision 91c474935eb4a06e46716a0188b9082f0af0eda9 \
  --output /tmp/nebula-history-replay
```

Some partial clones need historical objects fetched first. Preserve existing branch names and use a fresh ref namespace for audit heads. The GitHub repository remains the authority for live state; this snapshot is evidence for a new additive review branch.
