/*
  Copyright (C) 2007 Helge Hess

  This file is part of Go JMI.

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
package org.getobjects.jmi;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOApplication;
import org.getobjects.appserver.publisher.IGoLocation;
import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.ofs.OFSBaseObject;

public class JMIDocTitleFormat extends Format {
  private static final long serialVersionUID = 1L;
  protected static final Log log = LogFactory.getLog("JMI");  
  
  /* formatting */

  @Override
  public StringBuffer format
    (Object _object, StringBuffer _sb, FieldPosition _pos)
  {
    if (_object == null) {
      _sb.append("[no object]");
      return _sb;
    }
    
    if (_object instanceof String) {
      _sb.append(_object);
      return _sb;
    }
    
    if (_object instanceof OFSBaseObject) {
      OFSBaseObject doc = (OFSBaseObject)_object;
      
      String title = null;
      if (IGoLocation.Utility.isContained(doc)) {
        title = (String)doc.valueForKey("NSFileSubject");
        if (title == null || title.length() == 0)
          title = (String)doc.valueForKey("NSFileName");
      }
      else
        title = "Manage!";
      
      if (title != null) _sb.append(title);
      return _sb;
    }
    
    if (_object instanceof NSKeyValueCoding) {
      String n = (String)((NSKeyValueCoding)_object).valueForKey("name");
      if (n != null) {
        _sb.append(n);
        return _sb;
      }
    }
    
    if (_object instanceof WOApplication) {
      _sb.append("Manage!");
      return _sb;
    }
    
    log.error("don't know how to format: " + _object);
    _sb.append("[ERROR: unexpected class:" + 
               _object.getClass().getCanonicalName() + "]");
    return _sb;
  }
  
  /* parsing */

  @Override
  public Object parseObject(String _s, ParsePosition _pos) {
    /* we do not parse things */
    return null;
  }

}
