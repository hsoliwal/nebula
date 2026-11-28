/****************************************************************************
* Copyright (c) 2008, 2009 Jeremy Dowdall
*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*    Jeremy Dowdall <jeremyd@aspencloud.com> - initial API and implementation
*    Thorsten Hake <mail@thorsten-hake.com> - Fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=419447
*    Hitesh Soliwal (Synexia) - tab traversal from a native control into a panel host that SWT skips
*****************************************************************************/

package org.eclipse.nebula.cwt.v;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

public class VTracker implements DisposeListener {

	private static VTracker tracker;

	static void addTopLevelPanel(VPanel panel) {
		VTracker tracker = instance();
		if(tracker.panels == null) {
			tracker.panels = new HashMap<Composite, VPanel>();
		}
		tracker.panels.put(panel.composite, panel);
		if(!tracker.listening) {
			tracker.listening = true;
			Display.getDefault().addFilter(SWT.FocusIn, tracker.filter);
			Display.getDefault().addFilter(SWT.MouseMove, tracker.filter);
			Display.getDefault().addFilter(SWT.MouseDown, tracker.filter);
			Display.getDefault().addFilter(SWT.MouseUp, tracker.filter);
			Display.getDefault().addFilter(SWT.Traverse, tracker.filter);
		}
		panel.composite.addDisposeListener(tracker);
	}
	
	public static void deactivate() {
		instance().deactivate(getActiveControl());
	}

	public static int getLastTraverse() {
		return instance().lastTraverse;
	}
	
	public static int getMouseDownButton() {
		return instance().mouseButton;
	}
	
	public static Point getMouseDownLocation() {
		return instance().mouseDown;
	}
	
	public static boolean isFocusControl(Control control) {
		VControl focusControl = instance().focusControl;
		return (focusControl != null) && focusControl.isSameWidgetAs(control);
	}

	public static boolean isMouseDown() {
		return instance().mouseDown != null;
	}
	
	public static VControl getFocusControl() {
		return instance().focusControl;
	}
	
	public static VControl getActiveControl() {
		return instance().activeControl;
	}

	private static boolean setFocusFromPrev(Control control) {
		Control c = null;
		Composite parent = control.getParent();
		if(parent == null) {
			c = control;
		} else {
			Control[] ca = parent.getTabList();
			for(int i = 0; i < ca.length; i++) {
				if(ca[i] == control) {
					if(i == ca.length-1) {
						c = ca[0];
					} else {
						c = ca[i+1];
					}
					break;
				}
			}
		}
		if(c != null) {
			c.setFocus();
		}
		return false;
	}
	
	private static boolean setFocusFromNext(Control control) {
		Control c = null;
		Composite parent = control.getParent();
		if(parent == null) {
			c = control;
		} else {
			Control[] ca = parent.getTabList();
			for(int i = 0; i < ca.length; i++) {
				if(ca[i] == control) {
					if(i == 0) {
						c = ca[ca.length-1];
					} else {
						c = ca[i-1];
					}
					break;
				}
			}
		}
		if(c != null) {
			c.setFocus();
		}
		return false;
	}
	
	private static void setFocusToNext(Composite comp) {
		if(comp != null) {
			Composite parent = comp.getParent();
			Control[] controls = parent.getTabList();
			if(parent instanceof Shell) {
				for(int i = 0; i < controls.length; i++) {
					if(controls[i] == comp) {
						for(int j = 0; j < controls.length; j++) {
							i++;
							if(i > controls.length-1) {
								i = 0;
							}
							if(controls[i].forceFocus()) {
								return;
							}
						}
					}
				}
			} else {
				for(int i = 0; i < controls.length; i++) {
					if(controls[i] == comp) {
						for( ; i < controls.length-1; i++) {
							if(controls[i+1].forceFocus()) {
								return;
							}
						}
						setFocusToNext(comp.getParent());
					}
				}
			}
		}
	}
	
	private static void setFocusToPrev(Composite comp) {
		if(comp != null) {
			Composite parent = comp.getParent();
			Control[] controls = parent.getTabList();
			if(parent instanceof Shell) {
				for(int i = 0; i < controls.length; i++) {
					if(controls[i] == comp) {
						for(int j = 0; j < controls.length; j++) {
							i--;
							if(i < 0) {
								i = controls.length-1;
							}
							if(controls[i].forceFocus()) {
								return;
							}
						}
					}
				}
			} else {
				for(int i = 0; i < controls.length; i++) {
					if(controls[i] == comp) {
						for(int j=i ; j > 0; j--) {
							if(controls[j-1].forceFocus()) {
								return;
							}
						}
						setFocusToPrev(comp.getParent());
					}
				}
			}
		}
	}

	private static Boolean lock = new Boolean(true);
	static VTracker instance() {
		if(tracker == null) {
			synchronized (lock) {
				if(tracker == null) {
					tracker = new VTracker();
				}
			}
		}
		return tracker;
	}

	private Map<Composite, VPanel> panels;
	
	private VControl activeControl = null;
	
	private Listener filter = new Listener() {
		public void handleEvent(Event event) {
			switch (event.type){
			case SWT.Traverse:
				lastTraverse = event.detail;
				if(SWT.TRAVERSE_TAB_NEXT == event.detail || SWT.TRAVERSE_TAB_PREVIOUS == event.detail) {
					if(focusControl != null) {
						event.doit = true;
						focusControl.handleEvent(event);
						if(event.doit) {
							Composite comp = focusControl.getWidget();
							if(SWT.TRAVERSE_TAB_NEXT == event.detail) {
								setFocusToNext(comp);
							} else {
								setFocusToPrev(comp);
							}
						}
					} else if(event.doit && event.widget instanceof Control) {
						traverseIntoPanel((Control) event.widget, event);
					}
				}
				break;
			case SWT.FocusIn:
				setFocusControl(getVControl(event.widget));
				break;
			case SWT.MouseDown:
				mouseButton = event.button;
				mouseDown = new Point(event.x, event.y);
				if(activeControl != null && activeControl.setState(VControl.STATE_MOUSE_DOWN, true)) {
					activeControl.redraw();
				}
				break;
			case SWT.MouseMove:
				if(panels.containsKey(event.widget)) {
					VControl vcontrol = panels.get(event.widget).getControl(event.x, event.y, true);
					if(vcontrol != activeControl && (vcontrol == null || vcontrol.isEnabled())) {
						activate(vcontrol);
					}
				} else if(activeControl != null && event.widget != activeControl.getControl()) {
					activeControl.deactivate();
					activeControl = null;
				}
				break;
			case SWT.MouseUp:
				mouseButton = -1;
				mouseDown = null;
				if(activeControl != null && activeControl.setState(VControl.STATE_MOUSE_DOWN, false)) {
					activeControl.redraw();
				}
				break;
			}
		}
	};

	public static VControl getVControl(Widget widget) {
		if(widget instanceof Shell) {
			Control[] ca = ((Shell) widget).getTabList();
			if(ca.length > 0) {
				widget = ca[0];
			}
		}
		Object o = widget.getData("cwt_vcontrol");
		if(o instanceof VControl) {
			return (VControl) o;
		}
		return null;
	}
	
	private VControl focusControl = null;

	private boolean listening = false;
	
	private int mouseButton = -1;
	
	private Point mouseDown = null;
	
	private int lastTraverse = -1;
	
	private VTracker() {
		// singleton
	}

	void activate(VControl vcontrol) {
		if(activeControl != null && !activeControl.isDisposed()) {
			activeControl.deactivate();
		}
		activeControl = vcontrol;
		if(activeControl != null) {
			activeControl.activate();
		}
	}
	
	void deactivate(VControl vcontrol) {
		if(vcontrol != null) {
			if(!vcontrol.isDisposed()) {
				vcontrol.deactivate();
			}
			if(activeControl == vcontrol) {
				activeControl = null;
			}
		}
	}

	/**
	 * Tab traversal from a native control into an adjacent top-level panel host.
	 * <p>
	 * SWT gives a Canvas the focus on keyboard traversal only when it hooks key events
	 * (Composite.setTabGroupFocus), and the virtual controls of a top-level panel attach their key
	 * listeners to its composite only while they have the focus. So a host such as {@link VCanvas},
	 * with no native children and no key listener of its own, is skipped by SWT's traversal and its
	 * virtual controls cannot be reached with the keyboard from a native control. When the tab-order
	 * neighbour of the native control is such a host with a focusable virtual control, give that
	 * control the focus and end the traversal here (TRAVERSE_NONE, doit), so that the focus moves
	 * once. Hosts that SWT already traverses (native children, key listeners, NO_FOCUS, hidden or
	 * disabled) and neighbours outside the parent (other than the wrap in a shell) are left to SWT.
	 * </p>
	 *
	 * @param from the native control the traversal starts on
	 * @param event the Traverse event, TRAVERSE_TAB_NEXT or TRAVERSE_TAB_PREVIOUS with doit set
	 * @return true if the focus moved into a panel host
	 */
	private boolean traverseIntoPanel(Control from, Event event) {
		Composite parent = from.getParent();
		if(panels == null || parent == null) {
			return false;
		}
		Control[] tabList = parent.getTabList();
		int index = -1;
		for(int i = 0; i < tabList.length; i++) {
			if(tabList[i] == from) {
				index = i;
				break;
			}
		}
		if(index < 0) {
			return false;
		}
		int target = (SWT.TRAVERSE_TAB_NEXT == event.detail) ? index + 1 : index - 1;
		if(target < 0 || target >= tabList.length) {
			if(!(parent instanceof Shell)) {
				return false; // SWT continues in the parent's tab order
			}
			target = (target < 0) ? tabList.length - 1 : 0; // a shell's tab order wraps
		}
		Control host = tabList[target];
		if(host == from || !isSkippedPanelHost(host)) {
			return false;
		}
		VPanel panel = panels.get(host);
		if(getNewFocus(panel) == null || !setFocusControl(panel)) {
			return false;
		}
		event.detail = SWT.TRAVERSE_NONE;
		event.doit = true;
		return true;
	}

	/* a top-level panel host that SWT's own traversal does not give the focus (see traverseIntoPanel) */
	private boolean isSkippedPanelHost(Control control) {
		return control instanceof Composite
				&& panels.containsKey(control)
				&& !control.isDisposed()
				&& control.isVisible()
				&& control.isEnabled()
				&& (control.getStyle() & SWT.NO_FOCUS) == 0
				&& !control.isListening(SWT.KeyDown)
				&& !control.isListening(SWT.KeyUp)
				&& ((Composite) control).getChildren().length == 0;
	}

	private VControl getNewFocus(VPanel panel) {
		for(VControl child : panel.getChildren()) {
			if(!child.hasStyle(SWT.NO_FOCUS)) {
				if(child instanceof VPanel) {
					VControl newFocus = getNewFocus((VPanel) child);
					if(newFocus != null) {
						return newFocus;
					}
				} else {
					return child;
				}
			}
		}
		return null;
	}
	
	boolean setFocusControl(VControl control) {
		VControl newFocus = control;
		if(newFocus instanceof VPanel) {
			newFocus = getNewFocus((VPanel) newFocus);
		} else if(control != null && control.hasStyle(SWT.NO_FOCUS)) {
			return false;
		}
		
		if(newFocus == focusControl) {
			if(newFocus != null && !newFocus.isDisposed()) {
				return newFocus.getControl().forceFocus();
			}
			return false;
		}

		try {
			Display.getDefault().removeFilter(SWT.FocusIn, filter);
			
			VControl oldFocus = focusControl;
			if(oldFocus != null && !oldFocus.isDisposed()) {
				oldFocus.setFocus(false);
			}
			if(newFocus != null) {
				if(!newFocus.isDisposed() && newFocus.setFocus(true)) {
					if (!newFocus.getControl().forceFocus()) {
						return false;
					}
				} else {
					return false;
				}
			}
			focusControl = newFocus;
			if(newFocus != null) {
				newFocus.redraw();
			}
			if(oldFocus != null && !oldFocus.isDisposed()) {
				oldFocus.redraw();
			}
			notifyWidgetFocusListeners(focusControl, oldFocus);
			if(newFocus != null) {
				return true;
			}
			return false;
		} finally {
			Display.getDefault().addFilter(SWT.FocusIn, filter);
		}
	}

	private void notifyWidgetFocusListeners(VControl newFocus, VControl oldFocus) {
		if(newFocus != null && !newFocus.isSameWidgetAs(oldFocus)) {
			Widget widget = newFocus.getWidget();
			if(widget.getData("cwt_focus") == null) { //$NON-NLS-1$
				widget.setData("cwt_focus", this); //$NON-NLS-1$
				Event event = new Event();
				event.widget = widget;
				event.data = this;
				event.type = SWT.FocusIn;
				event.widget.notifyListeners(SWT.FocusIn, event);
			}
		}
		if((newFocus == null || !(newFocus instanceof VButton) || ((VButton)newFocus).getParent().getData("PickerPart") == null) //$NON-NLS-1$
				&& (oldFocus != null && !oldFocus.isSameWidgetAs(newFocus))) {
			Widget widget = oldFocus.getWidget();
			if(widget.getData("cwt_focus") != null) { //$NON-NLS-1$
				widget.setData("cwt_focus", null); //$NON-NLS-1$
				Event event = new Event();
				event.widget = widget;
				event.data = this;
				event.type = SWT.FocusOut;
				event.widget.notifyListeners(SWT.FocusOut, event);
			}
		}
	}
	
	public void widgetDisposed(DisposeEvent e) {
		VTracker tracker = instance();
		if(tracker.panels != null && tracker.panels.containsKey(e.widget)) {
			tracker.panels.remove(e.widget);
			if(tracker.panels.isEmpty()) {
				Display.getDefault().removeFilter(SWT.FocusIn, tracker.filter);
				Display.getDefault().removeFilter(SWT.MouseMove, tracker.filter);
				Display.getDefault().removeFilter(SWT.MouseDown, tracker.filter);
				Display.getDefault().removeFilter(SWT.MouseUp, tracker.filter);
				Display.getDefault().removeFilter(SWT.Traverse, tracker.filter);
				tracker.listening = false;
				if(activeControl != null) {
					Control control = activeControl.getControl();
					if(control != null && !control.isDisposed()) {
						control.dispose();
					}
				}
				tracker.panels = null;
			}
		}
	}

}
