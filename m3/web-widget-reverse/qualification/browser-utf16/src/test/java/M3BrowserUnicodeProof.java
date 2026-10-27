// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SplittableRandom;
import org.eclipse.nebula.widgets.m3.internal.M3BrowserScripts;

/** Standalone Java21 proof of the existing optional Browser receiver; no SWT stand-ins. */
public final class M3BrowserUnicodeProof {
    private M3BrowserUnicodeProof() {}
    private static long accepted;
    private static long rejected;

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("output directory required");
        Path output = Path.of(args[0]);
        Files.createDirectories(output);
        SplittableRandom rng = new SplittableRandom(0x51f21a6c20261027L);
        int[] scalars = {'a', '\\', '"', '\n', '\r', '\t', '<', '>', '&',
                0x2028, 0x2029, 0x00e9, 0x0301, 0x4e2d, 0x1f642, 0x1f9ed,
                0x007f, 0, 0x2603, 0xffff};
        for (int i = 0; i < 50_000; i++) {
            StringBuilder data = new StringBuilder();
            for (int j = rng.nextInt(20); j >= 0; j--) {
                data.appendCodePoint(scalars[rng.nextInt(scalars.length)]);
            }
            String value = data.toString();
            String key = "key" + i;
            String literal = M3BrowserScripts.jsString(value);
            if (!literal.startsWith("\"") || !literal.endsWith("\"")
                    || literal.contains("</script>")) {
                throw new AssertionError("invalid encoded JavaScript string");
            }
            if (!M3BrowserScripts.validEvent(new Object[]{"input", key, value})) {
                throw new AssertionError("valid event was rejected");
            }
            String script = M3BrowserScripts.patch(Map.of(key, value), false);
            if (!script.contains("node.textContent=patch[id]") || script.contains("innerHTML")) {
                throw new AssertionError("unsafe DOM patch");
            }
            accepted += 3;
        }
        for (int unit = Character.MIN_HIGH_SURROGATE;
                unit <= Character.MAX_LOW_SURROGATE; unit++) {
            String malformed = "before" + (char) unit + "after";
            if (M3BrowserScripts.validEvent(new Object[]{"change", "id", malformed})
                    || M3BrowserScripts.validEvent(new Object[]{"input", malformed, "ok"})) {
                throw new AssertionError("malformed event accepted");
            }
            refuse(() -> M3BrowserScripts.jsString(malformed));
            refuse(() -> M3BrowserScripts.patch(Map.of("id", malformed), true));
            rejected += 4;
        }
        Map<String,String> text = new LinkedHashMap<>();
        text.put("a\"] [data-nebula-id=\"victim", "<img onerror=evil()> & "
                + new String(Character.toChars(0x1f642)));
        text.put("normal", "line 1\nline 2\u2028");
        Files.writeString(output.resolve("text.js"), M3BrowserScripts.patch(text, false),
                StandardCharsets.UTF_8);
        Files.writeString(output.resolve("values.js"),
                M3BrowserScripts.patch(Map.of("normal", "a<&\""), true), StandardCharsets.UTF_8);
        Files.writeString(output.resolve("install.js"),
                M3BrowserScripts.install(), StandardCharsets.UTF_8);
        System.out.println("M3_BROWSER_UTF16_JAVA_PASS accepted=" + accepted + " rejected=" + rejected);
    }

    private static void refuse(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("malformed UTF-16 not refused");
    }
}
