# Source-sealed physical ZIP central directory repair

The exact merged Nebula `GwtAttributedJavaInventory` preimage has SHA-256
`7760d95a44bccf97c107be34937688de24b8f77a0784045273d463570cd2775b`.

The original end-of-central-directory (EOCD) preflight checked only the advertised
number of entries. A crafted, otherwise valid ZIP containing 12,001 CEN headers
but an EOCD claiming a single entry passed that preflight; JDK 21 JarFile then
indexed all 12,001 entries before the caller's post-index iterator refused it.

This recipe changes just the private preflight atom: sequentially verify every
physical fixed 46-byte CEN header, bounded name/extra/comment lengths and the
actual entry count before Java's JarFile is constructed. It adds no wrapper,
no renderer, no native code, and changes no public type or method signatures.

The local reproduction in `CheckPreflight` reported
`PREFLIGHT_ADMITTED_FORGED_COUNT` before the change; after the change it reports
`PREFLIGHT_REFUSED: ... M3_GWT_AST_JAR_ENTRY_COUNT_BUDGET`.

STRICT local JDK 21 compilation, a 16-check self-contained JDK proof, five
JUnit-shaped tests under a clearly labeled shim (not actual Maven/JUnit), and
25 real Chromium source-to-DOM checks pass. Chromium generated JS/CSS/HTML
SHA receipts and combined stdout stay byte-identical to the preimage.

Original real Maven/OpenRewrite execution and native SWT/ABI qualification
remain NOT_RUN. The canonical Synexia recipe is an unqualified candidate until
those gates actually pass; public Nebula recipient output must remain draft.
