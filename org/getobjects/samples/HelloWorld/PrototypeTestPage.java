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

import java.util.Date;

import org.getobjects.appserver.core.WOActionResults;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOResponse;

public class PrototypeTestPage extends WOComponent {
  
  public String title;
  public String searchText;
  
  /* accessors */
  
  public Date now() {
    return new Date();
  }

  /* actions */
  
  public WOActionResults sayWhenAction() {
    WOResponse r = this.context().response();
    r.appendContentString
      ("<p>The time is <b>" + this.now() + "</b></p>");
    return r;
  }

  public WOActionResults addItemAction() {
    WOResponse r = this.context().response();
    r.appendContentString("<li>");
    r.appendContentHTMLString(this.title);
    r.appendContentString("</li>");
    return r;
  }

  public WOActionResults liveSearchAction() {
    WOResponse r = this.context().response();
    if (this.searchText == null || this.searchText.length() == 0) {
      r.appendContentString("<i>nothing to search for ...</i>");
    }
    else {
      r.appendContentString("<li>Should do a live search for: ");
      r.appendContentHTMLString(this.searchText);
      r.appendContentString("</li>");
    }
    return r;
  }
}
