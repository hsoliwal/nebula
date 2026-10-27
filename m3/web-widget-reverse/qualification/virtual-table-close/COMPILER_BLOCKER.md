<!-- SPDX-License-Identifier: Apache-2.0 -->
# Compiler-discovered leaf: constructor parameter capture

## Reproduction
The exact existing product source Git blob is
`e0c498146818a4c5ea633649759a4dcc2883037a`, SHA-256
`ec2c207407956cc4965f2ec2eda238356bb4b45dc8e017ea2dac9d87483655f5`.

Strict Java 11 `javac --release 11 -proc:none -Xlint:all -Werror`
with explicitly labelled test-only SWT API signatures fails:

```
M3VirtualTableBinding.java:140: error: local variables referenced from a
lambda expression must be final or effectively final
    rows = null;
```

The constructor has a parameter named `rows`, which shadows the field.
Inside `onDispose = event -> { ... }`, the bare assignment mutates the
captured parameter, not the instance field. This cannot compile in Java 11.

## Correction
One source-local atom, with complete original field/listener interfaces intact:

`rows = null; // release...` -> `this.rows = null; // release...`

A separate later atom must guard `pageAt` against a provider closing its
binding mid-`SetData` before a page is cached. Test the compiler-corrected
intermediate as the control image to avoid attributing behavior to a source
that cannot compile.

## Integrity and gates
Retain exact unmodified source, compiler failure log and pin the intermediate
postimage separately from the later closing-guard postimage. Canonical
Synexia Java snapshot receipts must prove original -> compiler-fixed ->
closure-fixed, with source drift refusal and replay fixed points. No public
API, JNI, SWT native owner or data structure changes.

The compiler-corrected intermediate is required before a behavioral comparison
can legitimately be made. Original Nebula Tycho/JUnit and native SWT/RWT
runtime remain separately unqualified until executed on actual toolkits.
