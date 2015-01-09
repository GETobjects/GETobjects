package org.getobjects.samples.testdav.controllers;

import org.getobjects.appserver.core.WOContext;
import org.getobjects.foundation.NSURL;
import org.getobjects.foundation.UList;
import org.getobjects.foundation.XMLNS;

public class DAVPrincipal extends DAVObject {
  
  final String name;
  NSURL principalURL;
  
  public DAVPrincipal(final String _name) {
    this.name = _name;
  }

  @Override
  public Object davResourceType() {
    return UList.create(
      createElementNS(XMLNS.WEBDAV, "collection"),
      createElementNS(XMLNS.WEBDAV, "principal")
    );
  }
  
  public Object principalInContext(final WOContext _ctx) {
    if (this.principalURL == null) {
      this.principalURL = new NSURL(
        _ctx.urlWithRequestHandlerKey("principals", this.name, null));
    }
    return this.principalURL;
  }
  
  public Object calendarUserAddressesInContext(final WOContext _ctx) {
    return UList.create(this.principalInContext(_ctx));
  }
  
  public Object calendarHomeSetInContext(final WOContext _ctx) {
    return UList.create(new NSURL(
        _ctx.urlWithRequestHandlerKey("dav", this.name, null)));    
  }
  
  static final String propPrincipalURL = "{DAV:}principal-URL";
  static final String propCUASet =
    "{urn:ietf:params:xml:ns:caldav}calendar-user-address-set";
  static final String propHomeSet =
    "{urn:ietf:params:xml:ns:caldav}calendar-home-set";
  
  @Override
  public Object valueForPropertyInContext(String _pn, WOContext _ctx) {
    /* Pending iOS7 props:
     * {http://calendarserver.org/ns/}dropbox-home-URL, 
     * {urn:ietf:params:xml:ns:caldav}schedule-outbox-URL,
     * {http://calendarserver.org/ns/}notification-URL, 
     * {urn:ietf:params:xml:ns:caldav}schedule-inbox-URL,
     * {http://calendarserver.org/ns/}email-address-set,
     * {DAV:}resource-id,
     * {DAV:}supported-report-set,
     * {urn:ietf:params:xml:ns:caldav}calendar-home-set, 
     */
    
    if (propPrincipalURL.equals(_pn))
      return this.principalInContext(_ctx);
    
    if (propCUASet.equals(_pn))
      return this.calendarUserAddressesInContext(_ctx);
    
    if (propHomeSet.equals(_pn))
      return this.calendarHomeSetInContext(_ctx);
    
    return super.valueForPropertyInContext(_pn, _ctx);
  }
}
