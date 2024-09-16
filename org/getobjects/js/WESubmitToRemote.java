package org.getobjects.js;

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.elements.WOInput;

public class WESubmitToRemote extends WOInput {
  protected WOElement onClick;

  public WESubmitToRemote
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.onClick = new WELinkToRemoteScript
      (_name + "Script", _assocs, 
       WELinkToRemoteScript.PARAMETERMODE_FORMELEMENT,
       null /* link generator */);
  }
  
  
  /* handle requests */
  
  @Override
  public void takeValuesFromRequest(final WORequest _rq, final WOContext _ctx) {
    if (this.onClick != null)
      this.onClick.takeValuesFromRequest(_rq, _ctx);
  }
  
  @Override
  public Object invokeAction(final WORequest _rq, final WOContext _ctx) {
    if (_ctx.elementID().equals(_ctx.senderID())) {
      if (this.onClick != null)
        return this.onClick.invokeAction(_rq, _ctx);
      return null;
    }
    return null;
  }
  
  
  /* generate response */
    
  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    if (_ctx.isRenderingDisabled())
      return;
    
    final Object cursor = _ctx.cursor(); 
    String lid = this.eid!=null ? this.eid.stringValueInComponent(cursor):null;
    
    /* start anker */

    _r.appendBeginTag("input");
    _r.appendAttribute("type",  "submit");
    if (lid != null) _r.appendAttribute("id", lid);
    _r.appendAttribute("name",  this.elementNameInContext(_ctx));
    
    if (this.readValue != null) {
      _r.appendAttribute("value",
          this.readValue.stringValueInComponent(cursor));
    }
    
    if (this.disabled != null) {
      if (this.disabled.booleanValueInComponent(cursor)) {
        _r.appendAttribute("disabled",
            _ctx.generateEmptyAttributes() ? null : "disabled");
      }
    }
    
    this.onClick.appendToResponse(_r, _ctx);
    if (this.coreAttributes != null)
      this.coreAttributes.appendToResponse(_r, _ctx);
    this.appendExtraAttributesToResponse(_r, _ctx);
    // TODO: otherTagString
    
    _r.appendBeginTagClose(_ctx.closeAllElements());
  }}
