/*
  Copyright (C) 2006 Helge Hess

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
package org.getobjects.ognl;

import java.util.Map;

import ognl.OgnlException;
import ognl.PropertyAccessor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.foundation.NSValidation;

/*
 * NSKeyValueCodingPropertyAccessor
 * 
 * Uses NSKeyValueCoding.Utility to retrieve values.
 */
public class NSKeyValueCodingPropertyAccessor implements PropertyAccessor {

  protected static final Log compLog = LogFactory.getLog("OGNL");

  public void setProperty
    (Map _context, Object _target, Object _name, Object _value)
    throws OgnlException
  {
    if (_target == null)
      return;

    try {
      String key = (String)_name;
      
      if (_target instanceof NSValidation)
        /* TODO: apparently this is done incorrectly? */
        _value = ((NSValidation)_target).validateValueForKey(_value, key);
      
      NSKeyValueCoding.Utility.takeValueForKey(_target, _value, key);
    }
    catch (Exception e) {
      throw new OgnlException(_name != null ? _name.toString() : null, e);
    }
  }

  public Object getProperty(Map _context, Object _target, Object _name)
    throws OgnlException
  {
    if (_target == null)
      return null;
    
    try {
      return NSKeyValueCoding.Utility.valueForKey(_target, (String)_name);
    }
    catch (Exception e) {
      throw new OgnlException(_name != null ? _name.toString() : null, e);
    }
  }
}
