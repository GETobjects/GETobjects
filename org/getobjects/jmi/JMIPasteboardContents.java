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

import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UString;

public class JMIPasteboardContents extends NSObject {
  
  protected String   verb;
  protected String   basePath;
  protected String[] ids;
  
  public JMIPasteboardContents(String _verb, String _basePath, String[] _ids) {
    this.verb     = _verb;
    this.basePath = _basePath;
    this.ids      = _ids;
  }

  public static JMIPasteboardContents contentsForString(String _s) {
    if (_s == null || _s.length() == 0)
      return null;
    
    String[] parts = _s.split("\\|");
    if (parts == null || parts.length < 3)
      return null;

    String[] ids = new String[parts.length - 2];
    System.arraycopy(parts, 2, ids, 0, ids.length);
    
    return new JMIPasteboardContents(parts[0], parts[1], ids);
  }
  
  /* accessors */
  
  public String verb() {
    return this.verb;
  }
  public String basePath() {
    return this.basePath;
  }
  
  public String[] ids() {
    return this.ids;
  }
  public boolean hasIds() {
    return this.ids != null && this.ids.length > 0;
  }
  
  public String[] pathes() {
    if (this.ids == null)
      return null;
    
    String[] pathes = new String[this.ids.length];
    for (int i = 0; i < this.ids.length; i++)
      pathes[i] = this.basePath + "/" + this.ids[i];
    return pathes;
  }
  
  /* external */
  
  public String stringValue() {
    if (this.verb == null || this.basePath == null)
      return null;
    if (this.ids == null || this.ids.length < 1)
      return null;
    
    StringBuilder sb = new StringBuilder(512);
    sb.append(this.verb);
    sb.append("|");
    sb.append(this.basePath);
    for (int i = 0; i < this.ids.length; i++) {
      sb.append("|");
      sb.append(this.ids[i]);
    }
    
    return sb.toString();
  }
  
  /* description */

  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.verb     != null) _d.append(" verb=" + this.verb);
    if (this.basePath != null) _d.append(" base=" + this.basePath);
    _d.append(" ids=");
    _d.append(UString.componentsJoinedByString(this.ids, ","));
  }
}
