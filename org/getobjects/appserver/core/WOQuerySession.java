/*
  Copyright (C) 2007-2008 Helge Hess

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.getobjects.foundation.NSObject;

/**
 * WOQuerySession
 * <p>
 * This object represents objects which got unarchived from the form values,
 * eg in the query parameters of a string, eg:<pre>
 *   /myAction?dg_batchindex=2</pre>
 * <p>
 * The query session can be manipulated in templates using the
 * WOChangeQuerySession dynamic element.
 */
public class WOQuerySession extends NSObject {
  /*
   * TBD: document more
   * - usually subclasses and handcoded? (eg display group setup)
   * - why is it a global (WOContext) object instead of component-local
   *   - should the state depend on the component and get unarchived by the comp
   *     - or by the Jo callable object?
   *     - well, *session* means those are parameters which are not component/
   *       resource specific. You can still use WOChangeQuerySession to handle
   *       component local things.
   *   - TBD: can we add specific component support?
   *     - ie WOSession can store components based on the context
   */
  protected WOContext           context;
  protected Map<String, Object> qpSession;
  protected List<String>        qpSessionKeys;
  
  public WOQuerySession(WOContext _ctx) {
    super();
    this.context = _ctx;
  }
  
  /* extract values */

  /**
   * This method instantiates and prepares the query session.
   * 
   * Note: you should not call this anymore in other code but rather use the
   * @see addToQueryDictionary() method.
   */
  protected Map<String, Object> allQuerySessionValues() {
    if (this.qpSession != null)
      return this.qpSession;
    
    this.qpSession = new HashMap<String, Object>(16);
    
    /* Next we copy all form values into the query session map. Note sure
     * whether copying everything is the best idea, we should probably just
     * refer to the WORequest?
     */
    WORequest rq = this.context.request();
    if (rq != null && rq.hasFormValues()) {
      Map<String, Object[]> fv = rq.formValues();
      
      /* we flatten single-object arrays and remove empty arrays */
      for (String fn: fv.keySet()) {
        Object[] v = fv.get(fn);
        
        if (v == null)     continue;
        if (v.length == 0) continue;
        this.qpSession.put(fn, (v.length == 1) ? v[0] : v);
      }
    }
    return this.qpSession;
  }
  
  /* active keys */
  
  public void setActiveQuerySessionKeys(List<String> _keys) {
    this.qpSessionKeys = _keys;
  }
  public List<String> activeQuerySessionKeys() {
    return this.qpSessionKeys;
  }
  
  public boolean hasActiveQuerySessionValues() {
    /* Note: do not access ivars in this method since activeQuerySessionKeys
     *       might be overridden in a subclass.
     */
    List<String> keys = this.activeQuerySessionKeys();
    return keys != null ? keys.size() > 0 : false;
  }
  
  /* operation */
  
  public Map<String, Object> addToQueryDictionary(Map<String, Object> _qd) {
    if (this.hasActiveQuerySessionValues()) {
      Map<String, Object> qsv = this.allQuerySessionValues();
      for (String k: this.activeQuerySessionKeys())
        _qd.put(k, qsv.get(k));
    }
    return _qd;
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.qpSessionKeys != null) {
      _d.append(" keys=");
      _d.append(this.qpSessionKeys);
    }
    
    if (this.qpSession != null) {
      _d.append(" #values=");
      _d.append(this.qpSession.size());
    }
  }  
}
