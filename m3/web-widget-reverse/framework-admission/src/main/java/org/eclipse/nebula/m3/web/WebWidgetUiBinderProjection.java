// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package org.eclipse.nebula.m3.web;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.SourceVersion;

/**
 * Projects explicitly inventoried, literal Java-web-widget declarations into
 * the existing Synexia static GWT UiBinder grammar.
 *
 * <p>This class neither parses donor source nor establishes that an inventory
 * has captured all donor effects. The caller must provide an attributed AST
 * inventory and an independently pinned source digest. The original
 * UiBinderInventory/UiBinderStaticWidgetCompiler still decide whether the
 * resulting XML can become Java; the original SWT/Nebula widgets own runtime
 * state, geometry, selection, event ordering, painting and disposal.</p>
 */
public final class WebWidgetUiBinderProjection {
    private static final int MAX_SOURCE_BYTES = 1_048_576;
    private static final int MAX_ROWS = 256;
    private static final int MAX_TEXT_UNITS = 4_096;
    private static final String HEADER = "<ui:UiBinder xmlns:ui=\"urn:ui:com.google.gwt.uibinder\""
            + " xmlns:g=\"urn:import:com.google.gwt.user.client.ui\"><g:VerticalPanel>";
    private static final String FOOTER = "</g:VerticalPanel></ui:UiBinder>";

    private WebWidgetUiBinderProjection() { }

    /** Source receipt is a claim that must be checked against actual UTF-8 bytes. */
    public record SourceReceipt(String relativePath, String sha256) {
        public SourceReceipt {
            Objects.requireNonNull(relativePath, "relativePath");
            Objects.requireNonNull(sha256, "sha256");
            if (relativePath.isEmpty() || relativePath.length() > 256
                    || !relativePath.matches("[A-Za-z0-9_.-]+(?:/[A-Za-z0-9_.-]+)*")
                    || relativePath.equals(".") || relativePath.equals("..")
                    || relativePath.contains("/../") || relativePath.contains("/./")
                    || relativePath.startsWith("../") || relativePath.startsWith("./")
                    || relativePath.endsWith("/..") || relativePath.endsWith("/.")) {
                throw new IllegalArgumentException("SOURCE_RELATIVE_PATH_REQUIRED");
            }
            requireSha256(sha256);
        }
    }

    /** One source-frontend declaration, not a new widget or an SWT event model. */
    public record Declaration(WebWidgetAdmission.Framework framework,
                              String attributedSourceType, String field,
                              String literalText, boolean enabled, int effectMask) {
        public Declaration {
            Objects.requireNonNull(framework, "framework");
            Objects.requireNonNull(attributedSourceType, "attributedSourceType");
            Objects.requireNonNull(field, "field");
            Objects.requireNonNull(literalText, "literalText");
        }
    }

    /** Immutable candidate proof; never sufficient to authorize SWT runtime promotion. */
    public record Projection(String sourceSha256, String xmlSha256,
                             String uiBinderXml, int rowCount) {
        public Projection {
            requireSha256(sourceSha256);
            requireSha256(xmlSha256);
            Objects.requireNonNull(uiBinderXml, "uiBinderXml");
            if (rowCount < 1 || rowCount > MAX_ROWS) {
                throw new IllegalArgumentException("ROW_COUNT_OUT_OF_RANGE");
            }
        }

        public boolean runtimeQualificationRequired() { return true; }
    }

    /**
     * Compile-time candidate projection: verify exact raw-source digest, then
     * emit the byte-stable existing UiBinder static vertical-panel grammar.
     * No source script, template expression or callback is executed.
     */
    public static Projection project(SourceReceipt receipt, byte[] sourceUtf8,
                                     List<Declaration> declarations) {
        Objects.requireNonNull(receipt, "receipt");
        Objects.requireNonNull(sourceUtf8, "sourceUtf8");
        Objects.requireNonNull(declarations, "declarations");
        if (sourceUtf8.length == 0 || sourceUtf8.length > MAX_SOURCE_BYTES) {
            throw new IllegalArgumentException("SOURCE_BYTE_BUDGET");
        }
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(sourceUtf8));
        } catch (CharacterCodingException invalid) {
            throw new IllegalArgumentException("SOURCE_INVALID_UTF8", invalid);
        }
        String actualDigest = sha256(sourceUtf8);
        if (!MessageDigest.isEqual(actualDigest.getBytes(StandardCharsets.US_ASCII),
                receipt.sha256().getBytes(StandardCharsets.US_ASCII))) {
            throw new IllegalArgumentException("SOURCE_DIGEST_DRIFT");
        }
        List<Declaration> rows = List.copyOf(declarations);
        if (rows.isEmpty() || rows.size() > MAX_ROWS) {
            throw new IllegalArgumentException("ROW_BUDGET");
        }
        StringBuilder xml = new StringBuilder(HEADER);
        Set<String> fields = new HashSet<>();
        for (Declaration row : rows) {
            WebWidgetAdmission.Decision decision = WebWidgetAdmission.classify(
                    row.framework(), row.attributedSourceType(), row.effectMask());
            if (decision.status() != WebWidgetAdmission.Status.STATIC_SHAPE_CANDIDATE) {
                throw new IllegalArgumentException("SOURCE_DECLARATION_NOT_ADMITTED_" + decision.status());
            }
            if (!row.field().matches("[A-Za-z][A-Za-z0-9_]{0,63}")
                    || !SourceVersion.isIdentifier(row.field())
                    || SourceVersion.isKeyword(row.field())) {
                throw new IllegalArgumentException("INVALID_UI_FIELD");
            }
            if (!fields.add(row.field())) {
                throw new IllegalArgumentException("DUPLICATE_UI_FIELD");
            }
            validateLiteral(row.literalText());
            String tag = decision.uiBinderElement();
            if (!"g:Label".equals(tag) && !"g:Button".equals(tag)) {
                throw new IllegalArgumentException("NO_STATIC_UIBINDER_RECEIVER");
            }
            if ("g:Label".equals(tag) && !row.enabled()) {
                throw new IllegalArgumentException("LABEL_DISABLED_NOT_REPRESENTABLE");
            }
            xml.append('<').append(tag).append(" ui:field=\"")
                    .append(row.field()).append("\" text=\"");
            appendXmlText(xml, row.literalText());
            xml.append('"');
            if ("g:Button".equals(tag)) {
                xml.append(" enabled=\"").append(row.enabled()).append('"');
            }
            xml.append("/>");
        }
        xml.append(FOOTER);
        String output = xml.toString();
        return new Projection(actualDigest, sha256(output.getBytes(StandardCharsets.UTF_8)),
                output, rows.size());
    }

    private static void validateLiteral(String literal) {
        if (literal.isBlank() || literal.length() > MAX_TEXT_UNITS
                || !literal.equals(literal.strip()) || literal.contains("  ")) {
            throw new IllegalArgumentException("LITERAL_TEXT_BUDGET_OR_WHITESPACE");
        }
        for (int i = 0; i < literal.length();) {
            int cp = literal.codePointAt(i);
            if (cp < 0x20 || cp == 0x7f || cp == '{' || cp == '}'
                    || cp == 0xfffe || cp == 0xffff
                    || cp >= 0xd800 && cp <= 0xdfff) {
                throw new IllegalArgumentException("LITERAL_NOT_XML_SCALAR");
            }
            // codePointAt preserves unmatched UTF-16 surrogates as surrogate code points.
            i += Character.charCount(cp);
        }
    }

    private static void appendXmlText(StringBuilder into, String text) {
        for (int i = 0; i < text.length(); i++) {
            switch (text.charAt(i)) {
                case '&' -> into.append("&amp;");
                case '<' -> into.append("&lt;");
                case '>' -> into.append("&gt;");
                case '"' -> into.append("&quot;");
                case '\'' -> into.append("&apos;");
                default -> into.append(text.charAt(i));
            }
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException unavailable) {
            throw new ExceptionInInitializerError(unavailable);
        }
    }

    private static void requireSha256(String digest) {
        if (!digest.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("LOWERCASE_SOURCE_SHA256_REQUIRED");
        }
    }
}
