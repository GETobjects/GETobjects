/*
  Copyright (C) 2007 Helge Hess <helge.hess@opengroupware.org>

  This file is part of JOPE.

  JOPE is free software; you can redistribute it and/or modify it under
  the terms of the GNU Lesser General Public License as published by the
  Free Software Foundation; either version 2, or (at your option) any
  later version.

  JOPE is distributed in the hope that it will be useful, but WITHOUT ANY
  WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
  License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with JOPE; see the file COPYING.  If not, write to the
  Free Software Foundation, 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
*/

package org.getobjects.appserver.elements;

import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;

/**
 * WONumberFormatter
 * <p>
 * This formatter formats a java.text.DecimalFormat pattern. Check the
 * DecimalFormat documentation for the possible patterns. 
 * 
 * <p>
 * Example:
 * <pre>
 * Text: WOString {
 *   value        = product.price;
 *   numberformat = "#,##0.00;(#,##0.00)";
 * }</pre>
 */
class WONumberFormatter extends WOFormatter {
  
  protected WOAssociation format = null;
  
  public WONumberFormatter(WOAssociation _fmt) {
    this.format = _fmt;
  }
  
  /* creating Format */
  
  @Override
  public Format formatInContext(WOContext _ctx) {
    // TODO: we should probably cache some formatter objects?
    String fmt = this.format.stringValueInComponent(_ctx.cursor());
    
    if (fmt == null)
      return null;
    
    NumberFormat lf = NumberFormat.getInstance(_ctx.locale());
    if (lf == null)
      return null;
    
    if (lf instanceof DecimalFormat)
      ((DecimalFormat)lf).applyPattern(fmt);
    
    return lf;
  }
  
  /* helper for things the Java Format cannot process */
  
  public String stringForObjectValue(Object _o, WOContext _ctx) {
    if (_o == null)
      return null;
    
    Format fmt = this.formatInContext(_ctx);
    if (fmt == null)
      return (_o != null ? _o.toString() : null);
    
    if (_o instanceof String) {
      String s = (String)_o;
      
      try {
        /* You might say that we could just return the string as-is. But not
         * so ;-)
         * Eg if the input says the string is '1', our formatter could still be
         * configured to return 1.00.
         */
        _o = (s.indexOf('.') >= 0)
          ? Double.parseDouble(s)
          : Integer.parseInt(s);
      }
      catch (NumberFormatException e) {}
    }
    
    if (!(_o instanceof Number)) {
      log.warn("object to format is not a number: " + _o);
      return _o.toString();
    }
    
    return fmt.format(_o);
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.format != null)
      _d.append(" format=" + this.format);
    else
      _d.append(" no-format");
  }    
}