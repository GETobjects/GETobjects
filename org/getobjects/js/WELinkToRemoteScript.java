package org.getobjects.js;

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

public class WELinkToRemoteScript extends WEDynamicElement {

  public static final int PARAMETERMODE_LINK        = 0;
  public static final int PARAMETERMODE_FORM        = 1;
  public static final int PARAMETERMODE_OBSERVE     = 2;
  public static final int PARAMETERMODE_FORMELEMENT = 3;

  protected WOAssociation   type;
  protected WOAssociation   update; /* DOM element 'id' to update */
  protected WOAssociation   position;
  protected WOAssociation   method;
  protected WOAssociation   event;
  protected WOAssociation   asJS;
  
  protected WOAssociation   confirm;
  protected WOAssociation   condition;
  protected WOAssociation   before;
  protected WOAssociation   after;
  protected WOAssociation   submit;

  protected WOAssociation   accept;
  protected WOAssociation   headers;

  protected WOAssociation   error;
  protected WOAssociation   success;
  protected WOAssociation   complete;

  protected WOAssociation   fragment;

  protected WOLinkGenerator link;

  /* callbacks */
  protected Map<String, WOAssociation> statusCallbacks;

  /* special WOFormToRemote support */
  protected int parameterMode;

  /* special WOObserveField support */
  protected WOAssociation with;
  protected WOAssociation name;

  public void init(final String _name, final Map<String, WOAssociation> _assocs) {
    this.parameterMode = PARAMETERMODE_LINK;

    this.update    = grabAssociation(_assocs, "update");
    this.method    = grabAssociation(_assocs, "method", "POST");
    this.event     = grabAssociation(_assocs, "event");
    // append the script, only - no event binding!
    this.asJS      = grabAssociation(_assocs, "scriptOnly");

    // before, after, top, bottom
    this.position  = grabAssociation(_assocs, "position");

    this.confirm   = grabAssociation(_assocs, "confirm");
    this.condition = grabAssociation(_assocs, "condition");
    this.before    = grabAssociation(_assocs, "before");
    this.after     = grabAssociation(_assocs, "after");
    this.submit    = grabAssociation(_assocs, "submit");

    this.accept    = grabAssociation(_assocs, "accept");
    this.headers   = grabAssociation(_assocs, "headers");

    /* callbacks */

    this.error = grabAssociation(_assocs, "error");
    if (this.error == null)
      this.error = grabAssociation(_assocs, "failure");
    if (this.error == null)
      this.error = grabAssociation(_assocs, "exception");

    this.success  = grabAssociation(_assocs, "success");
    this.complete = grabAssociation(_assocs, "complete");

    this.statusCallbacks = extractStatusCallbacks(_assocs);


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
    //System.err.println("A: " + _assocs);

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

  protected static Map<String, WOAssociation> extractStatusCallbacks
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
        case '1': case '2': case '3': case '4': case '5': /* HTTP status */
          if (nl == 3)
            cb = name;
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

  protected void beginCall(final NSJavaScriptWriter _js) {
    _js.append("(async () => {");
  }

  protected void endCall(final NSJavaScriptWriter _js) {
    _js.append("})();");
  }

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

  protected void appendScriptOrCallToJS(
      final String _body,
      final String _arguments,
      final NSJavaScriptWriter _js)
  {
    final String s = _body.trim();

    if (isSimpleJavaScriptID(s)) {
      _js.append(s);
      _js.append("(");
      _js.append(_arguments);
      _js.append(");");
    }
    else
      _js.append(s);
  }

  protected boolean appendStatusCallbacksToJS(final NSJavaScriptWriter _js, final Object cursor) {
    if (this.statusCallbacks == null)
      return false;

    final Map<String, String> cbMap = new HashMap<>(this.statusCallbacks.keySet().size());    
    for (final String cn: this.statusCallbacks.keySet()) {
      final String s = strForAssoc(this.statusCallbacks.get(cn), cursor);
      if (s != null)
        cbMap.put(cn, s);
    }
    if (cbMap.keySet().size() == 0)
      return false;
    
    _js.append("let status = response.status;");

    boolean needElse = false;
    for (final String cn : cbMap.keySet()) {
      if (needElse)
        _js.append("else ");
      _js.append("if (status == ");
      _js.append(cn);
      _js.append(") {");
      appendScriptOrCallToJS(cbMap.get(cn), "response", _js);
      _js.append("}");
      needElse = true;
    }
    return true;
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
      _js.append(", headers: ");
      _js.appendMap(lRequestHeaders);
    }
  }

  protected boolean isScriptOnly(final WOContext _ctx) {
    if (this.asJS != null && this.asJS.booleanValueInComponent(_ctx.cursor()))
      return true;
    return false;
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

    beginCall(_js);

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

    _js.append("const response = await fetch(");
    _js.appendConstant(s);
    
    /* begin options */
    _js.append(",{");

    /* method */

    _js.append("method:");
    final String m = this.method.stringValueInComponent(cursor);
    _js.appendString(m != null ? m : "POST");

    /* headers */

    if (this.headers != null || this.accept != null)
      appendHeadersToJS(_js, cursor);

    /* generate data */

    /* form parameters */
    if ((s = strForAssoc(this.submit, cursor)) != null) {
      _js.append(", body: ");
      _js.append("new FormData(");
      appendGetElementById(s, _js);
      _js.append(")");
    }
    else if (this.parameterMode == PARAMETERMODE_FORM) {
      _js.append(", body: ");
      _js.append("new FormData(this)");
    }
    else if (this.parameterMode == PARAMETERMODE_FORMELEMENT) {
      _js.append(", body: ");
      _js.append("new FormData(this.form)");
    }
    else if (this.parameterMode == PARAMETERMODE_OBSERVE) {
      // TODO: add support for queryParameters etc

      if ((s = strForAssoc(this.with, cursor)) != null) {
        /* eg "{'foo': 'bar'}" */
        _js.append(", body: ");
        _js.append(s);
      }
      else if ((s = strForAssoc(this.name, cursor)) != null) {
        /* eg "new FormData(document.getElementById('q'))" */
        _js.append(", body: new URLSearchParams({");
        _js.appendConstant(s);
        _js.append(":");
        appendGetElementById(s, _js);
        _js.append(".value})");
      }
    }
    /* end options and fetch */
    _js.append("});");

    /* begin processing result */

    _js.append("let data = null;if (response.ok){data = await response.text();");
    if ((s = strForAssoc(this.update, cursor)) != null) {

      /* update `update` with data */
      _js.append("let e = ");
      appendGetElementById(s, _js);
      _js.append(";");
      
      /* position */
      if ((s = strForAssoc(this.position, cursor)) != null) {
        s = s.toLowerCase();
        final String pos;
        if (s.equals("before"))
          pos = "beforebegin";
        else if (s.equals("after"))
          pos = "afterend";
        else if (s.equals("top")) // first child
          pos = "afterbegin";
        else if (s.equals("bottom")) // last child
          pos = "beforeend";
        else {
          log.error("Don't know position: " + s);
          pos = null;
        }
        if (pos != null) {
          _js.append("e.insertAdjacentHTML('");
          _js.append(pos);
          _js.append("', data);");
        }
      }
      else {
        _js.append("e.innerHTML = data;");        
      }

      /* evaluate contained JS */

      _js.append("let scripts = e.getElementsByTagName('script');");
      _js.append("for(let se of scripts){eval(se.innerHTML);};");
    }

    /* end processing result */
    _js.append("};");
    
    
    /* callbacks */
 
    final boolean haveCbs = appendStatusCallbacksToJS(_js, cursor);

    final String cbSuccess = strForAssoc(this.success, cursor);
    final String cbError = strForAssoc(this.error, cursor);

    if (haveCbs && (cbSuccess != null || cbError != null))
      _js.append("else ");

    /* success */
    if (cbSuccess != null) {
      _js.append("if (response.ok) {");
      appendScriptOrCallToJS(cbSuccess, "data,response", _js);
      _js.append("}");
    }

    /* error */
    if (cbError != null) {
      if (cbSuccess != null)
        _js.append(" else {");
      else
        _js.append("if (!response.ok) {");

      appendScriptOrCallToJS(cbError, "response", _js);
      _js.append("}");        
    }
    if (haveCbs || cbSuccess != null || cbError != null)
      _js.append(";");

    /* complete */
    if ((s = strForAssoc(this.complete, cursor)) != null) {
      appendScriptOrCallToJS(s, "data,response", _js);
    }

    /* close AJAX call */
    endCall(_js);

    /* after */
    if ((s = strForAssoc(this.after, cursor)) != null)
      appendScriptOrCallToJS(s, "", _js);

    /* finish JavaScript and add to anchor */

    for (int i = 0; i < braceCloseCount; i++)
      _js.append(" };");
    
    if (!isScriptOnly(_ctx))
      _js.append("return false;");
  }

  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    if (_ctx.isRenderingDisabled())
      return;

    NSJavaScriptWriter js = new NSJavaScriptWriter();
    appendJavaScript(js, _ctx);

    if (isScriptOnly(_ctx)) {
      _r.appendContentString(js.script());
    }
    else if (this.event != null) {
      final String onWhat = this.event.stringValueInComponent(_ctx.cursor());

      if (onWhat != null)
        _r.appendAttribute(onWhat, js.script());
      else {
        /* NOTE: What was the purpose of this?
         * I'll leave it here as it was in the original implementation, but
         * escaping the script as HTML looks suspiciously incorrect.
         */

        /* if the event is bound to null, we instead render the output
         * directly */
        _r.appendContentHTMLString(js.script());
      }
    }
    else {
      _r.appendAttribute("onclick", js.script());
    }
  }
}
