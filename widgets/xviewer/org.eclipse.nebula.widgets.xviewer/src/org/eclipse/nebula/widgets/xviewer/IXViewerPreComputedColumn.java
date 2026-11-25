/*
 * Created on Feb 25, 2015
 *
 * PLACE_YOUR_DISTRIBUTION_STATEMENT_RIGHT_HERE
 */
package org.eclipse.nebula.widgets.xviewer;

import java.util.Collection;
import java.util.Map;

/**
 * Columns that implement this interface will be given a chance to compute their values in the background before the
 * XViewer is loaded. Any item that does not have a value (null or otherwise) in preComputedValueMap will call getText
 * as normal.<br/>
 * <br/>
 * Important: XViewer.setInputXViewer(Object input) should be called instead of XViewer.setInput for precomputed columns
 * to work.<br/>
 * <br/>
 * When the XViewer's tree was created with SWT.VIRTUAL (a viewport), the background call still receives every input
 * object, but its map is used only to load the viewer and is then dropped: the column's preComputedValueMap keeps the
 * values of a bounded window of rows (those shown, their overscan and recently used ones). A row whose value is not
 * resident is computed again, in a call with that row and the following rows not resident, and a sort or filter pass
 * computes the children it orders in one call, kept for that pass only. Such calls run on the UI thread, so
 * populateCachedValues should give the same value for the same object each time it is called.
 *
 * @author Donald G. Dunne
 */
public interface IXViewerPreComputedColumn {

   /**
    * Called to get index for element for retrieval of cached text value
    */
   Long getKey(Object obj);

   /**
    * Called in a background thread to populate values in XViewerColumn.preComputedValueMap. For best performance, each
    * item in table should have an entry here and should not be null. See getText comment for cases where null is found.
    */
   void populateCachedValues(Collection<?> objects, Map<Long, String> preComputedValueMap);

   /**
    * Optional method available to make other checks during loading or otherwise change the cached value. If this method
    * returns null, populateCachedValues will be called one more time to attempt to resolve a possibly new object. If
    * null is returned, then empty string will be stored in preComputedValueMap for successive calls. This will handle
    * cases where a parent in the table expands to show an object that was not in the original objects sent to
    * populateCachedValues OR a new element is added to the viewer.
    *
    * @param key returned from getKey call
    * @param cachedValue that will be used if this method isn't overridden or returns null
    * @return string to display
    */
   String getText(Object obj, Long key, String cachedValue);
}
