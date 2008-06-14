/*
  Copyright (C) 2008 Helge Hess <helge.hess@opengroupware.org>

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
package org.getobjects.appserver.associations;

import java.util.HashMap;
import java.util.Map;

import org.getobjects.appserver.core.IWOAssociation;
import org.getobjects.foundation.NSObject;

/**
 * WOAssociationFacade
 * <p>
 * Wraps an object and provides access to its values using a set of
 * WOAssociation objects. That is, if a value for a key is requested
 * using KVC, the facade will actually ask the WOAssociation mapped to
 * this key to return the value.
 * <p>
 * Again: this object is NOT a WOAssociation itself :-) It wraps access
 * to another object using WOAssociations.
 */
public class WOAssociationFacade extends NSObject {
  
  protected Map<String, IWOAssociation> keyToAssociation;
  protected Object  object;
  protected boolean exposeUnmappedKeys;
  
  public WOAssociationFacade(Object _object, Map<String, IWOAssociation> _map) {
    this.object             = _object;
    this.keyToAssociation   = _map;
    this.exposeUnmappedKeys = true;
  }
  
  
  /* accessors */
  
  public void setObject(final Object _object) {
    this.object = _object;
  }
  public Object object() {
    return this.object;
  }
  
  public void addMapping(final String _key, final IWOAssociation _assoc) {
    if (this.keyToAssociation == null)
      this.keyToAssociation = new HashMap<String, IWOAssociation>(16);
    this.keyToAssociation.put(_key, _assoc);
  }
  
  public void removeMapping(final String _key) {
    if (this.keyToAssociation != null)
      this.keyToAssociation.remove(_key);
  }
  
  
  /* KVC */

  @Override
  public void takeValueForKey(final Object _value, final String _key) {
    if (this.keyToAssociation != null) {
      IWOAssociation a = this.keyToAssociation.get(_key);
      if (a != null) {
        a.setValue(_value, this.object);
        return;
      }
    }
    
    if (this.exposeUnmappedKeys)
      super.takeValueForKey(_value, _key);
  }

  @Override
  public Object valueForKey(final String _key) {
    if (this.keyToAssociation != null) {
      IWOAssociation a = this.keyToAssociation.get(_key);
      if (a != null) return a.valueInComponent(this.object);
    }

    if (this.exposeUnmappedKeys)
      return super.valueForKey(_key);
    
    return null;
  }

}
