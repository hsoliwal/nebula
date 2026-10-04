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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves a property descriptor and delegates interface traversal.
 * Extracted from BeanUtils by M3NebulaBeanAtoms; do not edit independently of its recipe.
 */
final class PropertyDescriptorAtom {

	private PropertyDescriptorAtom() { }

	static PropertyDescriptor getPropertyDescriptor(Class<? extends Object> beanClass,
			String propertyName) {
		if (!beanClass.isInterface()) {
			BeanInfo beanInfo;
			try {
				beanInfo = Introspector.getBeanInfo(beanClass);
			} catch (IntrospectionException e) {
				// cannot introspect, give up
				return null;
			}
			PropertyDescriptor[] propertyDescriptors = beanInfo
					.getPropertyDescriptors();
			for (int i = 0; i < propertyDescriptors.length; i++) {
				PropertyDescriptor descriptor = propertyDescriptors[i];
				if (descriptor.getName().equals(propertyName)) {
					return descriptor;
				}
			}
		} else {
			try {
				PropertyDescriptor propertyDescriptors[];
				List<PropertyDescriptor> pds = new ArrayList<>();
				InterfacePropertiesAtom.getInterfacePropertyDescriptors(pds, beanClass);
				if (pds.size() > 0) {
					propertyDescriptors = pds
							.toArray(new PropertyDescriptor[pds.size()]);
					PropertyDescriptor descriptor;
					for (int i = 0; i < propertyDescriptors.length; i++) {
						descriptor = propertyDescriptors[i];
						if (descriptor.getName().equals(propertyName))
							return descriptor;
					}
				}
			} catch (IntrospectionException e) {
				// cannot introspect, give up
				return null;
			}
		}
		throw new IllegalArgumentException(
				"Could not find property with name " + propertyName + " in class " + beanClass); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
