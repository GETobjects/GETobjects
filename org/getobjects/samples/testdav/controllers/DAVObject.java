package org.getobjects.samples.testdav.controllers;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.publisher.GoNotFoundException;
import org.getobjects.appserver.publisher.GoPermission;
import org.getobjects.appserver.publisher.GoRole;
import org.getobjects.appserver.publisher.annotations.DefaultRoles;
import org.getobjects.appserver.publisher.annotations.GoMethod;
import org.getobjects.appserver.publisher.annotations.ProtectedBy;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.NSURL;
import org.getobjects.foundation.UList;
import org.getobjects.foundation.UString;
import org.getobjects.foundation.XMLNS;
import org.getobjects.samples.testdav.objects.DAVMultiStatus;
import org.getobjects.samples.testdav.objects.DAVResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

// Note: this should really be restricted to the 'owner' role, but we don't
//       have that yet?
@ProtectedBy(GoPermission.WebDAVAccess)
@DefaultRoles( anonymous = { }, authenticated = { GoPermission.WebDAVAccess })
public class DAVObject extends NSObject {
  protected static final Log log = LogFactory.getLog("TestDAV");
  
  /* default properties */
  
  public Set<String> allDAVPropertyNames() {
    return new HashSet<String>(UList.create(
        "{DAV:}resourcetype", "{DAV:}getdisplayname"));
  }
  
  /* util */
  
  static DocumentBuilder documentBuilder;
  static {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    try {
      dbf.setNamespaceAware(true);;
      documentBuilder = dbf.newDocumentBuilder();
    }
    catch (ParserConfigurationException e) {
      log.error("Failed to create XML document builder!", e);
    }
  }
  
  static Element createElementNS(String _ns, String _tag, Object _content) {
    // isn't that nice? :-) Just want an Element representing the tag :-)
    Document d = documentBuilder.newDocument();
    Element  e = d.createElementNS(_ns, _tag);
    if (_content != null)
      e.setTextContent(_content.toString());
    return e;
  }
  static Element createElementNS(String _ns, String _tag) {
    return createElementNS(_ns, _tag, null);
  }
  
  /**
   * This returns a '{DAV:}anonymous' Element if no user is authenticated.
   * Otherwise it returns a URL to the principal resource.
   * 
   * @param _ctx
   * @return
   */
  public Object currentUserPrincipalInContext(final WOContext _ctx) {
    final String u = _ctx.activeUser().getName();
    
    if (GoRole.Anonymous.equals(u)) {
      Element e = createElementNS(XMLNS.WEBDAV, "anonymous");
      return e != null ? e : GoRole.Anonymous;
    }
    
    final String url = _ctx.urlWithRequestHandlerKey("principals", u, null);
    return new NSURL(url); // URL can't do relative URLs
  }
  
  public Object principalCollectionSetInContext(final WOContext _ctx) {
    final String url = _ctx.urlWithRequestHandlerKey("principals/", null, null);
    return new NSURL(url); // URL can't do relative URLs
  }
  public Object davOwnerInContext(final WOContext _ctx) {
    final String u = _ctx.activeUser().getName();
    
    if (GoRole.Anonymous.equals(u))
      return null;
    
    // all our objects are owned by the fake account :-)
    return this.currentUserPrincipalInContext(_ctx);
  }
  
  public Object davDisplayName() {
    return null;
  }
  public Object davResourceType() {
    return null;
  }
  public Object davETag() {
    return null;
  }
  public Object davContentType() {
    return null;
  }
  
  public Object valueForPropertyInContext(String _propName, WOContext _ctx) {
    // FIXME: all this should be more automagic and beautiful
    // could be KVC based if the DAVObject's would keep the context?
    if (_propName == null)
      return null;
    
    if (_propName.equals("{DAV:}resourcetype"))
      return this.davResourceType();    
    if (_propName.equals("{DAV:}getetag"))
      return this.davETag();
    if (_propName.equals("{DAV:}getcontenttype"))
      return this.davContentType();
    
    if (_propName.equals("{DAV:}displayname"))
      return this.davDisplayName();
    
    if (_propName.equals("{DAV:}current-user-principal"))
      return this.currentUserPrincipalInContext(_ctx);

    if (_propName.equals("{DAV:}principal-collection-set"))
      return this.principalCollectionSetInContext(_ctx);

    if (_propName.equals("{DAV:}owner"))
      return this.davOwnerInContext(_ctx);
    
    return new GoNotFoundException();
  }
  
  
  /* OPTIONS */
  
  public Object davOptions() {
    // calendar-schedule, calendar-auto-schedule, calendar-availability, 
    // inbox-availability, calendar-proxy, calendarserver-private-events, 
    // calendarserver-private-comments, calendarserver-sharing, 
    // calendarserver-sharing-no-scheduling, calendar-query-extended, 
    // calendar-default-alarms, calendarserver-principal-property-search
    return new String[] {
      "1", "access-control",
      "calendar-access",
      "addressbook",
      "extended-mkcol"
    };
  }

  
  /* Go Actions */
  
  @GoMethod(slot = "PROPPATCH", protectedBy=GoPermission.WebDAVAccess)
  public Object DoPropPatch(WOContext _ctx) {
    /* Sample:
     * <A:propertyupdate xmlns:A="DAV:" xmlns:D="http://apple.com/ns/ical/">
     * <A:set><A:prop>
     * <D:calendar-order >0</D:calendar-order>
     * </A:prop></A:set></A:propertyupdate>
     */
    final WORequest rq = _ctx.request();
    
    final Document d = rq.contentAsDOMDocument();
    Map<String, Object> props = DAVParsers.extractPropPatchProperties(d);
    
    System.err.println("PATCH: " + rq.contentString());
    System.err.println("  Props: " + props);
    return null;
  }
  
  @GoMethod(slot = "REPORT", protectedBy=GoPermission.WebDAVAccess)
  public Object doReport(WOContext _ctx) {
    final WORequest rq   = _ctx.request();
    final Document  d    = rq.contentAsDOMDocument();
    final Element   root = d != null ? d.getDocumentElement() : null;
    if (root == null)
      return null;
    
    return this.doReport(root.getNamespaceURI(), root.getLocalName(), d, _ctx);
  }
  
  @GoMethod(slot = "PROPFIND", protectedBy=GoPermission.WebDAVAccess)
  public Object doPropfind(WOContext _ctx) {
    final WORequest rq = _ctx.request();
    System.err.println("PROPFIND, depth: " + rq.headerForKey("Depth"));
    
    final Document d = rq.contentAsDOMDocument();
    Set<String> props = DAVParsers.extractPropfindProperties(d);
    if (props == null) {
      log.warn("PROPFIND had no body, unusual. Parse error?");
      props = this.allDAVPropertyNames();
    }
    
    log.info("PROPS: " + UString.componentsJoinedByString(props, ", "));
    
    final DAVMultiStatus ms = new DAVMultiStatus(props);
    
    if (true) // FIXME: whats the prefer value for no-depth-0?
      this.addSelfResponse(ms, _ctx.request().uri(), _ctx);
    
    return ms;
  }
  
  
  /* reports */
  
  protected Object doReport
    (String _ns, String _report, Document _rq, WOContext _ctx)
  {
    final WORequest rq = _ctx.request();
    
    System.err.println("REPORT: " + rq.contentString());
       
    // quick hack
    WOResponse r = _ctx.response();
    r.setStatus(403);
    r.setHeaderForKey("text/xml", "content-type");
    r.appendContentString("<error xmlns='DAV:'><supported-report/></error>");
    return r;
  }
  
  /* properties */
  
  public void addSelfResponse(DAVMultiStatus _ms, String _url, WOContext _ctx) {
    final DAVResponse dr    = new DAVResponse(_url);
    final Set<String> props = _ms.requestedProperties();
    
    for (String prop: props)
      dr.setValueForProperty(this.valueForPropertyInContext(prop, _ctx),prop);

    _ms.addResponse(dr);
  }
  
}
