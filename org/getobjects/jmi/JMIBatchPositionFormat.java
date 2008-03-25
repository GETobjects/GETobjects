/*
  Copyright (C) 2007 Helge Hess

  This file is part of JOPE JMI.

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
package org.getobjects.jmi;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WODisplayGroup;

public class JMIBatchPositionFormat extends Format {
  private static final long serialVersionUID = 1L;
  protected static final Log log = LogFactory.getLog("JMI");  
  
  /* formatting */

  @Override
  public StringBuffer format
    (Object _object, StringBuffer _sb, FieldPosition _pos)
  {
    if (_object == null)
      return null;
    
    if (_object instanceof String) {
      _sb.append(_object);
      return _sb;
    }
    
    if (_object instanceof WODisplayGroup)
      return this.formatDisplayGroup((WODisplayGroup)_object, _sb, _pos);
    
    
    log.error("don't know how to format: " + _object);
    _sb.append("[ERROR: unexpected class " + _object.getClass() + "]");
    return null;
  }
  
  protected StringBuffer formatDisplayGroup
    (WODisplayGroup _dg, StringBuffer _sb, FieldPosition _pos)
  {
    // TODO: localize
    
    int batchCount = _dg.batchCount();
    if (batchCount < 2) /* add nothin' */
      return _sb;
    
    _sb.append("Page ");
    _sb.append(_dg.currentBatchIndex());
    _sb.append(" of ");
    _sb.append(batchCount);
    
    return _sb;
  }
  
  /* parsing */

  @Override
  public Object parseObject(String _s, ParsePosition _pos) {
    /* we do not parse things */
    return null;
  }

}
