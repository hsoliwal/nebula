# EyeButton hover-damage acceptance

`EyeButtonViewportTest` exercises the package-private production `EyeButton`
directly. In image mode, mouse enter and exit must schedule no paint because
both states draw the same supplied image. The test counts actual SWT Paint
events, checks the incremental pixels, and compares them with a forced full
repaint.

The password-revealer bundle has no dedicated test plug-in. Keep this
headful acceptance source outside the product bundle and compile it against
the exact bundle output instead of adding a product dependency or public API.

From the configured Nebula worktree:

```sh
docs/m3/every-widget/tools/build_bundle.sh passwordrevealer
docs/m3/every-widget/tools/run_eye_button_viewport_test.sh
```

Set `SWT_JAR` to the exact SWT fork jar when validating against the M3 viewport
backend. The runner uses the repository's hidden desktop lock and records a
per-run log. A source addition is not a test pass; update the every-widget
ledger only after the marker is observed on the intended backend(s).
