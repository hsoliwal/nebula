// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package org.eclipse.nebula.m3.web;

import java.util.Objects;

/**
 * Compile-time admission for typed declarations from Java web widget toolkits.
 *
 * <p>This is not an HTML/JavaScript parser, an SWT Control, a renderer, or a
 * runtime widget adapter. Framework-specific parsers must first provide a
 * source-bound, attributed declaration and a complete effect mask. A candidate
 * still needs the existing Synexia UiBinder compiler, SWT/Nebula ownership
 * validation and native/runtime proof before it becomes executable code.</p>
 *
 * <p>The fixed table is ordered by framework ordinal and exact Java/DOM source
 * name, and uses allocation-free binary search over that ordered array. It is
 * deliberately small: unsupported types are held, never guessed.</p>
 */
public final class WebWidgetAdmission {
    public enum Framework {
        RAP_RWT, GWT, JWT, APACHE_WICKET, APACHE_TAPESTRY,
        SPRING_THYMELEAF, VAADIN_FLOW
    }

    public enum Status {
        STATIC_SHAPE_CANDIDATE,
        HOLD_UNSUPPORTED_SOURCE,
        HOLD_RUNTIME_EFFECTS,
        HOLD_REQUIRED_SEMANTICS,
        HOLD_INTRINSIC_VIEWPORT
    }

    /** Effect bits are supplied by an attributed source inventory, not inferred from a class name. */
    public static final int DATA_BINDING = 1;
    public static final int EVENT_CALLBACK = 1 << 1;
    public static final int BROWSER_API = 1 << 2;
    public static final int SESSION_STATE = 1 << 3;
    public static final int STYLE_OR_LAYOUT = 1 << 4;
    public static final int VIRTUAL_ITEMS = 1 << 5;
    public static final int ACCESSIBILITY = 1 << 6;

    /** A candidate is metadata, not evidence of equivalent SWT/Nebula behavior. */
    public record Decision(Status status, String swtReceiver, String uiBinderElement,
                           String reason) {
        public Decision {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(reason, "reason");
        }

        public boolean needsQualification() {
            return true; // Even admitted shapes never authorize runtime materialization alone.
        }
    }

    private record Rule(Framework framework, String sourceType, String receiver,
                        String uiBinderElement, boolean intrinsicViewport,
                        boolean sourceDefaultHasForeignSemantics) {
        private Rule(Framework framework, String sourceType, String receiver,
                     String uiBinderElement, boolean intrinsicViewport) {
            this(framework, sourceType, receiver, uiBinderElement, intrinsicViewport, false);
        }
    }

    // Exact source symbols: reference names only; no third-party implementation is copied.
    // Keep each framework's subrange lexicographically sorted for binary search.
    private static final Rule[] RULES = {
        new Rule(Framework.RAP_RWT, "org.eclipse.swt.widgets.Button", "org.eclipse.swt.widgets.Button", "g:Button", false),
        new Rule(Framework.RAP_RWT, "org.eclipse.swt.widgets.Label", "org.eclipse.swt.widgets.Label", "g:Label", false),
        new Rule(Framework.RAP_RWT, "org.eclipse.swt.widgets.Table", "org.eclipse.swt.widgets.Table", "", true),

        new Rule(Framework.GWT, "com.google.gwt.user.cellview.client.CellTable", "org.eclipse.nebula.widgets.grid.Grid", "", true),
        new Rule(Framework.GWT, "com.google.gwt.user.client.ui.Button", "org.eclipse.swt.widgets.Button", "g:Button", false),
        new Rule(Framework.GWT, "com.google.gwt.user.client.ui.Label", "org.eclipse.swt.widgets.Label", "g:Label", false),

        new Rule(Framework.JWT, "eu.webtoolkit.jwt.WPushButton", "org.eclipse.swt.widgets.Button", "g:Button", false),
        new Rule(Framework.JWT, "eu.webtoolkit.jwt.WTable", "org.eclipse.nebula.widgets.grid.Grid", "", true),
        new Rule(Framework.JWT, "eu.webtoolkit.jwt.WText", "org.eclipse.swt.widgets.Label", "g:Label", false),

        new Rule(Framework.APACHE_WICKET, "org.apache.wicket.markup.html.basic.Label", "org.eclipse.swt.widgets.Label", "g:Label", false),
        new Rule(Framework.APACHE_WICKET, "org.apache.wicket.markup.html.form.Button", "org.eclipse.swt.widgets.Button", "", false, true),
        new Rule(Framework.APACHE_WICKET, "org.apache.wicket.markup.html.list.ListView", "org.eclipse.nebula.widgets.grid.Grid", "", true),

        new Rule(Framework.APACHE_TAPESTRY, "org.apache.tapestry5.corelib.components.Label", "org.eclipse.swt.widgets.Label", "", false, true),
        new Rule(Framework.APACHE_TAPESTRY, "org.apache.tapestry5.corelib.components.Loop", "org.eclipse.nebula.widgets.grid.Grid", "", true),
        new Rule(Framework.APACHE_TAPESTRY, "org.apache.tapestry5.corelib.components.Submit", "org.eclipse.swt.widgets.Button", "", false, true),

        new Rule(Framework.SPRING_THYMELEAF, "html:button", "org.eclipse.swt.widgets.Button", "g:Button", false),
        new Rule(Framework.SPRING_THYMELEAF, "html:label", "org.eclipse.swt.widgets.Label", "g:Label", false),

        new Rule(Framework.VAADIN_FLOW, "com.vaadin.flow.component.button.Button", "org.eclipse.swt.widgets.Button", "g:Button", false),
        new Rule(Framework.VAADIN_FLOW, "com.vaadin.flow.component.grid.Grid", "org.eclipse.nebula.widgets.grid.Grid", "", true),
        new Rule(Framework.VAADIN_FLOW, "com.vaadin.flow.component.html.Span", "org.eclipse.swt.widgets.Label", "g:Label", false)
    };

    static {
        for (int i = 1; i < RULES.length; i++) {
            if (compare(RULES[i - 1], RULES[i].framework(), RULES[i].sourceType()) >= 0) {
                throw new ExceptionInInitializerError("non-canonical or duplicate widget map");
            }
        }
    }

    private WebWidgetAdmission() { }

    public static Decision classify(Framework framework, String sourceType, int effectMask) {
        Objects.requireNonNull(framework, "framework");
        Objects.requireNonNull(sourceType, "sourceType");
        if (sourceType.isBlank() || sourceType.length() > 256) {
            throw new IllegalArgumentException("invalid source widget type");
        }
        Rule rule = find(framework, sourceType);
        if (rule == null) {
            return new Decision(Status.HOLD_UNSUPPORTED_SOURCE, "", "",
                    "No exact source-symbol mapping; inventory the actual AST/component owner");
        }
        if (rule.intrinsicViewport()) {
            return new Decision(Status.HOLD_INTRINSIC_VIEWPORT, rule.receiver(), "",
                    "Collection geometry, items, events, selection and viewport require original owner proof");
        }
        if (rule.sourceDefaultHasForeignSemantics()) {
            return new Decision(Status.HOLD_REQUIRED_SEMANTICS, rule.receiver(), "",
                    "Form submission or field association is intrinsic to this source component");
        }
        if (effectMask != 0) {
            return new Decision(Status.HOLD_RUNTIME_EFFECTS, rule.receiver(), rule.uiBinderElement(),
                    "Dynamic binding, events, browser/session effects, accessibility or unrecognized signal bits");
        }
        return new Decision(Status.STATIC_SHAPE_CANDIDATE, rule.receiver(), rule.uiBinderElement(),
                "Only literal declaration shape; source parsing, Java emission and SWT execution unqualified");
    }

    private static Rule find(Framework framework, String sourceType) {
        int low = 0;
        int high = RULES.length - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            Rule candidate = RULES[mid];
            int comparison = compare(candidate, framework, sourceType);
            if (comparison == 0) return candidate;
            if (comparison < 0) low = mid + 1;
            else high = mid - 1;
        }
        return null;
    }

    private static int compare(Rule rule, Framework framework, String sourceType) {
        int compared = Integer.compare(rule.framework().ordinal(), framework.ordinal());
        return compared != 0 ? compared : rule.sourceType().compareTo(sourceType);
    }
}
