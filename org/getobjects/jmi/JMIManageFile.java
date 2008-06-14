/*
  Copyright (C) 2007-2008 Helge Hess

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

import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.foundation.NSException;
import org.getobjects.foundation.UString;
import org.getobjects.ofs.OFSBaseObject;

public class JMIManageFile extends JMIComponent {

  public Object saveContentAction() {
    WORequest rq = this.context().request();
    
    OFSBaseObject doc = (OFSBaseObject)this.clientObject();
    
    Object content = rq.formValueForKey("content");
    if (content == null) {
      if (rq.method().equals("PUT"))
        content = rq.content();
    }
    
    if (content == null)
      return new NSException("got no content to write");
    
    if (false) {
    System.err.println("PC: " + UString.componentsJoinedByString(
        this.context().goTraversalPath().pathToClientObject(), "/"));
    
    System.err.println("CONTENT TYPE: " + content.getClass());
    }
    
    Exception error = doc.writeContent(content);
    if (error != null)
      return error;
    
    WOResponse r = this.context().response();
    r.appendContentHTMLString(rq.formValues().toString());
    r.appendContentString("<br/>");
    r.appendContentHTMLString(rq.toString());
    r.appendContentString("<br/><pre>");
    r.appendContentHTMLString(rq.stringFormValueForKey("content"));
    r.appendContentString("</pre>");
    return r;
  }
}
