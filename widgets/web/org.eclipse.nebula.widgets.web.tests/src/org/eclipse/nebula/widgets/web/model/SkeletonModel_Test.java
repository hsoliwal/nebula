/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
// Generated fromtemplates/nebula/Test.tmpl; edit the template and regenerate.
package org.eclipse.nebula.widgets.web.model;

import org.junit.Test;

/** Headless model contract of the webskeleton (family ANIMVALUE, pattern ANIMVALUE-1). */
public class SkeletonModel_Test {
	@Test
	public void sampleContract() {
		WebChecks.core(SkeletonModel.sample(), SkeletonApi.SAMPLE, "progressbar");
	}

	@Test
	public void familyContract() {
		AnimvalueChecks.run(SkeletonModel.sample());
	}
}
