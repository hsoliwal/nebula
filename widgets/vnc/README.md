<!-- SPDX-License-Identifier: EPL-2.0 -->
# Nebula RFB / VNC Viewer (Incubation)

This bundle provides an embeddable SWT VNC viewer backed by a bounded, pure-Java RFB session and framebuffer. It adapts the historical Eclipse DSDP/TmL VNC **capability** into the current Nebula product layout. No archived DSDP implementation source or third-party binary is copied; the protocol reference is RFC 6143.

## Supported, deliberately narrow protocol

* RFB 3.8 with security type **None** only. Callers must explicitly approve this mode **after** establishing a trusted local, SSH-forwarded, or TLS-protected transport; otherwise the handshake fails closed. No socket, credentials, host trust or authentication are created by this bundle.
* Negotiates 32-bpp, depth-24, little-endian true colour; Raw, overlap-safe CopyRect and DesktopSize updates.
* Maximum 8192 pixels per axis, 16,777,216 total pixels, 4096 rectangles/update, 4096-byte server name and 65536-byte ignored clipboard message. Unknown encodings and malformed rectangles fail closed.
* Emits immutable whole-frame snapshots. The SWT viewer coalesces frame updates to one queued UI refresh and disposes images with the widget.
* Framebuffer decoding and Java-side ImageData conversion run off the SWT event thread. Only native SWT Image creation, painting and disposal run on the UI thread; closing transport streams is also dispatched off the UI thread.
* Remote keyboard and pointer forwarding are **off by default** and must be explicitly enabled after the caller's authorization.
* Authorized input is sent by a bounded, serial virtual-thread queue; SWT event handlers never write directly to network streams. Overflow disables further remote input and reports failure. Open the RFB client off the UI thread.

## Example

```java
// Input/output come from a transport already established and authenticated
// under the host application's policy (e.g. an SSH/TLS tunnel).
RfbClient client = RfbClient.open(input, output, true); // approves RFB None *inside the tunnel*
VncViewer viewer = new VncViewer(parent, SWT.NONE);
viewer.setFailureListener(error -> log(error));
viewer.attach(client); // viewer now owns client streams and reader lifecycle
viewer.setInputEnabled(true); // optional, explicit remote-input authorization
```

## Deliberately unsupported

RFB 3.3/3.7, VNC password authentication, SASL/VeNCrypt, ZRLE/Hextile/Tight, Cursor pseudo-encoding, clipboard synchronization, IME, extended keyboard negotiation, native GPU rendering and measured performance. These require separate compatibility/security lanes before broad use. The current module is *not* a fully interoperable VNC client.

## Ownership and qualification

* Eclipse terminal is an independent text-grid subsystem; see Nebula Terminal widget PR #588. RFB has no terminal-parser dependency.
* Historical source provenance: Eclipse DSDP/TmL `org.eclipse.tml.protocol`, `org.eclipse.tml.vncviewer`, `org.eclipse.tml.vncviewer.vncviews` (archived; licenses must be reviewed per file before copying).
* New Nebula product source: EPL-2.0. Canonical Synexia donor/recipe work: `hsoliwal/com.synexia` PRs #10739 and #10742.
* Promotion gates: Java 21 compilation, Tycho reactor, SWT UI tests on Windows/Linux/macOS, screenshots, disposal/threading/resource checks, RFB 3.8 interoperability and security review. An unexecuted gate is NOT_RUN, never PASS.
