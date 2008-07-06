/*
 * Copyright (C) 2008 Helge Hess <helge.hess@opengroupware.org>
 *
 * This file is part of Go.
 *
 * Go is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2, or (at your option) any later version.
 *
 * Go is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Go; see the file COPYING. If not, write to the Free Software
 * Foundation, 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.getobjects.jsapp;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UString;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

/**
 * JSKeyValueCodingScope
 * <p>
 * This is a combination of a *sealed* JavaScript scope plus a cache of KVC
 * wrappers.
 * Eg its used in the JSCachedKVCScriptScope class.
 */
public abstract class JSKeyValueCodingScope extends NSObject {

  public final ScriptableObject scope; /* our scope */
  
  public JSKeyValueCodingScope(final ScriptableObject _scope) {
    this.scope = _scope;
  }
  
  /* factory */
  
  public static JSKeyValueCodingScope wrap(final ScriptableObject _scope) {
    if (_scope == null)
      return null;
    
    final Map<String, Object> kvc = extractKeyValueCodingMap(_scope);
    
    if (kvc == null)
      return new JSKeyValueCodingEmptyScope(_scope);
    
    // TBD: a special imp for small sets to avoid a hashmap
    
    return new JSKeyValueCodingMapScope(_scope, kvc); 
  }

  
  /* KVC */
  
  public abstract Object valueForKeyInScope(String _key, Scriptable _thisScope);
  public abstract Set<String> keySet();
  
  
  /**
   * Walks over the 'getIds()' of the scope. If the ID is a String, we check
   * whether its a Callable. If so, we put the callable into the Map,
   * if not, we convert the JavaScript object into a Java one and store it in
   * the Map.
   * 
   * @param _scope - the scope to scan for KVC keys
   * @return a Map containing the KVC bindings of the given slot
   */
  protected static Map<String, Object> extractKeyValueCodingMap
    (final ScriptableObject _scope)
  {
    if (_scope == null)
      return null;
    
    final Map<String, Object> keyToObject = new HashMap<String, Object>();

    // TBD: this excludes slots which are not enumerated, which should be OK
    for (Object slotId: _scope.getIds()) {
      if (!(slotId instanceof String))
        continue;
      
      String slotIdS = (String)slotId;
      
      /* Using the scope as the start is not necessarily the best idea,
       * but should be OK here.
       */
      Object v = _scope.get(slotIdS, _scope);
      if (v == null || v == Scriptable.NOT_FOUND)
        continue; /* does not contain the slot, which is OK */
      
      
      /* OK, got a slot value, now it gets interesting. If its a function,
       * we store it. It must be evaluated each time.
       * If its *not*, we already unwrap the value and cache it!
       */
      if (v instanceof Callable) {
        // TBD: do we need to protect 'set' accessors? Currently we could call
        //      valueForKey("setAddress").
        // TBD: should we support getX style GET accessors? Or maybe use only
        //      getX style ones (would allow us to have an ivar slot with the
        //      same name). => no we need no-get ones for actions (doIt())
        /* Note: we cannot bind the function yet, because the binder is per
         *       component. (hm, maybe we need per-object caches?)
         */
        //System.err.println("MAP " + slotIdS + " to " + v);
        keyToObject.put(slotIdS, v);
      }
      else {
        /* its a regular variable slot */
        
        if (v instanceof Undefined)
          continue; /* we do not expose JavaScript Undefined values */

        /* Note: this seems to convert Undefined to a String */
        v = Context.jsToJava(v, Object.class);
        
        /* Note: we do not distinguish between missing and null */
        keyToObject.put(slotIdS, v);
        //System.err.println("  key[" + slotIdS + "]GOT VALUE: " + v);
      }
    }
    
    return (keyToObject.size() == 0) ? null : keyToObject;
  }

  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);

    final Set<String> keys = this.keySet();
    if (keys != null && keys.size() > 0) {
      _d.append(" keys=");
      _d.append(UString.componentsJoinedByString(keys,","));
    }
    else
      _d.append(" no-keys");
    
    if (this.scope != null) {
      _d.append(" scope[");
      _d.append(this.scope.getClass().getSimpleName());
      _d.append(']');
    }
    else
      _d.append(" no-scope");
  }
  
  
  /* class cluster */
  
  private static class JSKeyValueCodingMapScope extends JSKeyValueCodingScope {
    
    final protected Map<String, Object> keyToObject;
    
    public JSKeyValueCodingMapScope
      (final ScriptableObject _scope, final Map<String, Object> _kvc)
    {
      super(_scope);
      this.keyToObject = _kvc;
    }

    @Override
    public Set<String> keySet() {
      return this.keyToObject.keySet();
    }
    
    /**
     * Retrieve the KVC key for the given object '_thisScope'.
     */
    @Override
    public Object valueForKeyInScope(String _key, Scriptable _thisScope) {
      if (this.keyToObject == null || _key == null)
        return null;
      
      /* Using the scope as the start is not necessarily the best idea,
       * but should be OK here.
       */
      Object v = this.keyToObject.get(_key);
      if (v == null)
        return null;
      
      // hh test
      // maybe the lookup chain is broken? But the thing below does not help
      //v = this.scope.get(_key, _thisScope);
      
      /* check whether the value is a getter */

      if (v instanceof Callable) {
        /*
         * Notes:
         * - do we need to bind the function? Doesn't look like!
         *   - bindings is not necessary because JSComp.valueForKey() already
         *     calls the function with a proper scope/this (_thisScope)
         * - we could pass in more parameters, eg shared-scope or the key
         */
        Callable c = (Callable)v;
        
        /*
        System.err.println("CALL " + _key + " IN scope: ");
        System.err.println("  " + _thisScope);
        System.err.println("  P: " + _thisScope.getPrototype());
        System.err.println("  F: " + c);
         */
        //if (!(v instanceof JSBoundFunction) && (v instanceof Function)) {
        //  v = new JSBoundFunction(_thisScope, (Function)v);
        //}
        
        // TBD: this is strange, we cannot use _thisScope as the scope. If we
        //      do, system stuff like 'string.charAt()' does not work.
        v = c.call(Context.getCurrentContext(),
            this.scope /* scope (where to lookup variables) */,
            _thisScope /* this  */,
            JSUtil.emptyArgs);
      }


      if (v instanceof Undefined)
        v = null; /* we do not expose JavaScript Undefined values */
      else {
        /* Note: this seems to convert Undefined to a String */
        v = Context.jsToJava(v, Object.class);
      }
      //System.err.println("  GOT VALUE: " + v);

      return v;
    }
  }
  
  
  /* empty scope (no KVC keys) */
  
  private static class JSKeyValueCodingEmptyScope extends JSKeyValueCodingScope {

    public JSKeyValueCodingEmptyScope(final ScriptableObject _scope) {
      super(_scope);
    }
    
    @Override
    public Set<String> keySet() {
      return null;
    }
    
    @Override
    public Object valueForKeyInScope(String _key, Scriptable _thisScope) {
      return null; // contains no KVC keys
    }
  }
}
