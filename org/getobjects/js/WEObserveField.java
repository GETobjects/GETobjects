package org.getobjects.js;

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.elements.links.WOLinkGenerator;
import org.getobjects.foundation.NSJavaScriptWriter;

public class WEObserveField extends WEDynamicElement {
  protected WOAssociation name;
  protected WOAssociation frequency;
  protected WOAssociation function;
  protected WOAssociation on; /* 'change' */

  protected WELinkToRemoteScript callElement;

  public WEObserveField
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);

    this.name      = _assocs.get("name"); // consumed by WELinkToRemoteScript
    this.frequency = grabAssociation(_assocs, "frequency");
    this.function  = grabAssociation(_assocs, "function");
    this.on        = grabAssociation(_assocs, "on");

    /* aliases */
    if (this.on == null)
      this.on = grabAssociation(_assocs, "event"); /* like in WELinkToRemote */

    /* AJAX call generator */
    /* Note: might report missing link if 'function' is used */
    this.callElement = new WELinkToRemoteScript
      (_name + "Script", _assocs,
       WELinkToRemoteScript.PARAMETERMODE_OBSERVE,
       null /* link generator */);
  }

  /* handle requests */

  @Override
  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    /* links can take form values !!!! (for query-parameters) */

    WOLinkGenerator link = this.callElement.getLink();
    if (link != null)
      link.takeValuesFromRequest(_rq, _ctx);
  }

  /* generate response */

  protected static final String defaultFrequency = "0.75";
  protected static final String events = "input";

  public void appendJavaScript(NSJavaScriptWriter _js, WOContext _ctx) {
    final Object cursor = _ctx.cursor();

    final String lName = strForAssoc(this.name, cursor);
    final String timerVar = getProperIDName((lName != null ? lName : "form") + _ctx.elementID() + "_timer");

    _js.append("var " + timerVar + "=null;");
    if (lName == null)
      appendQuerySelector("form", _js);
    else
      appendGetElementById(lName, _js);

    
//  document.addEventListener(eventName, (event) => {
//    if (event.target.closest(elementSelector)) {
//      handler.call(event.target, event);
//    }
//  });

    _js.append(".addEventListener(");

    /* on parameter */
    String lOn = strForAssoc(this.on, cursor);
    if (lOn == null)
      lOn = events;
    _js.appendConstant(lOn);
    _js.append(",");
  
    /* function or link */
  
    final String lF = strForAssoc(this.function, cursor);
    if (lF != null) {
      _js.append(lF); /* function object, like 'myCallback' */
    }
    else if (this.callElement != null) {
      _js.append("function(event){");
      _js.append("if(");
      _js.append(timerVar);
      _js.append(" != null){");
      _js.append("clearTimeout(");
      _js.append(timerVar);
      _js.append(");};");
      _js.append(timerVar);
      _js.append("=setTimeout(function(){");
      this.callElement.appendJavaScript(_js, _ctx);
      _js.append("},");
  
      /* timeout */
  
      String timeout = strForAssoc(this.frequency, cursor);
       if (timeout == null)
         timeout = defaultFrequency;
      _js.append(timeout);
      _js.append(" * 1000");
  
      /* end setTimeout  */
      _js.append(");");
  
      /* end function(event) (and provide options) */
      _js.append("},{passive:true}");
    }
    else {
      log.warn("got no observer object ...");
      _js.reset();
      return;
    }
  
    /* close addEventListener() function call */
    _js.append(");");
  }

  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    if (_ctx.isRenderingDisabled())
      return;

    NSJavaScriptWriter js = new NSJavaScriptWriter();
    this.appendJavaScript(js, _ctx);
    _r.appendContentScript(js);
  }
}
