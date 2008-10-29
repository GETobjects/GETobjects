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

import java.text.Format;
import java.text.ParseException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.foundation.NSObject;

/**
 * WOFormatter
 * <p>
 * Helper class which deals with formatting attributes of WODynamicElement's.
 * It is based upon java.text.Format.
 * <p>
 * THREAD: remember that Format objects are usually not thread-safe.
 */
public abstract class WOFormatter extends NSObject {
  protected static final Log log = LogFactory.getLog("WOFormatter");
  
  /**
   * Extracts a WOFormatter for the given associations. This checks the
   * following bindings:
   * <ul>
   *   <li>dateformat
   *   <li>numberformat
   *   <li>formatter
   *   <li>formatterClass
   * </ul>
   * 
   * @param _assocs - the bindings of the element
   * @return a WOFormatter object used to handle the bindings
   */
  public static WOFormatter formatterForAssociations
    (final Map<String, WOAssociation> _assocs)
  {
    // TODO: warn about multiple formatters, OR: wrap multiple formatters in an
    //       compound formatter and return that.
    final WOAssociation  formatterClass;
    WOAssociation fmt;
    
    /* specific formatters */
    
    fmt = WODynamicElement.grabAssociation(_assocs,"dateformat");
    if (fmt != null) {
      return new WODateFormatter(fmt,
          WODynamicElement.grabAssociation(_assocs,"lenient"),
          WODynamicElement.grabAssociation(_assocs,"locale"),
          java.util.Date.class);
    }

    fmt = WODynamicElement.grabAssociation(_assocs,"calformat");
    if (fmt != null) {
      return new WODateFormatter(fmt,
          WODynamicElement.grabAssociation(_assocs,"lenient"),
          WODynamicElement.grabAssociation(_assocs,"locale"),
          java.util.Calendar.class);
    }
    
    fmt = WODynamicElement.grabAssociation(_assocs,"numberformat");
    if (fmt != null) return new WONumberFormatter(fmt, 0);
    
    fmt = WODynamicElement.grabAssociation(_assocs,"currencyformat");
    if (fmt != null) return new WONumberFormatter(fmt, 1 /* currency */);
    
    fmt = WODynamicElement.grabAssociation(_assocs,"percentformat");
    if (fmt != null) return new WONumberFormatter(fmt, 2 /* percent */);
    
    fmt = WODynamicElement.grabAssociation(_assocs,"intformat");
    if (fmt != null) return new WONumberFormatter(fmt, 3 /* integer */);
    
    /* generic formatter */
    
    formatterClass = WODynamicElement.grabAssociation(_assocs,"formatterClass");
    fmt            = WODynamicElement.grabAssociation(_assocs,"formatter");
    
    if (formatterClass != null)
      return new WOClassFormatter(formatterClass, fmt);
    
    if (fmt != null)
      return new WOObjectFormatter(fmt);
    
    if (log.isInfoEnabled())
      log.info("did not find formatter bindings in given assocs: " + _assocs);
    return null;
  }
  
  protected WOFormatter() {
  }
  
  /* methods */
  
  public abstract Format formatInContext(final WOContext _ctx);
  
  
  /* NSFormatter like wrappers */

  /**
   * This method extracts the java.text.Format object of the formatter. It then
   * calls parseObject() on the given _s string.
   * <p>
   * As a special hack, it converts Long objects into Integer objects when the
   * latter can cover the scope.
   * 
   * @param _s   - String to parse
   * @param _ctx - WOContext in which the operation takes place
   * @return the parsed Object
   */
  public Object objectValueForString(final String _s, final WOContext _ctx)
    throws ParseException
  {
    if (_s == null)
      return null;
    
    final Format fmt = this.formatInContext(_ctx);
    //System.err.println("FORMAT WITH: " + fmt);
    //System.err.println("  value: " + _s + " [" + _s.getClass() + "]");
    if (fmt == null)
      return _s;
    
    Object v = fmt.parseObject(_s);
    
    /* Downcast large values, eg if a Long fits into an Integer,
     * we return an Integer.
     * A bit hackish, but better for various reasons (eg JS bridge)
     */
    if (v instanceof Long) {
      long l = ((Long)v).longValue();
      if (l <= Integer.MAX_VALUE && l >= Integer.MIN_VALUE)
        v = new Integer((int)l);
    }
    //System.err.println("  IS: " + v + " [" + v.getClass() + "]");
    return v;
  }
  
  /**
   * This method extracts the java.text.Format object of the formatter. It then
   * calls format() on it to retrieve the String representation of the given
   * value object _o.
   * 
   * @param _o   - Object to render as a String, eg java.util.Date
   * @param _ctx - WOContext in which the operation takes place
   * @return a String representing the value, or null
   */
  public String stringForObjectValue(final Object _o, final WOContext _ctx) {
    if (_o == null)
      return null;
    
    final Format fmt = this.formatInContext(_ctx);
    if (fmt == null)
      return (_o != null ? _o.toString() : null);
    
    return fmt.format(_o);
  }
  
  /**
   * The default implementation of this method just returns the
   * stringForObjectValue().
   * <p>
   * Its intended for situations where the rendered label is different from
   * the editing string. For example a date might be displayed as<pre>
   *   Mon, 21. May 2032</pre>
   * But when the textfield is used to edit the date, it would be rendered
   * as<pre>
   *   2032-05-21</pre>
   * 
   * @param _o   - object to render, eg java.util.Date
   * @param _ctx - the WOContext
   * @return a String suitable for editing fields, eg WOTextField
   */
  public String editingStringForObjectValue(final Object _o, WOContext _ctx) {
    return this.stringForObjectValue(_o, _ctx);
  }
}
