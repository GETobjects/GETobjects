/*
  Copyright (C) 2006 Helge Hess

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

package org.getobjects.weprototype;

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.elements.WOJavaScriptWriter;
import org.getobjects.appserver.elements.links.WOLinkGenerator;

/**
 * WEObserveField
 * <p>
 * TODO: document
 */
public class WEObserveField extends WEPrototypeElement {

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

  public void appendJavaScript(WOJavaScriptWriter _js, WOContext _ctx) {
    String lName, lFrequency, lOn, lF;

    if ((lFrequency = strForAssoc(this.frequency, _ctx.cursor())) != null)
      _js.append("new Form.Element.Observer(");
    else
      _js.append("new Form.Element.EventObserver(");

    /* form name */
    if ((lName = strForAssoc(this.name, _ctx.cursor())) != null) {
      _js.appendConstant(lName);
      _js.append(", ");
    }
    else /* maybe we could somehow auto-attach the prev field in the DOM? */
      _js.append("'form', ");

    /* frequency */

    if (lFrequency != null) {
      _js.append(lFrequency);
      _js.append(", ");
    }

    /* function or link */

    if ((lF = strForAssoc(this.function, _ctx.cursor())) != null) {
      _js.append(lF); /* function object, like 'myCallback' */
    }
    else if (this.callElement != null) {
      _js.append("function(element, value) { ");
      this.callElement.appendJavaScript(_js, _ctx);
      _js.append("}");
    }
    else {
      log.warn("got no observer object ...");
      _js.reset();
      return;
    }

    /* on parameter */
    if ((lOn = strForAssoc(this.on, _ctx.cursor())) != null) {
      _js.append(", ");
      _js.appendConstant(lOn);
      _js.append(lOn);
    }

    /* close Observer function call */
    _js.append(");");
  }

  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    if (_ctx.isRenderingDisabled())
      return;

    WOJavaScriptWriter js = new WOJavaScriptWriter();
    this.appendJavaScript(js, _ctx);
    _r.appendContentScript(js);
  }
}
