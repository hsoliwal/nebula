<!-- SPDX-License-Identifier: Apache-2.0 -->
# Original Browser DOM snapshot: synchronous root-change short circuit

Repository owner: `hsoliwal/nebula`, original `M3BrowserScripts.patch`.
Canonical recipe owner: `hsoliwal/com.synexia`; follow-up to Synexia #13678
and Nebula #290, not a competing Browser, SWT receiver, JavaScript compiler,
JNI owner or widget API.

## Admission and mechanical contract

The existing fix already pins the initial `document.documentElement`, and
skips nodes detached by earlier `textContent` writes. If an application
synchronously replaces the document root while the static `querySelectorAll`
NodeList is being processed, the earlier code still tests every remaining
stale node even though no later node from that snapshot can be updated.

One original-Java script-builder atom now returns the successfully updated
count as soon as `document.documentElement !== root`, or the original root is
absent. Disconnected nodes under the unchanged document root still use
`continue`. The original sequence of valid connected element updates and
counts is retained; no asynchronous DOM semantics, queued event replay or
cross-document selection is inferred.

### Expected behavior

- Synchronous root replacement after the first update: same successful update
  count (1), but no further stale NodeList traversal.
- Child detachment under a still-current root: skip the detached child and
  continue, allowing later connected siblings to be processed.
- Unchanged root and ordinary sibling/value/duplicate-ID/empty patch: exact
  original JavaScript output *behavior*, with no new `innerHTML` execution.
- Input patch and text patch share the same root-identity gate.
- Java public/protected class descriptors and original SWT Browser ownership
  remain unchanged. Source language floor remains Java 11.

## Independent source proof

One deterministic Node.js DOM model instruments root reads for a 16,384-node
static snapshot after the first entry replaces the root. The earlier script
processes all stale records; the candidate returns on the first root mismatch.
The model proves equal positive update counts, not wall-clock throughput or
real SWT/RAP browser behavior. Additional child-detachment and
normal sibling/value cases guard against accidental blanket return.

Required execution order: source seals -> documentation -> canonical OpenRewrite
recipe -> drift refusal/fixed point -> original Java11 compile -> original
JUnit4 and Node behavior -> Tycho/native SWT/RWT browser/session proof.

Local Maven/OpenRewrite dependency closure and native SWT/RWT are unavailable;
those gates must remain `NOT_RUN` until actual execution. No existing master or
develop history is rewritten or auto-merged.
