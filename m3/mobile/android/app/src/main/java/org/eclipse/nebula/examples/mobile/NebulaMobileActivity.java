// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.examples.mobile;

import org.eclipse.swt.android.SWTActivity;
import org.eclipse.swt.widgets.Shell;

/** Hosts unchanged Nebula widget sources on the Android SWT backend. */
public final class NebulaMobileActivity extends SWTActivity {
    volatile NebulaMobileContents.State state;
    volatile Throwable failure;

    @Override protected void createContents(Shell shell) {
        state = NebulaMobileContents.create(shell);
    }

    @Override protected void onSWTFailure(Throwable error) { failure = error; }
}
