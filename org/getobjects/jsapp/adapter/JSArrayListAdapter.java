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

import java.util.List;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

/**
 * JSArrayListAdapter
 * <p>
 * Wraps a java.util.List object in a more convinient, Java-script like Array
 * API. Eg it adds the 'length' property (in addition to the size() method),
 * and supports indexed 'get' operations.
 */
public class JSArrayListAdapter extends JSCollectionAdapter {
  // Note: 'implements Wrapper' (aka call 'unwrap()' to unwrap)
  private static final long serialVersionUID = 1L;

  public JSArrayListAdapter() {
  }

  public JSArrayListAdapter(Scriptable _scope, Object _javaObject, Class _type){
    super(_scope, _javaObject, _type);
  }

  public JSArrayListAdapter
    (Scriptable _scope, Object _javaObject, Class _type, boolean _isAdapter)
  {
    super(_scope, _javaObject, _type, _isAdapter);
  }

  /* slots */

  @Override
  public boolean has(final int _idx, final Scriptable _start) {
    // Note: the super method always returns false
    if (log != null && log.isDebugEnabled())
      log.debug("ADAPTOR HAS?: [" + _idx + "] from " + this.javaObject);
    
    if (_idx < 0)
      return super.has(_idx, _start);
    
    if (_idx >= ((List)this.javaObject).size())
      return super.has(_idx, _start);
    
    return true; /* yes, index is in our range */
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
  public Object get(final int _idx, final Scriptable _start) {
    if (_idx < 0)
      return super.get(_idx, _start);
    
    final List l = (List)this.javaObject;
    if (_idx >= l.size())
      return super.get(_idx, _start);
    
    final Object value = l.get(_idx);
    
    if (value instanceof Scriptable ||
        value instanceof String ||
        value instanceof Number ||
        value instanceof Boolean)
    {
      /* Note: the WrapFactory somehow doesn't convert basetypes properly */
      return value; // return JS stuff as-is!
    }

    Context cx = Context.getCurrentContext();
    return cx.getWrapFactory().wrap(cx,
        this  /* scope? */,
        value /* Java object to be wrapped for JS */,
        null  /* static type? */); 
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public void put(final int _idx, final Scriptable _start, Object _value) {
    if (_idx < 0) {
      super.put(_idx, _start, _value);
      return;
    }
    
    try {
      final List l       = (List)this.javaObject;
      final int  addSize = _idx - l.size() + 1; // eg idx[5], len[3] => +3

      // hm, here we get Undefined!
      Object v;
      
      if (_value instanceof Undefined) {
        /* We keep 'Undefined' (we could map to NSNull?). Undefined is pushed
         * when the script does 'var a'. Note that the assignment (var a = 5)
         * is performed later (after checking has('a')!).
         */
        v = _value;
      }
      else {
        // TBD: should we convert numbers and such?
        v = Context.jsToJava(_value, Object.class);
      }
      
      
      /* OK, now we have the value, put or add it to the List */

      if (addSize < 0)
        l.set(_idx, v);
      else if (addSize == 1)
        l.add(v);
      else {
        /* add undefineds in missing indices, we could also do null on most
         * but not all Lists? */
        for (int i = 1; i < addSize; i++)
          l.add(Undefined.instance);
        l.add(v);
      }
    } 
    catch(RuntimeException e) {
      Context.throwAsScriptRuntimeEx(e); 
    }
  }

  @Override
  public void delete(final int _idx) {
    if (_idx < 0)
      return;
    
    try {
      List l = (List)this.javaObject;
      if (_idx >= l.size())
        return;
    
      // hm, in JS, does deleting an index really resize the array?
      l.remove(_idx);
    }
    catch(RuntimeException e) {
      Context.throwAsScriptRuntimeEx(e); 
    } 
  }
  
  @Override
  public Object[] getIds() {
    // hm, return all IDs, our JOPE API does not support that. We would need to
    // merge with super?
    //return ((Map)this.javaObject).keySet().toArray();
    if (log != null && log.isDebugEnabled())
      log.error("GETIDS on " + this.javaObject);
    return super.getIds();
  }
}
