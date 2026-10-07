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
		if (property == null) {
			return source;
		}
		if (property.indexOf('.') == -1) {
			return getSimpleValue(source, property);
		}
		return getNestedValue(source, property);
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
		try {
			return getReadMethod(propertyDescriptor).invoke(source, (Object[])null);
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
		if (!beanClass.isInterface()) {
			BeanInfo beanInfo;
			try {
				beanInfo = Introspector.getBeanInfo(beanClass);
			} catch (IntrospectionException e) {
				// cannot introspect, give up
				return null;
			}
			PropertyDescriptor descriptor = findPropertyDescriptor(
					beanInfo.getPropertyDescriptors(), propertyName);
			if (descriptor != null) {
				return descriptor;
			}
		} else {
			try {
				PropertyDescriptor descriptor = findPropertyDescriptor(
						getInterfaceDescriptors(beanClass), propertyName);
				if (descriptor != null) {
					return descriptor;
				}
			} catch (IntrospectionException e) {
				// cannot introspect, give up
				return null;
			}
		}
		throw new IllegalArgumentException(
				"Could not find property with name " + propertyName + " in class " + beanClass); //$NON-NLS-1$ //$NON-NLS-2$
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
		BeanInfo beanInfo = Introspector.getBeanInfo(iface);
		PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
		for (int i = 0; i < pds.length; i++) {
			PropertyDescriptor pd = pds[i];
			propertyDescriptors.add(pd);
		}
		Class<?>[] subIntfs = iface.getInterfaces();
		for (int j = 0; j < subIntfs.length; j++) {
			getInterfacePropertyDescriptors(propertyDescriptors, subIntfs[j]);
		}
	}

	private static Object getSimpleValue(Object source, String property) {
		if (source == null) {
			return null;
		}
		PropertyDescriptor descriptor = getPropertyDescriptor(source.getClass(), property);
		return getValue(source, descriptor);
	}

	private static Object getNestedValue(Object source, String property) {
		String[] properties = property.split("[.]");
		for (int i = 0; i < properties.length; i++) {
			source = getValue(source, properties[i]);
		}
		return source;
	}

	private static Method getReadMethod(PropertyDescriptor descriptor) {
		Method readMethod = descriptor.getReadMethod();
		if (readMethod == null) {
			throw new IllegalArgumentException(descriptor.getName()
					+ " property does not have a read method."); //$NON-NLS-1$
		}
		if (!readMethod.isAccessible()) {
			readMethod.setAccessible(true);
		}
		return readMethod;
	}

	private static PropertyDescriptor[] getInterfaceDescriptors(Class<? extends Object> iface)
			throws IntrospectionException {
		List<PropertyDescriptor> descriptors = new ArrayList<>();
		getInterfacePropertyDescriptors(descriptors, iface);
		return descriptors.toArray(new PropertyDescriptor[descriptors.size()]);
	}

	private static PropertyDescriptor findPropertyDescriptor(PropertyDescriptor[] descriptors,
			String propertyName) {
		for (int i = 0; i < descriptors.length; i++) {
			PropertyDescriptor descriptor = descriptors[i];
			if (descriptor.getName().equals(propertyName)) {
				return descriptor;
			}
		}
		return null;
	}

}
