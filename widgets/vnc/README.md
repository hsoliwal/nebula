<!-- SPDX-License-Identifier: EPL-2.0 -->
# Nebula VNC / Remote Framebuffer Viewer

The Eclipse SWT `VncCanvas` embeds the standalone `RfbClient` protocol engine
without introducing a second toolkit, copying an obsolete TmL plugin, or
modifying SWT public APIs. Parsing and transport use worker threads. SWT Image,
GC, Canvas and input ownership remain on the Display thread.

## Embedding and transport security

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

## Implemented protocol coverage

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

## Acceptance limitations

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

## CPU/GUI ownership and native repainting

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

## Focused qualification

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

## Tight encoding (7)

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


## Remote cursor shapes

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
