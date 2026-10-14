#!/usr/bin/env python3
# SPDX-License-Identifier: EPL-2.0
"""Fail-closed scope check for a disposable M3 OpenRewrite FILE candidate.

This is a proof gate, not a source-rewriting engine. For this Nebula recipe
frontier, only modifications to already-tracked Java source files are admitted.
Add/delete/rename/copy/type-change, non-Java files, and untracked source
outputs require a separately reviewed broader-scope recipe and contract proof.
"""

from __future__ import annotations

import argparse
from pathlib import Path, PurePosixPath
import subprocess
import sys


def candidate_paths(name_status: bytes) -> tuple[str, ...]:
    """Decode Git -z --name-status records without ambiguously splitting filenames."""
    if not name_status:
        return ()
    if not name_status.endswith(b"\0"):
        raise ValueError("incomplete Git name-status record")
    fields = name_status[:-1].split(b"\0")
    if len(fields) % 2:
        raise ValueError("incomplete Git status/path pair")

    admitted: list[str] = []
    for offset in range(0, len(fields), 2):
        try:
            status = fields[offset].decode("ascii")
            path = fields[offset + 1].decode("utf-8")
        except UnicodeError as error:
            raise ValueError("non-UTF-8 Git candidate metadata") from error
        if status != "M":
            raise ValueError(f"unreviewed Git edit status {status!r}: {path!r}")
        if not path or "\\" in path or ":" in path:
            raise ValueError(f"noncanonical candidate path: {path!r}")
        parsed = PurePosixPath(path)
        parts = parsed.parts
        if parsed.is_absolute() or parsed.as_posix() != path or any(
            part in ("", ".", "..", ".git") for part in parts
        ):
            raise ValueError(f"unsafe candidate path: {path!r}")
        if any(ord(ch) < 32 or ord(ch) == 127 for ch in path):
            raise ValueError(f"control character in candidate path: {path!r}")
        if not path.endswith(".java"):
            raise ValueError(f"non-Java FILE candidate: {path!r}")
        if path in admitted:
            raise ValueError(f"duplicate FILE candidate: {path!r}")
        admitted.append(path)
    return tuple(admitted)


def review(repo: Path) -> tuple[str, ...]:
    """Check the complete Git worktree delta, including unexpected new source files."""
    base = ["git", "-C", str(repo)]
    diff = subprocess.check_output(
        base + ["diff", "HEAD", "--name-status", "--no-renames", "-z", "--"]
    )
    paths = candidate_paths(diff)

    untracked = subprocess.check_output(
        base + ["ls-files", "--others", "--exclude-standard", "-z"]
    )
    if untracked:
        files = sorted(
            part.decode("utf-8", "replace")
            for part in untracked[:-1].split(b"\0")
        )
        raise ValueError(f"untracked candidate outputs need explicit scope review: {files}")

    subprocess.run(
        base + ["diff", "--check", "HEAD", "--"],
        check=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    return paths


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, default=Path("."))
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    try:
        paths = review(args.repo.resolve())
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(
            "".join(path + "\n" for path in paths), encoding="utf-8"
        )
        print(f"M3_FILE_SCOPE_PASS modified_tracked_java_files={len(paths)}")
        return 0
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        print(f"M3_FILE_SCOPE_HOLD: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
