<!-- SPDX-License-Identifier: EPL-2.0 -->
# Nebula VNC / Remote Framebuffer Viewer

Provides a standalone protocol engine (\`RfbClient\`) and an SWT Canvas
(\`VncCanvas\`). The engine is a new EPL-2.0 implementation informed by the
Eclipse TmL RFB donor structure and RFC 6143; it is not a copied TmL source
tree. The existing Nebula widget conventions and Tycho test layout are used.

\`\`\`java
VncCanvas viewer = new VncCanvas(parent, SWT.NONE);
viewer.connect("127.0.0.1", 5900, new char[0], false); // local SSH tunnel
\`\`\`

Supported in this version:
- RFB versions 3.3, 3.7 and 3.8; None and legacy DES VNC authentication.
- Raw, CopyRect, Hextile and DesktopSize framebuffer updates.
- 32-bit little-endian RGB888, pointer, wheel, keyboard, clipboard and bell.
- A capped framebuffer, capped clipboard sizes, bounded output queue,
  immutable read snapshots, coalesced SWT repainting, cancellation on dispose.

Not yet supported: Tight/ZRLE, VeNCrypt/TLS, remote clipboard Unicode
extensions, software cursor pseudo-encoding, full keyboard layout/IME and
native cross-platform GUI pixel-acceptance. Do **not** expose bare RFB to the
public Internet: even VNC authentication does not encrypt a session. Connect
through a secured SSH tunnel, or explicitly opt into a private trusted network.

Validation: the bundled JUnit scripted peer exercises handshake, Raw,
CopyRect, snapshot defensiveness and authentication input validation.
Native SWT/JNI acceptance and performance benchmarking remain separate gates.

Protocol: RFC 6143, https://www.rfc-editor.org/rfc/rfc6143
Historical donor: org.eclipse.tml.protocol, org.eclipse.tml.vncviewer,
org.eclipse.tml.vncviewer.vncviews (Eclipse TmL, EPL).


Extended RFB decoding now negotiates RRE (2), TRLE (15), and ZRLE (16) with bounded
inflation and per-connection continuous zlib state. `VncCanvas.connectTls(host,
port, password)` supports *direct TLS* using the JVM truststore and strict
hostname verification; it does not negotiate VeNCrypt. The legacy RFB clipboard
is Latin-1 only and rejects characters it cannot represent.
