/*
  Copyright (C) 2006-2008 Helge Hess

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

import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;

/**
 * Format:
 * <pre>
 * G       Era designator          Text    AD
 * y       Year                    Year    1996; 96
 * M       Month in year           Month   July; Jul; 07
 * w       Week in year            Number  27
 * W       Week in month           Number  2
 * D       Day in year             Number  189
 * d       Day in month            Number  10
 * F       Day of week in month    Number  2
 * E       Day in week             Text    Tuesday; Tue
 * a       Am/pm marker            Text    PM
 * H       Hour in day (0-23)      Number  0
 * k       Hour in day (1-24)      Number  24
 * K       Hour in am/pm (0-11)    Number  0
 * h       Hour in am/pm (1-12)    Number  12
 * m       Minute in hour          Number  30
 * s       Second in minute        Number  55
 * S       Millisecond             Number  978
 * Z       Time zone               RFC 822 time zone       -0800
 * z       Time zone               General time zone
 *                                   Pacific Standard Time; PST; GMT-08:00</pre>
 */
class WODateFormatter extends WOFormatter {
  protected WOAssociation format;
  
  /* optimization for constant formats */
  protected String        fmtString;
  protected boolean       isCustomFormat;
  protected boolean       returnCalendar;
  
  public WODateFormatter(final WOAssociation _fmt, boolean _returnCal) {
    this.format = _fmt;
    this.returnCalendar = _returnCal;
    if (_fmt != null) {
      if (_fmt.isValueConstant()) {
        this.fmtString      = _fmt.stringValueInComponent(null /* cursor */);
        this.isCustomFormat = isCustomDateFormat(this.fmtString);
      }
    }
  }
  
  /* creating Format */
  
  protected static final String[] dateStyleFormats = {
    "SHORT", "MEDIUM", "LONG", "FULL",
    "TIME",  "DATE", "DATETIME",
    "TIME.SHORT",     "TIME.MEDIUM",     "TIME.LONG",     "TIME.FULL",
    "DATE.SHORT",     "DATE.MEDIUM",     "DATE.LONG",     "DATE.FULL",
    "DATETIME.SHORT", "DATETIME.MEDIUM", "DATETIME.LONG", "DATETIME.FULL"
  };
  
  protected static boolean isCustomDateFormat(final String _fmt) {
    /* this is kinda slow, maybe we should do something else or detect
     * constant dateformats so that we can do it once
     */
    if (_fmt == null || _fmt.length() == 0) return false;
    
    if ("SMLFTD".indexOf(_fmt.charAt(0)) == -1)
      return true;
    
    for (int i = 0; i < dateStyleFormats.length; i++) {
      if (_fmt.equals(dateStyleFormats[i]))
        return false;
    }
    
    return true;
  }
  
  /**
   * Returns a java.text.Format object suitable for the bindings in the given
   * context.
   * 
   * @param _ctx - the WOContext
   * @return a java.text.Format, or null if none could be built
   */
  @Override
  public Format formatInContext(final WOContext _ctx) {
    // TODO: we should probably cache some formatter objects?
    // TODO: find out how to do date patterns
    //
    // eg: DateFormat expiresFormat1
    //     = new SimpleDateFormat("E, dd-MMM-yyyy k:m:s 'GMT'", Locale.US)
    String  fmt;
    boolean isCustom;
    
    if (this.fmtString != null) {
      fmt      = this.fmtString;
      isCustom = this.isCustomFormat;
    }
    else {
      fmt = this.format.stringValueInComponent
        (_ctx != null ? _ctx.cursor() : null);

      if (fmt == null)
        return null;
      
      isCustom = isCustomDateFormat(fmt);
    }
    
    DateFormat lFormat;
    
    final Locale locale = _ctx != null ? _ctx.locale() : null;

    if (isCustom) {
      try {
        lFormat = locale != null
          ? new SimpleDateFormat(fmt, locale)
          : new SimpleDateFormat(fmt);
      }
      catch (IllegalArgumentException e) {
        WOFormatter.log.error("invalid date format: " + fmt, e);
        lFormat =
          DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL);
      }
    }
    else {
      int mode = DateFormat.SHORT;
      if (fmt.endsWith("SHORT"))       mode = DateFormat.SHORT;
      else if (fmt.endsWith("MEDIUM")) mode = DateFormat.MEDIUM;
      else if (fmt.endsWith("LONG"))   mode = DateFormat.LONG;
      else if (fmt.endsWith("FULL"))   mode = DateFormat.FULL;
      
      if (fmt.startsWith("DATETIME")) {
        lFormat = _ctx != null
          ? DateFormat.getDateTimeInstance(mode, mode, locale)
          : DateFormat.getDateTimeInstance(mode, mode);
      }
      else if (fmt.startsWith("TIME")) {
        lFormat = _ctx != null
          ? DateFormat.getTimeInstance(mode, locale)
          : DateFormat.getTimeInstance(mode);
      }
      else {
        lFormat = _ctx != null
          ? DateFormat.getDateInstance(mode, locale)
          : DateFormat.getDateInstance(mode);
      }
    }
    
    /* apply timezone */
    
    final TimeZone tz = _ctx != null ? _ctx.timezone() : null;
    if (tz != null && lFormat != null)
      lFormat.setTimeZone(tz);
    
    return lFormat;
  }

  
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
    
    Object v = super.objectValueForString(_s, _ctx);
    
    if (this.returnCalendar) {
      if (v instanceof Date) {
        Calendar cal = Calendar.getInstance(_ctx.locale());
        cal.setTime((Date)v);
        v = cal;
      }
      else if (v instanceof Number) {
        Calendar cal = Calendar.getInstance(_ctx.locale());
        cal.setTimeInMillis(((Number)v).longValue());
        v = cal;
      }
    }
    
    return v;
  }
  
  
  /* support for Calendar objects */
  
  @Override
  public String stringForObjectValue(Object _o, final WOContext _ctx) {
    if (_o == null) // do not trigger DateFormat on 'null'
      return null;
    
    /* convert calendars into dates for formatting */
    if (_o instanceof Calendar)
      _o = ((Calendar)_o).getTime();

    return super.stringForObjectValue(_o, _ctx);
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.format != null) {
      _d.append(" format=");
      _d.append(this.format);
    }
    else
      _d.append(" no-format");
  }    
}