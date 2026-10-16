// SPDX-License-Identifier: Apache-2.0
package org.eclipse.nebula.widgets.grid.model;

import org.junit.Test;

/** Native toolkit suite discovery wrapper; the same contract runs standalone. */
public class SparseMirrorBoundary_Test {
    @Test public void sparseStateContract() { SparseMirrorContract.verify(); }
}
