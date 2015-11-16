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

package org.getobjects.appserver.elements;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOMessage;
import org.getobjects.appserver.core.WOResponse;

/*
 * WOSetHeader
 * 
 * This element can set/add a header field using -setHeader:forKey:. Usually its
 * used with a WOResponse (context.response is the default 'object'), but can be
 * used with arbitrary objects implementing the same API (eg context.request).
 * 
 * Sample:
 *   ChangeContentType: WOSetHeader {
 *     header = "content-type";
 *     value  = "text/plain";
 *   }
 *
 * Renders:
 *   This element doesn't render any HTML. It adds a header to the contexts
 *   response.
 * 
 * Bindings:
 *   header|key|name [in] - string
 *   value           [in] - object
 *   addToExisting   [in] - boolean   (use appendHeader instead of setHeader)
 *   object          [in] - WOMessage (defaults to context.response)
 */
public class WOSetHeader extends WODynamicElement {
  
  protected WOAssociation object;
  protected WOAssociation header;
  protected WOAssociation value;
  protected WOAssociation addToExisting;

  public WOSetHeader(String _name, Map<String, WOAssociation> _assocs,
                     WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.object        = grabAssociation(_assocs, "object");
    this.value         = grabAssociation(_assocs, "value");
    this.addToExisting = grabAssociation(_assocs, "addToExisting");
    
    /* check variants for 'header' binding ... */
    if ((this.header = grabAssociation(_assocs, "header")) == null) {
      if ((this.header = grabAssociation(_assocs, "key")) == null) {
        if ((this.header = grabAssociation(_assocs, "name")) == null) {
          // TODO: improve logging
          System.err.println("ERROR: no header binding in WOSetHeader?");
        }
      }
    }
  }
  
  /* responder */
  
  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    Object cursor = _ctx.cursor();
    
    boolean doAdd = false;
    if (this.addToExisting != null)
      doAdd = this.addToExisting.booleanValueInComponent(cursor);

    String k = null, v = null;
    if (this.header != null) k = this.header.stringValueInComponent(cursor);
    if (k == null)
      return;

    if (this.value != null) {
      Object ov = this.value.valueInComponent(cursor);

      if (ov != null) {
        if (ov instanceof String)
          v = (String)ov;
        else if (ov instanceof Date)
          v = WOMessage.httpFormatDate((Date)ov);
        else if (ov instanceof GregorianCalendar)
          v = WOMessage.httpFormatDate((GregorianCalendar)ov);
        else
          v = ov.toString();
      }
    }

    /* determine target object */
    
    WOMessage lObject;
    if (this.object != null) {
      Object llObject = this.object.valueInComponent(cursor);
      if (llObject instanceof String) {
        String s = (String)llObject;
        
        if (s.equals("response"))
          lObject = _ctx.response();
        else if (s.equals("request"))
          lObject = _ctx.request();
        else {
          _r.appendContentHTMLString
            ("[ERROR: incorrect 'object' binding in WOSetHeader]");
          lObject = null;
        }
      }
      else if (llObject != null)
        lObject = (WOMessage)llObject;
      else
        lObject = null;
    }
    else
      lObject = _ctx.response();
    
    /* apply */
    
    if (doAdd) {
      if (v != null)
        lObject.appendHeader(v, k);
      else
        lObject.removeHeadersForKey(k);
    }
    else if (v != null)
      lObject.setHeaderForKey(v, k);
    else
      lObject.removeHeadersForKey(k);
  }
}
