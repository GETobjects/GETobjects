/*
  Copyright (C) 2007-2008 Helge Hess <helge.hess@opengroupware.org>
  Copyright (C) 2007 Marcus Mueller <znek@mulle-kybernetik.com>

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

package org.getobjects.jsapp;

import java.io.File;
import java.io.UnsupportedEncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Context;

/**
 * JSCachedScriptFile
 * <p>
 * The JSCachedObjectFile caches the contents of the given file as a Rhino
 * 'Script' object, that is, as a compiled JavaScript source. This Script
 * object can then be executed against arbitrary scopes (preferably global
 * scopes which got a call to initStandardObjects).
 * <p>
 * Note: the subclass JSCachedScriptScope manages a fully prepared scope.
 */
public class JSCachedScriptFile extends JSCachedObjectFile {
  protected static final Log jslog = LogFactory.getLog("JSBridge");
  
  public JSCachedScriptFile(final File _dir, final String _script) {
    super(_dir, _script);
  }
  
  /**
   * This returns a Script object compiled from the given JS script code.
   * 
   * @param _path    - the path the script was read from
   * @param _content - the String containing the script
   * @return a compiled Script object
   */
  @Override
  public Object parseObject(final String _path, final Object _content) {
    if (_content == null)
      return null;
    
    /* extract content */
    
    String src = null;
    
    if (_content instanceof String)
      src = (String)_content;
    else if (_content instanceof byte[]) {
      try {
        src = new String((byte[])_content, "utf-8");
      }
      catch (UnsupportedEncodingException e) {
        return null;
      }
    }
    else { // TBD: print a warning
      jslog.warn("could not deal with JavaScript source: " + _path);
      return null;
    }
    
    /* compile script in current Rhino context */
    
    Context jscx = Context.getCurrentContext();
    if (jscx == null) { // TBD: log
      jslog.error("no JavaScript Context active to compile: " + _path);
      return null;
    }
   
    /* returns a Rhino 'Script' object */
    return jscx.compileString(src, _path, 1 /* line */, null /* security */);
  }
}
