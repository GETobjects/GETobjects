package org.getobjects.samples.testdav.controllers;

import java.util.Collection;

import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.foundation.UList;


public class DAVHome extends DAVCollection {

  protected final String name;
    
  public DAVHome(final String _name) {
    this.name = _name;
  }
  
  @Override
  public String davDisplayName() {
    return "Home of " + this.name;
  }
  
  @Override
  public Collection<String> davChildKeysInContext(WOContext _ctx) {
    return UList.create("calendar");
  }
  
  @Override
  public Object lookupName(String _name, IGoContext _ctx, boolean _acquire) {
    if (_name.equals("calendar"))
      return new DAVCalendar(_name);
      
    return super.lookupName(_name, _ctx, _acquire);
  }

/* iOS7 props:
 * {DAV:}add-member, 
 * {DAV:}owner, 
 * {DAV:}resource-id, 
 * {DAV:}sync-token, 
 * {DAV:}supported-report-set, 
 * {DAV:}resourcetype, 
 * {DAV:}quota-used-bytes, 
 * {DAV:}quota-available-bytes, 
 * {DAV:}current-user-privilege-set, 
 * {urn:ietf:params:xml:ns:caldav}supported-calendar-component-set, 
 * {urn:ietf:params:xml:ns:caldav}default-alarm-vevent-date, 
 * {urn:ietf:params:xml:ns:caldav}schedule-default-calendar-URL, 
 * {urn:ietf:params:xml:ns:caldav}calendar-description, 
 * {urn:ietf:params:xml:ns:caldav}supported-calendar-component-sets, 
 * {urn:ietf:params:xml:ns:caldav}calendar-free-busy-set, 
 * {urn:ietf:params:xml:ns:caldav}calendar-timezone, 
 * {urn:ietf:params:xml:ns:caldav}schedule-calendar-transp, 
 * {urn:ietf:params:xml:ns:caldav}default-alarm-vevent-datetime
 * {http://apple.com/ns/ical/}autoprovisioned, 
 * {http://apple.com/ns/ical/}location-code, 
 * {http://apple.com/ns/ical/}calendar-order, 
 * {http://apple.com/ns/ical/}refreshrate, 
 * {http://apple.com/ns/ical/}language-code, 
 * {http://apple.com/ns/ical/}calendar-color, 
 * {http://calendarserver.org/ns/}subscribed-strip-alarms, 
 * {http://calendarserver.org/ns/}pre-publish-url, 
 * {http://calendarserver.org/ns/}pushkey, 
 * {http://calendarserver.org/ns/}push-transports, 
 * {http://calendarserver.org/ns/}source, 
 * {http://calendarserver.org/ns/}getctag,
 * {http://calendarserver.org/ns/}publish-url, 
 * {http://calendarserver.org/ns/}subscribed-strip-attachments, 
 * {http://calendarserver.org/ns/}allowed-sharing-modes, 
 * {http://calendarserver.org/ns/}subscribed-strip-todos, 
 * {http://me.com/_namespace/}bulk-requests, 
 */
}
