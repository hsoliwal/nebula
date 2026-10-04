// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import java.util.Objects;

/** One immutable Python class/function atom discovered without executing Python code. */
public record M3PythonAtom(
        String sourcePath,
        Kind kind,
        String qualifiedName,
        String name,
        int indentation,
        int startOffset,
        int headerEndOffset,
        int bodyStartOffset,
        int endOffset,
        boolean externallyVisible,
        boolean leaf,
        String contractSha256,
        String bodySha256,
        String root) {

    public enum Kind {
        CLASS,
        FUNCTION,
        ASYNC_FUNCTION
    }

    public M3PythonAtom {
        sourcePath = M3OpenRewriteTranspiler.normalizeSourcePathOrDirectory(sourcePath);
        if (M3SourceKind.classify(sourcePath) != M3SourceKind.PYTHON) {
            throw new IllegalArgumentException("Python source path required");
        }
        kind = Objects.requireNonNull(kind, "kind");
        qualifiedName = text(qualifiedName, "qualifiedName");
        name = text(name, "name");
        if (indentation < 0
                || startOffset < 0
                || headerEndOffset < startOffset
                || bodyStartOffset < headerEndOffset
                || endOffset < bodyStartOffset) {
            throw new IllegalArgumentException("invalid Python atom coordinates");
        }
        contractSha256 = sha(contractSha256, "contractSha256");
        bodySha256 = sha(bodySha256, "bodySha256");
        String expected = M3PythonAtomizer.sha256(
                String.join(
                        "\n",
                        "M3-PYTHON-ATOM/1",
                        sourcePath,
                        kind.name(),
                        qualifiedName,
                        name,
                        Integer.toString(indentation),
                        Integer.toString(startOffset),
                        Integer.toString(headerEndOffset),
                        Integer.toString(bodyStartOffset),
                        Integer.toString(endOffset),
                        Boolean.toString(externallyVisible),
                        Boolean.toString(leaf),
                        contractSha256,
                        bodySha256));
        root = root == null || root.isBlank() ? expected : sha(root, "root");
        if (!expected.equals(root)) {
            throw new IllegalArgumentException("M3_PYTHON_ATOM_ROOT_MISMATCH");
        }
    }

    public String identity() {
        return kind + ":" + qualifiedName;
    }

    private static String text(String value, String field) {
        String checked = Objects.requireNonNull(value, field).strip();
        if (checked.isEmpty()
                || checked.length() > 4096
                || checked.indexOf('\0') >= 0
                || checked.indexOf('\n') >= 0
                || checked.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(field);
        }
        return checked;
    }

    private static String sha(String value, String field) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(field);
        }
        return value;
    }
}
