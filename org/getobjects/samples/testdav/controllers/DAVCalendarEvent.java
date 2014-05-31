package org.getobjects.samples.testdav.controllers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DAVCalendarEvent extends DAVCalendarItem {
  
  Date   startDate;
  Date   endDate;
  String title;

  public DAVCalendarEvent(String _uid, Date _start, Date _end, String _title) {
    super(_uid);
    this.startDate = _start;
    this.endDate   = _end;
    this.title     = _title;
  }

  @Override
  public Object componentType() {
    return "VEVENT"; // for now
  }

  @Override
  public Object calendarData() {
    DateFormat     icsDateFmt = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
    final Date     now = new Date();
    final TimeZone tz  = TimeZone.getTimeZone("UTC");
    
    icsDateFmt.setTimeZone(tz);
    
    return
        "BEGIN:VCALENDAR" + CRLF +
        "VERSION:2.0" + CRLF +
        "PRODID:-//AlwaysRightInstitute//GoFakeServ 0.1//EN" + CRLF +
        "BEGIN:VEVENT" + CRLF +
        "CREATED:20100201T090000Z" + CRLF +
        "DTSTAMP:" + icsDateFmt.format(now)            + "Z" + CRLF +
        "DTSTART:" + icsDateFmt.format(this.startDate) + "Z" + CRLF +
        "DTEND:"   + icsDateFmt.format(this.endDate)   + "Z" + CRLF +
        "UID:"     + this.uid + CRLF +
        "SUMMARY:" + this.title + CRLF + 
        "END:VEVENT" + CRLF +
        "END:VCALENDAR" + CRLF;
  }
}
