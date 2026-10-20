/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.m3.rwt.examples;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.rap.rwt.application.Application;
import org.eclipse.rap.rwt.application.ApplicationConfiguration;
import org.eclipse.rap.rwt.client.WebClient;

/**
 * The RAP application of the RWT column: one entry point per example, at {@link RwtExample#path()}. Examples are
 * listed by class name in matrix rank order, so a build that leaves out a widget whose sources do not compile on
 * RWT still runs the others.
 */
public final class RwtExamples implements ApplicationConfiguration {

	private static final Logger LOG = Logger.getLogger ("org.eclipse.nebula.widgets.m3.rwt.examples");

	/** Example classes in matrix rank order (docs/m3/rwt/RWT_COLUMN.tsv). */
	static final String[] CLASSES = {
		"AutocompleteExample", "ChartExample", "GalleryExample", "TreeTableExample", "GanttTimelineExample", "HeatMapExample",
		"DatePickerExample", "TaskPaneExample", "PGroupTaskPaneExample", "PropertySheetExample", "MapViewExample", "TableComboExample",
		"BreadcrumbExample", "TabFolderRichExample", "DataGridExample", "CheckListExample", "DualListExample", "BusyIndicatorExample",
		"PromptDecorationExample", "SplitButtonExample", "HeaderPanelExample", "StackLayoutExample", "ToastExample", "PaginationExample",
		"ColorChooserExample", "RangeSliderExample", "CalendarViewExample", "HyperlinkExample", "CodeEditorExample", "StatusBarExample",
		"ToggleSwitchExample", "TreeComboExample", "FontChooserExample", "RadioGroupExample", "RatingExample", "FormattedTextExample",
		"SearchFieldExample", "CalculatorExample", "GaugeKnobExample", "SegmentedButtonExample", "VirtualListExample",
		"GroupableHeaderExample", "BadgeExample", "TokenFieldExample", "PopoverExample", "FloatingLabelExample", "WizardExample",
		"PinnedColumnsExample", "HorizontalSpinnerExample", "PasswordRevealExample", "StepperExample"};

	private static List<RwtExample> all;

	/** @return the examples whose classes are present in this build, in rank order */
	public static synchronized List<RwtExample> all () {
		if (all == null) {
			List<RwtExample> examples = new ArrayList<> ();
			for (String name : CLASSES) {
				try {
					Class<?> type = Class.forName (RwtExamples.class.getPackageName () + "." + name);
					examples.add ((RwtExample) type.getDeclaredConstructor ().newInstance ());
				} catch (ClassNotFoundException absent) {
					LOG.log (Level.FINE, "example {0} is not in this build", name);
				} catch (ReflectiveOperationException | LinkageError failure) {
					LOG.log (Level.WARNING, "example " + name + " cannot be created", failure);
				}
			}
			all = Collections.unmodifiableList (examples);
		}
		return all;
	}

	@Override
	public void configure (Application application) {
		for (RwtExample example : all ()) {
			application.addEntryPoint (example.path (), () -> new RwtExampleEntryPoint (example), Map.of (WebClient.PAGE_TITLE, example.concept));
		}
		LOG.info (() -> "RWT examples: " + all ().size () + " entry points");
	}
}
