<!-- SPDX-License-Identifier: Apache-2.0 -->
# Read-only web/widget catalogue qualification, Java 21

This separate Java21 **qualification tool** is not a runtime Nebula Control, SWT
adapter, renderer, transpiler, or public API. It checks exact corpus structure
for the first cross-framework typed semantic admission proposal. It cannot
claim that an untested widget mapping works. It refuses promoted statuses,
malformed digests, license route erosion, symlink traversal, UTF-8 errors,
ambiguous TSV cells, duplicate records and expected-corpus count drift.

Do not copy donor runtime Java/JavaScript. Maintain canonical OpenRewrite
recipe ownership in `hsoliwal/com.synexia`. Nebula retains the source-sealed
catalogues, original widget owners and Tycho acceptance. No compile/test result
is a substitute for native SWT Win32/GTK/Cocoa and real RAP/RWT session proofs.

Execute from the root of Nebula after applying the admitted Synexia recipe:

```
mkdir -p /tmp/m3web-audit-classes
javac --release 21 -Xlint:all -Werror -d /tmp/m3web-audit-classes \
  m3/web-widget-reverse/qualification/src/main/java/org/eclipse/nebula/widgets/m3/qualification/M3WebWidgetMappingAudit.java \
  m3/web-widget-reverse/qualification/src/test/java/org/eclipse/nebula/widgets/m3/qualification/M3WebWidgetMappingAuditTest.java
java -cp /tmp/m3web-audit-classes org.eclipse.nebula.widgets.m3.qualification.M3WebWidgetMappingAuditTest
java -cp /tmp/m3web-audit-classes org.eclipse.nebula.widgets.m3.qualification.M3WebWidgetMappingAudit .
```

The executable's `result=CANDIDATE_HOLD` means *catalogue evidence accepted*,
never transformation, behavior, browser render or widget parity accepted.

Order: inventory -> documentation -> recipe -> diff -> lint -> Java compile
-> qualification test -> canonical OpenRewrite fixed point -> original Tycho
build/tests -> actual SWT/RWT runtime. Keep each missing stage NOT_RUN.
