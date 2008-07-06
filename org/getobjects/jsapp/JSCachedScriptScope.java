/*
  Copyright (C) 2008 Helge Hess <helge.hess@opengroupware.org>

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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Script;

/**
 * JSCachedScriptScope
 * <p>
 * The parent class, JSCachedScriptFile, manages a compiled Rhino 'Script'
 * object. That is, it takes the contents of the file and compiles that into
 * a scope-independed 'Script' object.
 * <p>
 * This class enhances that behaviour. It creates a new ImporterTopLevel
 * (scope) object and executes the previously compiled Script against it.
 * In other words, it maintains a fully prepared scope with a JavaScript file
 * applied.
 * <p>
 * Notes
 * <ul>
 *   <li>Does the ImporterTopLevel work properly as a shared scope?
 *       We cannot seal it, because properties are added dynamically.
 *   <li>Remember that the scope cannot be used a global object!
 *       If some other script adds global variables, the cached script
 *       changes for all consumers ...
 *   <li>Again: the returned scope should be considered immutable.
 *   <li>TBD: can we simply copy a scope?
 * </ul>
 * Also remember the JSCachedKVCScriptScope class, which adds reflection on top
 * of the loaded scope ...
 */
public class JSCachedScriptScope extends JSCachedScriptFile {

  public JSCachedScriptScope(final File _dir, final String _script) {
    super(_dir, _script);
  }

  /**
   * This returns a Scriptable scope object (an ImporterTopLevel object)
   * which is filled with the given JS script code.
   * <p>
   * Note: even if there is no script, we still return a scope!
   * 
   * @param _path    - the path the script was read from
   * @param _content - the String containing the script
   * @return an ImporterTopLevel object with the script run against it
   */
  @Override
  public Object parseObject(final String _path, final Object _content) {
    /* the superclass returns a Script */
    final Script script = (Script)super.parseObject(_path, _content);

    final Context jscx = Context.getCurrentContext();
    if (jscx == null) { // TBD: log
      jslog.error("no JavaScript Context active to exec: " + _path);
      return null;
    }
    
    /* setup shared scope */

    /* This calls initStandardObjects (which is slow). But I don't know
     * how to use ImporterTopLevel as a shared object since its read/write.
     * Maybe we could implement the 'import' in the component?
     */
    final ImporterTopLevel scriptScope =
      new ImporterTopLevel(jscx, false /* not sealed */);
    
    /* eval script if there is one */
    
    if (script != null) {
      try {
        script.exec(jscx, scriptScope);
      }
      catch (Exception e) {
        // TBD: reset scriptScope/script?
        jslog.error("could not execute JS: " + _path, e);
      }
    }
    
    /* seal scope (its a global cache, may not be modified) */
    // hm, if we seal, the ImporterTopLevel does not work. Apparently it adds
    // stuff lazily
    // TBD: in a thread safe way?!
    // does not work: scriptScope.sealObject();
    
    return scriptScope;
  }
}
