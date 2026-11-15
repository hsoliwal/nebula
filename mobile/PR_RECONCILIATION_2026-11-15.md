# Nebula Android/iOS PR reconciliation — 2026-11-15

## Selected GitHub state

- [Nebula #396](https://github.com/hsoliwal/nebula/pull/396) merged the original RoundedCheckbox Android/iOS preview using SWT native mobile owners from [SWT #369](https://github.com/hsoliwal/eclipse.platform.swt/pull/369).
- [Nebula #403](https://github.com/hsoliwal/nebula/pull/403) is closed without merge. Its 15 changed paths do **not** overlap the 15 paths changed by #396, but path disjointness does not prove application compatibility, freshness or test parity.
- [Nebula #452](https://github.com/hsoliwal/nebula/pull/452) remains open (head `b105f8843e5046b4a0e4d84b4778302a7f7ad504`). It adds a release workflow dispatching existing desktop/mobile checks, enforces real test failures for previously skipped CDateTime/CWT/CompositeTable suites and retains Surefire reports and an Android Grid debug APK. This PR's description explicitly says the full device matrix has not been executed.
- Selected baseline: Nebula `master` `12bb87d67b93d231fb48030e6db5460eaf798a7a` on 2026-11-15.

## Distinct implementation lineages

The merged #396 preview is under `m3/mobile/**`, with a Gradle Android consumer and an iOS Java entrypoint. The unmerged #403 provides a different root `mobile/**`, a Maven-controlled SDK-only APK builder, native OS instrumentation, source hash pins and output receipts, plus workflow additions. The *current* Nebula `mobile/**` instead owns the mobile Grid viewport projection and release candidate workflow. These root directories are now semantically occupied; a wholesale transfer of #403 risks overwriting current Grid work or introducing conflicting product models.

## Candidate atoms / comparison gates

| Candidate #403 atom | Current-lineage comparison | Acceptance criterion |
| --- | --- | --- |
| `mobile/build_mobile.py`, Maven launcher and APK/JAR source selection | Merged original-widget Android Gradle flow and existing Grid APK workflows | Import only if it produces a distinct tested artifact, has exact source closure and does not replace current `mobile/**` owners |
| `mobile/src/test/.../AndroidMobileInstrumentation.java` | Merged RoundedCheckbox instrumentation and current original-widget workflow | Translate missing assertions to the **current** Android test source and run on a real API-35 device/emulator |
| `mobile/android/run_instrumentation.py` | Current original-widget native acceptance workflow | Require unique successful terminal result, reject missing/duplicate markers and retain diagnostics; local parser success is not device success |
| `mobile/source-manifest.json`, `swt-revision.txt` and receipt | Current SWT `mobile/source-manifest.json` and Nebula exact CI pins | Verify raw source SHA-256 and Git commits, reject drift instead of silently repinning |
| iOS bytecode JAR and UIKit integration description | Current SWT Java/JNI build and iOS native workflow | Separate Java compilation, Apple native compilation, embedded JVM and device execution; no implied pass |

## Immediate safe release decision

Keep #452 as a release-candidate change until test suites are unskipped and **their actual results** are available. Neither its mergeability nor the successful parser/unit checks in the PR body prove an executed Android device or iOS runtime matrix. Retain #403 as a historical independent implementation for test-atom extraction; do not reopen or merge it wholesale.

## Order for a future implementation patch

1. Inventory the exact current paths and their blob IDs; identify missing test atoms through assertion-level comparison, not filename matching.
2. Document the intended behavior for each atom and freeze existing public signatures, JNI, events and source pins.
3. Make one behavior-preserving test or gate change per patch; lint, compile, unit test, platform build, and device execution in that order, preserving logs.
4. Create separate reviewable additive PRs for missing test coverage. Do not claim parity until native and product acceptance reports exist.

## Verification boundary

This file records a read-only GitHub review of actual PR metadata, filenames and selected source/test files. It **does not** claim that the 15 unmerged files were built against current `master`, that Android/iOS native tests passed, or that the underlying runtime implementations are semantically equivalent. No production code was modified or merged by this audit.
