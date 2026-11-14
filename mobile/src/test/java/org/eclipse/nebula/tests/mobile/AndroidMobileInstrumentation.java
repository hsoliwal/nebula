/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.tests.mobile;

import android.app.Activity;
import android.app.Instrumentation;
import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.nebula.widgets.roundedcheckbox.RoundedCheckbox;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.internal.android.SwtActivity;
import org.eclipse.swt.internal.android.AndroidPlatform;
import org.eclipse.swt.internal.mobile.MobilePlatform;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

/**
 * Executed on Android, with real window input and SWT GC(Display)/PixelCopy.
 * The target is the unchanged Nebula widget; no fake mobile widget or renderer
 * is substituted. This test does not establish the unimplemented widget families.
 */
public final class AndroidMobileInstrumentation extends Instrumentation {
	private int checks;
	private SwtActivity activity;
	private Display display;
	private RoundedCheckbox checkbox;
	private final AtomicInteger selections = new AtomicInteger();
	private final AtomicBoolean veto = new AtomicBoolean();
	private final AtomicReference<Boolean> beforeSelection = new AtomicReference<>();

	@Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
	@Override public void onStart() {
		Bundle result = new Bundle();
		try {
			runContracts();
			runTransportCleanupContracts();
			result.putString("status", "PASS"); result.putInt("checks", checks);
			result.putString("scope", "Android original SWT Canvas/Shell and unchanged Nebula RoundedCheckbox");
			finish(Activity.RESULT_OK, result);
		} catch (Throwable failure) {
			StringWriter text = new StringWriter(); failure.printStackTrace(new PrintWriter(text));
			result.putString("status", "FAIL"); result.putInt("checks", checks); result.putString("failure", text.toString());
			finish(Activity.RESULT_CANCELED, result);
		}
	}

	private void runContracts() throws Exception {
		Intent intent = new Intent(Intent.ACTION_MAIN).setClassName(getTargetContext(), SwtActivity.class.getName()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		activity = (SwtActivity) startActivitySync(intent);
		await(() -> { Thread owner = activity.applicationThread(); display = owner == null ? null : Display.findDisplay(owner); return display != null; }, "Display launch");
		await(() -> {
			display.syncExec(() -> { for (var shell : display.getShells()) { RoundedCheckbox found = find(shell); if (found != null) checkbox = found; } });
			return checkbox != null;
		}, "Original Nebula widget creation");
		display.syncExec(() -> {
			checkbox.setSelection(false);
			checkbox.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
				selections.incrementAndGet(); beforeSelection.set(checkbox.getSelection());
				if (veto.get()) event.doit = false;
			}));
		});
		check(selections.get() == 0, "Programmatic selection does not emit Selection");
		await(this::windowReady,"Visible Android window has input focus");
		display.syncExec(checkbox::update);
		waitForIdleSync();
		int[] before = null;
		for(int attempt=0;attempt<40;attempt++){
			try {
				before=capture("unselected.png");
				if(Arrays.stream(before).distinct().limit(8).count()>=3)break;
			} catch(AssertionError failure) {
				if(!(failure.getCause() instanceof AndroidPlatform.ScreenCaptureException nativeFailure)
					|| nativeFailure.resultCode!=android.view.PixelCopy.ERROR_SOURCE_NO_DATA)throw failure;
			}
			SystemClock.sleep(50);
		}
		check(before!=null && Arrays.stream(before).distinct().limit(8).count() >= 3, "Actual screen capture contains painted widget geometry");
		tap(false);
		await(() -> selections.get() == 1, "Native touch Selection");
		check(Boolean.FALSE.equals(beforeSelection.get()), "Selection listener observes original pre-toggle state");
		check(selected(), "Native touch toggles original Nebula widget");
		int[] after = null;
		for (int attempt = 0; attempt < 40; attempt++) {
			after = capture("selected.png");
			if (!Arrays.equals(before, after)) break;
			SystemClock.sleep(50);
		}
		check(!Arrays.equals(before, after), "Selection changes actual screen pixels");
		veto.set(true); tap(false); await(() -> selections.get() == 2, "Vetoed native touch callback");
		check(selected(), "SelectionEvent.doit=false cancels original toggle");
		veto.set(false);
		sendKeyDownUpSync(KeyEvent.KEYCODE_SPACE); await(() -> !selected(), "Physical Space toggles on original KeyUp listener");
		// Original RoundedCheckbox keyboard handling calls setSelection directly.
		check(selections.get() == 2, "Original keyboard toggle does not emit an added Selection event");
		tap(true); SystemClock.sleep(150); display.syncExec(() -> {});
		check(selections.get() == 2, "Cancelled pointer does not activate widget");
		tap(false); await(() -> selections.get() == 3, "Fresh pointer after cancellation");
		check(selected(), "Pointer state recovers after cancellation");
		AtomicBoolean accepted = new AtomicBoolean();
		runOnMainSync(() -> { View focus = activity.getWindow().getDecorView().findFocus(); accepted.set(focus != null && focus.performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, null)); });
		check(accepted.get(), "Native accessibility activation is accepted");
		await(() -> selections.get() == 4, "Native accessibility Selection");
		check(!selected(), "Accessibility runs original cancellable widget action");

		Display preservedDisplay = display; RoundedCheckbox preservedWidget = checkbox;
		ActivityMonitor monitor = addMonitor(SwtActivity.class.getName(), null, false);
		runOnMainSync(() -> activity.recreate());
		Activity replacement = waitForMonitorWithTimeout(monitor, 15000); removeMonitor(monitor);
		check(replacement instanceof SwtActivity, "Activity recreated");
		activity = (SwtActivity) replacement;
		await(() -> Display.findDisplay(activity.applicationThread()) == preservedDisplay, "Display preserved across recreation");
		await(this::windowReady,"Recreated Android window has input focus and layout");
		check(display == preservedDisplay && checkbox == preservedWidget && !checkbox.isDisposed(), "SWT owners survive native Activity recreation");
		check(!selected(), "Selection survives Activity recreation");
		tap(false); await(() -> selections.get() == 5, "Input after Activity recreation");
		check(selected(), "Recreated Android peer updates same original widget");
		twoPointers(); await(() -> selections.get() == 6,"Primary pointer lifted before second pointer");
		check(!selected(),"Primary pointer activates once while remaining pointer release does not activate");
		tap(false); await(() -> selections.get() == 7,"Fresh tap after primary multi-pointer release");
		check(selected(),"Pointer tracking resets after multi-pointer sequence");
		capture("recreated.png");
		AtomicInteger closeRequests = new AtomicInteger();
		display.syncExec(() -> checkbox.getShell().addListener(SWT.Close, event -> { closeRequests.incrementAndGet(); event.doit = false; }));
		check(getUiAutomation().performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK),"System Back action accepted");
		await(() -> closeRequests.get() == 1,"System Back reaches vetoable SWT Shell.Close");
		check(!display.isDisposed() && !activity.isFinishing(),"Shell.Close veto preserves Activity after system Back");
		runOnMainSync(() -> activity.finish());
		await(() -> activity.applicationFinished(), "Non-vetoable OS destruction finishes SWT thread");
		check(display.isDisposed() && checkbox.isDisposed(), "Final Activity destruction disposes original SWT owners");
		check(activity.applicationFailure() == null, "No hidden application failure");
	}

	private RoundedCheckbox find(Composite parent) {
		for (Control child : parent.getChildren()) {
			if (child instanceof RoundedCheckbox box) return box;
			if (child instanceof Composite composite) { RoundedCheckbox found = find(composite); if (found != null) return found; }
		}
		return null;
	}
	/** Isolated native host for transport lifetime regressions after the SWT app exits. */
	public static final class TransportActivity extends Activity {
		@Override protected void onCreate(Bundle state) { super.onCreate(state); }
	}
	private void runTransportCleanupContracts() throws Exception {
		Intent intent = new Intent().setClassName(getTargetContext(), TransportActivity.class.getName()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		TransportActivity host = (TransportActivity) startActivitySync(intent);
		AtomicReference<AndroidPlatform> reference = new AtomicReference<>();
		runOnMainSync(() -> reference.set(new AndroidPlatform(host)));
		AndroidPlatform platform = reference.get();
		AtomicInteger resizeCallbacks = new AtomicInteger();
		AtomicInteger closeCallbacks = new AtomicInteger();
		AtomicInteger terminationCallbacks = new AtomicInteger();
		MobilePlatform.EventSink receiver = new MobilePlatform.EventSink() {
			@Override public void input(long peer,int type,int x,int y,int button,int keyCode,char character,int stateMask,int count,int time) {}
			@Override public void resize(long peer,int width,int height) { resizeCallbacks.incrementAndGet(); }
			@Override public void paint(long peer) {}
			@Override public void close(long peer) { closeCallbacks.incrementAndGet(); }
			@Override public void lifecycle(boolean active) {}
			@Override public void terminate() { terminationCallbacks.incrementAndGet(); }
			@Override public void accessibility(long peer,int action) {}
		};
		boolean connected = false;
		try {
			platform.connect(receiver); connected = true;
			long first = platform.createControl(MobilePlatform.SHELL,0,SWT.NONE);
			long parent = platform.createControl(MobilePlatform.SHELL,0,SWT.NONE);
			platform.setBounds(parent,0,0,40,40);
			platform.disposeControl(first);
			long child = platform.createControl(MobilePlatform.CANVAS,parent,SWT.NONE);
			long image = platform.createImage(4,4,null);
			AtomicBoolean staleCallback = new AtomicBoolean();
			AtomicBoolean staleKeyOrAccessibility = new AtomicBoolean();
			runOnMainSync(() -> {
				ViewGroup content = host.findViewById(android.R.id.content);
				ViewGroup oldRoot = (ViewGroup) content.getChildAt(0);
				View oldPeer = oldRoot.getChildAt(0);
				View oldChild = ((ViewGroup)oldPeer).getChildAt(0);
				platform.bind(host);
				int before = resizeCallbacks.get();
				oldPeer.layout(0,0,43,47);
				staleCallback.set(resizeCallbacks.get() != before);
				int closes=closeCallbacks.get();
				oldChild.onKeyUp(KeyEvent.KEYCODE_BACK,new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK));
				boolean activated=oldChild.performAccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK,null);
				staleKeyOrAccessibility.set(closeCallbacks.get()!=closes || activated);
			});
			check(!staleCallback.get(),"Detached Activity peer cannot publish resize under replacement handle");
			check(!staleKeyOrAccessibility.get(),"Detached child cannot close replacement Shell or accept accessibility activation");
			// Child reused the lower registry slot; native ancestry, not slot order, controls cleanup.
			platform.disconnect(); connected = false;
			AtomicBoolean empty = new AtomicBoolean();
			runOnMainSync(() -> { ViewGroup content=host.findViewById(android.R.id.content); empty.set(((ViewGroup)content.getChildAt(0)).getChildCount()==0); });
			check(empty.get(),"Disconnect releases native child before parent after handle-slot reuse");
			platform.connect(receiver); connected = true;
			boolean staleControl=false, staleImage=false;
			try { platform.setVisible(child,true); } catch(IllegalStateException expected) { staleControl=true; }
			try { platform.imagePixels(image); } catch(IllegalStateException expected) { staleImage=true; }
			check(staleControl && staleImage,"Disconnected native control/image handles remain invalid after reconnect");
			platform.disconnect(); connected = false;
			runOnMainSync(platform::closeActivity);
			platform.connect(receiver); connected = true;
			check(terminationCallbacks.get()==1,"Host destruction before Display connection is retained");
		} finally {
			try { if(connected) platform.disconnect(); } finally { runOnMainSync(host::finish); }
		}
	}
	private boolean selected() { AtomicBoolean value = new AtomicBoolean(); display.syncExec(() -> value.set(checkbox.getSelection())); return value.get(); }
	private boolean windowReady() {
		AtomicBoolean ready=new AtomicBoolean();
		runOnMainSync(() -> {
			ViewGroup content=activity.findViewById(android.R.id.content);
			View root=content.getChildAt(0);
			ready.set(activity.getWindow().getDecorView().hasWindowFocus() && root!=null && root.isShown()
				&& root.getWidth()>root.getPaddingLeft()+root.getPaddingRight()
				&& root.getHeight()>root.getPaddingTop()+root.getPaddingBottom());
		});
		return ready.get();
	}
	private int[] screenLocation() {
		int[] location = new int[2];
		display.syncExec(() -> { Point origin = checkbox.toDisplay(0,0); Point size = checkbox.getSize(); location[0] = origin.x + size.x / 2; location[1] = origin.y + size.y / 2; });
		runOnMainSync(() -> {
			ViewGroup content = activity.findViewById(android.R.id.content); View root = content.getChildAt(0);
			int[] origin = new int[2]; root.getLocationOnScreen(origin);
			float density = activity.getResources().getDisplayMetrics().density;
			location[0] = origin[0] + root.getPaddingLeft() + Math.round(location[0] * density);
			location[1] = origin[1] + root.getPaddingTop() + Math.round(location[1] * density);
		});
		return location;
	}
	private void tap(boolean cancel) {
		int[] location = screenLocation();
		long time = SystemClock.uptimeMillis();
		MotionEvent down = MotionEvent.obtain(time,time,MotionEvent.ACTION_DOWN,location[0],location[1],0);
		MotionEvent up = MotionEvent.obtain(time,time+40,cancel?MotionEvent.ACTION_CANCEL:MotionEvent.ACTION_UP,location[0],location[1],0);
		try { sendPointerSync(down); sendPointerSync(up); } finally { down.recycle(); up.recycle(); }
	}
	private void twoPointers() {
		int[] location=screenLocation(); long time=SystemClock.uptimeMillis();
		MotionEvent.PointerProperties first=new MotionEvent.PointerProperties(),second=new MotionEvent.PointerProperties();
		first.id=0;first.toolType=MotionEvent.TOOL_TYPE_FINGER;second.id=1;second.toolType=MotionEvent.TOOL_TYPE_FINGER;
		MotionEvent.PointerCoords a=new MotionEvent.PointerCoords(),b=new MotionEvent.PointerCoords();
		a.x=location[0];a.y=location[1];a.pressure=1;a.size=1;b.copyFrom(a);b.x+=5;
		MotionEvent.PointerProperties[][] properties={{first},{first,second},{first,second},{second}};
		MotionEvent.PointerCoords[][] coordinates={{a},{a,b},{a,b},{b}};
		int[] actions={MotionEvent.ACTION_DOWN,MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),MotionEvent.ACTION_POINTER_UP,MotionEvent.ACTION_UP};
		for(int i=0;i<actions.length;i++){
			MotionEvent event=MotionEvent.obtain(time,time+i*20,actions[i],properties[i].length,properties[i],coordinates[i],0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);
			try{sendPointerSync(event);}finally{event.recycle();}
		}
	}
	private int[] capture(String file) throws Exception {
		AtomicReference<int[]> pixels = new AtomicReference<>(); int[] extent = new int[2];
		AtomicReference<Throwable> failure = new AtomicReference<>();
		display.syncExec(() -> {
			Point origin = checkbox.toDisplay(0,0); Point size = checkbox.getSize();
			extent[0] = size.x; extent[1] = size.y;
			Image image = new Image(display,size.x,size.y); GC screen = new GC(display);
			try {
				screen.copyArea(image,origin.x,origin.y);
				ImageData data = image.getImageData(); int[] argb = new int[Math.multiplyExact(data.width,data.height)];
				for(int y=0;y<data.height;y++)for(int x=0;x<data.width;x++){RGB rgb=data.palette.getRGB(data.getPixel(x,y));argb[y*data.width+x]=(data.getAlpha(x,y)<<24)|(rgb.red<<16)|(rgb.green<<8)|rgb.blue;}
				pixels.set(argb);
			} catch(Throwable problem){failure.set(problem);} finally {screen.dispose();image.dispose();}
		});
		if(failure.get()!=null)throw new AssertionError("Actual SWT screen capture failed",failure.get());
		File directory = new File(getTargetContext().getFilesDir(),"mobile-proof");
		if(!directory.isDirectory()&&!directory.mkdirs())throw new IllegalStateException("Cannot create screenshot directory");
		Bitmap bitmap=Bitmap.createBitmap(pixels.get(),extent[0],extent[1],Bitmap.Config.ARGB_8888);
		try(FileOutputStream stream=new FileOutputStream(new File(directory,file))){if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,stream))throw new IllegalStateException("PNG encoding failed");}finally{bitmap.recycle();}
		return pixels.get();
	}
	private interface Condition { boolean ready() throws Exception; }
	private void await(Condition condition,String operation) throws Exception {
		long deadline=SystemClock.uptimeMillis()+15000;
		while(SystemClock.uptimeMillis()<deadline){if(activity!=null&&activity.applicationFailure()!=null)throw new AssertionError(operation,activity.applicationFailure());if(condition.ready()){checks++;return;}SystemClock.sleep(25);}
		throw new AssertionError("Timed out: "+operation);
	}
	private void check(boolean condition,String message){if(!condition)throw new AssertionError(message);checks++;}
}
