package org.getobjects.samples.HelloDAV.controllers;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.foundation.UDate;
import org.getobjects.foundation.UList;
import org.getobjects.foundation.UMap;
import org.getobjects.foundation.UString;
import org.getobjects.foundation.XMLNS;
import org.getobjects.samples.HelloDAV.objects.DAVMultiStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DAVCalendar extends DAVCollection {

  protected final String name;
  protected final Map<String, DAVObject> children;
  
  @SuppressWarnings("unchecked")
  public DAVCalendar(final String _name) {
    this.name = _name;
    
    final TimeZone tz  = TimeZone.getTimeZone("UTC");
    final Date now     = new Date();
    final Date start1  = UDate.dateByAdding(now,    0, 0, 0, 1, 42, 10, tz);
    final Date end1    = UDate.dateByAdding(start1, 0, 0, 0, 0, 42,  0, tz);
    final Date start2  = UDate.dateByAdding(now,    0, 0, 0, 0, 0,   0, tz);
    final Date end2    = UDate.dateByAdding(start2, 0, 0, 0, 0, 0,  15, tz);
    this.children = UMap.create(
      "event3.ics", 
        new DAVCalendarEvent("Reg7-9823891234", start1, end1, "Register 7"),
      "now.ics", 
        new DAVCalendarEvent("Reg7-9823dsaf98983", start2, end2, "now()")
    );
  }
  
  @Override
  public Object lookupName(String _name, IGoContext _ctx, boolean _acquire) {
    if (this.children != null) {
      Object c = this.children.get(_name);
      if (c != null)
        return c;
    }
    return super.lookupName(_name, _ctx, _acquire);
  }  
  
  /* children */

  public Collection<String> davChildKeysInContext(WOContext _ctx) {
    // This is very inefficient, but quite OK for the 10 records of our demo :-)
    return this.children != null ? this.children.keySet() : null;
  }
  
  
  /* DAV properties */
  
  @Override
  public String davDisplayName() {
    return "CalyCal";
  }

  @Override
  public Object davResourceType() {
    return UList.create(
      createElementNS(XMLNS.WEBDAV, "collection"),
      createElementNS(XMLNS.CALDAV, "calendar")
    );
  }
  
  public Object davSupportedCalendarComponentSet() {
    Element e = createElementNS(XMLNS.CALDAV, "comp");
    e.setAttribute("name", "VEVENT");
    return UList.create(e);
  }
  
  public Object davCalendarOrder() {
    // iOS really likes to have this, even if there is just one collection :-)
    return 0;
  }
  public Object davCalendarColor() {
    // this is special. the 'symbolic-color: yellow' is an attribute of the
    // *property*. The child value is still a plain string.
    return "#FFCC00";
  }
  
  static final String propCalCompSet =
    "{urn:ietf:params:xml:ns:caldav}supported-calendar-component-set";
  static final String propCalOrder =
    "{http://apple.com/ns/ical/}calendar-order";
  static final String propCalColor =
    "{http://apple.com/ns/ical/}calendar-color";

  @Override
  public Object valueForPropertyInContext(String _propName, WOContext _ctx) {
    if (propCalCompSet.equals(_propName))
      return this.davSupportedCalendarComponentSet();
    
    if (propCalOrder.equals(_propName))
      return this.davCalendarOrder();
    if (propCalColor.equals(_propName))
      return this.davCalendarColor();
    
    return super.valueForPropertyInContext(_propName, _ctx);
  }
  
  protected Object doCalendarQuery(Document d, WOContext _ctx) {
    /* Sample:
     * <B:calendar-query xmlns:B="urn:ietf:params:xml:ns:caldav">
     *   <A:prop xmlns:A="DAV:">
     *     <A:getetag/>
     *     <A:getcontenttype/>
     *   </A:prop>
     *   <B:filter>
     *     <B:comp-filter name="VCALENDAR">
     *       <B:comp-filter name="VEVENT">
     *         <B:time-range start="20140430T000000Z"/>
     *       </B:comp-filter>
     *     </B:comp-filter>
     *   </B:filter>
     * </B:calendar-query>
     */
    Element     root  = d != null ? d.getDocumentElement() : null;
    Set<String> props = DAVParsers.extractPropertyNameList(root);
    if (props == null) {
      log.warn("CalQuery had no body, unusual. Parse error?");
      props = this.allDAVPropertyNames();
    }
    
    log.info("PROPS: " + UString.componentsJoinedByString(props, ", "));
    
    // Exactly, we don't do an actual query for our Fake imp ;-)
    final DAVMultiStatus ms = new DAVMultiStatus(props);
    this.addResponsesForChildren(ms, _ctx);
    return ms;
  }
  
  protected Object doMultiGetReport(Document d, WOContext _ctx) {
    Element     root  = d != null ? d.getDocumentElement() : null;
    Set<String> props = DAVParsers.extractPropertyNameList(root);
    if (props == null) {
      log.warn("MultiGet REPORT had no body, unusual. Parse error?");
      props = this.allDAVPropertyNames();
    }
    
    log.info("PROPS: " + UString.componentsJoinedByString(props, ", "));
    final DAVMultiStatus ms = new DAVMultiStatus(props);
    this.addResponsesForChildren(ms, _ctx);
    return ms;
  }
  
  @Override
  protected Object doReport
    (String _ns, String _report, Document _rq, WOContext _ctx)
  {
    if (XMLNS.CALDAV.equals(_ns)) {
      if ("calendar-query".equals(_report))
        return this.doCalendarQuery(_rq, _ctx);

      if ("calendar-multiget".equals(_report))
        return this.doMultiGetReport(_rq, _ctx);
    }
    
    return super.doReport(_ns, _report, _rq, _ctx);
  }

  /* iOS7 props:
   * {DAV:}add-member, 
   * {DAV:}resource-id, 
   * {DAV:}sync-token, 
   * {DAV:}supported-report-set, 
   * {DAV:}quota-used-bytes, 
   * {DAV:}quota-available-bytes, 
   * {DAV:}current-user-privilege-set, 
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
