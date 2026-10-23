// SPDX-License-Identifier: Apache-2.0
// Generated static subset; candidate only. UiBinder root SHA256: e17b47ebeae39aa76ced6baaf91490d84662c81dea43de0a8348d76f7d251693
package com.synexia.mindex.viewer.web.generated;
public final class OrdersScreen {
  private OrdersScreen() {}
  private static java.lang.String utf8(java.lang.String data) { return new java.lang.String(java.util.Base64.getDecoder().decode(data), java.nio.charset.StandardCharsets.UTF_8); }
  public static com.synexia.mindex.viewer.web.M3WebWidgetScreen build() {
    return new com.synexia.mindex.viewer.web.M3WebWidgetScreen("UiBinder screen", java.util.List.of(
      new com.synexia.mindex.viewer.web.M3WebWidgetScreen.Row("title", com.synexia.mindex.viewer.web.M3WebWidgetScreen.Kind.LABEL, utf8("T3JkZXJzICYgc2hpcHBpbmc="), true),
      new com.synexia.mindex.viewer.web.M3WebWidgetScreen.Row("description", com.synexia.mindex.viewer.web.M3WebWidgetScreen.Kind.LABEL, utf8("QSBicm93c2VyICYgbmF0aXZlIHZpZXdwb3J0IGNhbmRpZGF0ZQ=="), true),
      new com.synexia.mindex.viewer.web.M3WebWidgetScreen.Row("refresh", com.synexia.mindex.viewer.web.M3WebWidgetScreen.Kind.BUTTON, utf8("UmVmcmVzaCBvcmRlcnM="), true),
      new com.synexia.mindex.viewer.web.M3WebWidgetScreen.Row("cancel", com.synexia.mindex.viewer.web.M3WebWidgetScreen.Kind.BUTTON, utf8("Q2FuY2Vs"), false)));
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
