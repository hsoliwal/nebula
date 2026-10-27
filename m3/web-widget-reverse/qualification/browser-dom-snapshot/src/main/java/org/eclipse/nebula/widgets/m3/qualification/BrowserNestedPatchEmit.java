// SPDX-License-Identifier: Apache-2.0
// Test-only emitter against ORIGINAL Nebula Java class. No copied JavaScript algorithm.
package org.eclipse.nebula.widgets.m3.qualification;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.nebula.widgets.m3.internal.M3BrowserScripts;

public final class BrowserNestedPatchEmit {
    private BrowserNestedPatchEmit() {}

    private static void save(Path dir, String name, Map<String,String> patches, boolean values)
            throws Exception {
        Files.writeString(dir.resolve(name + ".js"), M3BrowserScripts.patch(patches, values),
                StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("one output directory");
        Path out = Path.of(args[0]);
        if (!Files.isDirectory(out)) throw new IllegalArgumentException("missing output directory");
        Map<String,String> parentAndChild = new LinkedHashMap<>();
        parentAndChild.put("parent", "parent replacement <em>literal</em> 😃");
        parentAndChild.put("child", "THIS MUST NOT COUNT WHEN DETACHED");
        save(out, "parent-child", parentAndChild, false);
        Map<String,String> ancestorBranch = new LinkedHashMap<>();
        ancestorBranch.put("outer", "new outer");
        ancestorBranch.put("middle", "old middle");
        ancestorBranch.put("inner", "old inner");
        save(out, "deep-nested", ancestorBranch, false);
        Map<String,String> siblings = new LinkedHashMap<>();
        siblings.put("one", "new one");
        siblings.put("two", "new two");
        save(out, "siblings", siblings, false);
        save(out, "child-only", Map.of("child", "new child"), false);
        save(out, "duplicate-id", Map.of("same", "changed"), false);
        save(out, "text-safe", Map.of("parent", "<img src=x onerror=evil()> & \"😃\""), false);
        save(out, "values", Map.of("field", "new value", "other", "a<b&😃"), true);
        save(out, "empty", Map.of(), false);
        save(out, "proto", Map.of("__proto__", "safe", "constructor", "clean"), false);
        System.out.println("JAVA_ORIGINAL_PATCH_EMITTER_PASS scenarios=9");
    }
}
