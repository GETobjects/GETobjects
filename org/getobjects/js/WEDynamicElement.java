package org.getobjects.js;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.foundation.NSJavaScriptWriter;

public class WEDynamicElement extends WODynamicElement {
  protected static Log log = LogFactory.getLog("WEDynamicElement");

  public WEDynamicElement(String _name, Map<String, WOAssociation> _assocs,
      WOElement _template) {
    super(_name, _assocs, _template);
  }

  protected static String strForAssoc(final WOAssociation _assoc, final Object _cursor) {
    if (_assoc == null)
      return null;
    return _assoc.stringValueInComponent(_cursor);
  }

  protected String getProperIDName(final String _name) {
    return _name.replaceAll("\\.|\\+| |@|\"|\'", "_");
  }

  protected void appendGetElementById(final String _id, final NSJavaScriptWriter _js) {
    _js.append("document.getElementById(");
    _js.appendConstant(_id);
    _js.append(")");
  }

  protected void appendQuerySelector(final String _selector, final NSJavaScriptWriter _js) {
    _js.append("document.querySelector(");
    _js.appendConstant(_selector);
    _js.append(")");
  }
}
