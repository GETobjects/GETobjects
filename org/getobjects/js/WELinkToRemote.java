package org.getobjects.js;

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.elements.WOHTMLElementAttributes;

public class WELinkToRemote extends WEDynamicElement {
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
