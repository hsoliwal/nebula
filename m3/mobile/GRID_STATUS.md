# Nebula mobile Grid core — qualified scope

The array-backed `MobileGridViewport` has been received unchanged from
[hsoliwal/nebula#395](https://github.com/hsoliwal/nebula/pull/395)
into the original Grid plugin's internal Java package. It is source-compatible
with Java 17 and manages long logical row indices with bounded
viewport-plus-overscan slots and no per-logical-row object allocation.

To execute the portable algorithmic contract checks:

```sh
bash m3/mobile/test-grid-core.sh
```

The tests cover selection updates, partial invalidation, distant jumps,
slot reuse, shrinking item counts, zero-row cleanup and closing.

This is an additive, **not yet wired** internal Grid algorithm. It is not
a mobile implementation of the public `org.eclipse.nebula.widgets.grid.Grid`
widget. Native Android/iOS Grid integration, source recipe convergence,
platform-native painting/input, resource lifecycle and device qualification
remain open. Desktop Grid semantics and public APIs are unchanged.

Do not merge the older host-bound demo PR #395 directly into the SWT #369
backend; its `MobileGridViewBinding` imports a separate
`MobileWidgetRuntime` SPI from the obsolete SWT draft #358.
