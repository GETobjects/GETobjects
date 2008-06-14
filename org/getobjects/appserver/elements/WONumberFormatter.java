/*
  Copyright (C) 2007-2008 Helge Hess <helge.hess@opengroupware.org>

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

import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

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
  protected int numberType = 0; // 1=pcurrency, 2=percent, 3=int
  
  public WONumberFormatter(final WOAssociation _fmt, final int _type) {
    this.format     = _fmt;
    this.numberType = _type;
  }
  
  /* creating Format */
  
  @Override
  public Format formatInContext(final WOContext _ctx) {
    // TODO: we should probably cache some formatter objects?
    final Object fmt = this.format.valueInComponent(_ctx.cursor());
    
    if (fmt == null)
      return null;
    
    final Locale locale = _ctx != null ? _ctx.locale() : null;
    final Format lf;

    int type;
    if (fmt.equals("percent"))
      type = 2;
    else if (fmt.equals("currency"))
      type = 1;
    else if (fmt.equals("integer"))
      type = 3;
    else
      type = this.numberType;
    
    switch (type) {
      case 1:
        lf = NumberFormat.getCurrencyInstance(locale);
        break;
      case 2:
        lf = NumberFormat.getPercentInstance(locale);
        break;
      case 3:
        lf = NumberFormat.getIntegerInstance(locale);
        break;
      case 4:
        lf = NumberFormat.getNumberInstance(locale);
        break;
      default:
        lf = NumberFormat.getInstance(locale);
        break;
    }
    if (lf == null)
      return null;
    
    // TBD: NumberFormat supports min/max values, int-only and more
    
    if (lf instanceof DecimalFormat) {
      DecimalFormat df = (DecimalFormat)lf;
      
      if (fmt instanceof String)
        df.applyPattern((String)fmt);
      
      df.setParseBigDecimal(true);
    }
    
    return lf;
  }
  
  /* helper for things the Java Format cannot process */
  
  /* treat empty strings like null */
  
  @Override
  public Object objectValueForString(String _s, final WOContext _ctx)
    throws ParseException
  {
    // trimming should never hurt in date strings
    if (_s != null) {
      _s = _s.trim();
      
      // and empty strings are never parsed anyways ...
      if (_s.length() == 0)
        _s = null;
    }
    
    return super.objectValueForString(_s, _ctx);
  }
  
  @Override
  public String stringForObjectValue(Object _o, final WOContext _ctx) {
    if (_o == null)
      return null;
    
    final Format fmt = this.formatInContext(_ctx);
    if (fmt == null)
      return (_o != null ? _o.toString() : null);
    
    if (_o instanceof String) {
      String s = (String)_o;
      
      s = s.trim();
      if (s.length() == 0)
        return null;
      
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
      log.warn("object to format is not a number: " + _o.getClass());
      return _o.toString();
    }
    
    return fmt.format(_o);
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.format != null)
      _d.append(" format=" + this.format);
    else
      _d.append(" no-format");
  }    
}