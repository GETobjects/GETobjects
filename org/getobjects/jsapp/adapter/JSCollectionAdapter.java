/*
 * Copyright (C) 2007-2008 Helge Hess
 *
 * This file is part of JOPE.
 *
 * JOPE is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2, or (at your option) any later version.
 *
 * JOPE is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JOPE; see the file COPYING. If not, write to the Free Software
 * Foundation, 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.getobjects.jsapp.adapter;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;

public class JSCollectionAdapter extends NativeJavaObject {
  // Note: 'implements Wrapper' (aka call 'unwrap()' to unwrap)
  private static final long serialVersionUID = 1L;
  protected static final Log log = LogFactory.getLog("JSBridge");

  public JSCollectionAdapter() {
  }

  public JSCollectionAdapter(Scriptable _scope, Object _javaObject, Class _type){
    super(_scope, _javaObject, _type);
  }

  public JSCollectionAdapter
    (Scriptable _scope, Object _javaObject, Class _type, boolean _isAdapter)
  {
    super(_scope, _javaObject, _type, _isAdapter);
  }

  /* slots */

  @Override
  public boolean has(final String _name, final Scriptable _start) {
    /* Note: Its important to implement that. Rhino issues this when checking
     *       for a property (just having 'get' is insufficient).
     */
    if (log != null && log.isDebugEnabled())
      log.debug("ADAPTOR HAS?: " + _name + " from " + this.javaObject);
    
    if (_name != null && _name.equals("length"))
      return true;
    
    return super.has(_name, _start);
  }
  
  /**
   * Get the value of a property. First check the superclass for methods of the
   * Java class (will be returned as Callables), then check for WOComponent
   * extra variables.
   * <p>
   * The values that may be returned are limited to the following:
   * <UL>
   *   <LI>java.lang.Boolean objects</LI>
   *   <LI>java.lang.String objects</LI>
   *   <LI>java.lang.Number objects</LI>
   *   <LI>org.mozilla.javascript.Scriptable objects</LI>
   *   <LI>null</LI>
   *   <LI>The value returned by Context.getUndefinedValue()</LI>
   *   <LI>NOT_FOUND</LI>
   * </UL>
   */
  @Override
  public Object get(String _name, Scriptable _start) {
    if (_name != null) {
      if (_name.equals("length"))
        return ((Collection)this.javaObject).size();
    }
    
    return super.get(_name, _start); 
  }
}
