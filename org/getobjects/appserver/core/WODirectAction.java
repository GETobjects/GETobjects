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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSJavaRuntime;

/**
 * A WODirectAction object is pretty much like a servlet, it can accept
 * requests and is responsible for producing a result. The result can be
 * either a WOResponse or a WOComponent or anything else which can be
 * rendered by Go.
 * 
 * @author helge
 */
public class WODirectAction extends WOAction {
  protected static final Log daLog = LogFactory.getLog("WODirectAction");
  
  static Class[]  emptyClassArray  = new Class[0];
  static Object[] emptyObjectArray = new Object[0];
  
  public WODirectAction(WOContext _ctx) {
    super(_ctx);
  }
  
  /**
   * Just calls the matching static method which contains the actual
   * implementation. Subclasses can override the instance method to
   * implement a custom behaviour (eg clean up the name prior passing it
   * to the super implementation).
   * 
   * @param _name - the name of the action to invoke
   * @return the result, eg a WOComponent or WOResponse
   */
  public Object performActionNamed(String _name) {
    return WODirectAction.performActionNamed(this, _name, this.context());
  }
  
  /**
   * Implements the "direct action" request handling / method lookup. This is
   * a static method because its reused by WOComponent.
   * <p>
   * This implementation checks for a method with a name which ends in "Action"
   * and which has no parameters, eg:<pre>
   *   defaultAction
   *   viewAction</pre>
   * Hence only methods ending in "Action" are exposed (automagically) to the
   * web.
   * 
   * @param _o    - the WODirectAction or WOComponent
   * @param _name - the name of the action to invoke
   * @param _ctx  - the WOContext to run the action in
   * @return the result, eg a WOComponent or WOResponse
   */
  public static Object performActionNamed
    (Object _o, String _name, WOContext _ctx)
  {
    if (_o == null || _name == null)
      return null;
    
    Method m = NSJavaRuntime.NSMethodFromString
      (_o.getClass() , _name + "Action", emptyClassArray);
    
    if (m == null) {
      /* Hm, warn can be turned off since handleMissingAction could perform
       * a sensible implementation. Or should handleMissingAction() do the
       * log?
       */
      daLog.warn("did not find method for DA: " + _name +
                 " (object=" + _o + ")");
      return _ctx.application().handleMissingAction(_name, _ctx);
    }
    
    Object results;
    try {
      results = m.invoke(_o, emptyObjectArray);
    }
    catch (InvocationTargetException ite) {
      results = _ctx.application().handleException(ite.getCause(), _ctx);
    }
    catch (IllegalAccessException iae) {
      // TODO: improve logging
      daLog.error("invalid access for DA, must be public: " + iae);
      results = _ctx.application().handleMissingAction(_name, _ctx);      
    }
    return results;
  }
  
  /**
   * This method gets triggered if the direct action URL contained no action
   * name. The default implementation returns the Main component.
   * 
   * @return the result of the action
   */
  public Object defaultAction() {
    return this.pageWithName("Main");
  }
  
  
  /* form values */
  
  /**
   * Iterates over the given keys and invokes takeValueForKey for each key,
   * using a similiar named form values as the value.
   */
  public void takeFormValueArraysForKeyArray(String[] _keys) {
    WORequest r = this.request();

    for (String k: _keys)
      this.takeValueForKey(r.formValuesForKey(k), k);
  }
  public void takeFormValueArraysForKeyArray(Collection<String> _keys) {
    this.takeFormValueArraysForKeyArray((String[])_keys.toArray());
  }
}
