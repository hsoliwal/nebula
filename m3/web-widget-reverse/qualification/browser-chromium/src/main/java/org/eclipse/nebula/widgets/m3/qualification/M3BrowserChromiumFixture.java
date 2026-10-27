// SPDX-License-Identifier: Apache-2.0
package org.eclipse.nebula.widgets.m3.qualification;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.nebula.widgets.m3.internal.M3BrowserScripts;

/**
 * Test-only emitter of the original Nebula Browser JS, not a new widget owner.
 * Browser event/patch behavior is tested by Chromium against emitted JS, not
 * by copying or reimplementing the JavaScript into a second harness.
 */
public final class M3BrowserChromiumFixture {
    private M3BrowserChromiumFixture() { }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("one output directory required");
        }
        Path output = Path.of(args[0]);
        if (!Files.isDirectory(output)) {
            throw new IllegalArgumentException("output directory missing");
        }
        Map<String, String> text = new LinkedHashMap<>();
        text.put("a\"] [data-nebula-id=\"victim", "<img src=x onerror=evil()> & 🙂");
        text.put("__proto__", "safe proto");
        text.put("constructor", "safe constructor");
        text.put("normal", "line 1\nline 2\u2028");

        Map<String, String> values = new LinkedHashMap<>();
        values.put("field", "Ada <&\"🙂");
        values.put("select", "option2");

        Files.writeString(output.resolve("install.js"), M3BrowserScripts.install(),
                StandardCharsets.UTF_8);
        Files.writeString(output.resolve("text.js"), M3BrowserScripts.patch(text, false),
                StandardCharsets.UTF_8);
        Files.writeString(output.resolve("values.js"), M3BrowserScripts.patch(values, true),
                StandardCharsets.UTF_8);

        int denied = 0;
        for (String invalid : new String[] {"\ud800", "\udc00", "hello\ud800world", "a\udc00b"}) {
            try {
                M3BrowserScripts.jsString(invalid);
            } catch (IllegalArgumentException expected) {
                denied++;
            }
            try {
                M3BrowserScripts.patch(Map.of("field", invalid), false);
            } catch (IllegalArgumentException expected) {
                denied++;
            }
            if (!M3BrowserScripts.validEvent(new Object[] {"click", "field", invalid})) {
                denied++;
            }
        }
        if (denied != 12) {
            throw new AssertionError("malformed UTF-16 boundary count " + denied);
        }
        System.out.println("MERGED_BROWSER_JAVA_EMIT_PASS encoded_scripts=3 malformed_refusals=" + denied);
    }
}
