# Nebula review flag — upstream 5d0bcbb9 CSS-bundle config at the older revision (2026-10-19)

**Report only. Nothing was changed.** Found by the upstream-sync content audit (fork master compared blob by blob with `EclipseNebula/nebula` master 5d0bcbb9 and every historical upstream blob of each path).

## Finding

- The fork is not behind upstream by commits: upstream master 5d0bcbb9 ("Provide fix for deprecated CSS classes #709 (#710)", 2026-10-03) is an ancestor of fork master.
- Content: 0 upstream paths are missing from the fork. 605 paths differ from upstream.
- **30** of those paths hold **exactly the pre-5d0bcbb9 upstream blob**. That is, upstream's newer revision of these files is not on fork master. All 30 were set back by the fork commit 33aaf10f "First public release baseline" (2026-10-07).
- The other 10 files of 5d0bcbb9:
  - `.gitignore` equals upstream.
  - 9 files are fork-modified: the CSS property handlers / `CSSShelfRenderer` / `BaseCSSThemingTest`, and the cdatetime `example.e4` MANIFEST and `.product`.
  - For example, `GridPropertyHandler` keeps the W3C `CSSPrimitiveValue` facade with a documented "legacy CSS2FontHelper classification contract" instead of upstream's `CssValues.CssNumeric`.

## The 30 files (pre-5d0bcbb9 revision on fork master)

Per widget: cdatetime 6, grid 5, oscilloscope 5, pshelf 3, roundedswitch 5, tablecombo 6. Kinds: `*.css/.classpath`, `*.css/.settings/org.eclipse.jdt.core.prefs`, `*.css/META-INF/MANIFEST.MF`, `*.example.e4/META-INF/MANIFEST.MF`, `*.example.e4/*.product`, `*.tests|test/META-INF/MANIFEST.MF`.

Upstream's change in these files, using `grid.css` MANIFEST as the example:
- BREE `JavaSE-11` -> `JavaSE-17`;
- drops `org.eclipse.e4.ui.services`;
- `org.eclipse.e4.ui.css.core;bundle-version="0.10.0"` -> `"0.14.800"`.

The `.classpath` and jdt prefs files follow the BREE change.

```text
widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.css/.classpath
widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.css/.settings/org.eclipse.jdt.core.prefs
widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.css/META-INF/MANIFEST.MF
widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.tests/.classpath
widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.tests/.settings/org.eclipse.jdt.core.prefs
widgets/cdatetime/org.eclipse.nebula.widgets.cdatetime.tests/META-INF/MANIFEST.MF
widgets/grid/org.eclipse.nebula.widgets.grid.css/.classpath
widgets/grid/org.eclipse.nebula.widgets.grid.css/.settings/org.eclipse.jdt.core.prefs
widgets/grid/org.eclipse.nebula.widgets.grid.css/META-INF/MANIFEST.MF
widgets/grid/org.eclipse.nebula.widgets.grid.example.e4/META-INF/MANIFEST.MF
widgets/grid/org.eclipse.nebula.widgets.grid.example.e4/org.eclipse.nebula.widgets.grid.example.e4.product
widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.css/.classpath
widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.css/.settings/org.eclipse.jdt.core.prefs
widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.css/META-INF/MANIFEST.MF
widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.example.e4/META-INF/MANIFEST.MF
widgets/oscilloscope/org.eclipse.nebula.widgets.oscilloscope.example.e4/org.eclipse.nebula.widgets.oscilloscope.example.e4.product
widgets/pshelf/org.eclipse.nebula.widgets.pshelf.css/.classpath
widgets/pshelf/org.eclipse.nebula.widgets.pshelf.css/.settings/org.eclipse.jdt.core.prefs
widgets/pshelf/org.eclipse.nebula.widgets.pshelf.css/META-INF/MANIFEST.MF
widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.css/.classpath
widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.css/.settings/org.eclipse.jdt.core.prefs
widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.css/META-INF/MANIFEST.MF
widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.example.e4/META-INF/MANIFEST.MF
widgets/roundedswitch/org.eclipse.nebula.widgets.roundedswitch.example.e4/org.eclipse.nebula.widgets.roundedswitch.example.e4.product
widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.css/.classpath
widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.css/.settings/org.eclipse.jdt.core.prefs
widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.css/META-INF/MANIFEST.MF
widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.example.e4/META-INF/MANIFEST.MF
widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.example.e4/org.eclipse.nebula.widgets.tablecombo.example.e4.product
widgets/tablecombo/org.eclipse.nebula.widgets.tablecombo.test/META-INF/MANIFEST.MF
```

## Why this is a flag, not a fix

The fork's Java side intentionally did not adopt upstream's `CssValues` API (it keeps the W3C facade), and the older MANIFESTs match that choice (css.core 0.10.0, BREE 11). Taking upstream's 30 config files alone would require css.core 0.14.800 without the code that needs it. Taking them together with upstream's Java changes would drop the fork's compatibility layer.

## Decision needed (owner)

1. **Keep as is** (fork policy: W3C facade, BREE 11). Optionally record that in the bundles.
2. **Adopt upstream 5d0bcbb9 fully.** Restore the 30 files to upstream's blobs and port the 9 fork-modified files to `CssValues`, keeping fork edits. Then run the Nebula CSS tests (`BaseCSSThemingTest`, grid/cdatetime CSS tests).
3. **Mixed.** Take only the BREE 17 bump (`.classpath`, prefs, BREE line) and keep the css.core range.

Fork master at audit time: 4e2c91dce6 (2026-10-19); rechecked on 8a89327e8a (2026-10-20): all 30 still hold the pre-5d0bcbb9 blob.
