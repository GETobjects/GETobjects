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

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

import org.getobjects.appserver.core.WOContext;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

public class JSFormat extends Format {
  private static final long serialVersionUID = 1L;
  
  protected WOContext context;
  protected Function  function;

  public JSFormat(WOContext _ctx, Function _func) {
    this.context  = _ctx;
    this.function = _func;
  }
  
  /* accessors */
  
  public WOContext context() {
    return this.context;
  }
  public Function function() {
    return this.function;
  }
  
  /* formatting */

  @Override
  public StringBuffer format
    (Object _object, StringBuffer _sb, FieldPosition _pos)
  {
    Context cx = ((JSContext)this.context).jsContext();
    Scriptable rootScope =
      ((JSApplication)this.context.application()).jsScope();

    /* scope is interesting, not sure whats best */
    Scriptable locScope = (Scriptable)
      Context.javaToJS(this.context, rootScope /* scope */);
      //Context.javaToJS(_object, rootScope /* scope */);
    
    Scriptable thisObject =
      (Scriptable)Context.javaToJS(this, rootScope /* scope */);
    
    
    Object[] args = new Object[] { Context.javaToJS(_object, rootScope) };
    
    Object jr = this.function.call(cx,
        locScope   /* scope */,
        thisObject /* this  */,
        args);
    
    String s = (String)Context.jsToJava(jr, String.class);
    if (s != null) _sb.append(s);
    
    return _sb;
  }

  @Override
  public Object parseObject(String source, ParsePosition pos) {
    return null;
  }

}
