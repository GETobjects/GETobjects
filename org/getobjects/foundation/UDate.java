/*
 * Copyright (C) 2007 Helge Hess <helge.hess@opengroupware.org>
 * 
 * This file is part of Go.
 * 
 * Go is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2, or (at your option) any later version.
 * 
 * Go is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Go; see the file COPYING. If not, write to the Free Software
 * Foundation, 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.getobjects.foundation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * UDate
 * <p>
 * Helper methods for the Date/Calendar objects.
 */
public class UDate extends NSObject {
  protected static Log log = LogFactory.getLog("UDate");

  private UDate() { } /* do not allow construction */
  
  /* sleep */
  
  /**
   * Convenience function which wraps Thread.sleep().
   * 
   * @param _timeout - number of seconds to wait
   * @return true if the sleep() succeeded, false if the thread got interrupted
   */
  public static boolean sleep(double _timeout) {
    try {
      Thread.sleep((long)(_timeout * 1000));
    }
    catch (InterruptedException e) {
      return false;
    }
    return true;
  }
  
  /* dealing with time reference points for calendars */

  /**
   * This returns a Date object which represents the beginning of the week
   * in the given locale.
   * <p>
   * Eg in a German locale this could be
   *   'Mon, 2007-06-04 00:00:00'
   * if the Date being passed in is
   *   'Wed, 2007-06-06 12:34:37'.
   * In US locales the week would start on Sunday etc.
   * 
   * @param _date   - a Date in the week
   * @param _locale - the locale used to create a Calendar for the Date
   * @return a Date representing the start of the week
   */
  public static Date beginningOfWeekAsDateInLocale(Date _date, Locale _locale) {
    Calendar cal = Calendar.getInstance(_locale);
    if (_date != null) cal.setTime(_date);
    return UDate.beginningOfWeekAsDate(cal);
  }
  
  /**
   * This returns a Date object which represents the beginning of the week
   * in the Calendar's locale and timezone.
   * <p>
   * Eg in a German locale this could be
   *   'Mon, 2007-06-04 00:00:00'
   * if the Calendar being passed in is
   *   'Wed, 2007-06-06 12:34:37'.
   * In US locales the week would start on Sunday etc.
   * 
   * @param _calendar - a Calendar in the week
   * @return a Date representing the start of the week
   */
  public static Date beginningOfWeekAsDate(Calendar _calendar) {
    return UDate.beginningOfWeekAsCalendar(_calendar).getTime();
  }
  
  /**
   * This returns a Calendar object which represents the beginning of the week
   * in the Calendar's locale and timezone.
   * <p>
   * Eg in a German locale this could be
   *   'Mon, 2007-06-04 00:00:00'
   * if the Calendar being passed in is
   *   'Wed, 2007-06-06 12:34:37'.
   * In US locales the week would start on Sunday etc.
   * 
   * @param _calendar - a Calendar in the week
   * @return a Calendar representing the start of the week
   */
  public static Calendar beginningOfWeekAsCalendar(Calendar _calendar) {
    if (_calendar == null) {
      /* not recommended as the timezone is most likely incorrect */
      _calendar = Calendar.getInstance();
    }
    else
      _calendar = (Calendar)_calendar.clone();
    
    _calendar.set(Calendar.DAY_OF_WEEK, _calendar.getFirstDayOfWeek());
    _calendar.set(Calendar.HOUR_OF_DAY, 0);
    _calendar.set(Calendar.MINUTE,      0);
    _calendar.set(Calendar.SECOND,      0);
    _calendar.set(Calendar.MILLISECOND, 0);
    return _calendar;
  }
  
  /**
   * This returns a Calendar object which represents the beginning of the month
   * in the Calendar's locale and timezone.
   * <p>
   * Eg in a German locale this could be
   *   'Fri, 2007-06-01 00:00:00'
   * if the Calendar being passed in is
   *   'Wed, 2007-06-06 12:34:37'.
   * 
   * @param _calendar - a Calendar in the month
   * @return a Calendar representing the start of the month
   */
  public static Calendar beginningOfMonthAsCalendar(Calendar _calendar) {
    if (_calendar == null) {
      /* not recommended as the timezone is most likely incorrect */
      _calendar = Calendar.getInstance();
    }
    
    _calendar.set(Calendar.DAY_OF_MONTH,
        _calendar.getMinimum(Calendar.DAY_OF_MONTH));
    _calendar.set(Calendar.HOUR_OF_DAY,  0);
    _calendar.set(Calendar.MINUTE,      0);
    _calendar.set(Calendar.SECOND,      0);
    _calendar.set(Calendar.MILLISECOND, 0);
    return _calendar;
  }
  
  /**
   * Returns the quarter of the year (1-4) for the given Date or Calendar.
   * 
   * @param _object - a Date or a Calendar
   * @param _tz     - TimeZone
   * @param _loc    - Locale
   * @return the quarter (1-4), -1 if _object is no Date/Cal, -2 unknown month
   */
  public static int quarterOfYear(Object _object, TimeZone _tz, Locale _loc) {
    if (_object == null) return 0;
    
    if (_object instanceof Date) {
      final Calendar tcal;
      
      if (_tz != null && _loc != null)
        tcal = Calendar.getInstance(_tz, _loc);
      else if (_tz != null)
        tcal = Calendar.getInstance(_tz);
      else
        tcal = Calendar.getInstance();
      
      tcal.setTime((Date)_object);
      _object = tcal;
    }

    if (!(_object instanceof Calendar)) {
      log.warn("quarterOfYear got unexpected object: " + _object);
      return -1; /* unexpected object */
    }
    
    final Calendar cal = (Calendar)_object;
    int month = cal.get(Calendar.MONTH); // Jan=0 ... Dec=11
    if (month >= 0 && month < 3)  return 1;
    if (month >= 3 && month < 6)  return 2;
    if (month >= 6 && month < 9)  return 3;
    if (month >= 9 && month < 12) return 4;
    log.warn("quarterOfYear got unexpected month: " + month);
    return -2;
  }

  /**
   * Returns the quarter of the year (1-4) for the given Date or Calendar,
   * in the default timezone.
   * 
   * @param _dateOrCal - a Date or a Calendar
   * @return the quarter (1-4), -1 if _object is no Date/Cal, -2 unknown month
   */
  public static int quarterOfYear(final Object _dateOrCal) {
    return quarterOfYear(_dateOrCal, null /* timezone */, null /* locale */);
  }  
  
  /* adding */
  
  /**
   * Creates a new Date by creating a Calendar for the given Date in the given
   * timezone and then calling Calendar.add() for each non-zero argument.
   * 
   * @param _date   - the Date on which the operation is based
   * @param _years  - number of years to add, or 0
   * @param _months - number of months to add, or 0
   * @param _days   - number of days to add, or 0
   * @param _hours  - number of hours to add, or 0
   * @param _mins   - number of minutes to add, or 0
   * @param _secs   - number of seconds to add, or 0
   * @param _tz     - timezone used to create the Calendar for the given date
   * @return an adjusted Date
   */
  public static Date dateByAdding
    (Date _date, int _years, int _months, int _days,
     int _hours, int _mins, int _secs,
     TimeZone _tz)
  {
    if (_date == null) return null;
    
    Calendar cal = _tz != null
      ? Calendar.getInstance(_tz)
      : Calendar.getInstance();
      
    return UDate.calendarByAdding
      (cal, _years, _months, _days, _hours, _mins, _secs).getTime();
  }

  /**
   * Creates a new Calendar by cloning the Calendar which is passed in and then
   * calling Calendar.add() for each non-zero argument.
   * 
   * @param _cal    - the Calendar on which the operation is based
   * @param _years  - number of years to add, or 0
   * @param _months - number of months to add, or 0
   * @param _days   - number of days to add, or 0
   * @param _hours  - number of hours to add, or 0
   * @param _mins   - number of minutes to add, or 0
   * @param _secs   - number of seconds to add, or 0
   * @return an adjusted Calendar
   */
  public static Calendar calendarByAdding
    (Calendar _cal, int _years, int _months, int _days,
     int _hours, int _mins, int _secs)
  {
    if (_cal == null) return null;
    
    Calendar cal = (Calendar)_cal.clone();
    
    if (_years  != 0) cal.add(Calendar.YEAR,  _years);
    if (_months != 0) cal.add(Calendar.MONTH, _months);
    if (_days   != 0) cal.add(Calendar.DAY_OF_MONTH, _days);
    if (_hours  != 0) cal.add(Calendar.HOUR,   _hours);
    if (_mins   != 0) cal.add(Calendar.MINUTE, _mins);
    if (_secs   != 0) cal.add(Calendar.SECOND, _secs);
    
    return cal;
  }

  /**
   * Creates a new Date by cloning the Calendar which is passed in and then
   * calling Calendar.add() for each non-zero argument.
   * 
   * @param _cal    - the Calendar on which the operation is based
   * @param _years  - number of years to add, or 0
   * @param _months - number of months to add, or 0
   * @param _days   - number of days to add, or 0
   * @param _hours  - number of hours to add, or 0
   * @param _mins   - number of minutes to add, or 0
   * @param _secs   - number of seconds to add, or 0
   * @param _tz     - timezone used to create the Calendar for the given date
   * @return an adjusted Date
   */
  public static Date dateByAdding
    (Calendar _cal, int _years, int _months, int _days,
     int _hours, int _mins, int _secs)
  {
    return _cal != null ? UDate.calendarByAdding
      (_cal, _years, _months, _days, _hours, _mins, _secs).getTime() : null;
  }

  /**
   * Uses the SimpleDateFormat parser to parse a date string.
   * 
   * The method catches parse exceptions and returns null on such.
   * 
   * @param _fmt    - a date format as described in SimpleDateFormat
   * @param _date   - a datetime String (Date's are passed through)
   * @param _locale - some locale to retrieve parsing settings
   * @return a Date or null
   */
  public static Date parseSimpleDate
    (final String _fmt, final Object _date, Locale _locale)
  {
    if (_date == null)
      return null;
    if (_date instanceof Date)
      return (Date)_date;
    if (_date instanceof Calendar)
      return ((Calendar)_date).getTime();
    
    final String dateString = _date.toString();
    if (dateString == null || dateString.length() == 0)
      return null;
    
    final SimpleDateFormat df = new SimpleDateFormat(_fmt, _locale);

    Date date = null;
    try {
      date = df.parse(dateString);
    }
    catch (ParseException e) {
      log.info("could not parse date", e);
    }
    return date;
  }
  /**
   * Uses the SimpleDateFormat parser to parse a date string.
   * 
   * The method catches parse exceptions and returns null on such.
   * 
   * @param _fmt    - a date format as described in SimpleDateFormat
   * @param _date   - a datetime String (Date's are passed through)
   * @return a Date or null
   */
  public static Date parseSimpleDate(final String _fmt, final Object _date) {
    return parseSimpleDate(_fmt, _date, null /* locale */);
  }

  /**
   * Uses the SimpleDateFormat parser to parse a date string.
   * 
   * The method catches parse exceptions and returns null on such.
   * 
   * @param _fmt    - a date format as described in SimpleDateFormat
   * @param _date   - a datetime String (Date's are passed through)
   * @param _locale - some locale to retrieve parsing settings
   * @return a Calendar or null
   */
  public static Calendar parseSimpleCalendar
    (final String _fmt, final Object _date, TimeZone _tz, Locale _locale)
  {
    if (_date == null)
      return null;
    if (_date instanceof Calendar)
      return (Calendar)_date;

    final Date date = (_date instanceof Date)
      ? (Date)_date
      : parseSimpleDate(_fmt, _date, _locale);
    if (date == null) return null;
    
    final Calendar cal = Calendar.getInstance(_tz, _locale);
    cal.setTime(date);
    return cal;
  }
}
