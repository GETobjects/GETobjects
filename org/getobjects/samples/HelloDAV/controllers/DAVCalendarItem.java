package org.getobjects.samples.testdav.controllers;

import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.publisher.GoPermission;
import org.getobjects.appserver.publisher.annotations.GoMethod;

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
  
  abstract String calendarData();
  
  static final String CRLF = "\r\n";
  static final String propCalData =
    "{urn:ietf:params:xml:ns:caldav}calendar-data";

  @Override
  public Object valueForPropertyInContext(String _propName, WOContext _ctx) {
    if (propCalData.equals(_propName))
      return this.calendarData();
     
    return super.valueForPropertyInContext(_propName, _ctx);
  }
  
  /* Go methods */
  
  @GoMethod(slot = "default", protectedBy=GoPermission.WebDAVAccess)
  public Object doGET(WOContext _ctx) {
    String calData = this.calendarData();
    
    final WOResponse r = _ctx.response();
    r.setHeaderForKey(this.davContentType().toString(), "Content-Type");
    r.setHeaderForKey(this.davETag().toString(),        "ETag");
    r.appendContentString(calData);    
    return r;
  }
}
