/*
  Copyright (C) 2008 Marcus Mueller

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
import org.getobjects.appserver.elements.WOJavaScriptWriter;
import org.getobjects.appserver.elements.links.WOLinkGenerator;

/**
 * WEAutocompleteField
 * <p>
 * Inspired by Scriptaculous:
 *   http://github.com/madrobby/scriptaculous/wikis/ajax-autocompleter
 * <p>
 * Sample:<pre>
 *   Autocompleter: WEAutocompleteField {
 *     directActionName = "getCompletions";
 *     actionClass      = "SearchPage";
 *     name             = "myInputField";
 *     minChars         = "3";
 *     indicator        = "progressIndicator";
 *   }</pre>
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
 *   name               [in] - string  (referring to the id of the WOTextField)
 *   elementName        [in] - string  (default div)
 *   class              [in] - string  (class to use for elementName)
 *   fragment           [in] - string  (if set, acts as a fragment)
 *
 *   tokens             [in] - string  (JS function or code)
 *   updateElement      [in] - string  (JS function or code)
 *   afterUpdateElement [in] - string  (JS function or code)
 *   frequency          [in] - string  (default 0.4 s)
 *   indicator          [in] - string  (element id)
 *   select             [in] - string  (element id)
 *   minChars           [in] - string
 *
 * TODO: document
 */
public class WEAutocompleteField extends WEPrototypeElement {

  protected WOElement      template;
  protected WOAssociation  fragment;

  protected WOAssociation   name;
  protected WOAssociation   elementName;
  protected WOAssociation   clazz;
  protected WOLinkGenerator link;

  /* options */

  protected WOAssociation tokens;
  protected WOAssociation frequency;
  protected WOAssociation minChars;
  protected WOAssociation select;
  protected WOAssociation indicator;
  protected WOAssociation updateElement;
  protected WOAssociation afterUpdateElement;

  public WEAutocompleteField
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);

    this.name        = grabAssociation(_assocs, "name");
    this.elementName = grabAssociation(_assocs, "elementName");
    this.fragment    = grabAssociation(_assocs, "fragment");
    this.clazz       = grabAssociation(_assocs, "class");

    this.tokens             = grabAssociation(_assocs, "tokens");
    this.frequency          = grabAssociation(_assocs, "frequency");
    this.minChars           = grabAssociation(_assocs, "minChars");
    this.select             = grabAssociation(_assocs, "select");
    this.indicator          = grabAssociation(_assocs, "indicator");
    this.updateElement      = grabAssociation(_assocs, "updateElement");
    this.afterUpdateElement = grabAssociation(_assocs, "afterUpdateElement");

    if (this.fragment != null) {
      _assocs.put("?wofid", this.fragment);
      this.template = _template;
    }
    this.link = WOLinkGenerator.linkGeneratorForAssociations(_assocs);
  }

  /* support */

  /**
   * Checks whether the name binding is set. If so, the name is returned. If
   * not, the current element-id is returned.
   *
   * @param _ctx - the WOContext to operate in
   * @return a 'name' for the form element this element refers to
   */
  protected String inputElementNameInContext(final WOContext _ctx) {
    if (this.name == null) {
      return _ctx.elementID();
    }

    final String s = this.name.stringValueInComponent(_ctx.cursor());
    if (s != null) return s;

    return _ctx.elementID();
  }

  protected String autocompleteElementNameInContext(final WOContext _ctx) {
    return this.inputElementNameInContext(_ctx) + "_autocomplete";
  }

  protected boolean isFragmentActiveInContext(WOContext _ctx) {
    if (this.fragment == null) return false;

    String rqFragID = _ctx.fragmentID();
    if (rqFragID == null) {
      return false;
    }

    String fragName = strForAssoc(this.fragment, _ctx.cursor());
    if (fragName == null) { /* we have no fragid in the current state */
      return false; /* we only render on match */
    }

    if (!rqFragID.equals(fragName)) {
      return false;
    }

    return true;
  }

  /* handle requests */

  @Override
  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    /* links can take form values !!!! (for query-parameters) */
    if (this.link != null)
      this.link.takeValuesFromRequest(_rq, _ctx);
    if (this.template != null)
      this.template.takeValuesFromRequest(_rq, _ctx);
  }

  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    if (this.template == null)
      return this.template.invokeAction(_rq, _ctx);
    return null;
  }

  /* generate response */

  protected void appendOption(WOJavaScriptWriter _js, String _name,
      WOAssociation _assoc, WOContext _ctx)
  {
    if (_assoc == null) return;
    _js.appendString(_name);
    _js.append(": ");
    _js.appendString(strForAssoc(_assoc, _ctx.cursor()));
    _js.append(",");
  }

  protected void appendLiteralOption(WOJavaScriptWriter _js, String _name,
      WOAssociation _assoc, WOContext _ctx)
  {
    if (_assoc == null) return;
    _js.appendString(_name);
    _js.append(": ");
    _js.append(strForAssoc(_assoc, _ctx.cursor()));
    _js.append(",");
  }

  public void appendJavaScript(WOJavaScriptWriter _js, WOContext _ctx) {
    _js.append("new Ajax.Autocompleter(");
    _js.appendConstant(this.inputElementNameInContext(_ctx));
    _js.append(", ");
    _js.appendConstant(this.autocompleteElementNameInContext(_ctx));
    _js.append(", ");
    _js.appendString(this.link.fullHrefInContext(_ctx));
    _js.append(", ");

    /* options */

    _js.beginMap();
    this.appendLiteralOption(_js, "tokens", this.tokens, _ctx);
    this.appendLiteralOption(_js, "updateElement", this.updateElement, _ctx);
    this.appendLiteralOption(_js, "afterUpdateElement",
        this.afterUpdateElement, _ctx);
    this.appendOption(_js, "frequency", this.frequency, _ctx);
    this.appendOption(_js, "minChars",  this.minChars, _ctx);
    this.appendOption(_js, "select",    this.select, _ctx);
    this.appendOption(_js, "indicator", this.indicator, _ctx);
    _js.endMap();

    /* close Ajax.Autocompleter */
    _js.append(");");
  }

  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    boolean isFragmentActive = this.isFragmentActiveInContext(_ctx);

    if (_ctx.isRenderingDisabled() && !isFragmentActive)
      return;

    if (isFragmentActive) {
      if (this.template != null)
        _ctx.enableRendering();
        this.template.appendToResponse(_r, _ctx);
        _ctx.disableRendering();
    }
    else {
      String tagName = strForAssoc(this.elementName, _ctx.cursor());
      if (tagName == null)
        tagName = "div";
      String tagClass = strForAssoc(this.clazz, _ctx.cursor());
      if (tagClass == null)
        tagClass = "autocomplete";

      _r.appendBeginTag(tagName);
      _r.appendAttribute("id", this.autocompleteElementNameInContext(_ctx));
      _r.appendAttribute("class", tagClass);
      _r.appendAttribute("style", "display: none;");
      _r.appendBeginTagEnd();

      // don't render a template in this "mode"

      _r.appendEndTag(tagName);

      WOJavaScriptWriter js = new WOJavaScriptWriter();
      this.appendJavaScript(js, _ctx);
      _r.appendContentScript(js);
    }
  }
}
