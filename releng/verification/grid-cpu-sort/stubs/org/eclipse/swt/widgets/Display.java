/* SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 */
package org.eclipse.swt.widgets;
import java.util.ArrayDeque;
import java.util.Queue;
import org.eclipse.swt.SWTException;
public final class Display {
    private final Thread owner = Thread.currentThread();
    private final Queue<Runnable> pending = new ArrayDeque<>();
    private volatile boolean disposed;
    public Thread getThread() { return owner; }
    public synchronized void asyncExec(Runnable task) {
        if (disposed) throw new SWTException(45);
        pending.add(task);
    }
    public boolean readAndDispatch() {
        if (Thread.currentThread() != owner) throw new AssertionError("UI dispatch on worker");
        final Runnable next;
        synchronized (this) { next = pending.poll(); }
        if (next == null) return false;
        next.run();
        return true;
    }
    public void dispose() { disposed = true; }
}
