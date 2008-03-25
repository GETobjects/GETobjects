package org.getobjects.weextensions;

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOElementWalker;
import org.getobjects.appserver.core.WOResourceManager;
import org.getobjects.appserver.core.WOResponse;

// TODO: finish the element

/*
 * WETableBox
 * 
 * Renders a simple box which uses icons for the edges.
 * 
 * TODO: document
 */
public class WETableBox extends WEDynamicElement {
  
  protected static final String defIconPattern = "box-%(icon)s.gif";
  
  protected WOAssociation icons;
  protected WOElement     template;

  public WETableBox
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    this.icons    = grabAssociation(_assocs, "icons");
    this.template = _template;
  }

  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    /* find resource manager */
    
    WOResourceManager rm = _ctx.component().resourceManager();
    if (rm == null) rm = _ctx.application().resourceManager();
    if (rm == null) {
      if (this.template != null) 
        this.template.appendToResponse(_r, _ctx);
      return;
    }
    
    /* retrieve settings */

    String pat    = null;
    
    if (this.icons != null)
      pat = this.icons.stringValueInComponent(_ctx.cursor());
    if (pat == null)
      pat = defIconPattern;
    
    /* URLs for icons */
    
    //Map<String, String> icon = new HashMap<String, String>(1);
    
    
    // TODO: finish me
    
    
    /* render content */
    
    if (this.template != null)
      this.template.appendToResponse(_r, _ctx);    
  }
  
  @Override
  public void walkTemplate(WOElementWalker _walker, WOContext _ctx) {
    if (this.template != null)
      _walker.processTemplate(this, this.template, _ctx);
  }
  
  /* description */

  @Override
  public void appendAssocToDescription(StringBuilder _d, String _name, WOAssociation _a) {
    // TODO Auto-generated method stub
    super.appendAssocToDescription(_d, _name, _a);
  }
}
