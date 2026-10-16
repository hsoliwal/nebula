// SPDX-License-Identifier: EPL-2.0
// Original Nebula Tiles geometry, deterministic Java21 differential oracle.
package org.eclipse.nebula.widgets.tiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class TilesHitGridProof {
    record Tile(int x, int y, int width, int height) {}
    record Directory(int columns, int stepX, int stepY, int count) {}
    private static long checks, admitted, denied;

    static List<Tile> originalFrame(int width, int height, int tileWidth,
            int tileHeight, int marginX, int marginY, int maxElements) {
        ArrayList<Tile> result = new ArrayList<>();
        int x = marginX, y = marginY;
        for (int i = 0; i < maxElements; i++) {
            result.add(new Tile(x, y, tileWidth, tileHeight));
            x += tileWidth + marginX;
            if (x + tileWidth + marginX > width) {
                y += tileHeight + marginY;
                x = marginX;
            }
            if (y + tileHeight + marginY > height) break;
        }
        return result;
    }

    static int originalHit(List<Tile> rendered, int x, int y) {
        for (int i = 0; i < rendered.size(); i++) {
            Tile t = rendered.get(i);
            // Exactly the original Tiles.getTileAt: signed int wrap and inclusive edges.
            if (x >= t.x && y >= t.y && x <= t.x + t.width && y <= t.y + t.height) {
                return i;
            }
        }
        return -1;
    }

    static Directory admit(List<Tile> rendered, int width, int height, int tileWidth,
            int tileHeight, int marginX, int marginY) {
        if (rendered.size() < 16 || tileWidth <= 0 || tileHeight <= 0
                || marginX <= 0 || marginY <= 0 || width <= 0 || height <= 0) return null;
        long stepX = (long)tileWidth + marginX;
        long stepY = (long)tileHeight + marginY;
        if (stepX > Integer.MAX_VALUE || stepY > Integer.MAX_VALUE
                || (long)marginX + stepX > width
                || (long)marginY + stepY > height) return null;
        long columns = ((long)width - marginX) / stepX;
        if (columns < 1 || columns > Integer.MAX_VALUE) return null;
        long rows = (rendered.size() - 1L) / columns + 1L;
        if ((long)marginX + (columns + 1L) * stepX > Integer.MAX_VALUE
                || (long)marginY + (rows + 1L) * stepY > Integer.MAX_VALUE
                || (long)marginY + (rows - 1L) * stepY + stepY > height) return null;
        for (int i = 0; i < rendered.size(); i++) {
            Tile t = rendered.get(i);
            long expectedX = marginX + (i % columns) * stepX;
            long expectedY = marginY + (i / columns) * stepY;
            if (t.x != expectedX || t.y != expectedY
                    || t.width != tileWidth || t.height != tileHeight) return null;
        }
        return new Directory((int)columns, (int)stepX, (int)stepY, rendered.size());
    }

    static int indexedHit(List<Tile> rendered, Directory d, int x, int y,
            int marginX, int marginY) {
        if (d == null) return originalHit(rendered, x, y);
        if (x < marginX || y < marginY || rendered.size() != d.count()) {
            return rendered.size() == d.count() ? -1 : originalHit(rendered, x, y);
        }
        long column = ((long)x - marginX) / d.stepX();
        long row = ((long)y - marginY) / d.stepY();
        if (column >= d.columns()) return -1;
        long slot = row * d.columns() + column;
        if (slot >= rendered.size()) return -1;
        Tile tile = rendered.get((int)slot);
        if (tile.x <= x && x <= tile.x + tile.width && tile.y <= y && y <= tile.y + tile.height) {
            return (int)slot;
        }
        return -1;
    }

    static void assertHit(List<Tile> tiles, Directory d, int x, int y, int marginX, int marginY) {
        int a = originalHit(tiles, x, y);
        int b = indexedHit(tiles, d, x, y, marginX, marginY);
        checks++;
        if (a != b) throw new AssertionError("case="+checks+" at="+x+","+y+" original="+a+" candidate="+b+" grid="+d+" tileCount="+tiles.size());
    }

    static void caseRun(int width, int height, int tw, int th, int mx, int my,
            int requested, Random r) {
        List<Tile> tiles = originalFrame(width, height, tw, th, mx, my, requested);
        Directory d = admit(tiles, width, height, tw, th, mx, my);
        if (d != null) admitted++; else denied++;
        for (int i = 0; i < 24; i++) {
            int x, y;
            if (!tiles.isEmpty() && i < 12) {
                Tile t = tiles.get(r.nextInt(tiles.size()));
                x = switch(i%6) {case 0->t.x;case 1->t.x+t.width;case 2->t.x-1;case 3->t.x+t.width+1;default->t.x+r.nextInt(20)-10;};
                y = switch(i%6) {case 0->t.y;case 1->t.y+t.height;case 2->t.y-1;case 3->t.y+t.height+1;default->t.y+r.nextInt(20)-10;};
            } else {
                x = r.nextBoolean()?r.nextInt():r.nextInt(2048)-512;
                y = r.nextBoolean()?r.nextInt():r.nextInt(2048)-512;
            }
            assertHit(tiles,d,x,y,mx,my);
        }
    }

    public static void main(String[] args) {
        Random r = new Random(0x4d3354494c45534cL);
        // Orthogonal positive, zero, negative, exact edge and signed-int-overflow frame geometry.
        for (int width : new int[]{0,1,16,24,64,128,256,480,1920,Integer.MAX_VALUE,Integer.MIN_VALUE})
            for (int height : new int[]{0,1,32,128,768,1920,Integer.MAX_VALUE,Integer.MIN_VALUE})
                for (int tw : new int[]{-1,0,1,12,32,96,Integer.MAX_VALUE})
                    caseRun(width,height,tw,20,5,4,70,r);
        // Four thousand random frames, including many warm positive grids and malformed dimensions.
        for (int i=0;i<4000;i++) {
            int w=i%5==0?r.nextInt():r.nextInt(1800);
            int h=i%7==0?r.nextInt():r.nextInt(1200);
            int tw=i%9==0?r.nextInt():r.nextInt(80)-10;
            int th=i%11==0?r.nextInt():r.nextInt(80)-10;
            int mx=i%13==0?r.nextInt():r.nextInt(30)-5;
            int my=i%17==0?r.nextInt():r.nextInt(30)-5;
            caseRun(w,h,tw,th,mx,my,r.nextInt(1000)+1,r);
        }
        // Valid large static grid, forced to exercise the fast path and warm mouse events.
        List<Tile> big=originalFrame(3000,3000,20,20,5,5,10000);
        Directory index=admit(big,3000,3000,20,20,5,5);
        if(index==null || big.size()<5000)throw new AssertionError("positive indexed test never admitted");
        for(int i=0;i<60000;i++)assertHit(big,index,r.nextInt(3200)-100,r.nextInt(3200)-100,5,5);
        // Same-size geometry corruption must invalidate the fast directory upon rebuild.
        List<Tile> broken = new ArrayList<>(big);
        Tile t=broken.get(99);broken.set(99,new Tile(t.x+1,t.y,t.width,t.height));
        if(admit(broken,3000,3000,20,20,5,5)!=null)throw new AssertionError("mutated tile accepted");
        System.out.println("TILES_INTRINSIC_HIT_GRID_PASS checks="+checks+" indexedFrames="+admitted+" fallbackFrames="+denied+" resident="+big.size());
    }
}
