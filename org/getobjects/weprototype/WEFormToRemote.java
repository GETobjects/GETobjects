/*
  Copyright (C) 2006-2008 Helge Hess

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

import org.getobjects.appserver.associations.WOValueAssociation;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.elements.WOForm;

/**
 * WEFormToRemote
 * <p>
 * Inspired by RoR:
 *   http://api.rubyonrails.org/classes/ActionView/Helpers/PrototypeHelper.html
 * <p>
 * Sample:
 * <pre>
 *   Link: WEFormToRemote {
 *     directActionName = "postComment";
 *     actionClass      = "CommentPage";
 *     ?comment         = "blub";
 *     update           = "time_div";
 *     position         = "after";
 *   }</pre>
 * <p>
 * Renders:
 * <pre>
 *   TODO: document
 *   &lt;form href="[link]" onsubmit="[script]"&gt;
 *     [sub-template]
 *   &lt;/form&gt;</pre>
 * <p>
 *
 * Bindings:<pre>
 *   id                 [in] - string (elementID and HTML DOM id)
 *   target             [in] - string
 *   method             [in] - string (POST/GET)
 *   forceTakeValues    [in] - boolean (whether the form *must* run takevalues)
 * </pre>
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
 *
 * AJAX Bindings:<pre>
 *   type               [in] - synchronous/asynchronous
 *   update             [in] - string|dict (element id or keyed on handler)
 *   position           [in] - string      (before/top/bottom/after/..)
 *   event              [in] - string      (event handler to use (def: onclick))
 *
 *   confirm            [in] - string (JS function or code)
 *   condition          [in] - string (JS function or code)
 *   before             [in] - string (JS function or code)
 *   after              [in] - string (JS function or code)
 *   submit             [in] - string (element-id of form)
 * </pre>
 * Callback Bindings:<pre>
 *   success            [in] - string (JS function or code)
 *   failure            [in] - string (JS function or code)
 *   complete           [in] - string (JS function or code)
 *   - triggered at the very end, all status callbacks got called
 *   &lt;3-digits&gt; (404)   [in] - string (JS function or code)
 *   - the code is wrapped in: "function(request, json) { code }"
 *   loading            [in] - string (JS function or code) [not guaranteed]
 *   loaded             [in] - string (JS function or code) [not guaranteed]
 *   interactive        [in] - string (JS function or code) [not guaranteed]
 * </pre>
 *
 * Extra Bindings:<pre>
 *   - !bindings are added to 'style'
 *   - .bindings are added to 'class' if they resolve to true
 * </pre>
 */
public class WEFormToRemote extends WOForm {
  static boolean alwaysPassIn = false;

  protected WELinkToRemoteScript onSubmit;


  public WEFormToRemote
    (final String _name, final Map<String, WOAssociation> _assocs, final WOElement _template)
  {
    super(_name, _assocs, _template);

    if (!_assocs.containsKey("event")) {
      /* we have a different default event */
      _assocs.put("event", new WOValueAssociation("onsubmit"));
    }

    this.onSubmit = new WELinkToRemoteScript
      (_name + "Script", _assocs,
       WELinkToRemoteScript.PARAMETERMODE_FORM,
       this.link);
  }


  /* generate response */

  @Override
  public void appendCoreAttributesToResponse
    (final String _id, final WOResponse _r, final WOContext _ctx)
  {
    final Object cursor = _ctx != null ? _ctx.cursor() : null;

    _r.appendBeginTag("form");
    if (_id != null) _r.appendAttribute("id", _id);

    if (this.link != null) {
      final String url = this.link.fullHrefInContext(_ctx);
      /* Note: this encodes the ampersands in query strings as &amp;! */
      if (url != null) _r.appendAttribute("action", url);
    }
    else {
      /* a form MUST have some target, no? */
      _r.appendAttribute("action", _ctx.componentActionURL());
    }
    
    this.onSubmit.appendToResponse(_r, _ctx);

    if (this.coreAttributes != null)
      this.coreAttributes.appendToResponse(_r, _ctx);

    this.appendExtraAttributesToResponse(_r, _ctx);
    // TODO: otherTagString
    _r.appendBeginTagEnd();
  }

}
