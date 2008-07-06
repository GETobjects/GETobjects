/*
 * Copyright (C) 2007-2008 Helge Hess
 *
 * This file is part of Go.
 *
 * Go is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2, or (at your option) any later version.
 *
 * Go is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Go; see the file COPYING. If not, write to the Free Software
 * Foundation, 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.getobjects.jsapp;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.foundation.NSKeyValueCoding;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;

/**
 * JSAssociation
 * <p>
 * An association which evaluates its value as a JavaScript within Rhino.
 */
public class JSAssociation extends WOAssociation {
  
  protected String scriptString;
  protected Script script;
  
  public JSAssociation(final String _script) {
    super();
    this.scriptString = _script;
    this.compile();
  }
  
  /* compilation */
  
  public void compile() {
    if (this.scriptString == null) {
      log.error("association has no script?");
      return;
    }
    
    try {
      Context ctx = ContextFactory.getGlobal().enterContext();
      // TBD: do we need the wrap factory here?
      ctx.setLanguageVersion(Context.VERSION_1_7);
      
      // TBD: could we track the line/file in the .wod/.html parser?!
      this.script = ctx.compileString
        (this.scriptString,
         "<association>", 1 /* line */,
         null /* security context */);
    }
    finally {
      Context.exit();
    }
  }
  
  
  /* accessors */

  @Override
  public String keyPath() {
    return this.scriptString;
  }

  
  /* value typing */
  
  @Override
  public boolean isValueConstant() {
    return false;
  }

  @Override
  public boolean isValueSettable() {
    return false;
  }
  
  
  /* value */
  
  @Override
  public Object valueInComponent(final Object _cursor) {
    if (this.script == null) {
      log.error("no compiled script available .."); // TODO: improve
      return null;
    }
    
    /* retrieve context from component */
    
    Context    jsContext;
    Scriptable jsScope;
    
    if (_cursor instanceof JSComponent) {
      JSComponent jsc = (JSComponent)_cursor;
      jsContext = jsc.jsContext();
      jsScope   = jsc.jsScope();
    }
    else if (_cursor instanceof NSKeyValueCoding) {
      NSKeyValueCoding kvc = (NSKeyValueCoding)_cursor;
      jsContext = (Context)kvc.valueForKey("jsContext");
      
      // TBD: this gets unwrapped via KVC?
      jsScope = (Scriptable)kvc.valueForKey("jsScope");
    }
    else {
      jsContext = (Context)
        NSKeyValueCoding.Utility.valueForKey(_cursor, "jsContext");
      jsScope   = (Scriptable)
        NSKeyValueCoding.Utility.valueForKey(_cursor, "jsScope");
    }
    
    
    /* directly run if we have a proper component environment */
    
    if (jsContext != null) {
      if (jsScope == null) {
        if (log.isWarnEnabled())
          log.warn("got no JavaScript scope from component: " + _cursor);
        jsScope = jsContext.initStandardObjects();
      }
      
      // TBD: better compile as a function? Or setup the associations scope
      //      as a SUB scope of the component scope?
      Object result = this.script.exec(jsContext, jsScope);
      
      result = Context.jsToJava(result, Object.class);
      return result;
    }
    
    
    /* setup own environment if we have none ... (expensive!) */

    if (log.isWarnEnabled())
      log.warn("got no JavaScript context from component: " + _cursor);
    
    try {
      // TBD: use JSApplication jsContextFactory() when available
      jsContext = ContextFactory.getGlobal().enterContext();
      // TBD: do we need the WrapFactory here?
      jsContext.setLanguageVersion(Context.VERSION_1_7);

      if (jsScope == null) {
        if (log.isWarnEnabled())
          log.warn("got no JavaScript scope from component: " + _cursor);
        jsScope = jsContext.initStandardObjects();
      }
      
      Object result = this.script.exec(jsContext, jsScope);
      
      result = Context.jsToJava(result, Object.class);
      return result;
    }
    finally {
      Context.exit();
    }
  }
  
  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.scriptString != null)
      _d.append(" script=" + this.scriptString);
    if (this.script != null)
      _d.append(" compiled");
  }
}
