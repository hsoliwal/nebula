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

/**
 * Facade for simple and nested JavaBean property access.
 * Descriptor discovery and reflective invocation have separate package-private owners.
 */
public class BeanUtils {

	/**
	 * Returns the property value, or the source itself when property is null.
	 *
	 * @param source the source bean
	 * @param property the property name or dot-separated path
	 * @return the property value under the existing BeanUtils contract
	 */
	public static Object getValue(Object source, String property) {
		if (property == null) {
			return source;
		}
		if (property.indexOf('.') == -1) {
			return readProperty(source, property);
		}
		return readPath(source, property);
	}

	private static Object readProperty(Object source, String property) {
		if (source == null) {
			return null;
		}
		return BeanPropertyAccess.read(source,
				BeanPropertyLookup.find(source.getClass(), property));
	}

	private static Object readPath(Object source, String property) {
		// Preserve split's leading/interior-empty and trailing-empty behavior exactly.
		String[] properties = property.split("[.]");
		for (int i = 0; i < properties.length; i++) {
			source = getValue(source, properties[i]);
		}
		return source;
	}
}
