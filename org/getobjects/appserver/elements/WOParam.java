package org.getobjects.appserver.elements;

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOResponse;

/**
 * WOParam
 * <p>
 * Parameter value for applets.
 * <p>
 * TODO: document
 * TODO: WO also allows for an 'action' binding. Not sure whethers thats useful.
 */
public class WOParam extends WOHTMLDynamicElement {
  
  protected WOAssociation name;
  protected WOAssociation value;

  public WOParam
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
  }
  
  /* generate responds */

  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    Object cursor = _ctx.cursor();
    
    _r.appendBeginTag("param");
    
    if (this.name != null)
      _r.appendAttribute("name", this.name.stringValueInComponent(cursor));
    if (this.value != null)
      _r.appendAttribute("value", this.value.stringValueInComponent(cursor));
    
    this.appendExtraAttributesToResponse(_r, _ctx);
    if (_ctx.closeAllElements())
      _r.appendBeginTagClose();
    else
      _r.appendBeginTagEnd();
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    this.appendAssocToDescription(_d, "name",  this.name);
    this.appendAssocToDescription(_d, "value", this.value);
  }  
}
