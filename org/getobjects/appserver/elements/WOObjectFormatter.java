/*
  Copyright (C) 2007 Helge Hess <helge.hess@opengroupware.org>
  Copyright (C) 2007 Marcus Mueller <znek@mulle-kybernetik.com>

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

import java.text.Format;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.foundation.NSKeyValueCodingAdditions;

/**
 * WOObjectFormatter
 * <p>
 * This formatter formats using an arbitrary java.text.Format object.
 * 
 * <p>
 * Example:
 * <pre>
 * Text: WOString {
 *   value  = event.startDate;
 *   format = session.userDateFormatter;
 * }</pre>
 */
class WOObjectFormatter extends WOFormatter {
  
  protected WOAssociation formatter = null;
  
  public WOObjectFormatter(WOAssociation _fmt) {
    this.formatter = _fmt;
  }

  /* creating Format */
  
  @Override
  public Format formatInContext(WOContext _ctx) {
    if (this.formatter == null)
      return null;
    
    Object o = this.formatter.valueInComponent(_ctx.cursor());
    
    if (o instanceof String) {
      /* Treat it as a keypath against the cursor. This usually happens if the
       * user forgot to use the proper 'var:' binding ..., eg:
       *   <#get formatter="abc" />
       * Correct:
       *   <#get var:formatter="abc" />
       */
      o = NSKeyValueCodingAdditions.Utility.valueForKeyPath
        (_ctx.cursor(), (String)o);
    }
    
    if (o instanceof Format)
      return (Format)o;
    if (o == null)
      return null;

    log.error("got unexpected object instead of Format: " + o);
    return null;
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.formatter != null)
      _d.append(" formatter=" + this.formatter);
    else
      _d.append(" no-formatter");
  }
}