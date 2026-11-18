# Nebula VNC / RFB

`org.eclipse.nebula.widgets.vnc.VncViewer` is an SWT `Canvas` that displays a
CPU-managed framebuffer decoded by a dedicated network thread. SWT images are
created and disposed only on the UI thread. Frame updates are versioned and
coalesced before painting, and the image is scaled with aspect ratio preserved.

Supported in this first integration:
- RFB protocol 3.3, 3.7 and 3.8; None or legacy VNC DES challenge security.
- Client-initiated 32-bit true-colour pixel format.
- Raw, CopyRect, Hextile (16x16 tiles), DesktopSize, clipboard notifications and Bell.
- Key and pointer events, wheel events, and explicit client clipboard sending.
- Byte/rectangle/dimension limits, overlapping CopyRect, disposed-widget
  cancellation and decoupled SWT/network ownership.

**Security:** VNC's None and classic DES challenge modes do **not** encrypt
the framebuffer, keystrokes or clipboard. The default `connect(host, port,
password)` only accepts explicit localhost/loopback names. Use an SSH tunnel
or caller-managed secure `Socket`; `connectInsecureRemote` is a deliberate
opt-in for trusted test networks. Never use legacy VNC password authentication
as a substitute for encryption.

**Scope:** This is a new Nebula RFB implementation, not a literal copy of the
historical Eclipse TmL bundles. TmL's protocol / viewer / view separation is
the architecture reference; the original project components were
`org.eclipse.tml.protocol`, `org.eclipse.tml.vncviewer`, and
`org.eclipse.tml.vncviewer.vncviews`.
See https://www.eclipse.org/lists/ecf-dev/msg02002.html. The code here is
EPL-2.0. Original upstream code and third-party notices are not repackaged.

Not yet supported: Tight/ZRLE, cursor pseudorectangles, VeNCrypt/TLS,
RFB 4.x, extended keyboard layouts, dedicated scaling/native-layer paths,
automatic clipboard integration, or audio. They require separate protocol
qualification and are intentionally not advertised as complete.

The Tycho feature is incubating; headless framebuffer and loopback RFB tests
are in `org.eclipse.nebula.widgets.vnc.tests`. Native pixel tests must also
qualify on Windows, GTK and Cocoa before release.

## Native Z-order and layered applications

`VncViewer` is a real SWT `Canvas`, so the existing SWT-fork
`org.eclipse.swt.custom.LayeredViewport` can place it in a native control layer.
The merged `org.eclipse.nebula.widgets.terminal.Terminal` is likewise a real
`Composite`. With both optional Nebula widgets and the SWT fork installed,
an application can put them in the same Z-ordered viewport:

```java
LayeredViewport layers = new LayeredViewport(parent, SWT.NONE);
VncViewer remote = new VncViewer(layers, SWT.NONE);
Terminal terminal = new Terminal(layers, SWT.NONE, connector);
var desktop = layers.addControlLayer("desktop", remote, 0, 0, 1280, 720);
var console = layers.addControlLayer("console", terminal, 24, 24, 700, 320);
console.setPinned(SWT.HORIZONTAL | SWT.VERTICAL);
```

The parent owns the viewport, while each child still owns its connection,
input, focus and lifecycle. The application must explicitly connect either
widget and dispose them when appropriate; construction does not open a
network connection. Layers can be reordered, moved, hidden and pinned, and
their native Controls retain keyboard, mouse, clipboard and IME ownership.

Native child Controls are above the viewport's painted layers. They cannot
be arbitrarily alpha-blended with the painted pixels or be interleaved below
them. `Layer.setNative(true)` instead applies to eligible *image* layers on
an installed compositor backend; it is not needed for a VNC or Terminal
control. This is optional SWT-fork integration, not a requirement imposed on
all users of the standard Nebula VNC bundle.

For each RFB frame, the viewer now maintains one SWT image, copies the union
of changed framebuffer rectangles, and schedules clipped screen damage.
First frames, desktop resizing and missing damage history still refresh the
whole image. This does not change the existing RFB transport or its security
limitations.
