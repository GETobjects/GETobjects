/*
  Copyright (C) 2006-2008 Helge Hess

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
package org.getobjects.foundation;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

/**
 * NSKeyValueStringFormat
 * <p>
 * Uses the NSKeyValueStringFormatter to format a given object using a key
 * pattern.
 * <p>
 * Example:<pre>
 *   Format f = new NSKeyValueStringFormat("%(firstname)s %(lastname)s");
 *   System.out.println("Name is: " + f.format(person));</pre>
 */
public class NSKeyValueStringFormat extends Format {
  private static final long serialVersionUID = 1L;
  
  protected String  format;
  protected boolean requiresAll;
  
  public NSKeyValueStringFormat(String _format, boolean _requiresAll) {
    this.format = _format;
    this.requiresAll = true;
  }
  public NSKeyValueStringFormat(String _format) {
    this(_format, true);
  }
  
  @Override
  public StringBuffer format
    (Object _object, StringBuffer _sb, FieldPosition _pos)
  {
    if (this.format == null)
      return null;
    
    String s = NSKeyValueStringFormatter
      .format(this.format, _object, this.requiresAll);
    
    if (s == null)
      return null;
    
    _sb.append(s);
    return _sb;
  }

  @Override
  public Object parseObject(String arg0, ParsePosition arg1) {
    return null;
  }

}
