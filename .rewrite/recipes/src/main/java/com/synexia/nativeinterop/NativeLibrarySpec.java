// SPDX-License-Identifier: Apache-2.0

package com.synexia.nativeinterop;

import java.nio.file.Path;
import java.util.Objects;

/** One native library expressed as either an explicit path or a platform-resolved logical name. */
public record NativeLibrarySpec(String logicalName, Path explicitPath) {

    public NativeLibrarySpec {
        logicalName = logicalName == null ? "" : logicalName.trim();
        explicitPath =
                explicitPath == null ? null : explicitPath.toAbsolutePath().normalize();
        if (logicalName.isBlank() && explicitPath == null) {
            throw new IllegalArgumentException("A logical library name or explicit path is required");
        }
    }

    public static NativeLibrarySpec named(String logicalName) {
        return new NativeLibrarySpec(Objects.requireNonNull(logicalName, "logicalName"), null);
    }

    public static NativeLibrarySpec at(Path explicitPath) {
        return new NativeLibrarySpec("", Objects.requireNonNull(explicitPath, "explicitPath"));
    }

    public static NativeLibrarySpec fromProperties(String prefix, String defaultLogicalName) {
        Objects.requireNonNull(prefix, "prefix");
        String path = System.getProperty(prefix + ".path");
        if (path != null && !path.isBlank()) {
            return at(Path.of(path));
        }
        String name = System.getProperty(prefix + ".name", defaultLogicalName);
        return named(name);
    }

    public boolean explicit() {
        return explicitPath != null;
    }

    public String loadTarget() {
        return explicit() ? explicitPath.toString() : logicalName;
    }

    public String cacheKey() {
        return explicit() ? "path:" + explicitPath : "name:" + logicalName;
    }
}
