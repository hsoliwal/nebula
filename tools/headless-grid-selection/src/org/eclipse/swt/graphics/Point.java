/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package org.eclipse.swt.graphics;

/** Headless compile-only SWT Point stand-in. Never distribute with Nebula runtime. */
public final class Point {
    public int x;
    public int y;
    public Point(int x, int y) {this.x=x;this.y=y;}
    @Override public boolean equals(Object object) {
        return object instanceof Point p && p.x==x && p.y==y;
    }
    @Override public int hashCode() {return 31*x+y;}
    @Override public String toString() {return "Point ["+x+", "+y+"]";}
}
