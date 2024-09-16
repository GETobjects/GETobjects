package org.getobjects.js;

import java.util.Map;

import org.getobjects.appserver.associations.WOValueAssociation;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.elements.WOForm;

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
    if (this.method != null)
      _assocs.put("method", this.method);

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

    String m = null;
    if (this.method != null)
      m = this.method.stringValueInComponent(cursor);
    _r.appendAttribute("method", m != null ? m : "POST");

    this.onSubmit.appendToResponse(_r, _ctx);

    if (this.coreAttributes != null)
      this.coreAttributes.appendToResponse(_r, _ctx);

    this.appendExtraAttributesToResponse(_r, _ctx);
    // TODO: otherTagString
    _r.appendBeginTagEnd();
  }
}
