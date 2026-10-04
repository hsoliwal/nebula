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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilities to retrieves values of POJO with the property name.
 *
 */
public class BeanUtils {

	/**
	 * Returns the value of the given property for the given bean.
	 *
	 * @param source
	 *            the source bean
	 * @param property
	 *            the property name to retrieve
	 * @return the value of the given property for the given bean.
	 */
	public static Object getValue(Object source, String property) {
		return PropertyPathAtom.getValue(source, property);
	}

	/**
	 * Returns the value of the given property for the given bean.
	 *
	 * @param source
	 *            the source bean
	 * @param propertyDescriptor
	 *            the property to retrieve
	 * @return the contents of the given property for the given bean.
	 */
	private static Object getValue(Object source,
			PropertyDescriptor propertyDescriptor) {
		return PropertyReadAtom.getValue(source, propertyDescriptor);
	}

	/**
	 * Returns the property descriptor of the given bean class and the given
	 * property.
	 *
	 * @param beanClass
	 * @param propertyName
	 * @return the PropertyDescriptor for the named property on the given bean
	 *         class
	 */
	private static PropertyDescriptor getPropertyDescriptor(Class<? extends Object> beanClass,
			String propertyName) {
		return PropertyDescriptorAtom.getPropertyDescriptor(beanClass, propertyName);
	}

	/**
	 * Goes recursively into the interface and gets all defined
	 * propertyDescriptors
	 *
	 * @param propertyDescriptors
	 *            The result list of all PropertyDescriptors the given interface
	 *            defines (hierarchical)
	 * @param iface
	 *            The interface to fetch the PropertyDescriptors
	 * @throws IntrospectionException
	 */
	private static void getInterfacePropertyDescriptors(
			List<PropertyDescriptor> propertyDescriptors, Class<? extends Object> iface)
			throws IntrospectionException {
		InterfacePropertiesAtom.getInterfacePropertyDescriptors(propertyDescriptors, iface);
	}

}
