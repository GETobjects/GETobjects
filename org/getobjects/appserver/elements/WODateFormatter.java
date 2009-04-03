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
 * WODateFormatter
 * <p>
 * This formatter takes a String and returns a java.util.Date or
 * java.util.Calendar object.
 * The transformation can either use a predefined key, like 'SHORT' or
 * 'DATETIME.SHORT', or a custom format (eg 'dd-MMM-yy') as implemented by
 * the java.text.SimpleDateFormat parser.
 * 
 * <p>
 * Custom Formats (of java.text.SimpleDateFormat):
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
  protected WOAssociation isLenient;
  protected WOAssociation locale;
  
  /* optimization for constant formats */
  protected String        fmtString;
  protected Boolean       isLenientConst;
  protected boolean       isCustomFormat;
  protected boolean       returnCalendar;
  
  public WODateFormatter
    (WOAssociation _fmt, WOAssociation _isLenient, WOAssociation _locale, 
     final Class _resultClass)
  {
    this.format    = _fmt;
    this.isLenient = _isLenient;
    this.locale    = _locale;
    this.returnCalendar = _resultClass == java.util.Calendar.class;
    
    this.isLenientConst = null;
    if (_fmt != null && this.locale == null) {
      if (_fmt.isValueConstant() &&
          (_isLenient == null || _isLenient.isValueConstant()))
      {
        Object v = _fmt.valueInComponent(null /* cursor */);
        if (v instanceof SimpleDateFormat)
          this.fmtString = ((SimpleDateFormat)v).toPattern();
        else if (v instanceof String)
          this.fmtString = (String)v;
        else if (v != null) {
          log.warn("using non-string value as date format: " + v);
          this.fmtString = v.toString();
        }
        
        this.isCustomFormat = isCustomDateFormat(this.fmtString);
        
        if (_isLenient != null)
          this.isLenientConst = _isLenient.booleanValueInComponent(null);
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
  
  /**
   * Returns true if the the given date format string contains a custom date
   * format, that is, not a keyword value like:
   * <ul>
   *   <li>SHORT
   *   <li>LONG
   *   <li>DATE.SHORT
   *   <li>DATETIME.MEDIUM
   *   <li>...
   * </ul>
   * Example:<pre>
   *   dd.MM.yyyy</pre>
   * 
   * @param _fmt
   * @return
   */
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
   * context (more exactly, this returns a java.text.DateFormat object).
   * 
   * @param _ctx - the WOContext
   * @return a java.text.Format, or null if none could be built
   */
  @Override
  public Format formatInContext(final WOContext _ctx) {
    // TODO: we should probably cache some formatter objects? (per thread!)
    // TODO: find out how to do date patterns
    //
    // eg: DateFormat expiresFormat1
    //     = new SimpleDateFormat("E, dd-MMM-yyyy k:m:s 'GMT'", Locale.US)
    final String  fmt;
    final boolean isCustom;
    final Boolean lIsLenient;
    
    if (this.fmtString != null) { /* we had constant bindings, faster */
      fmt        = this.fmtString;
      isCustom   = this.isCustomFormat;
      lIsLenient = this.isLenientConst;
    }
    else { /* format bindings were dynamic, we need to eval to get the format */
      final Object cursor = _ctx != null ? _ctx.cursor() : null;
      fmt = this.format.stringValueInComponent(cursor);
      if (fmt == null)
        return null;
      
      isCustom = isCustomDateFormat(fmt);
      
      lIsLenient = this.isLenient != null
        ? this.isLenient.booleanValueInComponent(cursor)
        : null;
    }
    
    /* determine locale */
    
    Locale lLocale = null;
    if (this.locale != null) {
      Object v = this.locale.valueInComponent(_ctx != null?_ctx.cursor() :null);
      if (v instanceof Locale)
        lLocale = (Locale)v;
      else if (v instanceof String)
        lLocale = new Locale((String)v);
      else if (v != null)
        log.error("unexpected 'locale' object: " + v);
    }
    if (lLocale == null)
      lLocale = _ctx != null ? _ctx.locale() : null;

    /* create the java.text.Format object for the given format string */
    
    DateFormat lFormat;
    
    if (isCustom) {
      /* a custom format String, eg: "E, dd-MMM-yyyy k:m:s 'GMT'" */
      try {
        lFormat = lLocale != null
          ? new SimpleDateFormat(fmt, lLocale)
          : new SimpleDateFormat(fmt);
      }
      catch (IllegalArgumentException e) {
        WOFormatter.log.error("invalid date format: " + fmt, e);
        lFormat =
          DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL);
      }
    }
    else {
      /* keyword format, use getDateTimeInstance() and companions */
      int mode = DateFormat.SHORT;
      if (fmt.endsWith("SHORT"))       mode = DateFormat.SHORT;
      else if (fmt.endsWith("MEDIUM")) mode = DateFormat.MEDIUM;
      else if (fmt.endsWith("LONG"))   mode = DateFormat.LONG;
      else if (fmt.endsWith("FULL"))   mode = DateFormat.FULL;
      
      if (fmt.startsWith("DATETIME")) {
        lFormat = _ctx != null
          ? DateFormat.getDateTimeInstance(mode, mode, lLocale)
          : DateFormat.getDateTimeInstance(mode, mode);
      }
      else if (fmt.startsWith("TIME")) {
        lFormat = _ctx != null
          ? DateFormat.getTimeInstance(mode, lLocale)
          : DateFormat.getTimeInstance(mode);
      }
      else {
        lFormat = _ctx != null
          ? DateFormat.getDateInstance(mode, lLocale)
          : DateFormat.getDateInstance(mode);
      }
    }

    /* final customizations */
    
    if (lFormat != null) {
      /* apply timezone stored in WOContext */
      final TimeZone tz = _ctx != null ? _ctx.timezone() : null;
      if (tz != null) lFormat.setTimeZone(tz);
      
      /* apply lenient configuration */
    
      if (lIsLenient != null && lFormat instanceof SimpleDateFormat)
        ((SimpleDateFormat)lFormat).setLenient(lIsLenient.booleanValue());
    }
    
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
    
    /* Invoke the WOFormatter method, which calls formatInContext() and then
     * use the returned Format to convert the String into an object.
     */
    Object v = super.objectValueForString(_s, _ctx);
    
    
    if (this.returnCalendar) {
      /* a 'calformat' binding was used (instead of 'dateformat') */
      if (v instanceof Date) {
        Calendar cal = Calendar.getInstance(_ctx.locale());
        cal.setTime((Date)v);
        v = cal;
      }
      else if (v instanceof Number) {
        // TBD: document how/when this can happen
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