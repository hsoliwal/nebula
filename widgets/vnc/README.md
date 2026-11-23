<!-- SPDX-License-Identifier: EPL-2.0 -->
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

## VncCanvas and RfbClient

The bundle also contains a second, independently developed RFB stack:
`org.eclipse.nebula.widgets.vnc.VncCanvas` over the standalone `RfbClient`
protocol engine and its worker-side decoders. The protocol support and
limitation lists above describe `VncViewer`/`RfbConnection`; the sections
below describe `VncCanvas`/`RfbClient`. Neither stack changes the other.

The Eclipse SWT `VncCanvas` embeds the standalone `RfbClient` protocol engine
without introducing a second toolkit, copying an obsolete TmL plugin, or
modifying SWT public APIs. Parsing and transport use worker threads. SWT Image,
GC, Canvas and input ownership remain on the Display thread.

### Embedding and transport security

```java
VncCanvas viewer = new VncCanvas(parent, SWT.NONE);

// Preferred for RFB servers reachable through a local SSH tunnel.
viewer.connect("127.0.0.1", 5900, new char[0], false);

// Endpoint speaks RFB within TLS immediately; checks JVM trust and hostname.
// viewer.connectTls("vnc.example.org", 5901, password);

// Endpoint starts unencrypted RFB, negotiates VeNCrypt v0.2, then X.509 TLS.
// Only X509None (260) and X509Vnc (261) are permitted.
// viewer.connectVeNCrypt("vnc.example.org", 5900, password);
```

Bare non-loopback RFB requires the explicit `allowUnencryptedRemote` opt-in;
use a local SSH tunnel instead wherever possible. Supplying a password cannot
silently downgrade authentication to RFB None. Direct TLS and VeNCrypt require
standard JVM X.509 trust validation and hostname verification and never fall
back to plaintext. Servers with untrusted or mismatched certificates fail closed.
VeNCrypt anonymous TLS, Plain, and other unvalidated subtypes are not enabled.

### Implemented protocol coverage

- RFB protocol versions 3.3, 3.7, 3.8, legacy VNC challenge-response, None,
  and VeNCrypt 0.2 X509None/X509Vnc with checked TLS.
- Framebuffer Raw (0), CopyRect (1), RRE (2), Hextile (5), Tight (7), TRLE (15),
  ZRLE (16) and DesktopSize (-223).
- 32-bit little-endian true-color RGB888 and compact pixels, 16x16 TRLE
  and 64x64 ZRLE tiles, packed palettes, palette reuse, plain/palette RLE,
  bounded compressed input, continuous per-session ZRLE inflater.
- Directional overlap-safe CopyRect with no rectangle-sized temporary array.
- Framebuffer size and clipboard length limits, bounded writer queue,
  immutable public frame snapshots, SWT-thread native rendering, UI update
  coalescing and stale-session suppression on reconnect.
- Pointer, mouse-wheel, basic X11 keysym keyboard mappings, bell and legacy
  Latin-1 clipboard. Unsupported clipboard characters are explicitly rejected.

### Acceptance limitations

This PR is **not yet a complete native/OSGi qualification**. VeNCrypt
X509Plain/SASL, encrypted clipboard extensions, software cursor pseudoencoding,
comprehensive international keyboards/IME, multiscreen/fence extensions and
non-Linux live-server interoperability have not been implemented or verified. The
viewer now reuses native Images for small updates; full framebuffer copies,
resource lifetime under prolonged load, and performance budgets still require qualification.

The standalone decoder tests exercise malformed inputs, palette variants and
continuous compression. Tycho/JUnit scripted-peer tests cover several actual
protocol exchanges. These tests do not substitute for native SWT/JNI acceptance
on Windows, macOS Cocoa and Linux GTK, and must not be reported as passed
before the corresponding CI execution completes.

Donor architecture: historical Eclipse TmL `org.eclipse.tml.protocol`,
`org.eclipse.tml.vncviewer` and `org.eclipse.tml.vncviewer.vncviews`.
This code is an EPL-2.0 implementation, not a copied subtree.

Specifications:
- https://www.rfc-editor.org/rfc/rfc6143
- https://github.com/rfbproto/rfbproto/blob/master/rfbproto.rst

### CPU/GUI ownership and native repainting

`RfbClient` decodes network rectangles on its single I/O worker and publishes
immutable versioned `Frame` snapshots. Each snapshot includes a conservative
dirty rectangle. `VncCanvas` coalesces pending frame damage by session
generation, so old/reconnecting sessions cannot repaint a newer connection.

The SWT event thread owns the Canvas, Image, GC, selection, focus and native
resources. For small framebuffer updates, the existing native Image is reused
and only a dirty sub-image is copied through a short-lived SWT GC. Full-size
and resized updates rebuild the image. Paint invalidation maps changed pixels
into the scaled/letterboxed viewport with a two-pixel boundary margin.

This is **not yet zero-copy**: each published immutable snapshot still clones
the full RGB array. Large desktops require explicit frame-rate/CPU and native
heap qualification before a throughput claim can be made.

Keysyms are retained across KeyPressed/KeyReleased so releases do not use a
different character mapping; focus loss releases outstanding keys and pointer
buttons. Full IME/compound Unicode and platform-specific physical key mapping
remain open.

### Focused qualification

The `.github/workflows/terminal-vnc-contracts.yml` workflow compiles the
Terminal widget separately and runs the VNC Tycho/JUnit bundle with a native
GTK3/Xvfb/Openbox window manager. Its fail-closed contract requires the
decoder/handshake tests, 900 deterministic property cases, queue-budget
concurrency tests, coalesced immutable-frame damage tests and actual visible
screen captures for initial and incremental RFB painting. A passing headless
decoder test is **not** a passing native screenshot or Windows/Cocoa check.

The full Nebula reactor and the SWT receiver/native resource gates remain
separate acceptance requirements. Do not merge or declare cross-platform
parity until each exact-head CI and native lane has executed successfully.

### Tight encoding (7)

Tight rectangles use four persistent zlib streams with reset-bit semantics,
compact RGB888 pixels, Fill, no-zlib Basic, palette, gradient and JPEG image
subtypes. Compressed byte counts, expanded pixel counts, JPEG dimensions and
palette indices are bounded before decoding. JPEG uses the JDK ImageIO codec
on the protocol worker, never the SWT UI thread. The host product must include
a Java 21 runtime with java.desktop for JPEG decoding.

The Tight decoder was checked locally against seven deterministic smoke
scenarios and 400 randomized no-zlib cases; the focused Tycho protocol
suite also contains codec, rejection and scripted-peer tests. These tests
are not a claim of compatibility with every TightVNC/TurboVNC version until
actual server interoperability has run with Tight negotiated.


### Remote cursor shapes

RichCursor (-239) and XCursor (-240) are negotiated as cursor pseudo-encodings.
Cursor rectangles use their origin as the hotspot, *not* the framebuffer
position. The network reader validates the 512x512 shape maximum and decodes
RGB, foreground/background and row-aligned alpha masks on the worker thread.
The immutable cursor shape is sent to \`VncCanvas\` on the SWT event thread.
The Canvas owns each native SWT Cursor, disposing old resources on replacement,
disconnect and parent-driven widget disposal. The cursor parser and native
ownership tests are mandatory in the focused Tycho workflow.

Remote PointerPos, cursor alpha extensions and cross-platform cursor
rendering are not yet qualified. Server-supplied cursor positions must not
be conflated with local mouse input events.

### RFC 6143 Hextile tile rules

The worker-side `RfbHextileDecoder` enforces per-rectangle background and
foreground carry state. A Raw tile's other subencoding bits are ignored
per the RFB protocol; Raw also invalidates the prior carried background and
foreground. A colored-subrectangle tile invalidates the previous foreground,
and an uncolored subrectangle requires an established foreground. Unknown
non-Raw flag combinations and subrectangles outside tile bounds fail closed.

An independent Java 21 contract runs these cases on Linux, Windows and macOS,
and Tycho runs the corresponding JUnit suite. These tests do not replace
native SWT or live-server interoperability.

### LastRect and DesktopSize interoperability

The client advertises LastRect (-224) and ends the current FramebufferUpdate
when that pseudo-rectangle arrives, even when the server advertised a large
placeholder rectangle count such as 65535. DesktopSize (-223) ignores the
rectangle's x/y fields as required by RFC 6143 and preserves overlapping
previous framebuffer contents while resizing. Neither pseudo-rectangle
contains additional payload. The Tycho scripted peer tests both cases.
