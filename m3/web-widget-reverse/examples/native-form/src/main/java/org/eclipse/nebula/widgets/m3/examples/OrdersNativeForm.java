// SPDX-License-Identifier: Apache-2.0
// Generated from admitted typed UiBinder rows; native/browser parity unqualified.
package org.eclipse.nebula.widgets.m3.examples;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/** Static admitted widget form; no DOM/React runtime or nested viewport. */
public final class OrdersNativeForm {
    private OrdersNativeForm() {
    }

    public static Composite create(Composite parent) {
        Composite form = new Composite(parent, SWT.NONE);
        form.setLayout(new GridLayout(1, false));
        form.setData("m3.title", "UiBinder screen");
        Label item0 = new Label(form, SWT.NONE);
        item0.setData("m3.id", "title");
        item0.setText("Orders & shipping");
        Label item1 = new Label(form, SWT.NONE);
        item1.setData("m3.id", "description");
        item1.setText("A browser & native viewport candidate");
        Button item2 = new Button(form, SWT.PUSH);
        item2.setData("m3.id", "refresh");
        item2.setText("Refresh orders");
        Button item3 = new Button(form, SWT.PUSH);
        item3.setData("m3.id", "cancel");
        item3.setText("Cancel");
        item3.setEnabled(false);
        return form;
    }
}
