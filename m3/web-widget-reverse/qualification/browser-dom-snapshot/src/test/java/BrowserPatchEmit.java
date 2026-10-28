// SPDX-License-Identifier: Apache-2.0
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.nebula.widgets.m3.internal.M3BrowserScripts;

/** Produce Java source-owned JavaScript; tests never hand-translate its semantics. */
public final class BrowserPatchEmit {
    private BrowserPatchEmit() { }
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("output dir");
        Path dir = Path.of(args[0]);
        Files.createDirectories(dir);
        Map<String, String> nested = new LinkedHashMap<>();
        nested.put("parent", "ROOT_REPLACEMENT");
        nested.put("child", "CHILD_REPLACEMENT");
        Map<String, String> siblings = new LinkedHashMap<>();
        siblings.put("left", "L&<");
        siblings.put("right", "R\\\"'🙂");
        Map<String,String> duplicate = Map.of("same", "DUPLICATE");
        for (var entry : Map.of(
                "nested-text.js", M3BrowserScripts.patch(nested, false),
                "nested-values.js", M3BrowserScripts.patch(nested, true),
                "siblings.js", M3BrowserScripts.patch(siblings, false),
                "duplicate.js", M3BrowserScripts.patch(duplicate, false),
                "child-only.js", M3BrowserScripts.patch(Map.of("child", "CHILD_ONLY"), false),
                "empty.js", M3BrowserScripts.patch(Map.of(), false)).entrySet()) {
            Files.writeString(dir.resolve(entry.getKey()), entry.getValue(), StandardCharsets.UTF_8);
        }
        System.out.println("JAVA_OWNED_SCRIPT_EMISSION_PASS cases=6");
    }
}
