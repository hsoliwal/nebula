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

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;

/** Descriptor lookup with class/interface discovery strategies and one shared scan. */
final class BeanPropertyLookup {
	private BeanPropertyLookup() {
	}

	static PropertyDescriptor find(Class<?> beanClass, String propertyName) {
		PropertyDescriptor[] descriptors;
		try {
			descriptors = Discovery.forType(beanClass).descriptors(beanClass);
		} catch (IntrospectionException e) {
			return null;
		}
		PropertyDescriptor descriptor = findNamed(descriptors, propertyName);
		if (descriptor != null) {
			return descriptor;
		}
		throw new IllegalArgumentException(
				"Could not find property with name " + propertyName + " in class " + beanClass); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static PropertyDescriptor findNamed(PropertyDescriptor[] descriptors, String name) {
		for (int i = 0; i < descriptors.length; i++) {
			PropertyDescriptor descriptor = descriptors[i];
			if (descriptor.getName().equals(name)) {
				return descriptor;
			}
		}
		return null;
	}

	private enum Discovery {
		CLASS {
			@Override
			PropertyDescriptor[] descriptors(Class<?> type) throws IntrospectionException {
				return Introspector.getBeanInfo(type).getPropertyDescriptors();
			}
		},
		INTERFACE {
			@Override
			PropertyDescriptor[] descriptors(Class<?> type) throws IntrospectionException {
				List<PropertyDescriptor> descriptors = new ArrayList<>();
				collectInterface(descriptors, type);
				return descriptors.toArray(new PropertyDescriptor[descriptors.size()]);
			}
		};

		abstract PropertyDescriptor[] descriptors(Class<?> type) throws IntrospectionException;

		static Discovery forType(Class<?> type) {
			return type.isInterface() ? INTERFACE : CLASS;
		}
	}

	private static void collectInterface(List<PropertyDescriptor> descriptors, Class<?> type)
			throws IntrospectionException {
		PropertyDescriptor[] own = Introspector.getBeanInfo(type).getPropertyDescriptors();
		for (int i = 0; i < own.length; i++) {
			descriptors.add(own[i]);
		}
		Class<?>[] parents = type.getInterfaces();
		for (int i = 0; i < parents.length; i++) {
			collectInterface(descriptors, parents[i]);
		}
	}
}
