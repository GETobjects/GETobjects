/*
  Copyright (C) 2006-2008 Helge Hess

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

package org.getobjects.appserver.templates;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.IWOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;

/**
 * WOTemplate
 * <p>
 * This is just a simple element wrapper used for the root element of a 
 * template. It can transport additional information like the URL the
 * template is living at.
 */
public class WOTemplate extends WOElement {
  protected static final Log log = LogFactory.getLog("WOTemplates");
  
  protected final URL url;
  protected WOElement rootElement;
  protected Map<String, WOSubcomponentInfo> subcomponentInfos;
  
  public WOTemplate(URL _url, WOElement _root) {
    this.url         = _url;
    this.rootElement = _root;
  }
  
  /* accessors */
  
  public void setRootElement(WOElement _element) {
    this.rootElement = _element;    
  }
  public WOElement rootElement() {
    return this.rootElement;
  }

  public String addSubcomponent
    (final String _name, final Map<String, IWOAssociation> _assocs)
  {
    final WOSubcomponentInfo info = new WOSubcomponentInfo(_name, _assocs);
    
    if (this.subcomponentInfos == null)
      this.subcomponentInfos = new HashMap<String, WOSubcomponentInfo>(16);

    /* generate unique component name */
    
    String cname = _name;
    if (this.subcomponentInfos.containsKey(cname)) {
      cname = null;
      StringBuilder sb = new StringBuilder(_name.length() + 64);
      for (int i = 0; i < 200; i++) {
        sb.setLength(0); // reuse
        sb.append(_name);
        sb.append('[');
        sb.append(i);
        sb.append(']');
        cname = sb.toString();
        if (!this.subcomponentInfos.containsKey(cname))
          break;
        cname = null;
      }
      if (cname == null) {
        // TODO: improve logging
        log.error("failed to generate unique component name: " + _name);
        return _name; /* reuse first entry ..., not exactly a solution */
      }
    }
    
    /* register info */
    this.subcomponentInfos.put(cname, info);
    return cname /* return the name we registered the component under */;
  }
  
  public Map<String, WOSubcomponentInfo> subcomponentInfos() {
    return this.subcomponentInfos;
  }
  
  
  /* responder */
  
  @Override
  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    this.rootElement.takeValuesFromRequest(_rq, _ctx);
  }
  
  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    return this.rootElement.invokeAction(_rq, _ctx);
  }
  
  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    this.rootElement.appendToResponse(_r, _ctx);
  }

  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.url != null) {
      _d.append(" url=");
      _d.append(this.url);
    }
    
    if (this.rootElement == null)
      _d.append(" no-root");
    
    if (this.subcomponentInfos == null)
      _d.append(" no-subs");
    else {
      _d.append(" #subs=#");
      _d.append(this.subcomponentInfos.size());
    }
  }
}
