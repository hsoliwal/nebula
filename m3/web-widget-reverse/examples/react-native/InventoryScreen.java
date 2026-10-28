// SPDX-License-Identifier: Apache-2.0
// Generated static subset; candidate only. UiBinder root SHA256: f24fda5c6a60c94183e705d71296a2e448b2183b251d775265916f416e8042e3
package com.synexia.mindex.viewer.web.generated;
public final class InventoryScreen {
  private InventoryScreen() {}
  private static java.lang.String utf8(java.lang.String data) { return new java.lang.String(java.util.Base64.getDecoder().decode(data), java.nio.charset.StandardCharsets.UTF_8); }
  public static com.synexia.mindex.viewer.web.M3WebWidgetScreen build() {
    return new com.synexia.mindex.viewer.web.M3WebWidgetScreen("UiBinder screen", java.util.List.of(
      new com.synexia.mindex.viewer.web.M3WebWidgetScreen.Row("heading", com.synexia.mindex.viewer.web.M3WebWidgetScreen.Kind.LABEL, utf8("SW52ZW50b3J5ICYgc3RvY2s="), true),
      new com.synexia.mindex.viewer.web.M3WebWidgetScreen.Row("detail", com.synexia.mindex.viewer.web.M3WebWidgetScreen.Kind.LABEL, utf8("QXZhaWxhYmxlOiAyMSBpdGVtcw=="), true),
      new com.synexia.mindex.viewer.web.M3WebWidgetScreen.Row("refresh", com.synexia.mindex.viewer.web.M3WebWidgetScreen.Kind.BUTTON, utf8("UmVmcmVzaCBzdG9jaw=="), true),
      new com.synexia.mindex.viewer.web.M3WebWidgetScreen.Row("export", com.synexia.mindex.viewer.web.M3WebWidgetScreen.Kind.BUTTON, utf8("RXhwb3J0"), false)));
  }
  /** Same immutable model, now delivered through the existing SWT Canvas owner. */
  public static org.eclipse.swt.widgets.Control createSwt(
      org.eclipse.swt.widgets.Composite parent, int style) {
    return new com.synexia.mindex.viewer.swt.M3NebulaWebScreen(parent, style, build());
  }
  public static void main(java.lang.String[] args) throws java.lang.Exception {
    if (args.length != 1) throw new java.lang.IllegalArgumentException("output dir required");
    com.synexia.mindex.viewer.web.M3WebCompiler.write(build(), java.nio.file.Path.of(args[0]));
  }
}
