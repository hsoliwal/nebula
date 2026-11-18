# Nebula VNC / RFB

`org.eclipse.nebula.widgets.vnc.VncViewer` is an SWT `Canvas` that displays a
CPU-managed framebuffer decoded by a dedicated network thread. SWT images are
created and disposed only on the UI thread. Frame updates are versioned and
coalesced before painting; a maximum 512x512-pixel temporary image is produced
for each visible paint tile, avoiding a second native copy of the full desktop.
The image is scaled with aspect ratio preserved.

Supported in this first integration:
- RFB protocol 3.3, 3.7 and 3.8; None or legacy VNC DES challenge security.
- Client-initiated 32-bit true-colour pixel format.
- Raw, CopyRect, RRE, Hextile (16x16 tiles), streaming Zlib and ZRLE
  (raw, solid, packed palette and run-length tiles), DesktopSize, LastRect,
  clipboard notifications and Bell.
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

Not yet supported: Tight/JPEG, cursor pseudorectangles, VeNCrypt/TLS,
RFB 4.x, extended keyboard layouts, dedicated scaling/native-layer paths,
automatic clipboard integration, or audio. They require separate protocol
qualification and are intentionally not advertised as complete.

The Tycho feature is incubating; headless framebuffer, compression, malformed
stream and loopback RFB tests are in `org.eclipse.nebula.widgets.vnc.tests`.
`VncViewerNativeTest` captures the *visible screen* using SWT `GC.copyArea`;
it must actually run on Windows, GTK and Cocoa before release.
