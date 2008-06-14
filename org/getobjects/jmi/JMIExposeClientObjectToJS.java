/*
  Copyright (C) 2007 Helge Hess

  This file is part of Go JMI.

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
package org.getobjects.jmi;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOMessage;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.elements.WOJavaScriptWriter;
import org.getobjects.appserver.publisher.IGoCallable;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.appserver.publisher.IGoLocation;
import org.getobjects.appserver.publisher.GoClass;
import org.getobjects.ofs.IGoFolderish;

/*
 * JMIExposeClientObjectToJS
 * 
 * Provide a JavaScript API to the current clientObject.
 * 
 * TODO: document more
 * TODO: traverse SoClass'es and expose methods
 */
public class JMIExposeClientObjectToJS extends WODynamicElement {
  protected static final Log log = LogFactory.getLog("JoOFS");

  public JMIExposeClientObjectToJS
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
  }
  
  /* response generation */
  
  // TODO: would be nice to have support for query-parameter-sessions
  protected static String jsOFSBaseObject =
    "function initClientObject" +
    "(_o, _isFolderish, _path, _contextPath, _baseURL) {\n" +
    "  _o.path          = _path;\n" +
    "  _o.depth         = _path.length;\n" +
    "  _o.isFolderish   = _isFolderish;\n" +
    "  _o.traversalPath = _contextPath;\n" +
    "  _o.baseURL       = _baseURL;\n" +
    "\n" +
    "  _o.nameInContainer = " + 
    "    _path.length > 0 ? _path[_path.length - 1] : undefined;\n" +
    "\n" +
    "  _o.invokeOnLocation = function(_methodName, _params) {\n" +
    "    var url = this.baseURL + '/' + _methodName;\n" +
    "    if (_params != undefined) url += '?' + _params.toQueryString();\n" +
    "    window.location.href = url;\n" +
    "  };\n" +
    "}\n"
    ;

  protected boolean shouldExposeClass(GoClass _cls) {
    if (_cls == null)
      return false;
    
    String myName = _cls.className();
    if ("Object".equals(myName)) return false;
    if ("NSObject".equals(myName)) return false;
    
    return true;
  }
  
  protected void exposeMethod
    (String _methodName, WOJavaScriptWriter _js, IGoContext _ctx)
  {
    if (_methodName == null || _methodName.length() == 0)
      return;
    
    _js.append("  this['");
    _js.append(_methodName);
    _js.append("'] = function(_args) {\n");
    _js.append("    this.invokeOnLocation('");
    _js.append(_methodName);
    _js.append("', _args);\n");
    _js.append("  };\n");
  }
  
  protected String exposeJoClass
    (GoClass _cls, WOJavaScriptWriter _js, IGoContext _ctx)
  {
    /*
     * TODO: plenty of things, just a prototype ;-)
     * - add ability to let the JoClass provide an own JavaScript representation
     * - add different invocation styles, eg updating a fragment instead of a
     *   full refresh
     */
    if (_cls == null)
      return null;
    
    String myName = _cls.className();
    
    /* check superclass */
    
    GoClass superClass = _cls.joSuperClass();
    String  superName = this.shouldExposeClass(superClass)
      ? exposeJoClass(superClass, _js, _ctx)
      : null;
    
    /* expose class */
    
    _js.append("function ");
    _js.appendIdentifier(myName);
    _js.append("() {\n");
    
    String[] slots = _cls.slotNames();
    if (slots != null) {
      for (String slot: slots) {
        Object method = _cls.valueForSlot(slot);
        if (method == null || method instanceof Exception)
          continue;
        
        if (method instanceof IGoCallable)
          this.exposeMethod(slot, _js, _ctx);
      }
    }
    
    _js.append("}\n");
    
    /* setup prototype chain */
    
    if (superName != null) {
      _js.appendIdentifier(myName);
      _js.append(".prototype = new ");
      _js.appendIdentifier(superName);
      _js.append("();\n");
    }
    
    return myName;
  }
  
  protected void exposeClientObject
    (Object _object, WOJavaScriptWriter _js, IGoContext _ctx)
  {
    if (_object == null) {
      _js.append("var clientObject = ");
      _js.appendConstant(null);
      _js.append(";\n");
      return;
    }
    
    /* generate class hierarchy */
    
    _js.append(jsOFSBaseObject);
    String className = "OFSBaseObject";
    
    if (_ctx != null) {
      GoClass cls = _ctx.joClassRegistry().goClassForJavaObject(_object, _ctx);
      if (cls != null)
        className = this.exposeJoClass(cls, _js, _ctx);
    }
    
    /* generate object */
    
    _js.append("var clientObject = new ");
    _js.appendIdentifier(className);
    _js.append("();\n");
    
    _js.append("initClientObject(clientObject, ");
    
    if (_object instanceof IGoFolderish)
      _js.append("true, ");
    else
      _js.append("false, ");
    
    /* append containment path */
    
    String[] path = IGoLocation.Utility.pathToRoot(_object);
    if (path == null || path.length == 0) {
      _js.append("[], ");
    }
    else {
      _js.append("[");
      for (int i = 0; i < path.length; i++) {
        if (i != 0) _js.append(", ");
        _js.appendConstant(path[i]);
      }
      _js.append("], ");
    }
    
    /* append traversal path */
    
    StringBuilder sb = new StringBuilder(512);
    path = _ctx.joTraversalPath().pathToClientObject();
    if (path == null || path.length == 0) {
      _js.append("[], ");
      sb.append("/");
    }
    else {
      _js.append("[");
      for (int i = 0; i < path.length; i++) {
        if (i != 0) _js.append(", ");
        _js.appendConstant(path[i]);

        // TODO: this belongs into JoTraversalPath
        sb.append("/");
        try {
          sb.append(URLEncoder.encode(path[i], WOMessage.defaultURLEncoding()));
        }
        catch (UnsupportedEncodingException e) {
          sb.append("[ENCERR]");
          log.error("could not encode path: '" + path[i] + "'", e);
        }
      }
      _js.append("], ");
    }
    
    /* append base URL parameter */
    _js.appendConstant(sb.toString());
    
    /* close ctor call */
    _js.append(");\n");
  }

  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    Object clientObject = _ctx.clientObject();
    if (clientObject == null)
      return;
    
    WOJavaScriptWriter js = new WOJavaScriptWriter();
    
    /* add document information */
    
    this.exposeClientObject(clientObject, js, _ctx);
    
    /* add JavaScript to response */
    
    String script = js.script();
    if (script != null && script.length() > 0 && !_ctx.isRenderingDisabled()) {
      _r.appendBeginTag("script");
      _r.appendAttribute("type",     "text/javascript");
      _r.appendAttribute("language", "JavaScript");
      _r.appendBeginTagEnd();
      _r.appendContentString("\n");
      
      _r.appendContentString(script);

      // Note: The &gt; somehow doesn't work in a script section of JMI?
      //       Possibly because we miss a proper doctype?
      //_r.appendContentString("\n//<![CDATA[\n");
      //_r.appendContentHTMLString(script);
      //_r.appendContentString("\n//]]>\n");
      _r.appendEndTag("script");
    }
  }

}
