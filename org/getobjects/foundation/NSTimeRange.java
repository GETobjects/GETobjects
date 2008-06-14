/*
  Copyright (C) 2007-2008 Helge Hess

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
package org.getobjects.foundation;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * NSTimeRange
 * <p>
 * An object to represent a timerange. The endtime is EXCLUSIVE.
 */
public class NSTimeRange extends NSObject
  implements Cloneable, Comparable
{
  /* Note: from/to are sorted */
  protected long    fromTime; // ms since 1970-01-01 00:00:00 GMT
  protected long    toTime;   // ms, EXCLUSIVE
  protected boolean isEmpty;
  
  // TBD: support iCal duration strings

  public NSTimeRange(final long _from, final long _to) {
    super();
    if (_from <= _to) {
      this.fromTime = _from;
      this.toTime   = _to;
    }
    else {
      this.fromTime = _to;
      this.toTime   = _from;
    }
    this.isEmpty  = _from == _to;
  }
  
  public NSTimeRange(final Date _from, final Date _to) {
    this.fromTime = _from == null ? 0 : _from.getTime();
    this.toTime   = _to   == null ? 0 : _to.getTime();
    
    /* Note: a missing _to (toTime=0) marks an OPEN range, not an empty one */
    this.isEmpty  = _from == _to;
  }
  public NSTimeRange(final Date _from, final int _durationInSeconds) {
    this.fromTime = _from == null ? 0 : _from.getTime();
    this.toTime   = this.fromTime + (_durationInSeconds * 1000);
    if (_durationInSeconds < 0) {
      long x = this.fromTime;
      this.fromTime = this.toTime;
      this.toTime = x;
    }
    this.isEmpty  = _durationInSeconds == 0 || this.fromTime == this.toTime;
  }
  
  public NSTimeRange(final Calendar _from, Calendar _to) {
    super();
    
    /* ensure proper ordering */
    if (_from != null && (_to == null || _from.before(_to))) {
      this.fromTime = _from.getTimeInMillis();
    }
    else {
      this.fromTime = _to.getTimeInMillis();
      _to = _from;
    }
    this.toTime  = _to == null ? 0 : _to.getTimeInMillis();
    /* Note: a missing _to (toTime=0) marks an OPEN range, not an empty one */
    this.isEmpty = this.fromTime == this.toTime;
  }
  public NSTimeRange(Calendar _from, final int _durationInSeconds) {
    super();
    
    if (_from == null) _from = Calendar.getInstance(); // not recommended
    
    if (_durationInSeconds == 0) {
      this.fromTime = _from.getTimeInMillis();
      this.isEmpty = true;
    }
    else {
      Calendar to = 
        UDate.calendarByAdding(_from, 0,0,0, 0,0, _durationInSeconds);
      if (_durationInSeconds < 0) {
        this.fromTime = to.getTimeInMillis();
        this.toTime   = _from.getTimeInMillis();
      }
      else {
        this.fromTime = _from.getTimeInMillis();
        this.toTime   = to.getTimeInMillis();
      }
      this.isEmpty = false; /* cannot be empty */
    }
  }
  
  /* accessors */
  
  public long fromTime() {
    return this.fromTime;
  }
  public long toTime() {
    return this.toTime;
  }
  
  public long duration() {
    return this.isEmpty ? 0 : (this.toTime - this.fromTime);
  }
  public int durationInSeconds() {
    return (int)(this.duration() / 1000);
  }
  
  public Date fromDate() {
    return new Date(this.fromTime);
  }
  public Date toDate() {
    return this.isEmpty ? this.fromDate() : new Date(this.toTime);
  }
  
  
  /* operations */
  
  public NSTimeRange nextTimeRange() {
    // Note: be careful, you probably want to use the Calendar to do such
    //       calculations!
    return new NSTimeRange
      (this.toTime, this.toTime + (this.toTime - this.fromTime));
  }
  public NSTimeRange previousTimeRange() {
    // Note: be careful, you probably want to use the Calendar to do such
    //       calculations!
    return new NSTimeRange
      (this.fromTime - (this.toTime - this.fromTime), this.fromTime);
  }

  
  /**
   * Checks whether the given Calendar, Date, NSTimeRange or timestamp (Number)
   * is contained in the timerange.
   * 
   * @param _o - a Calendar, NSTimeRange, Date or Number object
   * @return true if the object is contained in the range
   */
  public boolean contains(final Object _o) {
    if (_o == null)
      return false;
    
    if (_o instanceof Calendar)
      return this.containsCalendar((Calendar)_o);
    
    if (_o instanceof NSTimeRange)
      return this.containsCalendarRange((NSTimeRange)_o);
    
    if (_o instanceof Date)
      return this.containsDate((Date)_o);
    
    if (_o instanceof Number)
      return this.containsDate(new Date(((Number)_o).longValue()));
    
    /* unexpected object */
    return false;
  }
  
  /**
   * Checks whether the given NSTimeRange is completely contained in the
   * time range.
   * To check for overlaps use the overlaps() or intersectWithRange() methods.
   * <p>
   * The method always returns false if the time range is empty
   * 
   * @param _o - a NSTimeRange object
   * @return true if the object is contained in the range
   */
  public boolean containsCalendarRange(final NSTimeRange _range) {
    if (_range == null)
      return false;
    if (this.isEmpty)
      return false; /* TBD: should we 'contain' other empty ranges? */
    
    if (this.fromTime > 0 && _range.fromTime < this.fromTime)
      return false;
    
    if (this.toTime > 0 && _range.toTime >= this.toTime)
      return false;
    
    return true;
  }
  
  /**
   * Checks whether the instant represented by the given Calendar is contained
   * in the time range.
   * 
   * @param _date - some time instant
   * @return true if the date is in the timerange, false otherwise
   */
  public boolean containsCalendar(final Calendar _date) {
    if (_date == null)
      return false;
    if (this.isEmpty)
      return false; /* TBD: should we 'contain' the exact 'from' instant? */
    
    final long dateTime = _date.getTimeInMillis();
    if (this.fromTime > 0 && dateTime < this.fromTime)
      return false;
    if (this.toTime > 0 && dateTime >= this.toTime)
      return false; /* Note: 'to' is *non-inclusive* */
    
    return true;
  }
  
  /**
   * Checks whether the instant represented by the given Date is contained
   * in the time range.
   * 
   * @param _date - some time instant
   * @return true if the date is in the timerange, false otherwise
   */
  public boolean containsDate(final Date _date) {
    if (_date == null)
      return false;
    if (this.isEmpty)
      return false; /* TBD: should we 'contain' the exact 'from' instant? */
    
    final long dateTime = _date.getTime();
    if (this.fromTime > 0 && dateTime < this.fromTime)
      return false;
    if (this.toTime > 0 && dateTime >= this.toTime)
      return false; /* Note: 'to' is *non-inclusive* */
    
    return true;
  }
  
  
  /**
   * Checks whether the <u>whole</u> range is before the object _o.
   * Unless its a NSTimeRange. In this case we just compare the start dates.
   * <p>
   * If _o is an NSTimeRange, this checks whether the fromDate of the
   * receiver is before the fromDate of the _o range.
   * <p>
   * Also check out startsBefore(), it only checks whether the range
   * <u>starts</u> before the given instant.
   * <p>
   * Example:<pre>
   *   Date        now = new Date();
   *   NSTimeRange cebitWeek;
   *   if (cebitWeek.before(now)) // the whole week is before now!
   *     System.out.println("CeBIT is over");
   *   else if (cebitWeek.contains(now))
   *     System.out.println("CeBIT is now");
   *   else
   *     System.out.println("CeBIT is upcoming!");</pre>
   * 
   * @param _o - an NSTimeRange, a Date, Calendar or Number object.
   * @return true if the whole range is before the given instant
   */
  public boolean before(final Object _o) {
    if (_o == null)
      return false;
    if (this.fromTime == 0)
      return false; /* can't be before a range which has no start :-) */
    
    if (_o instanceof NSTimeRange)
      return this.fromTime < ((NSTimeRange)_o).fromTime;
    
    // TBD: before() on instants, should the whole range be before the instant?
    // probably! (other thing is 'startsBefore')
    
    final long dateTime;
    if (_o instanceof Date)
      dateTime = ((Date)_o).getTime();
    else if (_o instanceof Calendar)
      dateTime = ((Calendar)_o).getTimeInMillis();
    else if (_o instanceof Number)
      dateTime = ((Number)_o).longValue();
    else
      return false;
    
    if (dateTime <= this.fromTime) /* _o is before our range */
      return false;
    
    if (!this.isEmpty && dateTime < this.toTime) /* _o is before our end */
      return false;
      
    return true;
  }

  /**
   * Checks whether the time range starts before the time instance given by the
   * object.
   * <p>
   * Example:<pre>
   *   Date        now = new Date();
   *   NSTimeRange cebitWeek;
   *   if (cebitWeek.startsBefore(now)) // over or running
   *     System.out.println("CeBIT is upcoming or now");
   *   else
   *     System.out.println("CeBIT is over!");</pre>
   * 
   * @param _o - an NSTimeRange, a Date, Calendar or Number object.
   * @return true if the range starts before the given instant
   */
  public boolean startsBefore(final Object _o) {
    if (_o == null)
      return false;
    if (this.fromTime == 0)
      return false; /* can't be before a range which has no start :-) */
    
    if (_o instanceof NSTimeRange)
      return this.fromTime < ((NSTimeRange)_o).fromTime;
    
    // TBD: before() on instants, should the whole range be before the instant?
    // probably! (other thing is 'startsBefore')
    
    final long dateTime;
    if (_o instanceof Date)
      dateTime = ((Date)_o).getTime();
    else if (_o instanceof Calendar)
      dateTime = ((Calendar)_o).getTimeInMillis();
    else if (_o instanceof Number)
      dateTime = ((Number)_o).longValue();
    else
      return false;
    
    return this.fromTime < dateTime;
  }
  
  public boolean endsBefore(final Object _o) {
    // TBD: fix for null ranges
    if (_o == null)
      return false;
    
    if (this.isEmpty) {
      /* empty range */
      // TBD: explain
      return this.startsBefore(_o);
    }
    
    if (_o instanceof NSTimeRange)
      return this.toTime <= ((NSTimeRange)_o).fromTime;
    
    // TBD: before() on instants, should the whole range be before the instant?
    // probably! (other thing is 'startsBefore')
    
    final long dateTime;
    if (_o instanceof Date)
      dateTime = ((Date)_o).getTime();
    else if (_o instanceof Calendar)
      dateTime = ((Calendar)_o).getTimeInMillis();
    else if (_o instanceof Number)
      dateTime = ((Number)_o).longValue();
    else
      return false;
    
    return this.toTime <= dateTime;
  }
  
  
  /**
   * Returns true if this timerange and the _other one cover a common section.
   * To calculate the overlapping range, use intersectWithRange().
   * 
   * @param _other - the other timerange
   * @return true on overlap, false if the two are distinct.
   */
  public boolean overlaps(final NSTimeRange _other) {
    if (_other == null)
      return false;
    
    /* if either of the ranges is empty, they cannot overlap */
    if (this.isEmpty || _other.isEmpty)
      return false;
    
    // TBD: fix for open ranges (fromTime/toTime == 0)
    if (this.fromTime < _other.fromTime) {
      /* we starts before other, check whether we end before the other starts */
      if (this.toTime <= _other.fromTime)
        return false;
    }
    else {
      /* _other starts before us, check whether the other ends before we start*/
      if (_other.toTime <= this.fromTime)
        return false;
    }
    
    return true; /* overlap */
  }
  
  /**
   * Calculates the timerange which is shared by this timerange and the _other
   * one. If there is no overlap, this returns null.
   * If you just want to know whether the two ranges overlap, use the overlaps()
   * method.
   * 
   * @param _other - the other timerange
   * @return null if the two ranges do not overlap, or the overlapping range
   */
  public NSTimeRange intersectWithRange(final NSTimeRange _other) {
    if (_other == null)
      return null;
    
    /* if either of the ranges is empty, they cannot overlap */
    if (this.isEmpty || _other.isEmpty)
      return null;
    
    long inFrom;
    long inTo;
    
    if (this.fromTime < _other.fromTime) {
      /* we starts before other, check whether we end before the other starts */
      if (this.toTime <= _other.fromTime)
        return null;
      
      inFrom = _other.fromTime;
    }
    else {
      /* _other starts before us, check whether the other ends before we start*/
      if (_other.toTime <= this.fromTime)
        return null;
      
      inFrom = this.fromTime;
    }
    inTo = (this.toTime < _other.toTime ? this.toTime : _other.toTime);
    
    return new NSTimeRange(inFrom, inTo);
  }
  
  
  /* comparison */
  
  /**
   * Returns true if the two ranges are equal (have the same start/endtime).
   * 
   * @return true if the two ranges are equal, false otherwise
   */
  public boolean isEqualToCalendarRange(final NSTimeRange _other) {
    if (_other == null) return false;
    if (_other == this) return true;
    
    return (_other.fromTime == this.fromTime && _other.toTime == this.toTime);
  }
  
  @Override
  public boolean equals(final Object _obj) {
    if (_obj == null) return false;
    if (_obj == this) return true;
    
    return (_obj instanceof NSTimeRange)
      ? ((NSTimeRange)_obj).isEqualToCalendarRange(this)
      : false;
  }
  
  @Override
  public int hashCode() {
    // TBD: find a proper hashcode    
    return (int)(this.fromTime % 4096);
  }
  
  
  /**
   * This method calls compareTo() on the 'from' dates of the two ranges.
   */
  public int compareTo(final Object o) {
    if (!(o instanceof NSTimeRange))
      return -1; /* we are 'smaller' than null or other objects? */
    
    long res = this.fromTime - ((NSTimeRange)o).fromTime;
    return (res == 0 ? 0 : ((res < 0) ? -1 : 1));
  }

  
  /* cloning */

  @Override
  protected Object clone() throws CloneNotSupportedException {
    return new NSTimeRange(this.fromTime, this.toTime);
  }
  
  
  /* empty */
  
  @Override
  public boolean isEmpty() {
    return this.isEmpty;
  }
  
  public boolean isOpenRange() {
    return !this.isEmpty && (this.fromTime == 0 || this.toTime == 0);
  }
  
  
  /* utility functions */
  
  /**
   * Returns an NSTimeRange which covers a full, 24 hour day. The day is
   * selected by providing an arbitary instant using the _date parameter.
   * The _date parameter also provides the necessary timezone to perform the
   * calculation.
   * 
   * @param _date - the date
   * @return an NSTimeRange representing the day on which _date takes place
   */
  public static NSTimeRange getDayRange(final Calendar _date) {
    // TBD: add tests
    if (_date == null)
      return null;
    
    final Calendar workingCopy = (Calendar)_date.clone();
    
    /* reset to beginning of day */
    workingCopy.set(Calendar.HOUR_OF_DAY, 0);
    workingCopy.set(Calendar.MINUTE,      0);
    workingCopy.set(Calendar.SECOND,      0);
    workingCopy.set(Calendar.MILLISECOND, 0);
    
    final long from = workingCopy.getTimeInMillis();

    /* Add a day, remember the endDate is exclusive, so we don't need to play
     * 23:59:59:59 tricks.
     * 
     * Note: the 'add' respects timezone shifts (I think? ;-)
     */
    workingCopy.add(Calendar.DAY_OF_MONTH, 1);
    
    final long to = workingCopy.getTimeInMillis();
    return new NSTimeRange(from, to);
  }
  public static NSTimeRange getDayRange(final Date _date, final TimeZone _tz) {
    if (_date == null) return null;
    Calendar cal = Calendar.getInstance(_tz);
    cal.setTime(_date);
    return getDayRange(cal);
  }
  
  /**
   * Returns an NSTimeRange which covers a full, 7 day week. The week is
   * selected by providing an arbitary instant using the _date parameter.
   * The _date parameter also provides the necessary timezone to perform the
   * calculation, AND is used to select the first day of the week
   * (getFirstDayOfWeek() method of the java.util.Calendar class).
   * 
   * @param _date - the date
   * @return an NSTimeRange representing the week on which _date takes place
   */
  public static NSTimeRange getWeekRange(final Calendar _date) {
    // TBD: add tests
    if (_date == null)
      return null;
    
    final Calendar workingCopy = (Calendar)_date.clone();
    
    /* reset to beginning of week */
    workingCopy.set(Calendar.DAY_OF_WEEK, _date.getFirstDayOfWeek());
    workingCopy.set(Calendar.HOUR_OF_DAY, 0);
    workingCopy.set(Calendar.MINUTE,      0);
    workingCopy.set(Calendar.SECOND,      0);
    workingCopy.set(Calendar.MILLISECOND, 0);
    
    final long from = workingCopy.getTimeInMillis();

    /* Add a week, remember the endDate is exclusive, so we don't need to play
     * 6days 23:59:59:59 tricks.
     * 
     * Note: the 'add' respects timezone shifts (I think? ;-)
     */
    workingCopy.add(Calendar.WEEK_OF_YEAR, 1);
    
    final long to = workingCopy.getTimeInMillis();
    return new NSTimeRange(from, to);
  }
  public static NSTimeRange getWeekRange(final Date _date, final TimeZone _tz) {
    if (_date == null) return null;
    Calendar cal = Calendar.getInstance(_tz);
    cal.setTime(_date);
    return getWeekRange(cal);
  }
  
  /**
   * Returns an NSTimeRange which covers a month. The month is
   * selected by providing an arbitary instant using the _date parameter.
   * The _date parameter also provides the necessary timezone to perform the
   * calculation, AND is used to select the first day of the month
   * (getMinimum() method of the java.util.Calendar class).
   * 
   * @param _date - the date
   * @return an NSTimeRange representing the week on which _date takes place
   */
  public static NSTimeRange getMonthRange(final Calendar _date) {
    // TBD: add tests
    if (_date == null)
      return null;
    
    final Calendar workingCopy = (Calendar)_date.clone();
    
    /* reset to beginning of month */
    workingCopy.set(Calendar.DAY_OF_MONTH,
        _date.getMinimum(Calendar.DAY_OF_MONTH));
    workingCopy.set(Calendar.HOUR_OF_DAY,  0);
    workingCopy.set(Calendar.MINUTE,       0);
    workingCopy.set(Calendar.SECOND,       0);
    workingCopy.set(Calendar.MILLISECOND,  0);
    
    final long from = workingCopy.getTimeInMillis();

    /* Add a month, remember the endDate is exclusive, so we don't need to play
     * 6days 23:59:59:59 tricks.
     * 
     * Note: the 'add' respects timezone shifts (I think? ;-)
     */
    workingCopy.add(Calendar.MONTH, 1);
    
    final long to = workingCopy.getTimeInMillis();
    return new NSTimeRange(from, to);
  }
  public static NSTimeRange getMonthRange(final Date _date, final TimeZone _tz){
    if (_date == null) return null;
    Calendar cal = Calendar.getInstance(_tz);
    cal.setTime(_date);
    return getMonthRange(cal);
  }
  
  /**
   * Returns an NSTimeRange which covers a full year. The year is
   * selected by providing an arbitary instant using the _date parameter.
   * The _date parameter also provides the necessary timezone to perform the
   * calculation, AND is used to select the first day of the year
   * (getMinimum() method of the java.util.Calendar class).
   * 
   * @param _date - the date
   * @return an NSTimeRange representing the week on which _date takes place
   */
  public static NSTimeRange getYearRange(final Calendar _date) {
    // TBD: add tests
    if (_date == null)
      return null;
    
    final Calendar workingCopy = (Calendar)_date.clone();
    
    /* reset to beginning of year */
    workingCopy.set(Calendar.DAY_OF_YEAR,
        _date.getMinimum(Calendar.DAY_OF_YEAR));
    workingCopy.set(Calendar.HOUR_OF_DAY,  0);
    workingCopy.set(Calendar.MINUTE,       0);
    workingCopy.set(Calendar.SECOND,       0);
    workingCopy.set(Calendar.MILLISECOND,  0);
    
    final long from = workingCopy.getTimeInMillis();

    /* Add a month, remember the endDate is exclusive, so we don't need to play
     * 6days 23:59:59:59 tricks.
     * 
     * Note: the 'add' respects timezone shifts (I think? ;-)
     */
    workingCopy.add(Calendar.YEAR, 1);
    
    final long to = workingCopy.getTimeInMillis();
    return new NSTimeRange(from, to);
  }
  public static NSTimeRange getYearRange(final Date _date, final TimeZone _tz) {
    if (_date == null) return null;
    Calendar cal = Calendar.getInstance(_tz);
    cal.setTime(_date);
    return getYearRange(cal);
  }

  /**
   * Creates a new NSTimeRange by adding each non-zero argument to the
   * given 'from' Calendar.
   * 
   * @param _from   - the start of the range
   * @param _years  - number of years to add, or 0
   * @param _months - number of months to add, or 0
   * @param _days   - number of days to add, or 0
   * @param _hours  - number of hours to add, or 0
   * @param _mins   - number of minutes to add, or 0
   * @param _secs   - number of seconds to add, or 0
   * @return an adjusted Calendar
   */
  public static NSTimeRange getRangeByAdding
    (final Calendar _from, final int _years, final int _months, final int _days,
     final int _hours, final int _mins, final int _secs)
  {
    if (_from == null) return null;
    
    final Calendar to = (Calendar)_from.clone();
    
    if (_years  != 0) to.add(Calendar.YEAR,  _years);
    if (_months != 0) to.add(Calendar.MONTH, _months);
    if (_days   != 0) to.add(Calendar.DAY_OF_MONTH, _days);
    if (_hours  != 0) to.add(Calendar.HOUR,   _hours);
    if (_mins   != 0) to.add(Calendar.MINUTE, _mins);
    if (_secs   != 0) to.add(Calendar.SECOND, _secs);
    
    return new NSTimeRange(_from.getTimeInMillis(), to.getTimeInMillis());
  }

  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.fromTime > 0) {
      _d.append(" from=");
      _d.append(new Date(this.fromTime));
    }
    else if (!this.isEmpty)
      _d.append(" open-from");

    if (this.isEmpty)
      _d.append(" empty");
    else if (this.toTime > 0) {
      _d.append(" to=");
      _d.append(new Date(this.toTime));
    }
    else
      _d.append(" open-end");
  }
}
