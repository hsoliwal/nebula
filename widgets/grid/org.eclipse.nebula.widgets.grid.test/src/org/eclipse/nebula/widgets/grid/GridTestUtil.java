/*******************************************************************************
 * Copyright (c) 2012 EclipseSource and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 *    Hitesh Soliwal (Synexia) - viewport mode
 ******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;


public class GridTestUtil {

  private GridTestUtil() {
    // prevent instantiation
  }

  public static GridItem[] createGridItems( Grid grid, int rootItems, int childItems ) {
    GridItem[] result = new GridItem[ rootItems * ( childItems + 1 ) ];
    GridColumn column = new GridColumn( grid, SWT.NONE );
    column.setText( "Column 1" );
    column.setWidth( 100 );
    int counter = 0;
    for( int i = 0; i < rootItems; i++ ) {
      GridItem rootItem = new GridItem( grid, SWT.NONE );
      rootItem.setText( "root_" + i );
      result[ counter ] = rootItem;
      counter++ ;
      for( int j = 0; j < childItems; j++ ) {
        GridItem childItem = new GridItem( rootItem, SWT.NONE );
        childItem.setText( "root_" + i + "_child_" + j );
        result[ counter ] = childItem;
        counter++ ;
      }
    }
    return result;
  }

  public static GridColumn[] createGridColumns( Grid grid, int columns, int style ) {
    GridColumn[] result = new GridColumn[ columns ];
    for( int i = 0; i < columns; i++ ) {
      GridColumn column = new GridColumn( grid, style );
      column.setText( "col_" + i );
      column.setWidth( 20 * ( i + 1 ) );
      result[ i ] = column;
    }
    return result;
  }

  public static GridColumn[] createGridColumns( GridColumnGroup group, int columns, int style ) {
    GridColumn[] result = new GridColumn[ columns ];
    for( int i = 0; i < columns; i++ ) {
      GridColumn column = new GridColumn( group, style );
      column.setText( "col_" + i );
      column.setWidth( 20 * ( i + 1 ) );
      result[ i ] = column;
    }
    return result;
  }

  /**
   * Whether the Grid under test fixes the stock defects listed in
   * docs/m3/grid-viewport/UPSTREAM_DEFECTS.tsv: every mode but original. Stock Nebula Grid (no
   * GridViewport class) answers false, as the original mode does.
   */
  public static boolean fixesUpstreamDefects() {
    try {
      java.lang.reflect.Field field = Class.forName( "org.eclipse.nebula.widgets.grid.GridViewport" )
        .getDeclaredField( "UPSTREAM_FIXES" );
      field.setAccessible( true );
      return field.getBoolean( null );
    } catch( ReflectiveOperationException stock ) {
      return false;
    }
  }

  /**
   * Runs a call that throws {@code fixed} in the default mode and {@code stock} in the original
   * mode and on stock Nebula Grid (a {@code null} class: the call returns normally).
   */
  public static void assertThrowsByMode( Class<? extends Throwable> fixed, Class<? extends Throwable> stock,
                                         Runnable call )
  {
    Class<? extends Throwable> expected = fixesUpstreamDefects() ? fixed : stock;
    try {
      call.run();
    } catch( Throwable thrown ) {
      if( expected != null && expected.isInstance( thrown ) ) {
        return;
      }
      throw new AssertionError( "expected " + ( expected == null ? "no exception" : expected.getName() ) + " but got "
                                + thrown, thrown );
    }
    if( expected != null ) {
      throw new AssertionError( "expected " + expected.getName() );
    }
  }

  /**
   * Pumps events until the Grid has handled at least {@code minimumPaintPasses} paint events in
   * total ({@link Grid#paintPasses}) and then none for 200 ms. It waits at most two seconds for the
   * minimum and five seconds in all, requests no paint and never fails: the caller's own
   * assertions judge what was painted.
   * <p>
   * A plain {@code while (display.readAndDispatch())} drain is not enough on GTK3. win32 paints
   * inside {@code Control.update()} and delivers WM_PAINT before its queue runs empty. SWT GTK3's
   * {@code Control.update()} only flushes the queued X11 Expose events (it no longer calls
   * {@code gdk_window_process_updates}), and GTK paints from its frame clock once the queue is
   * empty, after {@code readAndDispatch()} has returned false. The Grid computes its scroll bar
   * values in that paint, so they are only current after it.
   * </p>
   */
  public static void awaitGridPaints( Display display, Grid grid, int minimumPaintPasses ) {
    long start = System.nanoTime();
    long quietSince = start;
    int seen = grid.paintPasses;
    for( ;; ) {
      long now = System.nanoTime();
      if( now - start >= 5_000_000_000L ) {
        return;
      }
      if( seen < minimumPaintPasses ? now - start >= 2_000_000_000L : now - quietSince >= 200_000_000L ) {
        return;
      }
      if( !display.readAndDispatch() ) {
        try {
          Thread.sleep( 5 );
        } catch( InterruptedException interrupted ) {
          Thread.currentThread().interrupt();
          return;
        }
      }
      if( grid.isDisposed() ) {
        return;
      }
      if( grid.paintPasses != seen ) {
        seen = grid.paintPasses;
        quietSince = System.nanoTime();
      }
    }
  }

  public static Image loadImage( Display display, String name ) {
    Image result = null;
    InputStream stream = Fixture.class.getClassLoader().getResourceAsStream( name );
    if( stream != null ) {
      try {
        result = new Image( display, stream );
      } finally {
        try {
          stream.close();
        } catch( IOException unexpected ) {
          throw new RuntimeException( "Failed to close image input stream", unexpected );
        }
      }
    }
    return result;
  }
}
