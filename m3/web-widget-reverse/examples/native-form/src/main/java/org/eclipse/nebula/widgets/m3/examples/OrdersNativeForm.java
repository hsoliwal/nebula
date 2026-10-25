// SPDX-License-Identifier: Apache-2.0
// Exact static-four-control candidate. Native/RWT qualification remains HOLD.
package org.eclipse.nebula.widgets.m3.examples;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/** Static Orders.jsx form, not a React runtime or data-grid viewport. */
public final class OrdersNativeForm {
    private OrdersNativeForm() {
    }

    public static Composite create(Composite parent) {
        Composite form = new Composite(parent, SWT.NONE);
        form.setLayout(new GridLayout(1, false));
        Label title = new Label(form, SWT.NONE);
        title.setText("Orders & shipping");
        Label description = new Label(form, SWT.NONE);
        description.setText("A browser & native viewport candidate");
        Button refresh = new Button(form, SWT.PUSH);
        refresh.setText("Refresh orders");
        Button cancel = new Button(form, SWT.PUSH);
        cancel.setText("Cancel");
        cancel.setEnabled(false);
        return form;
    }
}
