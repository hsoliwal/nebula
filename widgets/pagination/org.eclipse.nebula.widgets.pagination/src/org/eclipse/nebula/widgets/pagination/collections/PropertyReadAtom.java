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

/**
 * Invokes a bean read method with the original accessibility and exception behavior.
 * Extracted from BeanUtils by M3NebulaBeanAtoms; do not edit independently of its recipe.
 */
final class PropertyReadAtom {

	private PropertyReadAtom() { }

	static Object getValue(Object source,
			PropertyDescriptor propertyDescriptor) {
		try {
			Method readMethod = propertyDescriptor.getReadMethod();
			if (readMethod == null) {
				throw new IllegalArgumentException(propertyDescriptor.getName()
						+ " property does not have a read method."); //$NON-NLS-1$
			}
			if (!readMethod.isAccessible()) {
				readMethod.setAccessible(true);
			}
			return readMethod.invoke(source, (Object[])null);
		} catch (InvocationTargetException e) {
			/*
			 * InvocationTargetException wraps any exception thrown by the
			 * invoked method.
			 */
			throw new RuntimeException(e.getCause());
		} catch (Exception e) {
			return null;
		}
	}
}
