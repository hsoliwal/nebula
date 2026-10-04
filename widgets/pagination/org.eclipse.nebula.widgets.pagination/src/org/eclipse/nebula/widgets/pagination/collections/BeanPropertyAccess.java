/*******************************************************************************
 * Copyright (C) 2011 Angelo Zerr <angelo.zerr@gmail.com>, Pascal Leclercq <pascal.leclercq@gmail.com>
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Angelo ZERR - initial API and implementation
 *     Pascal Leclercq - initial API and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.pagination.collections;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Owns reflective access and the existing getter-exception translation policy. */
final class BeanPropertyAccess {
	private BeanPropertyAccess() {
	}

	static Object read(Object source, PropertyDescriptor descriptor) {
		try {
			return readableMethod(descriptor).invoke(source, (Object[]) null);
		} catch (InvocationTargetException e) {
			// Preserve the original wrapping of exceptions thrown by the getter.
			throw new RuntimeException(e.getCause());
		} catch (Exception e) {
			return null;
		}
	}

	@SuppressWarnings("deprecation")
	private static Method readableMethod(PropertyDescriptor descriptor) {
		Method readMethod = descriptor.getReadMethod();
		if (readMethod == null) {
			throw new IllegalArgumentException(descriptor.getName()
					+ " property does not have a read method."); //$NON-NLS-1$
		}
		// Intentionally preserve the accessibility-override policy during decomposition.
		if (!readMethod.isAccessible()) {
			readMethod.setAccessible(true);
		}
		return readMethod;
	}
}
