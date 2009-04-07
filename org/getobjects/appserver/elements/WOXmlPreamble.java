/*
  Copyright (C) 2009 Marcus Mueller

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
package org.getobjects.appserver.elements;

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.foundation.NSHtmlAttributeEntityTextCoder;
import org.getobjects.foundation.NSXmlEntityTextCoder;
import org.getobjects.foundation.UObject;

/**
 * WOXmlPreamble
 * <p>
 * Used to generate the <code>&lt;?xml?&gt;</code> preamble and to configure
 * proper content coders for the used response.
 * <p>
 * Sample:
 * <pre>
 *   &lt;#WOXmlPreamble&gt;&lt;/#WOXmlPreamble&gt;</pre>
 * Renders:
 * <pre>
 *   &lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;
 * </pre>
 *
 *
 * Bindings:
 * <pre>
 *   version    [in] - string (default: "1.0")
 *   encoding   [in] - IANA charset to use, i.e. "UTF-8"
 *   standalone [in] - bool (default: empty)
 * </pre>
 */
public class WOXmlPreamble extends WOHTMLDynamicElement {

  /* constants */

  public static final WOAssociation versionAssoc =
    WOAssociation.associationWithValue("1.0");
  public static final WOAssociation encodingAssoc =
    WOAssociation.associationWithValue("UTF-8");

  protected WOAssociation version;
  protected WOAssociation encoding;
  protected WOAssociation standalone;

  public WOXmlPreamble
  (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
{
  super(_name, _assocs, _template);

  if ((this.version = grabAssociation(_assocs, "version")) == null)
    this.version = versionAssoc;

  if ((this.encoding = grabAssociation(_assocs, "encoding")) == null)
    this.encoding = encodingAssoc;

  this.standalone = grabAssociation(_assocs, "standalone");
}

  /* response generation */

  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    _r.setTextCoder(NSXmlEntityTextCoder.sharedCoder,
        NSHtmlAttributeEntityTextCoder.sharedCoder);

    if (_ctx.isRenderingDisabled())
      return;

    final Object cursor = _ctx.cursor();

    /* render tag */

    _r.appendBeginTag("?xml");

    String lVersion = this.version.stringValueInComponent(cursor);
    if (UObject.isNotEmpty(lVersion))
      _r.appendAttribute("version", lVersion);

    String lEncoding = this.encoding.stringValueInComponent(cursor);
    if (UObject.isNotEmpty(lEncoding))
      _r.appendAttribute("encoding", lEncoding);

    if (this.standalone != null) {
      final boolean tf = this.standalone.booleanValueInComponent(cursor);
      _r.appendAttribute("standalone", tf ? "yes" : "no");
    }

    this.appendExtraAttributesToResponse(_r, _ctx);
    _r.appendContentCharacter('?');
    _r.appendBeginTagEnd();
  }
}
