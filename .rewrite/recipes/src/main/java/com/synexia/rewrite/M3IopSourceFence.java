// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.nio.file.Path;

/** Hard source fence for the OpenRewrite M3 lane that is allowed to mutate only Synexia IOP code. */
public final class M3IopSourceFence {
    private static final String ROOT_PREFIX = "synexia-iop/src/main/java/com/synexia/iop/";
    private static final String MODULE_PREFIX = "src/main/java/com/synexia/iop/";

    private M3IopSourceFence() {}

    public static boolean admits(Path sourcePath) {
        if (sourcePath == null) {
            return false;
        }
        String path = sourcePath.normalize().toString().replace('\\', '/');
        if (!path.endsWith(".java") || path.contains("../")) {
            return false;
        }
        return path.startsWith(ROOT_PREFIX)
                || path.startsWith(MODULE_PREFIX)
                || path.contains("/" + ROOT_PREFIX);
    }

    public static void requireIop(Path sourcePath) {
        if (!admits(sourcePath)) {
            throw new IllegalArgumentException("OpenRewrite IOP lane rejected non-IOP source: " + sourcePath);
        }
    }
}
