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
import java.util.List;

/**
 * Collects interface descriptors in the original recursive order.
 * Extracted from BeanUtils by M3NebulaBeanAtoms; do not edit independently of its recipe.
 */
final class InterfacePropertiesAtom {

	private InterfacePropertiesAtom() { }

	static void getInterfacePropertyDescriptors(
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
			InterfacePropertiesAtom.getInterfacePropertyDescriptors(propertyDescriptors, subIntfs[j]);
		}
	}
}
