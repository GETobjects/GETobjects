/*
 * Copyright (C) 2008 Helge Hess <helge.hess@opengroupware.org>
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
package org.getobjects.jsapp.adapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOSession;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Wrapper;

/**
 * JSComponentWrapper
 * <p>
 * This object can be used as the scope of a JSScopedComponent. It explicitly
 * defines the JavaScript API of a WOComponent.
 * (this is different to JSComponentAdapter which directly uses the component).
 * <p>
 * IMPORTANT: Why does this have issues? The problem is that we want to
 * override functions of the Java class. Eg while we want to expose
 *   <code>pageWithName</code>
 * to the script, we also want to allow the script to override the method with
 * a scripted function (which can then call the super implementation using
 * this.super_pageWithName() or something).
 * 
 * <p>
 * Hm, but maybe this DOES indeed work? Not sure whether ScriptableObject allows
 * us to override functions defined using jsFunction_()?
 * [JavaNativeObject itself does NOT allow overriding or enhancing objects]
 * 
 * <p>
 * Hm, something we could do is naming the JS functions differently? Eg call
 * them <code>function override_pageWithName()</code>? This would allow the Java
 * side to call an overridden method.<br>
 * And even the other way around. pageWithName() would call the Java side which
 * would then call <code>override_pageWithName()</code>.
 * Note that it may not call <code>pageWithName</code> to call the super
 * implementation. We would need to provide an appropriate
 * <code>super_pageWithName</code> Or a <code>super</code> trampoline object
 * which calls the extensible methods with a proper flag. 
 * <br>
 * Of course all this would quite a bit hackish ;-)
 * 
 * <p>
 * And yet another idea: the NativeJavaObject wrapper could be the prototype of
 * a generic scope? This way the scope could overide native Java methods. (and
 * still call them using a prototype?)
 */
public class JSComponentWrapper extends ImporterTopLevel implements Wrapper {
  // TBD: should we subclass ImporterTopLevel?
  private static final long serialVersionUID = 1L;
  protected static final Log log = LogFactory.getLog("JSBridge");
  
  protected WOComponent javaObject;

  public JSComponentWrapper(Context _cx, WOComponent _javaComponent) {
    super(_cx, false /* not sealed */);
    this.javaObject = _javaComponent;
  }

  public JSComponentWrapper(Context _cx, boolean _sealed, WOComponent _comp) {
    super(_cx, _sealed);
    this.javaObject = _comp;
  }
  
  /* JavaScript class */
  
  @Override
  public String getClassName() {
    return "JSComponentWrapper";
  }

  /* Wrapper */
  
  public Object unwrap() {
    return this.javaObject;
  }
  
  /* properties */

  public WOContext jsGet_context() {
    return this.javaObject.context();
  }
  public WOSession jsGet_session() {
    return this.javaObject.session();
  }
  public boolean jsGet_hasSession() {
    return this.javaObject.hasSession();
  }
  public WOComponent jsGet_parent() {
    return this.javaObject.parent();
  }
  
  /* functions */
  
  public WOComponent jsFunction_pageWithName(String _name) {
    return this.javaObject.pageWithName(_name);
  }
  public Object jsFunction_performParentAction(String _action) {
    return this.javaObject.performParentAction(_action);
  }
}
