/*
 * Copyright (C) 2008 Helge Hess
 *
 * This file is part of JOPE.
 *
 * JOPE is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2, or (at your option) any later version.
 *
 * JOPE is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JOPE; see the file COPYING. If not, write to the Free Software
 * Foundation, 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.getobjects.jsapp.adapter;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

/**
 * JSBoundFunction
 * <p>
 * Functions which we store in INSExtraVariables objects shall always be called
 * with the scope AND <code>this</code> set to the object. Hence we need to wrap
 * the functions to setup the appropriate context.
 * <p>
 * Note: if we don't, <code>this</code> is only properly set when we call the
 * function using <code>this.function()</code>. If we do just
 * <code>function()</code>, <code>this</code> will point to the compilation
 * context.
 * <p>
 * This is pretty similiar to tricks done by Prototype to ensure proper 'this'.
 */
public class JSBoundFunction extends BaseFunction implements Function {
  /* Note: just implementing Callable isn't sufficient. Rhino sometimes checks
   *       whether its 'really' a Function (not sure why).
   * Note: BaseFunction is not sufficient either, gives:
   *         Cannot find function create
   */
  private static final long serialVersionUID = 1L;
  
  private final Scriptable masterOfDesaster;
  private final Callable   function;
  
  public JSBoundFunction(Scriptable _self, Callable _function) {
    this.masterOfDesaster = _self;
    this.function         = _function;
  }
  
  /* Callable */
  
  public Object call
    (final Context _jscx, final Scriptable _scope, final Scriptable _this,
     final Object[] _args)
  {
    return this.function.call(_jscx,
        this.masterOfDesaster, /* scope */
        this.masterOfDesaster, /* this  */
        _args);
  }

}
