/*
  Copyright (C) 2008 Helge Hess <helge.hess@opengroupware.org>

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
package org.getobjects.jsapp;

import java.io.File;

import org.mozilla.javascript.ImporterTopLevel;

/**
 * This object wraps a Scope returned by the superclass in a
 * JSKeyValueCodingScope, that is, it scans the Scope for values
 * which should be exposed via KVC and caches that information.
 */
public class JSCachedKVCScriptScope extends JSCachedScriptScope {

  public JSCachedKVCScriptScope(File _dir, String _script) {
    super(_dir, _script);
  }

  /**
   * This returns a JSKeyValueCodingScope scope object. This is a combination
   * of a *sealed* JavaScript scope plus a cache of KVC wrappers.
   * 
   * @param _path    - the path the script was read from
   * @param _content - the String containing the script
   * @return an ImporterTopLevel object with the script run against it
   */
  @Override
  public Object parseObject(String _path, Object _content) {
    ImporterTopLevel scriptScope =
      (ImporterTopLevel)super.parseObject(_path, _content);
    if (scriptScope == null) {
      jslog.warn("got no script scope for path: " + _path);
      return null;
    }

    /* build the KVC cache */
    return JSKeyValueCodingScope.wrap(scriptScope);
  }
}
