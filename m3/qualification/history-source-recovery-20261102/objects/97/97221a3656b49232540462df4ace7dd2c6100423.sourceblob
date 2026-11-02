/*******************************************************************************
 * Copyright (c) 2012, 2014 EclipseSource and others.
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
 *    Hitesh Soliwal (Synexia) - fork changes (see the Git history)
 ******************************************************************************/
package org.eclipse.nebula.widgets.grid;

import static org.eclipse.nebula.widgets.grid.GridTestUtil.createGridColumns;
import static org.eclipse.nebula.widgets.grid.GridTestUtil.createGridItems;
import static org.eclipse.nebula.widgets.grid.GridTestUtil.loadImage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.eclipse.nebula.widgets.grid.GridTestUtil.fixesUpstreamDefects;
import static org.eclipse.nebula.widgets.grid.GridTestUtil.assertThrowsByMode;


public class GridItem_Test {

  private Display display;
  private Shell shell;
  private Grid grid;
  private List<Event> eventLog;

  @Before
  public void setUp() {
    display = Display.getDefault();
    shell = new Shell( display );
    grid = new Grid( shell, SWT.H_SCROLL | SWT.V_SCROLL );
    grid.setSize( 200, 200 );
    eventLog = new ArrayList<Event>();
  }

  @After
  public void tearDown() {
  }

  @Test
  public void testGridItemCreation_GridParent() {
    GridItem item = new GridItem( grid, SWT.NONE );
    assertSame( grid, item.getParent() );
    assertSame( item, grid.getItem( 0 ) );
    assertSame( item, grid.getRootItem( 0 ) );
    assertNull( item.getParentItem() );
    assertEquals( 0, grid.indexOf( item ) );
    assertEquals( 1, grid.getItemCount() );
    assertEquals( 1, grid.getRootItemCount() );
  }

  @Test
  public void testGridItemCreation_GridItemParent() {
    GridItem parentItem = new GridItem( grid, SWT.NONE );
    GridItem item = new GridItem( parentItem, SWT.NONE );
    assertSame( grid, item.getParent() );
    assertSame( item, grid.getItem( 1 ) );
    assertSame( parentItem, item.getParentItem() );
    assertEquals( 1, grid.indexOf( item ) );
    assertEquals( 2, grid.getItemCount() );
    assertEquals( 1, grid.getRootItemCount() );
    assertEquals( 1, parentItem.getItemCount() );
  }

  @Test
  public void testGridItemCreation_AtIndexWithGridParent() {
    createGridItems( grid, 5, 5 );
    GridItem item = new GridItem( grid, SWT.NONE, 2 );
    assertSame( item, grid.getItem( 12 ) );
    assertSame( item, grid.getRootItem( 2 ) );
    assertEquals( 12, grid.indexOf( item ) );
    assertEquals( 31, grid.getItemCount() );
    assertEquals( 6, grid.getRootItemCount() );
  }

  @Test
  public void testGridItemCreation_AtIndexWithGridItemParent() {
    createGridItems( grid, 5, 5 );
    GridItem parentItem = grid.getItem( 6 );
    GridItem item = new GridItem( parentItem, SWT.NONE, 2 );
    assertSame( item, grid.getItem( 9 ) );
    assertEquals( 9, grid.indexOf( item ) );
    assertEquals( 31, grid.getItemCount() );
    assertEquals( 5, grid.getRootItemCount() );
    assertEquals( 6, parentItem.getItemCount() );
  }

  @Test
  public void testGridItemCreation_onVirtual_doesNotResolveItem() {
    grid = new Grid( shell, SWT.VIRTUAL );
    grid.addListener( SWT.SetData, new LoggingListener() );
    grid.setItemCount( 1 );
    GridItem item = grid.getItem( 0 );
    // neither creating nor looking up a virtual item requests its data
    assertTrue( eventLog.isEmpty() );
    item.getText();
    // the getter without a column index requests it in the default mode only (UPSTREAM_DEFECTS.tsv)
    assertEquals( fixesUpstreamDefects() ? 1 : 0, eventLog.size() );
  }

  @Test
  public void testGetItemCount() {
    createGridItems( grid, 1, 10 );
    assertEquals( 10, grid.getItem( 0 ).getItemCount() );
  }

  @Test
  public void testGetItemCount_AfterDispose() {
    GridItem[] items = createGridItems( grid, 1, 10 );
    items[ 5 ].dispose();
    assertEquals( 10, grid.getItemCount() );
    assertEquals( 9, grid.getItem( 0 ).getItemCount() );
  }

  @Test
  public void testGetItems() {
    GridItem[] items = createGridItems( grid, 1, 10 );
    GridItem[] expected = new GridItem[ 10 ];
    System.arraycopy( items, 1, expected, 0, 10 );
    assertTrue( Arrays.equals( expected, items[ 0 ].getItems() ) );
  }

  @Test
  public void testGetItem() {
    GridItem[] items = createGridItems( grid, 1, 10 );
    assertSame( items[ 5 ], items[ 0 ].getItem( 4 ) );
  }

  @Test( expected = IndexOutOfBoundsException.class )
  public void testGetItem_InvalidIndex() {
    GridItem[] items = createGridItems( grid, 1, 10 );
    items[ 0 ].getItem( 100 );
  }

  @Test
  public void testIndexOf() {
    GridItem[] items = createGridItems( grid, 1, 10 );
    assertEquals( 5, items[ 0 ].indexOf( items[ 6 ] ) );
  }

  @Test
  public void testIndexOf_AfterDispose() {
    GridItem[] items = createGridItems( grid, 1, 10 );
    items[ 2 ].dispose();
    assertEquals( 4, items[ 0 ].indexOf( items[ 6 ] ) );
  }

  @Test( expected = IllegalArgumentException.class )
  public void testIndexOf_NullArgument() {
    GridItem[] items = createGridItems( grid, 1, 10 );
    items[ 0 ].indexOf( null );
  }

  @Test( expected = IllegalArgumentException.class )
  public void testIndexOf_WithDisposedItem() {
    GridItem[] items = createGridItems( grid, 1, 10 );
    items[ 6 ].dispose();
    items[ 0 ].indexOf( items[ 6 ] );
  }

  @Test
  public void testHasChildren() {
    GridItem[] items = createGridItems( grid, 1, 10 );
    assertTrue( items[ 0 ].hasChildren() );
  }

  @Test
  public void testHasChildren_NoChildren() {
    GridItem[] items = createGridItems( grid, 1, 0 );
    assertFalse( items[ 0 ].hasChildren() );
  }

  @Test
  public void testHasChildren_AfterItemRemove() {
    GridItem[] items = createGridItems( grid, 1, 1 );
    items[ 1 ].dispose();
    assertFalse( items[ 0 ].hasChildren() );
  }

  @Test
  public void testDispose() {
    GridItem[] items = createGridItems( grid, 1, 1 );
    items[ 0 ].dispose();
    assertTrue( items[ 0 ].isDisposed() );
    assertTrue( items[ 1 ].isDisposed() );
    assertEquals( 0, grid.getItemCount() );
    assertEquals( 0, grid.getRootItemCount() );
  }

  @Test
  public void testSendDisposeEvent() {
    GridItem[] items = createGridItems( grid, 1, 1 );
    items[ 0 ].addListener( SWT.Dispose, new LoggingListener() );
    items[ 0 ].dispose();
    assertEquals( 1, eventLog.size() );
    assertSame( items[ 0 ], eventLog.get( 0 ).widget );
  }

  @Test
  public void testSendDisposeEventOnGridDispose() {
    GridItem[] items = createGridItems( grid, 1, 1 );
    items[ 0 ].addListener( SWT.Dispose, new LoggingListener() );
    grid.dispose();
    assertEquals( 1, eventLog.size() );
    assertSame( items[ 0 ], eventLog.get( 0 ).widget );
  }

  @Test
  public void testGetLevel() {
    GridItem[] items = createGridItems( grid, 1, 1 );
    assertEquals( 0, items[ 0 ].getLevel() );
    assertEquals( 1, items[ 1 ].getLevel() );
  }

  @Test
  public void testSetExpanded() {
    GridItem[] items = createGridItems( grid, 1, 1 );
    assertFalse( items[ 0 ].isExpanded() );
    assertFalse( items[ 1 ].isVisible() );
    items[ 0 ].setExpanded( true );
    assertTrue( items[ 0 ].isExpanded() );
    assertTrue( items[ 1 ].isVisible() );
  }

  @Test
  public void testSetExpanded_ChangedFocusItem() {
    GridItem[] items = createGridItems( grid, 1, 1 );
    items[ 0 ].setExpanded( true );
    grid.setFocusItem( items[ 1 ] );
    items[ 0 ].setExpanded( false );
    assertSame( items[ 0 ], grid.getFocusItem() );
  }

  @Test
  public void testSetExpanded_ChangeSelection() {
    GridItem[] items = createGridItems( grid, 1, 1 );
    items[ 0 ].setExpanded( true );
    grid.setSelection( 1 );
    items[ 0 ].setExpanded( false );
    assertFalse( grid.isSelected( 1 ) );
  }

  @Test
  public void testSetExpanded_FireSelectionEvent() {
    GridItem[] items = createGridItems( grid, 1, 1 );
    items[ 0 ].setExpanded( true );
    grid.setSelection( 1 );
    grid.addListener( SWT.Selection, new LoggingListener() );
    items[ 0 ].setExpanded( false );
    assertEquals( 1, eventLog.size() );
    Event event = eventLog.get( 0 );
    assertSame( items[ 0 ], event.item );
  }

  @Test
  public void testIsVisibleOnCreation() {
    GridItem[] items = createGridItems( grid, 1, 1 );
    items[ 0 ].setExpanded( true );
    new GridItem( items[ 0 ], SWT.NONE );
    assertTrue( items[ 1 ].isVisible() );
  }

  @Test
  public void testFireEvent() {
    GridItem item = new GridItem( grid, SWT.NONE );
    grid.addListener( SWT.Expand, new LoggingListener() );
    item.fireEvent( SWT.Expand );
    assertEquals( 1, eventLog.size() );
    Event event = eventLog.get( 0 );
    assertSame( item.getDisplay(), event.display );
    assertSame( grid, event.widget );
    assertSame( item, event.item );
  }

  @Test
  public void testFireCheckEvent() {
    GridItem item = new GridItem( grid, SWT.NONE );
    grid.addListener( SWT.Selection, new LoggingListener() );
    item.fireCheckEvent( 3 );
    assertEquals( 1, eventLog.size() );
    Event event = eventLog.get( 0 );
    assertSame( item.getDisplay(), event.display );
    assertSame( grid, event.widget );
    assertSame( item, event.item );
    assertEquals( SWT.CHECK, event.detail );
    assertEquals( 3, event.index );
  }

  @Test
  public void testGetText_Inital() {
    GridItem item = new GridItem( grid, SWT.NONE );
    assertEquals( "", item.getText() );
  }

  @Test
  public void testGetText_InvalidColumn() {
    GridItem item = new GridItem( grid, SWT.NONE );
    // Nebula: a column index without a column answers the empty text
    assertEquals( "", item.getText( 5 ) );
  }

  @Test
  public void testGetText_AfterSet() {
    GridItem item = new GridItem( grid, SWT.NONE );
    item.setText( "foo" );
    assertEquals( "foo", item.getText() );
  }

  @Test
  public void testGetText_WithColumns() {
    GridItem item = new GridItem( grid, SWT.NONE );
    createGridColumns( grid, 3, SWT.NONE );
    item.setText( 0, "0" );
    item.setText( 1, "1" );
    item.setText( 2, "2" );
    assertEquals( "0", item.getText( 0 ) );
    assertEquals( "1", item.getText( 1 ) );
    assertEquals( "2", item.getText( 2 ) );
  }

  @Test
  public void testGetText_AfterAddColumn() {
    GridItem item = new GridItem( grid, SWT.NONE );
    createGridColumns( grid, 1, SWT.NONE );
    item.setText( "foo" );
    new GridColumn( grid, SWT.NONE, 0 );
    assertEquals( "", item.getText( 0 ) );
    assertEquals( "foo", item.getText( 1 ) );
  }

  @Test
  public void testGetText_AfterRemoveColumn() {
    GridItem item = new GridItem( grid, SWT.NONE );
    GridColumn[] columns = createGridColumns( grid, 2, SWT.NONE );
    item.setText( 1, "foo" );
    columns[ 0 ].dispose();
    assertEquals( "foo", item.getText( 0 ) );
  }

  @Test
  public void testGetText_AfterRemoveAllColumns() {
    GridItem item = new GridItem( grid, SWT.NONE );
    GridColumn[] columns = createGridColumns( grid, 2, SWT.NONE );
    item.setText( 1, "foo" );
    // Nebula: the cells right of a disposed column move left; a disposed column's cells go with it
    columns[ 0 ].dispose();
    assertEquals( "foo", item.getText( 0 ) );
    columns[ 1 ].dispose();
    assertEquals( "", item.getText( 0 ) );
  }

  @Test
  public void testSetText_InvalidColumn() {
    GridItem item = new GridItem( grid, SWT.NONE );
    // Nebula keeps the text of a column index without a column
    item.setText( 5, "foo" );
    assertEquals( "foo", item.getText( 5 ) );
  }

  @Test( expected = IllegalArgumentException.class )
  public void testSetText_NullArgument() {
    GridItem item = new GridItem( grid, SWT.NONE );
    item.setText( 5, null );
  }

  @Test
  public void testSetText_marksItemCached() {
    grid = new Grid( shell, SWT.VIRTUAL );
    GridItem item = new GridItem( grid, SWT.NONE );
    grid.addListener( SWT.SetData, new LoggingListener() );
    item.setText( "foo" );
    // Nebula: setting a text does not mark the item as answered: its data is still requested once
    assertEquals( "foo", item.getText( 0 ) );
    item.getText( 0 );
    assertEquals( 1, eventLog.size() );
  }

  @Test
  public void testHandleVirtual_RootItem() {
    grid = new Grid( shell, SWT.VIRTUAL );
    GridItem[] items = createGridItems( grid, 3, 3 );
    grid.clearAll( true );
    grid.addListener( SWT.SetData, new LoggingListener() );
    requestData( items[ 4 ] );
    assertEquals( 1, eventLog.size() );
    Event event = eventLog.get( 0 );
    assertSame( grid, event.widget );
    assertSame( items[ 4 ], event.item );
    // Nebula: the index of a root item is its row index (the index of a child is its index in its parent)
    assertEquals( 4, event.index );
  }

  @Test
  public void testHandleVirtual_SubItem() {
    grid = new Grid( shell, SWT.VIRTUAL );
    GridItem[] items = createGridItems( grid, 3, 3 );
    grid.clearAll( true );
    grid.addListener( SWT.SetData, new LoggingListener() );
    requestData( items[ 2 ] );
    assertEquals( 1, eventLog.size() );
    Event event = eventLog.get( 0 );
    assertSame( grid, event.widget );
    assertSame( items[ 2 ], event.item );
    assertEquals( 1, event.index );
  }

  @Test
  public void testHandleVirtual_Twice() {
    grid = new Grid( shell, SWT.VIRTUAL );
    GridItem[] items = createGridItems( grid, 3, 3 );
    grid.clearAll( true );
    grid.addListener( SWT.SetData, new LoggingListener() );
    requestData( items[ 2 ] );
    requestData( items[ 2 ] );
    assertEquals( 1, eventLog.size() );
  }

  @Test
  public void testGetFont_Inital() {
    GridItem item = new GridItem( grid, SWT.NONE );
    assertSame( grid.getFont(), item.getFont() );
  }

  @Test
  public void testGetFont() {
    GridItem item = new GridItem( grid, SWT.NONE );
    Font font = new Font( display, "Arial", 20, SWT.BOLD );
    item.setFont( font );
    assertSame( font, item.getFont() );
  }

  @Test( expected = IllegalArgumentException.class )
  public void testSetFont_DisposedFont() {
    GridItem item = new GridItem( grid, SWT.NONE );
    Font font = new Font( display, "Arial", 20, SWT.BOLD );
    font.dispose();
    item.setFont( font );
  }

  @Test
  public void testGetFontByIndex() {
    createGridColumns( grid, 3, SWT.NONE );
    GridItem item = new GridItem( grid, SWT.NONE );
    Font font = new Font( display, "Arial", 20, SWT.BOLD );
    item.setFont( 1, font );
    // Nebula: a cell without its own font answers none (its renderer paints with the item's font)
    assertNull( item.getFont( 0 ) );
    assertSame( font, item.getFont( 1 ) );
    assertNull( item.getFont( 2 ) );
  }

  @Test
  public void testGetFontByIndex_InvalidIndex() {
    createGridColumns( grid, 3, SWT.NONE );
    GridItem item = new GridItem( grid, SWT.NONE );
    assertNull( item.getFont( 10 ) );
  }

  @Test( expected = IllegalArgumentException.class )
  public void testSetFontByIndex_DisposedFont() {
    createGridColumns( grid, 3, SWT.NONE );
    GridItem item = new GridItem( grid, SWT.NONE );
    Font font = new Font( display, "Arial", 20, SWT.BOLD );
    font.dispose();
    item.setFont( 1, font );
  }

  @Test
  public void testGetBackground_Initial() {
    GridItem item = new GridItem( grid, SWT.NONE );
    assertEquals( grid.getBackground(), item.getBackground() );
  }

  @Test
  public void testGetBackground() {
    GridItem item = new GridItem( grid, SWT.NONE );
    Color background = new Color(0, 0, 255 );
    item.setBackground( background );
    assertSame( background, item.getBackground() );
  }

  @Test( expected = IllegalArgumentException.class )
  public void testSetBackground_DisposedFont() {
    GridItem item = new GridItem( grid, SWT.NONE );
    Color background = new Color(0, 0, 255 );
    background.dispose();
    item.setBackground( background );
  }

  @Test
  public void testGetBackgroundByIndex() {
    createGridColumns( grid, 3, SWT.NONE );
    GridItem item = new GridItem( grid, SWT.NONE );
    Color background = new Color(0, 0, 255 );
    item.setBackground( 1, background );
    assertEquals( grid.getBackground(), item.getBackground( 0 ) );
    assertSame( background, item.getBackground( 1 ) );
    assertEquals( grid.getBackground(), item.getBackground( 2 ) );
  }

  @Test
  public void testGetBackgroundByIndex_InvalidIndex() {
    createGridColumns( grid, 3, SWT.NONE );
    GridItem item = new GridItem( grid, SWT.NONE );
    // Nebula: a column index without a column answers the default cell background
    assertEquals( grid.getBackground(), item.getBackground( 10 ) );
  }

  @Test( expected = IllegalArgumentException.class )
  public void testSetBackgroundByIndex_DisposedFont() {
    createGridColumns( grid, 3, SWT.NONE );
    GridItem item = new GridItem( grid, SWT.NONE );
    Color background = new Color(0, 0, 255 );
    background.dispose();
    item.setBackground( 1, background );
  }

  @Test
  public void testGetForeground_Initial() {
    GridItem item = new GridItem( grid, SWT.NONE );
    assertEquals( grid.getForeground(), item.getForeground() );
  }

  @Test
  public void testGetForeground() {
    GridItem item = new GridItem( grid, SWT.NONE );
    Color foreground = new Color(0, 0, 255 );
    item.setForeground( foreground );
    assertSame( foreground, item.getForeground() );
  }

  @Test( expected = IllegalArgumentException.class )
  public void testSetForeground_DisposedFont() {
    GridItem item = new GridItem( grid, SWT.NONE );
    Color foreground = new Color(0, 0, 255 );
    foreground.dispose();
    item.setForeground( foreground );
  }

  @Test
  public void testGetForegroundByIndex() {
    createGridColumns( grid, 3, SWT.NONE );
    GridItem item = new GridItem( grid, SWT.NONE );
    Color foreground = new Color(0, 0, 255 );
    item.setForeground( 1, foreground );
    assertEquals( grid.getForeground(), item.getForeground( 0 ) );
    assertSame( foreground, item.getForeground( 1 ) );
    assertEquals( grid.getForeground(), item.getForeground( 2 ) );
  }

  @Test
  public void testGetForegroundByIndex_InvalidIndex() {
    createGridColumns( grid, 3, SWT.NONE );
    GridItem item = new GridItem( grid, SWT.NONE );
    assertEquals( grid.getForeground(), item.getForeground( 10 ) );
  }

  @Test( expected = IllegalArgumentException.class )
  public void testSetForegroundByIndex_DisposedFont() {
    createGridColumns( grid, 3, SWT.NONE );
    GridItem item = new GridItem( grid, SWT.NONE );
    Color foreground = new Color(0, 0, 255 );
    foreground.dispose();
    item.setForeground( 1, foreground );
  }

  @Test
  public void testClear() {
    GridItem item = new GridItem( grid, SWT.NONE );
    Font font = new Font( display, "Arial", 20, SWT.BOLD );
    Color background = new Color(0, 255, 0 );
    Color foreground = new Color(0, 0, 255 );
    item.setFont( font );
    item.setBackground( background );
    item.setForeground( foreground );
    grid.clear( 0, false );
    // SWT answers equal, not identical, colour and font objects for a control's defaults
    assertEquals( grid.getFont(), item.getFont() );
    assertEquals( grid.getBackground(), item.getBackground() );
    assertEquals( grid.getForeground(), item.getForeground() );
  }

  @Test
  public void testGetToolTipText() {
    GridItem item = new GridItem( grid, SWT.NONE );
    item.setToolTipText( 0, "foo" );
    assertEquals( "foo", item.getToolTipText( 0 ) );
  }

  @Test
  public void testGetToolTipText_WithColumns() {
    createGridColumns( grid, 3, SWT.NONE );
    GridItem item = new GridItem( grid, SWT.NONE );
    item.setToolTipText( 1, "foo" );
    assertNull( item.getToolTipText( 0 ) );
    assertEquals( "foo", item.getToolTipText( 1 ) );
    assertNull( item.getToolTipText( 2 ) );
  }

  @Test
  public void testGetImage_Inital() {
    GridItem item = new GridItem( grid, SWT.NONE );
    assertNull( item.getImage() );
  }

  @Test
  public void testGetImage() {
    GridItem item = new GridItem( grid, SWT.NONE );
    Image image = loadImage( display, Fixture.IMAGE1 );
    item.setImage( image );
    assertSame( image, item.getImage() );
  }

  @Test
  public void testSetImage_DisposedImage() {
    GridItem item = new GridItem( grid, SWT.NONE );
    Image image = loadImage( display, Fixture.IMAGE1 );
    image.dispose();
    // the Javadoc of Item.setImage rejects a disposed image; stock accepts it (UPSTREAM_DEFECTS.tsv)
    assertThrowsByMode( IllegalArgumentException.class, null, () -> item.setImage( image ) );
  }

  @Test
  public void testGetImageByIndex() {
    createGridColumns( grid, 3, SWT.NONE );
    GridItem item = new GridItem( grid, SWT.NONE );
    Image image = loadImage( display, Fixture.IMAGE1 );
    item.setImage( 1, image );
    assertNull( item.getImage( 0 ) );
    assertSame( image, item.getImage( 1 ) );
    assertNull( item.getImage( 2 ) );
  }

  @Test
  public void testGetImagetByIndex_InvalidIndex() {
    createGridColumns( grid, 3, SWT.NONE );
    GridItem item = new GridItem( grid, SWT.NONE );
    assertNull( item.getImage( 10 ) );
  }

  @Test( expected = IllegalArgumentException.class )
  public void testSetImageByIndex_DisposedFont() {
    createGridColumns( grid, 3, SWT.NONE );
    GridItem item = new GridItem( grid, SWT.NONE );
    Image image = loadImage( display, Fixture.IMAGE1 );
    image.dispose();
    item.setImage( 1, image );
  }

  @Test
  public void testGetChecked_Inital() {
    GridItem item = new GridItem( grid, SWT.NONE );
    assertFalse( item.getChecked() );
  }

  @Test
  public void testGetChecked() {
    GridItem item = new GridItem( grid, SWT.NONE );
    item.setChecked( true );
    assertTrue( item.getChecked() );
  }

  @Test
  public void testGetCheckedByIndex() {
    createGridColumns( grid, 3, SWT.NONE );
    GridItem item = new GridItem( grid, SWT.NONE );
    item.setChecked( 1, true );
    assertFalse( item.getChecked( 0 ) );
    assertTrue( item.getChecked( 1 ) );
    assertFalse( item.getChecked( 2 ) );
  }

  @Test
  public void testGetCheckedByIndex_InvalidIndex() {
    createGridColumns( grid, 3, SWT.NONE );
    GridItem item = new GridItem( grid, SWT.NONE );
    // Nebula: a column index without a column answers unchecked
    assertFalse( item.getChecked( 10 ) );
  }

  @Test
  public void testGetGrayed_Inital() {
    GridItem item = new GridItem( grid, SWT.NONE );
    assertFalse( item.getGrayed() );
  }

  @Test
  public void testGetGrayed() {
    GridItem item = new GridItem( grid, SWT.NONE );
    item.setGrayed( true );
    assertTrue( item.getGrayed() );
  }

  @Test
  public void testGetGrayedByIndex() {
    createGridColumns( grid, 3, SWT.NONE );
    GridItem item = new GridItem( grid, SWT.NONE );
    item.setGrayed( 1, true );
    assertFalse( item.getGrayed( 0 ) );
    assertTrue( item.getGrayed( 1 ) );
    assertFalse( item.getGrayed( 2 ) );
  }

  @Test
  public void testGetGrayedByIndex_InvalidIndex() {
    createGridColumns( grid, 3, SWT.NONE );
    GridItem item = new GridItem( grid, SWT.NONE );
    assertFalse( item.getGrayed( 10 ) );
  }

  @Test
  public void testGetCheckable_Inital() {
    createGridColumns( grid, 3, SWT.NONE );
    GridItem item = new GridItem( grid, SWT.NONE );
    assertTrue( item.getCheckable( 1 ) );
  }

  @Test
  public void testGetCheckable() {
    createGridColumns( grid, 3, SWT.NONE );
    GridItem item = new GridItem( grid, SWT.NONE );
    item.setCheckable( 1, false );
    assertFalse( item.getCheckable( 1 ) );
  }

  @Test( expected = IllegalArgumentException.class )
  public void testGetCheckableByIndex_InvalidIndex() {
    createGridColumns( grid, 3, SWT.NONE );
    GridItem item = new GridItem( grid, SWT.NONE );
    item.getCheckable( 10 );
  }

  @Test
  public void testGetCheckable_NotCheckableColumn() {
    GridColumn[] columns = createGridColumns( grid, 3, SWT.NONE );
    columns[ 1 ].setCheckable( false );
    GridItem item = new GridItem( grid, SWT.NONE );
    assertFalse( item.getCheckable( 1 ) );
  }

  @Test
  public void testGetHeight_Initial() {
    GridItem item = new GridItem( grid, SWT.NONE );
    // an item starts at the Grid's default item height
    assertEquals( grid.getItemHeight(), item.getHeight() );
  }

  @Test
  public void testGetHeight() {
    GridItem item = new GridItem( grid, SWT.NONE );
    grid.setItemHeight( 30 );
    assertEquals( 30, item.getHeight() );
  }

  @Test
  public void testGetHeight_CustomHeight() {
    GridItem item = new GridItem( grid, SWT.NONE );
    item.setHeight( 30 );
    assertEquals( 30, item.getHeight() );
    assertTrue( grid.getItemHeight() != item.getHeight() );
  }

  @Test
  public void testGetCellSize_WithoutSpan() {
    createGridColumns( grid, 5, SWT.NONE );
    createGridItems( grid, 5, 5 );
    GridItem item = grid.getRootItem( 2 );
    assertEquals( new Point( 60, grid.getItemHeight() ), item.getCellSize( 2 ) );
  }

  @Test
  public void testGetBounds() {
    createGridColumns( grid, 3, SWT.NONE );
    createGridItems( grid, 3, 3 );
    // Nebula rows are getItemHeight() high with a one-pixel line between them
    int height = grid.getItemHeight();
    assertEquals( new Rectangle( 0, height + 1, 20, height ), grid.getItem( 4 ).getBounds( 0 ) );
    assertEquals( new Rectangle( 20, height + 1, 40, height ), grid.getItem( 4 ).getBounds( 1 ) );
    assertEquals( new Rectangle( 60, height + 1, 60, height ), grid.getItem( 4 ).getBounds( 2 ) );
  }

  @Test
  public void testGetBounds_WithOffset() {
    createGridColumns( grid, 5, SWT.NONE );
    createGridItems( grid, 20, 3 );
    // Nebula sizes its scroll bars when it paints
    showGrid();
    grid.getHorizontalBar().setSelection( 30 );
    grid.setTopIndex( 12 );
    int row = grid.getItemHeight() + 1;
    int height = grid.getItemHeight();
    assertEquals( new Rectangle( -30, 2 * row, 20, height ), grid.getItem( 20 ).getBounds( 0 ) );
    assertEquals( new Rectangle( -10, 2 * row, 40, height ), grid.getItem( 20 ).getBounds( 1 ) );
    assertEquals( new Rectangle( 30, 2 * row, 60, height ), grid.getItem( 20 ).getBounds( 2 ) );
  }

  @Test
  public void testGetBounds_InvisibleItem() {
    createGridColumns( grid, 3, SWT.NONE );
    createGridItems( grid, 3, 3 );
    // Nebula: getBoundsCorrected moves the cell of a collapsed child far away (for cell editors)
    Rectangle expected = new Rectangle( -1000, -1000, 0, 0 );
    assertEquals( expected, grid.getItem( 1 ).getBoundsCorrected( 0 ) );
  }

  @Test
  public void testGetBounds_HiddenItem() {
    createGridColumns( grid, 3, SWT.NONE );
    createGridItems( grid, 20, 3 );
    showGrid();
    grid.setTopIndex( 12 );
    // Nebula: getBoundsCorrected moves a cell scrolled out of view far away (for cell editors)
    Rectangle expected = new Rectangle( -1000, -1000, 0, 0 );
    assertEquals( expected, grid.getItem( 8 ).getBoundsCorrected( 0 ) );
  }

  //////////////////
  // Helping classes
  private class LoggingListener implements Listener {

    public void handleEvent( Event event ) {
      eventLog.add( event );
    }
  }

  /*
   * Reads the item's text: in the default mode getText() requests the data; stock requests it only
   * from the getters with a column index (UPSTREAM_DEFECTS.tsv).
   */
  private void requestData( GridItem item ) {
    int before = eventLog.size();
    item.getText();
    if( !fixesUpstreamDefects() ) {
      assertEquals( before, eventLog.size() );
      item.getText( 0 );
    }
  }

  /* opens the shell and paints the Grid: Nebula sizes its scroll bars when it paints */
  private void showGrid() {
    shell.pack();
    shell.open();
    grid.redraw();
    grid.update();
    while( display.readAndDispatch() ) {
    }
  }

  @Test
  public void testDispose_ListenerReadsTheItem() {
    createGridColumns( grid, 2, SWT.NONE );
    GridItem item = new GridItem( grid, SWT.NONE );
    item.setText( 1, "foo" );
    final String[] seen = new String[ 1 ];
    final int[] row = { -2 };
    item.addDisposeListener( event -> {
      GridItem disposed = ( GridItem )event.widget;
      seen[ 0 ] = disposed.getText( 1 );
      row[ 0 ] = grid.indexOf( disposed );
    } );
    item.dispose();
    // SWT sends Dispose before it releases a widget; stock removed the item's row and data first
    // (UPSTREAM_DEFECTS.tsv)
    assertEquals( fixesUpstreamDefects() ? "foo" : "", seen[ 0 ] );
    assertEquals( fixesUpstreamDefects() ? 0 : -1, row[ 0 ] );
    assertTrue( item.isDisposed() );
    assertEquals( 0, grid.getItemCount() );
  }
}
