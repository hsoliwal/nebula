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

/**
 * Traverses a bean property path and composes lookup and read atoms.
 * Extracted from BeanUtils by M3NebulaBeanAtoms; do not edit independently of its recipe.
 */
final class PropertyPathAtom {

	private PropertyPathAtom() { }

	static Object getValue(Object source, String property) {
		if (property == null) {
			return source;
		}
		if (property.indexOf('.') == -1) {
			if (source == null) {
				return null;
			}
			PropertyDescriptor propertyDescriptor = PropertyDescriptorAtom.getPropertyDescriptor(
					source.getClass(), property);
			return PropertyReadAtom.getValue(source, propertyDescriptor);
		}

		String[] properies = property.split("[.]");
		for (int i = 0; i < properies.length; i++) {
			source = PropertyPathAtom.getValue(source, properies[i]);
		}
		return source;
	}
}
