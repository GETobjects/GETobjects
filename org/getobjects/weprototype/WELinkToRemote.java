/*
  Copyright (C) 2006-2007 Helge Hess

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

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.elements.WOHTMLElementAttributes;

/**
 * WELinkToRemote
 * <p>
 * Inspired by RoR:
 *   http://api.rubyonrails.org/classes/ActionView/Helpers/PrototypeHelper.html
 * <p>
 * Sample:<pre>
 *   Link: WELinkToRemote {
 *     directActionName = "postComment";
 *     actionClass      = "CommentPage";
 *     ?comment         = "blub";
 *     update           = "time_div";
 *     position         = "after";
 *   }</pre>
 * <p>
 * Renders:<pre>
 *   &lt;a href="#" onclick="..."&gt;
 *     [sub-template]
 *   &lt;/a&gt;</pre>
 * <p>
 * Bindings (WOLinkGenerator):<pre>
 *   href               [in] - string
 *   action             [in] - action
 *   pageName           [in] - string
 *   directActionName   [in] - string
 *   actionClass        [in] - string
 *   fragmentIdentifier [in] - string
 *   queryDictionary    [in] - Map&lt;String,String&gt;
 *   - all bindings starting with a ? are stored as query parameters.</pre>
 * Bindings:<pre>
 *   string             [in] - string
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
 *   submit             [in] - string (element-id of form)</pre>
 * Callback Bindings:<pre>
 *   loading            [in] - string (JS function or code)
 *   loaded             [in] - string (JS function or code)
 *   interactive        [in] - string (JS function or code)
 *   success            [in] - string (JS function or code)
 *   failure            [in] - string (JS function or code)
 *   complete           [in] - string (JS function or code)
 *   <3-digits> (404)   [in] - string (JS function or code)</pre>
 */
public class WELinkToRemote extends WEPrototypeElement {
  protected WOAssociation string;
  protected WOAssociation fragmentIdentifier;
  protected WOElement     template;
  protected WOElement     onClick;
  protected WOElement     coreAttributes;

  public WELinkToRemote
    (final String _name, final Map<String, WOAssociation> _assocs,
     final WOElement _template)
  {
    super(_name, _assocs, _template);

    this.string   = grabAssociation(_assocs, "string");
    this.fragmentIdentifier = grabAssociation(_assocs, "fragmentIdentifier");
    this.onClick  = new WELinkToRemoteScript(_name + "Script", _assocs, null);

    /* core attributes, those do .class and !style binding handling */

    this.coreAttributes =
      WOHTMLElementAttributes.buildIfNecessary(_name + "_core", _assocs);

    this.template = _template;
  }

  /* handle requests */

  @Override
  public void takeValuesFromRequest(final WORequest _rq, final WOContext _ctx) {
    /* links can take form values !!!! (for query-parameters) */

    if (this.onClick != null)
      this.onClick.takeValuesFromRequest(_rq, _ctx);

    if (this.template != null)
      this.template.takeValuesFromRequest(_rq, _ctx);
  }

  @Override
  public Object invokeAction(final WORequest _rq, final WOContext _ctx) {
    if (_ctx.elementID().equals(_ctx.senderID())) {
      if (this.onClick != null)
        return this.onClick.invokeAction(_rq, _ctx);

      log.error("no action configured for link invocation");
      return null;
    }

    if (this.template != null)
      return this.template.invokeAction(_rq, _ctx);

    return null;
  }

  /* generate response */

  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    if (_ctx.isRenderingDisabled()) {
      if (this.template != null)
        this.template.appendToResponse(_r, _ctx);
      return;
    }

    /* start anker */

    _r.appendBeginTag("a");
    if (this.fragmentIdentifier != null) {
      final Object cursor = _ctx.cursor();
      _r.appendAttribute("href", "#" +
          this.fragmentIdentifier.stringValueInComponent(cursor));
    }
    else {
      _r.appendAttribute("href", "#");
    }

    this.onClick.appendToResponse(_r, _ctx);
    if (this.coreAttributes != null)
      this.coreAttributes.appendToResponse(_r, _ctx);

    this.appendExtraAttributesToResponse(_r, _ctx);
    // TODO: otherTagString
    _r.appendBeginTagEnd();

    /* render link content */

    if (this.template != null)
      this.template.appendToResponse(_r, _ctx);

    if (this.string != null) {
      final Object cursor = _ctx.cursor();
      _r.appendContentHTMLString(this.string.stringValueInComponent(cursor));
    }

    /* close anchor */
    _r.appendEndTag("a");
  }
}
