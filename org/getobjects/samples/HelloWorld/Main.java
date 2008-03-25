/*
  Copyright (C) 2006 Helge Hess

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

package org.getobjects.samples.HelloWorld;

import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOResponse;

public class Main extends WOComponent {
  
  public String headerKey = null;
  
  /* accessors */
  
  public String headerValue() {
    return this.context().request().headerForKey(this.headerKey);
  }
  
  /* response */

  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    if (false /* static generation */) {
      _r.appendContentString("Hello Component !<br />");
      _r.appendContentHTMLString(this.toString());    

      _r.appendContentString("<br /><h2>FormValues</h2>");
      _r.appendContentHTMLString(_ctx.request().formValues().toString());

      _r.appendContentString("<br /><h2>Headers</h2>");
      _r.appendContentHTMLString(_ctx.request().headers().toString());
    }
    else
      super.appendToResponse(_r, _ctx);
  }
}
