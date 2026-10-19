// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.widgets.tiles;
import java.util.*;

/** Model of the exact exposed-list contract and private hit-grid generation. */
public final class TilesMutationEpochProof {
    private static long checks, indexed, fallbacks;
    record Tile(int x, int y, int width, int height, int id) {}
    static final class Frame {
        private static final class RenderedTiles extends ArrayList<Tile> {
            private static final long serialVersionUID = 1L;
            private long contentEpoch;
            int structuralEpoch() { return modCount; }
            long contentEpoch() { return contentEpoch; }
            @Override public Tile set(int index, Tile tile) {
                Tile old = super.set(index, tile);
                contentEpoch++;
                return old;
            }
            @Override public ListIterator<Tile> listIterator() {
                ListIterator<Tile> it = super.listIterator();
                contentEpoch++;
                return it;
            }
            @Override public ListIterator<Tile> listIterator(int index) {
                ListIterator<Tile> it = super.listIterator(index);
                contentEpoch++;
                return it;
            }
            @Override public List<Tile> subList(int from, int to) {
                List<Tile> slice = super.subList(from, to);
                contentEpoch++;
                return slice;
            }
            @Override public void replaceAll(java.util.function.UnaryOperator<Tile> operator) {
                contentEpoch++; // before user callback observes a partially rebuilt list
                super.replaceAll(operator);
            }
            @Override public void sort(Comparator<? super Tile> order) {
                contentEpoch++; // comparator may re-enter original hit callbacks
                super.sort(order);
            }
            @Override public boolean removeIf(java.util.function.Predicate<? super Tile> filter) {
                contentEpoch++; // predicate callbacks may observe the current viewport
                return super.removeIf(filter);
            }
            @Override public boolean removeAll(Collection<?> elements) {
                contentEpoch++;
                return super.removeAll(elements);
            }
            @Override public boolean retainAll(Collection<?> elements) {
                contentEpoch++;
                return super.retainAll(elements);
            }
        }
        private final RenderedTiles rendered = new RenderedTiles();
        private int columns, stepX, stepY, count, structuralEpoch;
        private long contentEpoch;
        List<Tile> getTiles() { return rendered; }
        void create(int cols, int n) {
            columns = 0;
            rendered.clear();
            for (int i = 0; i < n; i++) {
                rendered.add(new Tile(5 + (i % cols) * 25, 5 + (i / cols) * 25, 20, 20, i));
            }
            // Complete original layout is validated at this point.
            stepX = 25; stepY = 25; count = n;
            structuralEpoch = rendered.structuralEpoch(); contentEpoch = rendered.contentEpoch(); columns = cols;
        }
        boolean hasIndex() { return columns != 0 && count == rendered.size()
                && structuralEpoch == rendered.structuralEpoch()
                && contentEpoch == rendered.contentEpoch(); }
        Tile oldHit(int x, int y) {
            for (Tile t : rendered) if (x >= t.x && y >= t.y
                    && x <= t.x + t.width && y <= t.y + t.height) return t;
            return null;
        }
        Tile sizeOnlyBug(int x, int y) {
            if (columns == 0 || count != rendered.size()) return oldHit(x,y);
            Tile first = rendered.get(0);
            if (x < first.x || y < first.y) return null;
            long col = ((long)x - first.x)/stepX, row = ((long)y-first.y)/stepY;
            if (col >= columns) return null;
            long slot = row * columns + col;
            if (slot >= rendered.size()) return null;
            Tile t = rendered.get((int)slot);
            return x >= t.x && y >= t.y && x <= t.x + t.width && y <= t.y + t.height ? t : null;
        }
        Tile fixedHit(int x, int y) {
            if (!hasIndex()) { fallbacks++; return oldHit(x,y); }
            indexed++;
            return sizeOnlyBug(x,y);
        }
    }
    /** The merged size-only optimization ignores same-count List.set/swap edits. */
    private static Tile oldSizeOnlyIndex(List<Tile> rendered, int columns, int stepX,
            int stepY, int x, int y) {
        Tile first = rendered.get(0);
        if (x < first.x || y < first.y) return null;
        long col = ((long) x - first.x) / stepX;
        long row = ((long) y - first.y) / stepY;
        if (col >= columns) return null;
        long slot = row * columns + col;
        if (slot >= rendered.size()) return null;
        Tile t = rendered.get((int) slot);
        return x >= t.x && y >= t.y && x <= t.x+t.width
                && y <= t.y+t.height ? t : null;
    }
    private static void same(Tile expected, Tile actual, String why) {
        checks++;
        if (expected != actual) throw new AssertionError("mismatch at "+checks+" reason="+why+
                " old="+expected+" new="+actual);
    }
    private static void condition(boolean ok, String why) {
        checks++; if (!ok) throw new AssertionError("assertion "+checks+" "+why);
    }
    public static void main(String[] args) {
        boolean quick = args.length == 1 && "--quick".equals(args[0]);
        Frame frame = new Frame();
        final int columns = 119, n = 10000;
        frame.create(columns,n);
        condition(frame.hasIndex(), "initial exact static grid admitted");
        Random random = new Random(0x4d3354494c45534cL);
        for (int i=0; i<(quick ? 500 : 40000); i++) {
            int x=random.nextInt(3100)-50,y=random.nextInt(2500)-50;
            same(frame.oldHit(x,y), frame.fixedHit(x,y), "warm hit");
        }
        // A same-size swap actually violates the previous optimization.
        Tile fifth = frame.getTiles().get(5), sixth = frame.getTiles().get(6);
        Collections.swap(frame.getTiles(), 5, 6);
        Tile old = frame.oldHit(fifth.x+2,fifth.y+2);
        Tile stale = frame.sizeOnlyBug(fifth.x+2,fifth.y+2); // candidate checks epoch first
        condition(old == fifth, "old scan preserves exposed reference identity");
        // The old size-only index would inspect position 5 and wrongly return null.
        condition(frame.getTiles().get(5) == sixth && frame.getTiles().get(6) == fifth,
                "reproduce baseline same-size mutation and incorrect direct slot");
        condition(fifth.x + 2 < sixth.x, "the stale position-5 candidate cannot hit original tile 5");
        condition(oldSizeOnlyIndex(frame.getTiles(), columns, 25, 25,
                fifth.x+2, fifth.y+2) != old,
                "reproduce the actual merged size-only index regression before fixing it");
        condition(stale != old, "old size-only index contradicts original first-hit scan");
        condition(!frame.hasIndex(), "same-size swap must invalidate directory through content epoch");
        for (int i=0; i<(quick ? 300 : 10000); i++) {
            int x=random.nextInt(3100)-50,y=random.nextInt(2500)-50;
            same(frame.oldHit(x,y), frame.fixedHit(x,y), "same-size direct set / swap fallback");
        }
        frame.create(columns,n);
        condition(frame.hasIndex(), "update makes directory valid again");
        List<Tile> slice = frame.getTiles().subList(100, 102);
        condition(!frame.hasIndex(), "subList escape conservatively advances content epoch");
        Collections.swap(slice,0,1);
        for (int i=0; i<(quick ? 100 : 1500); i++) {
            int x=random.nextInt(3100)-50,y=random.nextInt(2500)-50;
            same(frame.oldHit(x,y), frame.fixedHit(x,y), "subList set fallback");
        }
        frame.create(columns,n);
        condition(frame.hasIndex(), "directory valid after rebuilt sublist");
        ListIterator<Tile> iter = frame.getTiles().listIterator(10);
        condition(!frame.hasIndex(), "listIterator escape conservatively advances content epoch");
        Tile at10=iter.next();iter.set(frame.getTiles().get(11));
        same(frame.oldHit(at10.x+3,at10.y+3),frame.fixedHit(at10.x+3,at10.y+3),"listIterator set");
        frame.create(columns,n);
        condition(frame.hasIndex(), "directory valid after iterator rebuild");
        frame.getTiles().removeIf(t -> t.id % 777 == 0);
        condition(!frame.hasIndex(), "removeIf increments ArrayList.modCount");
        same(frame.oldHit(8,8),frame.fixedHit(8,8),"removeIf fallback");
        frame.create(columns,n);
        condition(frame.hasIndex(), "directory valid after removeIf rebuild");
        frame.getTiles().replaceAll(t -> new Tile(t.x,t.y,t.width,t.height,t.id+1));
        condition(!frame.hasIndex(), "replaceAll increments ArrayList.modCount");
        same(frame.oldHit(8,8),frame.fixedHit(8,8),"replaceAll fallback");
        frame.create(columns,n);
        condition(frame.hasIndex(), "directory valid after replaceAll rebuild");
        frame.getTiles().replaceAll(t -> {
            if (t.id == 7) condition(!frame.hasIndex(),
                    "reentrant replaceAll operator sees original ordered scan");
            return t;
        });
        same(frame.oldHit(8,8),frame.fixedHit(8,8),"reentrant replaceAll fallback");
        frame.create(columns,n);
        condition(frame.hasIndex(), "directory valid after reentrant replaceAll");
        frame.getTiles().sort((a,b) -> {
            condition(!frame.hasIndex(), "reentrant sort comparator never uses stale hit slot");
            return Integer.compare(b.id,a.id);
        });
        condition(!frame.hasIndex(), "ArrayList.sort updates modCount and invalidates grid");
        same(frame.oldHit(8,8),frame.fixedHit(8,8),"sort fallback");
        frame.create(columns,n);
        condition(frame.hasIndex(), "directory valid after sort rebuild");
        frame.getTiles().removeIf(t -> {
            if (t.id == 7) condition(!frame.hasIndex(),
                    "removeIf predicate can re-enter widget but cannot use fast directory");
            return false;
        });
        condition(!frame.hasIndex(), "even no-op callback-bearing removeIf is conservatively safe");
        frame.create(columns,n);
        condition(frame.hasIndex(), "directory valid after removeIf callback rebuild");
        Iterator<Tile> iterator = frame.getTiles().iterator();
        iterator.next();iterator.remove();
        condition(!frame.hasIndex(), "iterator.remove increments modCount");
        same(frame.oldHit(8,8),frame.fixedHit(8,8),"iterator.remove fallback");
        // Even an earlier client-held reference remains tracked across rebuild.
        List<Tile> escaped = frame.getTiles();
        frame.create(columns,n);
        condition(frame.hasIndex(), "same retained Java List across update");
        escaped.set(1, escaped.get(2));
        condition(!frame.hasIndex(), "escaped parent reference cannot bypass content epoch");
        same(frame.oldHit(32,7),frame.fixedHit(32,7),"escaped list reference fallback");
        condition(frame.getTiles() instanceof ArrayList<?>, "preserve ArrayList interface/mutability");
        System.out.println("TILES_MUTATION_EPOCH_PASS checks="+checks+" indexed="+indexed+" fallback="+fallbacks);
    }
}
