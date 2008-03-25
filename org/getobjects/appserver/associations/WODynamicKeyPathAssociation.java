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
package org.getobjects.appserver.associations;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.foundation.NSKeyValueCodingAdditions;

/**
 * WODynamicKeyPathAssociation
 * <p>
 * This association resolves its given String using KVC to another String which
 * is then evaluated again via KVC. Weird, eh? ;-)
 */
public class WODynamicKeyPathAssociation extends WOAssociation {
  
  /* this is the association which returns the key, which is then resolved */
  protected WOAssociation keyAssociation;

  public WODynamicKeyPathAssociation(String _keyPath) {
    if (_keyPath == null || _keyPath.length() == 0) {
      log.error("invalid keypath passed to WODynamicKeyPathAssociation");
    }
    else
      this.keyAssociation = WOAssociation.associationWithKeyPath(_keyPath);
  }
  public WODynamicKeyPathAssociation(WOAssociation _baseAssoc) {
    if (_baseAssoc == null)
      log.error("no base association passed to WODynamicKeyPathAssociation");
    else
      this.keyAssociation = _baseAssoc;
  }
  
  /* accessors */
  
  @Override
  public String keyPath() {
    return "eval(" + this.keyAssociation + ")";
  }

  /* reflection */
  
  @Override
  public boolean isValueConstant() {
    return false;
  }
  
  @Override
  public boolean isValueSettable() {
    return false;
  }
  
  @Override
  public boolean isValueConstantInComponent(Object _cursor) {
    return false;
  }
  
  @Override
  public boolean isValueSettableInComponent(Object _cursor) {
    return false; // TODO: add reflection!!!
  }
  
  
  /* value */

  @Override
  public Object valueInComponent(Object _cursor) {
    if (_cursor == null || this.keyAssociation == null)
      return null;
    
    /* first we determine the key to be resolved */
    
    String key = this.keyAssociation.stringValueInComponent(_cursor);
    if (key == null)
      return null;
    
    /* then we resolve the key against the cursor */
    
    if (_cursor instanceof NSKeyValueCodingAdditions)
      return ((NSKeyValueCodingAdditions)_cursor).valueForKeyPath(key);
    
    return NSKeyValueCodingAdditions.Utility.valueForKeyPath(_cursor, key);
  }
}
