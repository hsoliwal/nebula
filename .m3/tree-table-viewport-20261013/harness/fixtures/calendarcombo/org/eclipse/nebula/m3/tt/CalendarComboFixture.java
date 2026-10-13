/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.m3.tt;

import java.lang.reflect.Member;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import org.eclipse.nebula.widgets.calendarcombo.CalendarCombo;
import org.eclipse.nebula.widgets.calendarcombo.CustomCombo;
import org.eclipse.nebula.widgets.calendarcombo.DefaultColorManager;
import org.eclipse.nebula.widgets.calendarcombo.DefaultSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/** A CalendarCombo and a flat CalendarCombo, both set to 2023-11-14 (UTC). */
public final class CalendarComboFixture implements TtFixture {
	private CalendarCombo combo;
	private CalendarCombo flat;
	private CustomCombo custom;
	private Calendar date;

	@Override
	public void build(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout(1, false));
		date = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
		date.setTimeInMillis(1_700_000_000_000L);
		combo = new CalendarCombo(c, SWT.READ_ONLY);
		combo.setDate(date);
		flat = new CalendarCombo(c, SWT.READ_ONLY | SWT.FLAT);
		flat.setDate(date);
		custom = new CustomCombo(c, SWT.BORDER);
		custom.setItems(new String[] { "a", "b", "c" });
		custom.select(1);
	}

	@Override
	public Object instanceOf(Class<?> type) {
		if (combo == null || combo.isDisposed()) {
			return null;
		}
		if (type == Object.class) {
			return "probe";
		}
		Object[] candidates = { combo, custom, date, new DefaultSettings(), new DefaultColorManager() };
		for (Object c : candidates) {
			if (c != null && type.isInstance(c)) {
				return c;
			}
		}
		return null;
	}

	@Override
	public String idOf(Object o) {
		if (o == combo) {
			return "combo";
		}
		if (o == flat) {
			return "flat";
		}
		if (o == custom) {
			return "custom";
		}
		return null;
	}

	@Override
	public Object primary() {
		return combo;
	}

	@Override
	public String skipReason(Member member) {
		// the calendar pop-up runs a nested event loop on the active shell; it is probed through the bench instead
		return "showCalendar".equals(member.getName()) || "openCalendar".equals(member.getName())
				? "POPUP_EVENT_LOOP" : null;
	}
}
