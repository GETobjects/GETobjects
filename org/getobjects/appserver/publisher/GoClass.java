/*
  Copyright (C) 2006-2014 Helge Hess

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
package org.getobjects.appserver.publisher;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UString;

/**
 * A GoClass represents a set of names as well as declared security info.
 * <p>
 * It can map to a Java class, but it doesn't need to. Eg it could be a folder
 * object which contains (and 'names') method objects.
 * <p>
 * Further a GoClass can have 'class' slots. Those are static name=>object
 * mappings accessible to the GoObject.
 */
public class GoClass extends NSObject implements IGoObject {
  protected static final Log log = LogFactory.getLog("GoClass");
  
  protected final String   name;
  protected final GoClass  goSuperClass;
  protected String[]       slotNames;
  protected Object[]       slotValues;
  protected boolean        isSealed;
  protected GoSecurityInfo securityInfo; 
  
  public GoClass
    (final String _n, final GoClass _superClass, 
     final Map<String, Object> _slots)
  {
    this.name         = _n;
    this.goSuperClass = _superClass;
    this.securityInfo = new GoSecurityInfo();
    
    this._setAllSlots(_slots);;
    this.isSealed = false; /* allow categories to be attached */
  }
  
  void _setAllSlots(final Map<String, Object> _slots) {
    if (_slots == null || _slots.size() == 0)
      return;
    
    this.slotNames  = new String[_slots.size()];
    this.slotValues = new Object[this.slotNames.length];
    
    int i = 0;
    for (final String slotName: _slots.keySet()) {
      this.slotNames[i]  = slotName;
      this.slotValues[i] = _slots.get(slotName);
      i++;
    }
  }
  
  /* accessors */
  
  public GoClass goSuperClass() {
    return this.goSuperClass;
  }
  
  public String className() {
    return this.name != null ? this.name : this.getClass().getSimpleName();
  }
  
  public GoSecurityInfo securityInfo() {
    return this.securityInfo;
  }
  
  /* lookup names for objects of this class */
  
  public Object lookupName(Object _object, String _name, IGoContext _ctx) {
    /* Note that the signature of this method is different. It doesn't
     * allow acquisition and takes the object as the first argument.
     */
    if (_name == null)
      return null;
    
    /* check static slots of class */
    
    if (this.slotNames != null) {
      for (int i = 0; i < this.slotNames.length; i++) {
        if (_name.equals(this.slotNames[i])) {
          final Object value = this.slotValues[i];
          
          /* The registered slot is not the value to be exposed, but rather
           * a factory object which creates a context specific object.
           */
          if (value instanceof IGoClassValueFactory) {
            IGoClassValueFactory factory = ((IGoClassValueFactory)value);
            
            return factory.valueForObjectInContext(_object, _name, _ctx);
          }
          
          return value;
        }
      }
    }
    
    /* check superclass */
    
    if (this.goSuperClass != null)
      return this.goSuperClass.lookupName(_object, _name, _ctx);
    
    /* not found */
    return null;
  }
  
  public boolean declaresName(String _name, boolean _checkSuperClass) {
    if (_name == null)
      return false;
    
    /* check static slots of class */
    
    if (this.slotNames != null) {
      for (int i = 0; i < this.slotNames.length; i++) {
        if (_name.equals(this.slotNames[i]))
          return true;
      }
    }
    
    /* check superclass */
    
    if (_checkSuperClass && this.goSuperClass != null)
      return this.goSuperClass.declaresName(_name, true /* check superclass */);
    
    /* not found */
    return false;
  }
  
  public String[] slotNames() {
    return this.slotNames;
  }
  
  public void setValueForSlot(Object _value, String _name) {
    if (this.isSealed) {
      log.error("attempt to set value in sealed class: " + this);
      return;
    }
    if (_name == null) {
      log.error("attempt to set value for unnamed slot: " + this);
      return;
    }
    
    /* check for existing slots */
    
    if (this.slotNames != null) {
      for (int i = 0; i < this.slotNames.length; i++) {
        if (_name.equals(this.slotNames[i])) {
          this.slotValues[i] = _value;
          return;
        }
      }
    }
    
    /* definition of a new slot */
    
    if (this.slotNames != null) {
      int      len     = this.slotNames.length;
      String[] nNames  = new String[len + 1];
      Object[] nValues = new Object[len + 1];
      
      System.arraycopy(this.slotNames,  0, nNames,  0, len);
      System.arraycopy(this.slotValues, 0, nValues, 0, len);
      
      nNames[len]     = _name;
      nValues[len]    = _value;
      this.slotNames  = nNames;
      this.slotValues = nValues;
    }
    else {
      this.slotNames  = new String[] { _name  };
      this.slotValues = new Object[] { _value };
    }
  }
  
  public Object valueForSlot(String _name) {
    if (this.slotNames != null) {
      for (int i = 0; i < this.slotNames.length; i++) {
        if (_name.equals(this.slotNames[i]))
          return this.slotValues[i];
      }
    }
    return null;
  }
  
  /* Using a GoClass as a GoObject */

  public Object lookupName(String _name, IGoContext _ctx, boolean _aquire) {
    log.warn("tried to access GoClass as an GoObject: " + _name);
    return null;
  }
  
  public GoClass goClass() {
    return null; /* the metaclass of a GoClass, not sure whether we need it */
  }


  /* description */
  
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.name != null) {
      _d.append(' ');
      _d.append(this.name);
    }
    
    if (this.slotNames != null && this.slotNames.length > 0) {
      _d.append(" names=");
      _d.append(UString.componentsJoinedByString(this.slotNames, ","));
    }
    
    if (this.goSuperClass != null) {
      _d.append(" super=");
      
      boolean isFirst = true;
      for (GoClass c = this.goSuperClass(); c != null; c = c.goSuperClass()) {
        if (isFirst) isFirst = false;
        else _d.append(',');
        
        _d.append(c.name);
        if (c.slotNames != null && c.slotNames.length > 0) {
          _d.append('[');
          _d.append(UString.componentsJoinedByString(c.slotNames, ","));
          _d.append(']');
        }
      }
    }
  }
}
