package org.getobjects.woextensions;

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.elements.links.WOLinkGenerator;

/*
 * WOIFrame
 * 
 * TODO: document
 * 
 * Sample:
 *   Body: WOIFrame {
 *     src = "/images/mybackground.gif";
 *   }
 * 
 * Renders:
 *   <body background="/images/mybackground.gif">[sub-template]</body>
 * 
 * Bindings (regular extra attributes):
 *   name         [in] - string (unique frame id)
 *   frameborder  [in] - boolean (0/no or 1/yes)
 *   width        [in] - string (pixels or percent)
 *   height       [in] - string (pixels or percent)
 *   marginheight [in] - int (pixels)
 *   marginwidth  [in] - int (pixels)
 *   scrolling    [in] - string ([auto]/yes/no)
 *   align        [in] - string (bottom/middle/top/right/left)
 *   longdesc     [in] - URL
 * 
 * Bindings (WOLinkGenerator for image resource):
 *   src              [in] - string
 *   filename         [in] - string
 *   framework        [in] - string
 *   actionClass      [in] - string
 *   directActionName [in] - string
 *   queryDictionary  [in] - Map<String,String>
 *   ?wosid           [in] - boolean (constant!)
 *   - all bindings starting with a ? are stored as query parameters.
 *   
 * TODO: 'value', does 'pageName' work?
 */
public class WOIFrame extends WODynamicElement {

  protected WOElement       template;
  protected WOLinkGenerator link;

  public WOIFrame
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.link = WOLinkGenerator
      .rsrcLinkGeneratorForAssociations("src", _assocs);
    
    this.template = _template;
  }
  
  /* generate response */

  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    boolean doRender = !_ctx.isRenderingDisabled();
      
    if (doRender) {
      _r.appendBeginTag("iframe");
      
      if (this.link != null)
        _r.appendAttribute("src", this.link.fullHrefInContext(_ctx));
      
      this.appendExtraAttributesToResponse(_r, _ctx);
      // TODO: otherTagString
      
      _r.appendBeginTagEnd();
    }
    
    if (this.template != null)
      this.template.appendToResponse(_r, _ctx);
    
    if (doRender) _r.appendEndTag("iframe");
  }
  
  /* description */
  
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.link != null) {
      _d.append(" src=");
      _d.append(this.link.toString());
    }
  }  
}
