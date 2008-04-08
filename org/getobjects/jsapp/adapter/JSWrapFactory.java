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
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOApplication;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOSession;
import org.getobjects.eoaccess.EOActiveRecord;
import org.getobjects.eoaccess.EOEntity;
import org.getobjects.eoaccess.EOProperty;
import org.getobjects.foundation.INSExtraVariables;
import org.getobjects.foundation.NSObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaClass;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrapFactory;

/**
 * Our customized WrapFactory. We wrap some JOPE objects in enhanced wrapper
 * objects.
 */
public class JSWrapFactory extends WrapFactory {
  protected static final Log log = LogFactory.getLog("JSWrapFactory");
  protected static final boolean isDebugOn = log.isDebugEnabled(); //valid?

  public JSWrapFactory() {
    super();
  }

  /* wrapping */
  
  @Override
  public Scriptable wrapAsJavaObject
    (Context _ctx, Scriptable _scope, Object _javaObject, Class _staticType)
  {
    // TBD: should we maintain a weak map so that wrappers get reused?
    
    /* this is called by wrap() for non-basetypes */
    if (isDebugOn)
      log.debug("wrapAsJavaObject " + _javaObject + " class " + _staticType);
    
    
    // hm, don't know, maybe all this 'instanceof' is too expensive?
    
    /* wrap specific, known NSObjects */

    if (_javaObject instanceof NSObject) {
      if (_javaObject instanceof EOActiveRecord)
        return new JSActiveRecordAdapter(_scope, _javaObject, _staticType);
      
      if (_javaObject instanceof WOComponent)
        return new JSComponentAdapter(_scope, _javaObject, _staticType);

      if (_javaObject instanceof WOContext)
        return new JSContextAdapter(_scope, _javaObject, _staticType);

      if (_javaObject instanceof WOSession)
        return new JSSessionAdapter(_scope, _javaObject, _staticType);

      if (_javaObject instanceof WOApplication)
        return new JSApplicationAdapter(_scope, _javaObject, _staticType);
      
      if (_javaObject instanceof WORequest)
        return new JSRequestAdapter(_scope, _javaObject, _staticType);
      
      if (_javaObject instanceof EOEntity)
        return new EOEntityAdapter(_scope, _javaObject, _staticType);
      
      if (_javaObject instanceof EOProperty)
        // TBD: own wrappers for relships and attrs?
        return new EOPropertyAdapter(_scope, _javaObject, _staticType);
      
      /* fallback extra vars */
      
      if (_javaObject instanceof INSExtraVariables)
        return new JSExtraVarAdapter(_scope, _javaObject, _staticType);
    }
    
    
    /* wrap Java Lists and Maps in more convenient wrappers */
    
    if (_javaObject instanceof List)
      return new JSArrayListAdapter(_scope, _javaObject, _staticType);

    if (_javaObject instanceof Map)
      return new JSMapAdapter(_scope, _javaObject, _staticType);
    
    if (_javaObject instanceof Collection) {
      // this currently just adds 'length'
      return new JSCollectionAdapter(_scope, _javaObject, _staticType);
    }
    
    
    /* The default WrapFactory wraps Class objects in NativeJavaObject,
     * not NativeJavaClass. Not sure whether that is a bug or not.
     */
    if (_javaObject instanceof java.lang.Class)
      return new NativeJavaClass(_scope, (Class)_javaObject);
    
    
    // TBD: if the object is a generic object and the requested type is a
    //      Map, we might want to extract a Map from the object?!
    
    return super.wrapAsJavaObject(_ctx, _scope, _javaObject, _staticType);
  }

}
