// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.widgets.tiles;

import java.util.List;
import java.util.Random;

/** Test-only native geometry twin. The live SWT widget never invokes JNI per hit. */
public final class TilesHitGridJniProof {
    private static long comparisons;
    private static native int indexed(int firstX, int firstY, int columns,
            int stepX, int stepY, int count, int tileWidth, int tileHeight,
            int x, int y);
    static { System.loadLibrary("m3_tiles_hit_grid_oracle"); }

    private static void compare(List<TilesHitGridProof.Tile> tiles,
            TilesHitGridProof.Directory plan, int x, int y) {
        int old = TilesHitGridProof.originalHit(tiles, x, y);
        int javaIndex = TilesHitGridProof.indexedHit(tiles, plan, x, y,
                tiles.get(0).x(), tiles.get(0).y());
        TilesHitGridProof.Tile first = tiles.get(0);
        int nativeIndex = indexed(first.x(), first.y(), plan.columns(),
                plan.stepX(), plan.stepY(), tiles.size(),
                first.width(), first.height(), x, y);
        comparisons++;
        if (old != javaIndex || javaIndex != nativeIndex) {
            throw new AssertionError("Tiles JNI mismatch old="+old+" Java="+javaIndex
                    +" JNI="+nativeIndex+" x="+x+" y="+y);
        }
    }

    private static void invalid(Runnable call) {
        comparisons++;
        try { call.run(); }
        catch (IllegalArgumentException accepted) { return; }
        throw new AssertionError("unreviewed native geometry admitted");
    }

    public static void main(String[] args) {
        boolean quick = args.length == 1 && "--quick".equals(args[0]);
        Random random = new Random(0x4d3354494c454a4eL);
        List<TilesHitGridProof.Tile> large = TilesHitGridProof.originalFrame(
                3000, 3000, 20, 20, 5, 5, 10000);
        var plan = TilesHitGridProof.admit(large, 3000, 3000, 20, 20, 5, 5);
        if (plan == null || large.size() != 10000) throw new AssertionError("indexed frame missing");
        int cases = quick ? 1000 : 25000;
        for (int i=0; i<cases; i++) {
            var t = large.get(random.nextInt(large.size()));
            int x = (i & 1) == 0 ? t.x()+random.nextInt(25) : random.nextInt(3200)-100;
            int y = (i & 1) == 0 ? t.y()+random.nextInt(25) : random.nextInt(3200)-100;
            compare(large, plan, x, y);
        }
        if (!quick) {
            for (int run=0; run<1600; run++) {
                int width = 140 + random.nextInt(1200);
                int height = 110 + random.nextInt(950);
                int tw = 2 + random.nextInt(45);
                int th = 2 + random.nextInt(38);
                int mx = 1 + random.nextInt(18);
                int my = 1 + random.nextInt(16);
                List<TilesHitGridProof.Tile> tiles = TilesHitGridProof.originalFrame(
                        width, height, tw, th, mx, my, 180);
                var directory = TilesHitGridProof.admit(tiles, width, height, tw, th, mx, my);
                if (directory == null) continue;
                for (int i=0;i<8;i++) {
                    var t=tiles.get(random.nextInt(tiles.size()));
                    int x=t.x()+random.nextInt(tw+mx+2)-1;
                    int y=t.y()+random.nextInt(th+my+2)-1;
                    compare(tiles, directory, x, y);
                }
            }
        }
        invalid(() -> indexed(1, 1, 0, 10, 10, 20, 5, 5, 20, 20));
        invalid(() -> indexed(1, 1, 2, 0, 10, 20, 5, 5, 20, 20));
        invalid(() -> indexed(1, 1, 2, 10, -1, 20, 5, 5, 20, 20));
        invalid(() -> indexed(1, 1, 2, 10, 10, -1, 5, 5, 20, 20));
        invalid(() -> indexed(1, 1, 2, 10, 10, 20, 0, 5, 20, 20));
        System.out.println("TILES_HIT_GRID_JNI_PASS checks=" + comparisons + " quick=" + quick);
    }
}
