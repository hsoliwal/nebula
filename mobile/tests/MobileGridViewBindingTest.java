/* SPDX-License-Identifier: EPL-2.0 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.swt.internal.mobile.MobileWidgetRuntime;
import org.eclipse.swt.internal.mobile.MobileWidgetRuntime.*;
import org.eclipse.nebula.widgets.grid.internal.mobile.MobileGridViewBinding;

public final class MobileGridViewBindingTest {
	static final class Fake implements Host {
		EventSink events;
		final Map<Long,String> titles = new HashMap<>();
		final Map<Long,Bounds> geometry = new HashMap<>();
		final ArrayList<Long> children = new ArrayList<>();
		@Override public boolean isUiThread() { return true; }
		@Override public void post(Runnable runnable) { runnable.run(); }
		@Override public void attach(EventSink sink) { events=sink; }
		@Override public void create(long id,long parent,Kind kind) { children.add(id); }
		@Override public void setBounds(long id, Bounds bounds) { geometry.put(id,bounds); }
		@Override public void setText(long id,String text) { titles.put(id,text); }
		@Override public void setEnabled(long id,boolean value) { }
		@Override public void setVisible(long id,boolean value) { }
		@Override public void setAccessibilityLabel(long id,String label) { }
		@Override public void remove(long id) { titles.remove(id);geometry.remove(id);children.remove(id); }
	}
	static void check(boolean c,String message) { if (!c) throw new AssertionError(message); }
	public static void main(String[] args) {
		Fake host = new Fake();
		MobileWidgetRuntime runtime = new MobileWidgetRuntime(host);
		MobileWidgetRuntime.Node shell = runtime.createShell();
		ArrayList<Long> selected = new ArrayList<>();
		MobileGridViewBinding presentation = new MobileGridViewBinding(shell,1_000_000_000_000L,2,24,2,
				(row,col) -> "cell("+row+","+col+")",selected::add);
		presentation.show(0,320,120);
		check(host.children.size() <= 1 + 1 + 10, "native nodes bounded");
		MobileWidgetRuntime.Node button = presentation.nativeControl().children().get(0);
		check(host.geometry.get(button.id()).y() == 0, "initial row origin");
		presentation.show(11, 320, 120);
		check(host.geometry.get(button.id()).y() == -11,
				"fractional-row scroll repositions unchanged native row");
		host.events.accept(new Event(button.id(),EventType.SELECT,null,0,0));
		check(selected.size()==1 && presentation.projection().selectedRow()==selected.get(0), "native row selection");
		presentation.show(500_000_000_000L*24L,320,120);
		check(host.children.size() <= 1+1+10, "far-scroll reuses native rows");
		check(presentation.show(Long.MAX_VALUE, 320, 120).visibleStart() == 999_999_999_999L,
				"overscroll clamps safely to the last logical row");
		presentation.close();
		check(host.children.size()==1,"disposal owns all native row views");
		runtime.close();
		check(host.children.isEmpty(),"runtime fully disposes");
		System.out.println("MobileGridViewBindingTest PASSED");
	}
}
