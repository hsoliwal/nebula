<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
# Owned Chrome browser contract runner

This thin Node runner uses built-in HTTP, file/path, crypto and child-process APIs. It requires the unchanged root-qualified private Job launcher, a separately explicit browser execution release, verified native K TEMP/TMP, fresh capacity/RAM and the admitted runtime byte caps. No execution is authorized merely by the presence of this source.

Run the pure parser fixtures first with node run-browser-contract.js.txt --self-test browser-runner-contract.json. The actual browser CLI requires --scope S:/explicit-admitted-phase --war S:/phase/generated-war --runtime S:/phase/fresh-browser-runtime --fixtures S:/phase/generated/browser-runner-contract.json. The scope and WAR must exist; the runtime parent must exist and the runtime itself must be absent. All three must resolve within the explicit S scope. The Node executable and arguments are pinned by the root execution receipt.

The server binds 127.0.0.1 on port0, serves only bounded regular files inside the compiled WAR with explicit MIME types, and owns its connections. Only the pinned installed Chrome executable is spawned, with a fresh S profile/cache/crash/log and S HOME/APPDATA/LOCALAPPDATA/USERPROFILE; TEMP/TMP remain K. It uses headless dump-dom, a 10s virtual-time budget, a 1024x768 window and device scale1. No sandbox bypass, existing browser profile, CDP/session/cookie access, dependency install or external service is used.

Captured DOM is bounded to2MiB and stderr to64KiB, persisted only under the fresh S runtime. The 30s deadline kills only the directly spawned Chrome child. The direct child's close event and HTTP close are required before success; the unchanged enclosing private Job must independently establish descendant membership, complete terminal/reap/reader state and actual storage routing. The runner does not assert that environment/flags alone prove absence of all C writes.

Success requires a unique owned results element containing parsed JSON status PASS with a positive bounded assertion count. FAIL/RUNNING/missing/duplicate/wrong-owner/zero-assertion fixtures refuse. Authoring and fixture definitions are not executed browser proof. All29 earlier source inputs remain immutable.

Runner04 correction: preserve the incoming literal-equal TEMP/TMP from the private Job manifest. Its canonical direct parent must be K:/codex-work/agent-temp and its basename must start carousel-web-forward-20261015-; the actual path must be a real non-symlink directory with the same canonical spelling. The K base itself, C routes, other prefixes, differing TMP and dot aliases refuse. Chrome receives that same unique task temp, never the K base. The self-test now checks the original eight DOM result fixtures plus eight temp-admission fixtures (16 total). Chrome GPU is disabled for this real software Canvas2D contract; native GPU/platform performance remains unqualified. Runner03 and all earlier sealed inputs are preserved.

Optional --page selects only contract-tests.html (default) or example-behavior.html, the independent example observer page. Any other page refuses before profile/server/browser creation.
