/*
  Copyright (C) 2006-2007 Helge Hess

  This file is part of Go.

  Go is free software; you can redistribute it and/or modify it under
  the terms of the GNU Lesser General Public License as published by the
  Free Software Foundation; either version 2, or (at your option) any
  later version.

  Go is distributed in the hope that it will be useful, but WITHOUT ANY
  WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
  License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with Go; see the file COPYING.  If not, write to the
  Free Software Foundation, 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
*/

package org.getobjects.appserver.core;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.getobjects.foundation.NSClassLookupContext;

/**
 * WOCompoundResourceManager
 * <p>
 * This resource manager just keeps an array of other resource managers. If its
 * asked for a resource, it traverses the array of those until one manager can
 * return the requested resource.
 */
public class WOCompoundResourceManager extends WOResourceManager {
  
  protected WOResourceManager[]              resourceManagers;
  protected ConcurrentHashMap<String, Class> classCache;
  
  protected AtomicReference<Map<String, Class>> dynElemClassCache;
  
  public WOCompoundResourceManager(List<WOResourceManager> _rms, boolean _c) {
    super(_c);
    this.resourceManagers  = _rms.toArray(new WOResourceManager[0]);
    this.classCache        = new ConcurrentHashMap<String, Class>(32);
    
    /* We assume that PUTs are rare after a warmup phase, hence we don't want
     * to synchronize the full Map. */
    // TBD: we could prefill the cache in App.init
    this.dynElemClassCache = new AtomicReference<Map<String, Class>>();
  }
  
  
  /* accessors */
  
  public WOResourceManager[] resourceManagers() {
    return this.resourceManagers;
  }
  
  
  /* component definitions */

  @Override
  public IWOComponentDefinition definitionForComponent
    (String _name, String[] _langs, WOResourceManager _clsctx)
  {
    for (WOResourceManager rm: this.resourceManagers) {
      IWOComponentDefinition cdef;
      
      if ((cdef = rm.definitionForComponent(_name, _langs, _clsctx)) != null)
        return cdef;
    }
    
    return null;
  }
  
  /* resources */
  
  @Override
  public URL urlForResourceNamed(String _name, String[] _ls) {
    for (WOResourceManager rm: this.resourceManagers) {
      URL url;
      
      if ((url = rm.urlForResourceNamed(_name, _ls)) != null)
        return url;
    }
    
    return null;  
  }
  @Override
  public InputStream inputStreamForResourceNamed(String _name, String[] _ls) {
    for (WOResourceManager rm: this.resourceManagers) {
      InputStream is;
      
      if ((is = rm.inputStreamForResourceNamed(_name, _ls)) != null)
        return is;
    }
    
    return null;  
  }
  
  /* strings */
  
  @Override
  public ResourceBundle stringTableWithName(String _name, String _fwname, 
                                            String[] _langs)
  {
    for (WOResourceManager rm: this.resourceManagers) {
      ResourceBundle rb;
      
      if ((rb = rm.stringTableWithName(_name, _fwname, _langs)) != null)
        return rb;
    }
    
    return null;
  }
  
  /* class lookup */
  
  private static Class NULL_MARKER = WOCompoundResourceManager.class;
  
  @Override
  public Class lookupClass(String _name) {
    if (log.isDebugEnabled())
      log.debug("lookup class in compound: " + _name);
    if (_name == null) return null;
    
    /* Note: ConcurrentHashMap cannot store null values */
    Class cls = this.classCache.get(_name);
    if (cls != null) return cls == NULL_MARKER ? null : cls;
    
    for (NSClassLookupContext rm: this.resourceManagers) {
      if ((cls = rm.lookupClass(_name)) != null)
        break;
    }
    
    this.classCache.put(_name, cls != null ? cls : NULL_MARKER);
    
    if (log.isDebugEnabled()) {
      if (cls == null)
        log.debug("did not find class in compound: " + _name);
    }
    return cls;
  }

  @Override
  public Class lookupComponentClass(String _name) {
    if (log.isDebugEnabled())
      log.debug("lookup compclass in compound: " + _name);
    if (_name == null) return null;
    
    /* Note: ConcurrentHashMap cannot store null values */
    Class cls = this.classCache.get(_name);
    if (cls != null) return cls == NULL_MARKER ? null : cls;
    
    for (WOResourceManager rm: this.resourceManagers) {
      if ((cls = rm.lookupComponentClass(_name)) != null)
        break;
    }
    
    // TBD: same cache for classes and components?
    this.classCache.put(_name, cls != null ? cls : NULL_MARKER);
    
    if (log.isDebugEnabled()) {
      if (cls == null)
        log.debug("did not find compclass in compound: " + _name);
    }
    return cls;
  }
  
  @Override
  public Class lookupDynamicElementClass(String _name) {
    if (log.isDebugEnabled())
      log.debug("lookup dynclass in compound: " + _name);
    if (_name == null) return null;
    
    /* Note: ConcurrentHashMap cannot store null values */
    Map<String, Class> cache = this.dynElemClassCache.get();
    Class cls = cache != null ? cache.get(_name) : null;
    if (cls != null) return cls == NULL_MARKER ? null : cls;
    
    for (WOResourceManager rm: this.resourceManagers) {
      if ((cls = rm.lookupDynamicElementClass(_name)) != null)
        break;
    }
    
    /* Note: we cannot use the regular class cache because we need to ensure
     *       that the element is a WODynamicElement subclass. (well, not w/o
     *       additional checks, so we can use a separate map in the first
     *       place).
     */
    cache = (cache != null)
      ? new HashMap<String, Class>(cache)
      : new HashMap<String, Class>(1);

    cache.put(_name, cls != null ? cls : NULL_MARKER);
    this.dynElemClassCache.set(cache);
    
    if (log.isDebugEnabled()) {
      if (cls == null)
        log.debug("did not find dynclass in compound: " + _name);
    }
    return cls;
  }
  
  @Override
  public Class lookupDirectActionClass(String _name) {
    if (log.isDebugEnabled())
      log.debug("lookup daclass in compound: " + _name);
    if (_name == null) return null;
    
    /* Note: ConcurrentHashMap cannot store null values */
    Class cls = this.classCache.get(_name);
    if (cls != null) return cls == NULL_MARKER ? null : cls;
    
    for (WOResourceManager rm: this.resourceManagers) {
      if ((cls = rm.lookupDirectActionClass(_name)) != null)
        break;
    }
    
    // TBD: same cache for classes and components?
    this.classCache.put(_name, cls != null ? cls : NULL_MARKER);
    
    if (log.isDebugEnabled()) {
      if (cls == null)
        log.debug("did not find daclass in compound: " + _name);
    }
    return cls;
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.resourceManagers != null)
      _d.append(" #rms=" + this.resourceManagers.length);
    else
      _d.append(" no-rms");
  }
}
