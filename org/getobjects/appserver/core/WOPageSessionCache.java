/*
  Copyright (C) 2007 Helge Hess

  This file is part of JOPE.

  JOPE is free software; you can redistribute it and/or modify it under
  the terms of the GNU Lesser General Public License as published by the
  Free Software Foundation; either version 2, or (at your option) any
  later version.

  JOPE is distributed in the hope that it will be useful, but WITHOUT ANY
  WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
  License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with JOPE; see the file COPYING.  If not, write to the
  Free Software Foundation, 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
*/
package org.getobjects.appserver.core;

import java.util.LinkedList;
import java.util.Queue;

import org.getobjects.foundation.NSObject;

/**
 * WOPageSessionCache
 * <p>
 * LRU cache for saved page instances.
 */
public class WOPageSessionCache extends NSObject {
  
  protected Queue<WOPageSessionCachePair> cache;
  protected int cacheSize;
  
  public WOPageSessionCache(int _size) {
    this.setCacheSize(_size);
    
    this.cache = new LinkedList<WOPageSessionCachePair>();
  }
  
  /* accessors */
  
  /**
   * This sets the size of the page cache. Note that this currently flushes the
   * existing cache.
   * 
   * @param _size - the number of pages which can be stored in this cache
   */
  public void setCacheSize(int _size) {
    this.cacheSize = _size;
  }
  public int cacheSize() {
    return this.cacheSize;
  }
  
  /* operation */
  
  public boolean containsContextID(String _ctxId) {
    if (_ctxId == null)
      return false;
    if (this.cache == null) /* no space */
      return false;
    
    for (WOPageSessionCachePair pair: this.cache) {
      if (pair.contextId.equals(_ctxId))
        return true;
    }
    return false;
  }

  public WOComponent restorePageForContextID(String _ctxId) {
    if (_ctxId == null || this.cache == null)
      return null;

    for (WOPageSessionCachePair pair: this.cache) {
      if (pair.contextId.equals(_ctxId)) {
        /* move pair to end of queue */
        this.cache.remove(pair);
        this.cache.add(pair);
        
        /* ensure that the page does not refer to a context */
        pair.page.context = null;
        return pair.page;
      }
    }
    return null;
  }
  
  /**
   * Careful, because JOPE currently does not serialize() the session on save,
   * we might store the *same* WOComponent under different context-ids. And
   * changes to the component will update previous web transactions (which is
   * not what we usually want).
   * 
   * @param _page  - the page to save
   * @param _ctxId - the context-id to save the page for
   */
  public void savePageForContextID(WOComponent _page, String _ctxId) {
    if (_ctxId == null || this.cache == null)
      return;
    
    /* ensure that the page doesn't refer to the context anymore */
    _page.context = null;

    for (WOPageSessionCachePair pair: this.cache) {
      if (pair.contextId.equals(_ctxId)) {
        /* already in cache, move pair to end of queue */
        this.cache.remove(pair);
        this.cache.add(pair);
        pair.page = _page; /* might have changed or nulled */
        return;
      }
    }
    
    /* not in the cache yet, add it */
    if (this.cache.size() > this.cacheSize)
      this.cache.poll(); /* remove the head (oldest element) */
    
    this.cache.add(new WOPageSessionCachePair(_ctxId, _page));
  }
  
  
  protected static final class WOPageSessionCachePair extends NSObject {
    public String      contextId;
    public WOComponent page;
    
    public WOPageSessionCachePair(String _ctxId, WOComponent _page) {
      this.contextId = _ctxId;
      this.page      = _page;
    }
    
    @Override
    public boolean equals(Object _other) {
      if (_other == this)
        return true;
      
      if (!(_other instanceof WOPageSessionCachePair))
        return false;
      
      return ((WOPageSessionCachePair)_other).contextId.equals(this.contextId);
    }
  }

  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder d) {
    super.appendAttributesToDescription(d);
    // TODO: add some info
  }
}
