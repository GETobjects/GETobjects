/*
  Copyright (C) 2006-2007 Helge Hess

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

package org.getobjects.weprototype;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.elements.WOJavaScriptWriter;

/**
 * WEPrototypeElement
 * <p>
 * Abstract superclass for elements which use prototype features.
 */
// TBD: I guess we can drop this call?!
public abstract class WEPrototypeElement extends WODynamicElement {
  protected static Log log = LogFactory.getLog("WEPrototype");

  public WEPrototypeElement
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
  }

  protected static String strForAssoc(WOAssociation _assoc, Object _cursor) {
    if (_assoc == null)
      return null;
    return _assoc.stringValueInComponent(_cursor);
  }
  
  /* AJAX calls */
  
  protected void beginAjaxCallWithUpdater
    (WOJavaScriptWriter _js, Object _updater)
  {
    _js.append("new Ajax.");
    if (_updater != null) {
      _js.append("Updater(");
      _js.appendConstant(_updater);
      _js.append(", ");
    }
    else {
      _js.append("Request(");
    }
  }
  
  /* loading associated resources */
  
  static String loadResourceAsString(String _name) {
    // note used anywhere?
    InputStream ri = 
      WEPrototypeElement.class.getResourceAsStream(_name);
    BufferedReader r = new BufferedReader(new InputStreamReader(ri));
    ri = null;
    
    try {
      StringBuilder sb = new StringBuilder(4096);
      String line;
      
      while ((line = r.readLine()) != null)
        sb.append(line + "\n");
      
      r.close();
      
      return sb.toString();
    }
    catch (IOException e) {
      log.error("could not load resource file: " + _name);
    }
    return null;
  }
}
