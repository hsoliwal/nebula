/* ******************************************************************************
 * Copyright (c) 2015 Fabian Prasser.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * Fabian Prasser - initial API and implementation
 * ****************************************************************************
 */
package de.linearbits.swt.simplebrowser;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A very basic synchronized cache.
 * Nebula adaptation: an optional bounded access-order constructor.
 * @author Fabian Prasser
 *
 * @param <T>
 */
public class HTMLCache<T> {

    /** Backing map; synchronized operations retain the original donor contract. */
    private final Map<String, T> cache;

    /** Original unbounded donor behavior. */
    public HTMLCache() {
        cache = Collections.synchronizedMap(new HashMap<String, T>());
    }

    /**
     * Nebula viewport backing-store specialization. Old entries are evicted
     * after the given number of keys, with reads updating recency.
     */
    public HTMLCache(int maxEntries) {
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be positive");
        }
        cache = Collections.synchronizedMap(new LinkedHashMap<String, T>(16, 0.75f, true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, T> eldest) {
                return size() > maxEntries;
            }
        });
    }
    
    /**
     * Clears the cache
     */
    public void clear() {
        cache.clear();
    }
    
    /**
     * Returns whether content is stored for the given URL
     * @param url
     * @return
     */
    public boolean contains(String url) {
        return cache.containsKey(url);
    }
    
    /**
     * Returns the content associated with the given URL
     * @param url
     * @return
     */
    public T get(String url) {
        return cache.get(url);
    }
    
    /**
     * Lists all content
     * @return
     */
    public Collection<T> list() {
        return cache.values();
    }

    /**
     * Puts the given element into the cache
     * @param url
     * @param element
     */
    public void put(String url, T element) {
        cache.put(url, element);
    }
}
