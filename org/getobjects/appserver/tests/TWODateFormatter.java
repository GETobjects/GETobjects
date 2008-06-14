/*
  Copyright (C) 2006 Helge Hess

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

package org.getobjects.appserver.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.elements.WOFormatter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TWODateFormatter extends WOElementTest {
  
  protected Calendar cal19760921;
  protected Date     date19760921;
  
  @Before
  public void setUp() {
    super.setUp();
    
    this.cal19760921 = new GregorianCalendar();
    this.cal19760921.set(1976, Calendar.SEPTEMBER, 21, 12, 21, 00);

    this.date19760921 = this.cal19760921.getTime();
 }

  @After
  public void tearDown() {
    super.tearDown();
  }
  
  /* tests */

  @Test public void testShortDateFormat() {
    this.testDateFormat("SHORT", "21.09.76", this.date19760921);
    this.testDateFormat("SHORT", "21.09.76", this.cal19760921);
  }

  @Test public void testMediumDateFormat() {
    this.testDateFormat("MEDIUM", "21.09.1976", this.date19760921);
  }
  
  @Test public void testLongDateFormat() {
    this.testDateFormat("LONG",   "21. September 1976", this.date19760921);
  }
  
  @Test public void testFullDateFormat() {
    this.testDateFormat("FULL", "Dienstag, 21. September 1976",
                        this.date19760921);
  }

  @Test public void testDateTimeMediumDateFormat() {
    this.testDateFormat("DATETIME.MEDIUM", "21.09.1976 12:21:00",
                        this.date19760921);
  }
  
  @Test public void testDateMediumDateFormat() {
    this.testDateFormat("DATE.MEDIUM", "21.09.1976", this.date19760921);
  }
  @Test public void testTimeMediumDateFormat() {
    this.testDateFormat("TIME.MEDIUM", "12:21:00", this.date19760921);
  }
  
  @Test public void testCustomDateFormatISO() {
    this.testDateFormat("yyyy-MM-dd", "1976-09-21", this.date19760921);
  }

  @Test public void testCustomDateFormat() {
    this.testDateFormat("EEEE', 'dd. MMMM yyyy 'um' hh:mm",
                        "Dienstag, 21. September 1976 um 12:21",
                        this.date19760921);
  }
  
  @Test public void testParseDateTimeMediumDateFormat() throws ParseException {
    this.testParseFormat("DATETIME.MEDIUM", "21.09.1976 12:21:00",
                         this.date19760921);
  }
  
  @Test public void testParseCustomDateFormatISO() throws ParseException {
    WOFormatter formatter = this.formatterForFormat("yyyy-MM-dd");
    assertNotNull("got no formatter for ISO custom format", formatter);
    
    Object o = formatter.objectValueForString("1976-09-21", this.context);
    assertNotNull("formatter return no object for ISO string", o);
    assertTrue("formatter returned no Date object!", o instanceof Date);
    
    /* Note: milliseconds might differ because we don't specify them ...,
     *       apparently Java uses the milliseconds value from the current time?
     */
    Calendar d = Calendar.getInstance(this.context.locale());
    d.setTime((Date)o);
    
    assertEquals("year mismatch",  1976, d.get(Calendar.YEAR));
    assertEquals("month mismatch", 8,    d.get(Calendar.MONTH));
    assertEquals("day mismatch",   21,   d.get(Calendar.DAY_OF_MONTH));
  }
  
  /* support */
  
  protected WOFormatter formatterForFormat(String _format) {
    Map<String, WOAssociation> assocs = new HashMap<String, WOAssociation>(4);
    assocs.put("dateformat", WOAssociation.associationWithValue(_format));
    
    return WOFormatter.formatterForAssociations(assocs);
  }
  
  protected void testDateFormat(String _format, String _result, Object _date) {
    WOFormatter formatter = this.formatterForFormat(_format);
    assertNotNull("got no formatter for format: " + _format, formatter);
    
    String s = formatter.stringForObjectValue(_date, this.context);
    assertEquals("format did not match", _result, s);
  }
  
  protected void testParseFormat(String _format, String _str, Date _date) 
    throws ParseException
  {
    WOFormatter formatter = this.formatterForFormat(_format);
    assertNotNull("got no formatter for format: " + _format, formatter);
    
    Object o = formatter.objectValueForString(_str, this.context);
    assertNotNull("formatter return no object for string: " + _str, o);
    assertTrue("formatter returned no Date object!", o instanceof Date);
    
    /* Note: milliseconds might differ because we don't specify them ...,
     *       apparently Java uses the milliseconds value from the current time?
     */
    Date d = (Date)o;
    long diff = _date.getTime() - d.getTime();
    assertTrue("resulting Date did not match: " + _date + " vs " + d,
               (diff > -1000 || diff < 1000));
  }
}
