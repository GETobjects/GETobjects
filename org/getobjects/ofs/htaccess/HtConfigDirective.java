/*
  Copyright (C) 2008 Helge Hess

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
package org.getobjects.ofs.htaccess;

import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UString;

/**
 * HtConfigDirective
 * <p>
 * A directive is one of the statements in the Apache configuration file. It can
 * be added a plain one like:<pre>
 *   SetAppPort 2000</pre>
 * Or it can be an object of the HtConfigSection subclass:<pre>
 *   &lt;FilesMatch *.gif&gt;</pre>
 * <p>
 * This object just represents the parsed directive. No evaluation of the
 * parameters happened yet, nor did it check whether the directive is valid in
 * any given context.
 */
public class HtConfigDirective extends NSObject implements IHtConfigNode {

  protected String   name;
  protected String[] arguments;
  
  public HtConfigDirective(String _name, String[] _args) {
    this.name      = _name;
    this.arguments = _args;
  }
  
  /* accessors */
  
  public String name() {
    return this.name;
  }
  public String[] arguments() {
    return this.arguments;
  }


  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.name != null) {
      _d.append(" name=");
      _d.append(this.name);
    }
    
    if (this.arguments != null) {
      _d.append(" args=");
      _d.append(UString.componentsJoinedByString(this.arguments, ","));
    }
  }
}
