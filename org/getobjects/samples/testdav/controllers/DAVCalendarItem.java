package org.getobjects.samples.testdav.controllers;

import org.getobjects.appserver.core.WOContext;

public abstract class DAVCalendarItem extends DAVObject {
  
  protected final String uid;
  
  public DAVCalendarItem(String _uid) {
    this.uid = _uid;
  }

  abstract Object componentType();
  
  @Override
  public Object davContentType() {
    return "text/calendar; component=" + this.componentType();
  }
  
  static int etagCounter = 0;
  public Object davETag() {
    etagCounter++;
    return "\"NeverEndingStory" + etagCounter + "\"";
  }
  
  abstract Object calendarData();
  
  static final String CRLF = "\r\n";
  static final String propCalData =
    "{urn:ietf:params:xml:ns:caldav}calendar-data";

  @Override
  public Object valueForPropertyInContext(String _propName, WOContext _ctx) {
    if (propCalData.equals(_propName))
      return this.calendarData();
     
    return super.valueForPropertyInContext(_propName, _ctx);
  }
}
