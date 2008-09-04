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

import org.getobjects.appserver.associations.WOValueAssociation;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOElementWalker;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.elements.WOGenericContainer;
import org.getobjects.appserver.elements.WOJavaScriptWriter;
import org.getobjects.appserver.elements.WORepetition;
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
 * Bindings:<pre>
 *   name               [in] - string  (referring to the id of the WOTextField)
 *   elementName        [in] - string  (default div)
 *   class              [in] - string  (class to use for elementName)
 *
 *   tokens             [in] - string  (JS function or code)
 *   updateElement      [in] - string  (JS function or code)
 *   afterUpdateElement [in] - string  (JS function or code)
 *   frequency          [in] - string  (default 0.4 s)
 *   indicator          [in] - string  (element id)
 *   select             [in] - string  (element id)
 *   minChars           [in] - string</pre>
 *
 * TODO: document
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
 * <p>
 * WOListWalker Bindings:<pre>
 *   list       [in]  - java.util.List | Collection | Java array | DOM Node
 *   count      [in]  - int
 *   item       [out] - object (current row)
 *   index      [out] - int
 *   startIndex [in]  - int
 *   identifier [in]  - string (TODO: currently unescaped)
 *   sublist    [in]  - java.util.List | Collection | Java array | DOM Node
 *   isEven     [out] - boolean
 *   isFirst    [out] - boolean
 *   isLast     [out] - boolean
 *   filter     [in]  - EOQualifier/String
 *   sort       [in]  - EOSortOrdering/EOSortOrdering[]/Comparator/String/bool
</pre>
 */
public class WEAutocompleteField extends WEPrototypeElement {

  protected WOElement      template;

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
    this.clazz       = grabAssociation(_assocs, "class");

    this.tokens             = grabAssociation(_assocs, "tokens");
    this.frequency          = grabAssociation(_assocs, "frequency");
    this.minChars           = grabAssociation(_assocs, "minChars");
    this.select             = grabAssociation(_assocs, "select");
    this.indicator          = grabAssociation(_assocs, "indicator");
    this.updateElement      = grabAssociation(_assocs, "updateElement");
    this.afterUpdateElement = grabAssociation(_assocs, "afterUpdateElement");


    String fragmentID = this.name.stringValueInComponent(null) +
                        "_autocompletion";
    _assocs.put("?wofid", new WOValueAssociation(fragmentID));

    this.link = WOLinkGenerator.linkGeneratorForAssociations(_assocs);

    /* construct the template */

    // template needs to be wrapped in a <ul>...</ul>, and each element of
    // the repetition needs to be wrapped in <li>...</li>

    _assocs.put("elementName", new WOValueAssociation("li"));
    WODynamicElement element =
      new WORepetition(fragmentID + "_rep", _assocs, _template);

    _assocs.put("elementName", new WOValueAssociation("ul"));
    element = new WOGenericContainer("ul", _assocs, element);
    this.template = element;
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
    return this.inputElementNameInContext(_ctx) + "_autocompletion";
  }

  protected boolean isFragmentActiveInContext(WOContext _ctx) {
    String rqFragID = _ctx.fragmentID();
    if (rqFragID == null) {
      return false;
    }

    String fragName = autocompleteElementNameInContext(_ctx);
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

  /* generic walker */

  @Override
  public void walkTemplate(WOElementWalker _walker, WOContext _ctx) {
    if (this.template != null)
      _walker.processTemplate(this, this.template, _ctx);
  }
}
