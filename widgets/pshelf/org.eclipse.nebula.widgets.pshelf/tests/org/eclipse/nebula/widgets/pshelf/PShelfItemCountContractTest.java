// SPDX-License-Identifier: EPL-2.0
// Headless differential model: mirrors exactly the old/new PShelf owner boundary.
package org.eclipse.nebula.widgets.pshelf;

import java.util.Random;

/** Java21 proof of byte-for-byte logical predicate and subclass virtual dispatch. */
public final class PShelfItemCountContractTest {
    private static long checks;

    private static class Shelf {
        final Object[] items;
        boolean disposed;
        int getterCalls;
        int countCalls;
        int widgetChecks;

        Shelf(int count) {
            items = new Object[count];
            for (int i = 0; i < count; i++) items[i] = Integer.valueOf(i);
        }

        void checkWidget() {
            widgetChecks++;
            if (disposed) throw new IllegalStateException("disposed original shelf");
        }

        Object[] getItems() {
            getterCalls++;
            checkWidget();
            return items.clone();
        }

        int itemCountForInsertion() {
            countCalls++;
            checkWidget();
            return items.length;
        }

        boolean originalFirst(Object item) { return getItems()[0] == item; }

        boolean updatedFirst(Object item) {
            if (getClass() != Shelf.class || items.length == 0) {
                return getItems()[0] == item;
            }
            checkWidget();
            return items[0] == item;
        }

        boolean originalReject(int index) {
            return index < 0 || index > getItems().length;
        }

        boolean updatedReject(int index) {
            return index < 0 || index > (getClass() == Shelf.class
                    ? itemCountForInsertion() : getItems().length);
        }
    }

    /** An actual overriding getItems method may return an alternative snapshot. */
    private static final class OverriddenShelf extends Shelf {
        int customCalls;
        OverriddenShelf(int count) { super(count); }
        @Override Object[] getItems() {
            customCalls++;
            checkWidget();
            if (items.length == 0) return items;
            Object[] copy = items.clone();
            Object swap = copy[0];
            copy[0] = copy[copy.length - 1];
            copy[copy.length - 1] = swap;
            return copy;
        }
    }

    private record Outcome(Object value, String error, int getter, int counter,
                           int checks, int overrides) {}

    private static Outcome outcome(Shelf shelf, boolean insertion, int index, Object selected,
            boolean candidate) {
        try {
            Object value = insertion
                    ? candidate ? shelf.updatedReject(index) : shelf.originalReject(index)
                    : candidate ? shelf.updatedFirst(selected) : shelf.originalFirst(selected);
            return new Outcome(value, null, shelf.getterCalls, shelf.countCalls,
                    shelf.widgetChecks,
                    shelf instanceof OverriddenShelf sub ? sub.customCalls : 0);
        } catch (RuntimeException failure) {
            return new Outcome(null, failure.getClass().getName() + ":" + failure.getMessage(),
                    shelf.getterCalls, shelf.countCalls, shelf.widgetChecks,
                    shelf instanceof OverriddenShelf sub ? sub.customCalls : 0);
        }
    }

    private static void check(boolean ok, String reason) {
        checks++;
        if (!ok) throw new AssertionError(reason + " at check " + checks);
    }

    private static void compare(int count, int index, boolean subclass, boolean disposed,
            boolean insertion, int selectedIndex) {
        Shelf old = subclass ? new OverriddenShelf(count) : new Shelf(count);
        Shelf current = subclass ? new OverriddenShelf(count) : new Shelf(count);
        old.disposed = current.disposed = disposed;
        Object oldSelection = selectedIndex < 0 || selectedIndex >= count
                ? null : old.items[selectedIndex];
        Object newSelection = selectedIndex < 0 || selectedIndex >= count
                ? null : current.items[selectedIndex];
        Outcome a = outcome(old, insertion, index, oldSelection, false);
        Outcome b = outcome(current, insertion, index, newSelection, true);
        check(a.value == b.value || a.value != null && a.value.equals(b.value),
                "predicate equality for count=" + count + " index=" + index);
        check(java.util.Objects.equals(a.error, b.error),
                "exception family/message equality for count=" + count + " index=" + index);
        if (subclass) {
            check(a.overrides == b.overrides, "overridden virtual getItems call count");
            check(a.checks() == b.checks(), "subclass widget guard and callback order");
            check(b.counter == 0, "fast private path may not bypass a subclass");
        } else {
            check(a.error != null || a.getter >= b.getter,
                    "exact owner must not call public copying getter more often");
            if (insertion && index >= 0) {
                check(b.getter == 0 && b.counter == 1,
                        "exact owner insertion uses only private count");
            } else if (insertion && index < 0) {
                check(b.getter == 0 && b.counter == 0,
                        "negative index short circuit cannot call provider");
            } else if (!insertion && count > 0 && !disposed) {
                check(b.getter == 0 && b.counter == 0,
                        "exact owner first-item check must not allocate snapshot");
            }
        }
    }

    public static void main(String[] args) {
        for (int count = 0; count <= 40; count++) {
            for (int index = -4; index <= count + 4; index++) {
                for (boolean subclass : new boolean[] {false, true}) {
                    for (boolean disposed : new boolean[] {false, true}) {
                        compare(count, index, subclass, disposed, true, index);
                        compare(count, index, subclass, disposed, false, index);
                    }
                }
            }
        }
        Random random = new Random(0x4d33505348454c46L);
        for (int trial = 0; trial < 60_000; trial++) {
            int count = random.nextInt(300);
            int index = random.nextInt(count + 12) - 6;
            boolean subclass = (trial & 1) != 0;
            boolean disposed = trial % 97 == 0;
            boolean insertion = (trial & 2) == 0;
            compare(count, index, subclass, disposed, insertion, random.nextInt(count + 2) - 1);
        }
        System.out.println("PSHELF_ITEM_OWNER_CONTRACT_PASS checks=" + checks);
    }
}
