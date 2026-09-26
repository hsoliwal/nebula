# CWT universal virtual-control substrate

Baseline: `EclipseNebula/nebula@8e445a8be00c27eb1c77aa5698a0b7e57e3f8f6d`.

## Existing CWT mechanics

The existing `org.eclipse.nebula.cwt.v` package already supplies:

- `VCanvas`: one SWT Canvas hosting a virtual `VPanel`;
- `VControl`: retained bounds/state/data/event dispatch and painter delegation;
- `IControlPainter`: background/content/border painting contract;
- `VControlPainter`: reusable default painter implementation;
- `VPanel`: virtual child/layout owner;
- `VButton`, `VLabel`, `VSpacer`: concrete virtual controls;
- `VNative<T extends Control>`: embeds a real SWT control in the virtual hierarchy.

`VControl.Type` already declares `Custom`, but upstream has no concrete generic custom virtual
control.

## Additive extension

`VCustom` fills that missing leaf.

Contract:

- retains no application/domain model;
- uses the normal `VControl` data/event/state APIs;
- accepts any `IControlPainter`;
- defaults to `VControlPainter`;
- exposes an optional preferred size for layout;
- respects SWT width/height hints in `computeSize`;
- remains a normal virtual child of `VPanel`;
- reports `VControl.Type.Custom`.

This is intentionally lower-level than domain widgets. Higher layers may translate arbitrary model
state into a painter and event handlers while keeping identity/state outside CWT.

## Synexia use

Synexia will use `VCustom` as the universal native-painted fallback after checking for a more
specific Nebula/SWT/JFace/Draw2D/Zest/GEF owner.

The fallback order becomes:

1. specialized native widget/owner;
2. CWT `VCustom` retained native painting;
3. exact foreign Swing raster bridge only when byte/pixel-compatible foreign painting is explicitly
   required.

No AWT image buffer is required by `VCustom` itself.

## Compatibility

- no existing CWT public signature is modified;
- exported package remains unchanged;
- JavaSE-11 baseline is preserved;
- existing virtual controls retain behavior;
- upstream mergeability is preserved because the change is additive.
