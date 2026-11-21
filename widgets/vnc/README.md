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
- Framebuffer Raw (0), CopyRect (1), RRE (2), Hextile (5), TRLE (15),
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

This PR is **not yet a complete native/OSGi qualification**. Tight, VeNCrypt
X509Plain/SASL, encrypted clipboard extensions, software cursor pseudoencoding,
comprehensive international keyboards/IME, multiscreen/fence extensions and
live-server interoperability have not been implemented or verified. The
current SWT viewer also recreates the native Image per published frame; large
screen/dirty-region optimization and memory/CPU benchmarks remain necessary.

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
