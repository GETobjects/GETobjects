/*
  Copyright (C) 2006 Helge Hess

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.elements.links.WOLinkGenerator;
import org.getobjects.foundation.NSJavaScriptWriter;
import org.getobjects.foundation.UString;

/**
 * WELinkToRemoteScript
 * <p>
 * You usually don't use this element directly but rather WELinkToRemote or
 * WEFormToRemote.
 * <p>
 * Inspired by RoR:
 *   http://api.rubyonrails.org/classes/ActionView/Helpers/PrototypeHelper.html
 * <p>
 * Sample:
 * <pre>
 *   OnClick: WELinkToRemoteScript {
 *     directActionName = "postComment";
 *     actionClass      = "CommentPage";
 *     ?comment         = "blub";
 *     update           = "time_div";
 *     position         = "after";
 *   }</pre>
 *
 * Template:
 * <pre>
 *   &lt;a href="#" &lt;#OnClick/&gt;&gt;Do it!&lt;/a&gt;</pre>
 *
 * Renders:
 * <pre>
 *   TODO: document</pre>
 *
 * Bindings (WOLinkGenerator):
 * <pre>
 *   href               [in] - string
 *   action             [in] - action
 *   pageName           [in] - string
 *   directActionName   [in] - string
 *   actionClass        [in] - string
 *   fragment           [in] - string (name of fragment to update [?wofid])
 *   fragmentIdentifier [in] - string
 *   queryDictionary    [in] - Map&lt;String,String&gt;
 *   - all bindings starting with a ? are stored as query parameters.
 * </pre>
 * Bindings:
 * <pre>
 *   type               [in] - synchronous/asynchronous
 *   update             [in] - string|dict (element id or keyed on handler)
 *   position           [in] - string      (before/top/bottom/after/..)
 *   method             [in] - string      (GET or POST)
 *   event              [in] - string      (event handler to use (def: onclick))
 *
 *   confirm            [in] - string (JS function or code)
 *   condition          [in] - string (JS function or code)
 *   before             [in] - string (JS function or code)
 *   after              [in] - string (JS function or code)
 *   submit             [in] - string (element-id of form)
 * </pre>
 * Callback Bindings:
 * <pre>
 *   loading            [in] - string (JS function or code)
 *   loaded             [in] - string (JS function or code)
 *   interactive        [in] - string (JS function or code)
 *   success            [in] - string (JS function or code)
 *   failure            [in] - string (JS function or code)
 *   complete           [in] - string (JS function or code)
 *   &lt;3-digits&gt; (404)   [in] - string (JS function or code)
 * </pre>
 * Request Headers:
 * <pre>
 *   accept             [in] - string|List (accepted response content-type's)
 *   headers            [in] - Map         (request headers)
 * </pre>
 */
public class WELinkToRemoteScript extends WEPrototypeElement {
  public static final int PARAMETERMODE_LINK        = 0;
  public static final int PARAMETERMODE_FORM        = 1;
  public static final int PARAMETERMODE_OBSERVE     = 2;
  public static final int PARAMETERMODE_FORMELEMENT = 3;

  protected WOAssociation   type;
  protected WOAssociation   update; /* DOM element 'id' to update */
  protected WOAssociation   position;
  protected WOAssociation   method;
  protected WOAssociation   event;

  protected WOAssociation   confirm;
  protected WOAssociation   condition;
  protected WOAssociation   before;
  protected WOAssociation   after;
  protected WOAssociation   submit;

  protected WOAssociation   accept;
  protected WOAssociation   headers;

  protected WOAssociation   fragment;

  protected WOLinkGenerator link;

  /* callbacks */
  protected Map<String, WOAssociation> callbacks;

  /* special WOFormToRemote support */
  protected int parameterMode;

  /* special WOObserveField support */
  protected WOAssociation with;
  protected WOAssociation name;

  public void init(final String _name, final Map<String, WOAssociation> _assocs) {
    this.parameterMode = PARAMETERMODE_LINK;

    this.type      = grabAssociation(_assocs, "type");
    this.update    = grabAssociation(_assocs, "update");
    this.position  = grabAssociation(_assocs, "position");
    this.method    = grabAssociation(_assocs, "method", "POST");
    this.event     = grabAssociation(_assocs, "event");

    this.confirm   = grabAssociation(_assocs, "confirm");
    this.condition = grabAssociation(_assocs, "condition");
    this.before    = grabAssociation(_assocs, "before");
    this.after     = grabAssociation(_assocs, "after");
    this.submit    = grabAssociation(_assocs, "submit");

    this.accept    = grabAssociation(_assocs, "accept");
    this.headers   = grabAssociation(_assocs, "headers");

    /* callbacks */

    this.callbacks = extractCallbacks(_assocs);


    /* special fragment support */

    this.fragment = grabAssociation(_assocs, "fragment");
    if (this.update == null) this.update = this.fragment;

    if (this.fragment != null && this.link == null) {
      if (!WOLinkGenerator.containsLinkInAssociations(_assocs)) {
        /* user specified no link, so we just call default */
        _assocs.put("action", WOAssociation.associationWithValue("default"));
      }
    }

    /* link and template */

    // System.err.println("A: " + _assocs);
    if (this.link == null)
      this.link = WOLinkGenerator.linkGeneratorForAssociations(_assocs);
  }

  public WELinkToRemoteScript
    (final String _name, final Map<String, WOAssociation> _assocs, final WOElement _template)
  {
    super(_name, _assocs, _template);
    init(_name, _assocs);
  }

  public WELinkToRemoteScript
    (final String _name, final Map<String, WOAssociation> _assocs, final int _parameterMode,
     final WOLinkGenerator _link)
  {
    super(_name, _assocs, null /* template */);

    if (_link != null)
      this.link = _link; /* this makes the initializer skip the linkscan */

    init(_name, _assocs);

    this.parameterMode = _parameterMode;

    if (this.parameterMode == PARAMETERMODE_OBSERVE) {
      this.name = grabAssociation(_assocs, "name");
      this.with = grabAssociation(_assocs, "with");
    }
  }

  /* helper */

  protected static Map<String, WOAssociation> extractCallbacks
    (final Map<String, WOAssociation> _assocs)
  {
    if (_assocs == null)
      return null;

    final Map<String, WOAssociation> cbs = new HashMap<>(8);
    final List<String> keysToRemove = new ArrayList<>(8);

    for (final String name: _assocs.keySet()) {
      String cb = null;
      final char c0 = name.charAt(0);
      final int  nl = name.length();

      switch (c0) {
        case 'c':
          if (name.equals("complete")) cb = "onComplete";
          break;
        case 'e':
          if (name.equals("exception")) cb = "onException";
          break;
        case 'f':
          if (name.equals("failure")) cb = "onFailure";
          break;
        case 'i':
          if (name.equals("interactive")) cb = "onInteractive";
          break;
        case 'l':
          if (name.equals("loading")) cb = "onLoading";
          if (name.equals("loaded"))  cb = "onLoaded";
          break;
        case 's':
          if (name.equals("success")) cb = "onSuccess";
          break;
        case 'u':
          if (name.equals("uninitialized")) cb = "onUninitialized";
          break;

        case '1': case '2': case '3': case '4': case '5': /* HTTP status */
          if (nl == 3) /* eg 404 => on404 */
            cb = "on" + name;
          break;
      }

      if (cb != null) {
        cbs.put(cb, _assocs.get(name));
        keysToRemove.add(name);
      }
    }

    for (final String k: keysToRemove)
      _assocs.remove(k);

    return (cbs.size() == 0) ? null : cbs;
  }


  /* accessors (required by some wrapping elements */

  public WOLinkGenerator getLink() {
    return this.link;
  }


  /* handle requests */

  @Override
  public void takeValuesFromRequest(final WORequest _rq, final WOContext _ctx) {
    /* links can take form values !!!! (for query-parameters) */

    if (this.link != null)
      this.link.takeValuesFromRequest(_rq, _ctx);
  }

  @Override
  public Object invokeAction(final WORequest _rq, final WOContext _ctx) {
    if (_ctx.elementID().equals(_ctx.senderID())) {
      if (this.link != null)
        return this.link.invokeAction(_rq, _ctx);

      log.error("no action configured for link invocation");
      return null;
    }

    return null;
  }

  /* generate response */

  public static boolean isSimpleJavaScriptID(final String _js) {
    if (_js == null)
      return false;

    boolean isFirstChar = true;
    for (int i = 0, len = _js.length(); i < len; i++) {
      final char c = _js.charAt(i);

      if (isFirstChar) {
        if (!Character.isJavaIdentifierStart(c))
          return false;

        isFirstChar = false;
      }
      else if (c == '.') {
        isFirstChar = true; /* next char must start again */
      }
      else {
        if (!Character.isJavaIdentifierPart(c))
          return false;
      }
    }
    return true;
  }

  protected void appendCallbacksToJS(final NSJavaScriptWriter _js, final Object cursor) {
    if (this.callbacks == null)
      return;

    String s;
    for (final String cn: this.callbacks.keySet()) {
      if ((s = strForAssoc(this.callbacks.get(cn), cursor)) == null)
        continue;

      _js.append(", ");
      _js.appendIdentifier(cn); /* eg on404, onComplete, etc */
      _js.append(":");

      s = s.trim();

      /* Note: we always need to wrap it into a function so that 'this' is
       *       properly defined.
       */
      _js.append("function(request,json){");

      if (isSimpleJavaScriptID(s)) {
        _js.append(s);
        _js.append("(request,json);");
      }
      else
        _js.append(s);

      _js.append("}");
    }
  }

  @SuppressWarnings("unchecked")
  protected void appendHeadersToJS(final NSJavaScriptWriter _js, final Object cursor) {
    Map<String, Object> lRequestHeaders = null;

    if (this.headers != null) {
      lRequestHeaders = (Map<String, Object>)
        this.headers.valueInComponent(cursor);
    }

    if (this.accept != null) {
      final Object v = this.accept.valueInComponent(cursor);
      if (v != null) {
        if (lRequestHeaders == null)
          lRequestHeaders = new HashMap<>(2);
        else
          lRequestHeaders = new HashMap<>(lRequestHeaders);
      }

      if (v instanceof String)
        lRequestHeaders.put("Accept", v);
      else if (v instanceof List) {
        final String s = UString.componentsJoinedByString((List)v, ", ");
        lRequestHeaders.put("Accept", s);
      }
      else if (v instanceof StringBuilder)
        lRequestHeaders.put("Accept", v.toString());
      else if (v instanceof StringBuffer)
        lRequestHeaders.put("Accept", v.toString());
      else if (v != null) {
        log.warn("unexpected value for Accept header: " + v);
      }
    }

    if (lRequestHeaders != null && lRequestHeaders.size() > 0) {
      _js.append(", requestHeaders: [ ");

      boolean isFirst = true;
      for (final String key: lRequestHeaders.keySet()) {
        if (isFirst) isFirst = false;
        else _js.append(", ");

        _js.appendString(key);
        _js.append(", ");
        _js.appendConstant(lRequestHeaders.get(key));
      }
      _js.append("]");
    }

  }

  public void appendJavaScript(final NSJavaScriptWriter _js, final WOContext _ctx) {
    final Object cursor =_ctx.cursor();

    String s;
    int braceCloseCount = 0;

    /* confirm panel */
    if ((s = strForAssoc(this.confirm, cursor)) != null) {
      _js.append("if (");
      _js.appendCall("confirm", s);
      _js.append(") { ");
      braceCloseCount++;
    }

    /* condition */
    if ((s = strForAssoc(this.condition, cursor)) != null) {
      _js.append("if (");
      _js.append(s);
      _js.append(") { ");
      braceCloseCount++;
    }

    /* before */
    if ((s = strForAssoc(this.before, cursor)) != null)
      _js.append(s);

    /* now generate AJAX call */

    beginAjaxCallWithUpdater(_js,
        this.update != null ? this.update.valueInComponent(cursor) : null);


    /* generate link parameter */

    s = (this.link != null) ? this.link.fullHrefInContext(_ctx) : null;

    String fragId = this.fragment != null
      ? this.fragment.stringValueInComponent(cursor) : null;

    if (s == null) s = fragId != null ? _ctx.request().uri() : "#";

    if (fragId != null) {
      /* We do add the fragment id manually because the regular FORM action
       * should invoke the action w/o a fragment. That is, the link generator
       * will return a URL w/o a fragment-id.
       */
      fragId = WORequest.FragmentIDKey + "=" + fragId;
      s += (s.indexOf('?') < 0) ? ("?" + fragId) : ("&" + fragId);
    }
    _js.appendConstant(s);
    _js.append(", ");


    /* generate options dictionary */

    _js.beginMap();

    /* type */
    if (this.type != null) {
      _js.append(this.type.stringValueInComponent(cursor));
      _js.append(":true");
    }
    else
      _js.append("asynchronous:true");

    /* eval */
    _js.append(", evalScripts:true"); // this evaluates scripts in the content

    /* method */
    _js.append(", method:");
    final String m = this.method.stringValueInComponent(cursor);
    _js.appendString(m != null ? m : "POST");

    /* headers */

    if (this.headers != null || this.accept != null)
      appendHeadersToJS(_js, cursor);

    /* position */
    if ((s = strForAssoc(this.position, cursor)) != null) {
      if (s.length() > 0) {
        if (!Character.isUpperCase(s.charAt(0)))
          s = "" + Character.toUpperCase(s.charAt(0)) + s.substring(1);

        _js.append(", insertion:Insertion.");
        _js.append(s);
      }
    }

    /* form parameters */
    if ((s = strForAssoc(this.submit, cursor)) != null) {
      _js.append(", parameters:Form.serialize(");
      _js.appendConstant(s);
      _js.append(")");
    }
    else if (this.parameterMode == PARAMETERMODE_FORM) {
      _js.append(", parameters:Form.serialize(this)");
    }
    else if (this.parameterMode == PARAMETERMODE_FORMELEMENT) {
      _js.append(", parameters:Form.serialize(this.form)");
    }
    else if (this.parameterMode == PARAMETERMODE_OBSERVE) {
      _js.append(", parameters:");
      if ((s = strForAssoc(this.with, cursor)) != null)
        _js.append(s);
      else {
        // TODO: add support for queryParameters etc
        if ((s = strForAssoc(this.name, _ctx.cursor())) != null) {
          /* eg "{'q': $F('q')}" */
          s =  UString.replaceInSequence(s, NSJavaScriptWriter.JSEscapeList);
          _js.append("{'");
          _js.append(s);
          _js.append("': $F('");
          _js.append(s);
          _js.append("')}");
        }
        else
          _js.append("value");
      }
    }

    /* callbacks */

    if (this.callbacks != null)
      appendCallbacksToJS(_js, cursor);

    /* close AJAX call */
    _js.append("});");

    /* after */
    if ((s = strForAssoc(this.after, cursor)) != null)
      _js.append(s);

    /* finish JavaScript and add to anker */

    for (int i = 0; i < braceCloseCount; i++)
      _js.append(" };");

    _js.append("return false");
  }

  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    if (_ctx.isRenderingDisabled())
      return;

    NSJavaScriptWriter js = new NSJavaScriptWriter();
    appendJavaScript(js, _ctx);

    if (this.event != null) {
      final String onWhat = this.event.stringValueInComponent(_ctx.cursor());

      if (onWhat != null)
        _r.appendAttribute(onWhat, js.script());
      else {
        /* if the event is bound to null, we instead render the output
         * directly */
        _r.appendContentHTMLString(js.script());
      }
    }
    else
      _r.appendAttribute("onclick", js.script());

    js = null;
  }
}
