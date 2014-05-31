package org.getobjects.samples.testdav.objects;

import java.net.URL;
import java.util.Collection;
import java.util.Map;

import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.publisher.GoDefaultRenderer;
import org.getobjects.foundation.NSURL;
import org.getobjects.foundation.NSXmlEntityTextCoder;
import org.getobjects.foundation.UObject;
import org.getobjects.foundation.XMLNS;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

public class DAVRenderer extends GoDefaultRenderer {
  
  private final static boolean doIndent = true;
  private final static boolean doStream = false;

  @Override
  public boolean canRenderObjectInContext(Object _object, WOContext _ctx) {
    if (_object instanceof DAVMultiStatus)
      return true;
    
    return super.canRenderObjectInContext(_object, _ctx);
  }
  
  public Exception renderObjectInContext(Object _object, WOContext _ctx) {
    if (_object instanceof DAVMultiStatus)
      return this.renderMultiStatus((DAVMultiStatus)_object, _ctx);
    
    return super.renderObjectInContext(_object, _ctx);
  }
  
  protected void appendDAVPropStatToResponse
    (final int status, final Map<String, Object> propToValue,
     final WOResponse _r, final WOContext _ctx)
  {
    if (doIndent) _r.appendContentString("    ");
    _r.appendContentString("<propstat>");
    if (doIndent) _r.appendContentString("\n      ");
    _r.appendContentString("<status>HTTP/1.1 ");
    _r.appendContentString("" + status);
    _r.appendContentCharacter(' ');
    _r.appendContentString(WOResponse.stringForStatus(status));
    _r.appendContentString("</status>");
    if (doIndent) _r.appendContentString("\n      ");
    
    _r.appendContentString("<prop>");
    if (doIndent) _r.appendContentCharacter('\n');
    
    for (final String propName: propToValue.keySet()) {
      Object value = propToValue.get(propName);
      this.appendPropertyToResponse(propName, value, _r);
    }
    
    if (doIndent) _r.appendContentString("      ");
    _r.appendContentString("</prop>");
    
    if (doIndent) _r.appendContentString("\n    ");
    _r.appendContentString("</propstat>");
    if (doIndent) _r.appendContentCharacter('\n');
  }

  @SuppressWarnings({ "rawtypes" })
  private void appendPropertyValueToResponse
    (Object value, String ns, WOResponse _r)
  {
    if (value instanceof String)
      _r.appendContentHTMLString((String)value);
    else if (value instanceof Number)
      _r.appendContentString(value.toString());
    else if (value instanceof URL || value instanceof NSURL) {
      if (ns.equals(XMLNS.WEBDAV))
        _r.appendBeginTag("href");
      else
        _r.appendBeginTag("href", "xmlns", XMLNS.WEBDAV);
      _r.appendBeginTagEnd();
      _r.appendContentHTMLString(value.toString());
      _r.appendEndTag("href");
    }
    else if (value instanceof Element) { // structured value
      final Element e = (Element)value;
      
      String sns  = e.getNamespaceURI();
      String stag = e.getLocalName();
      
      if (sns == null)
        sns = XMLNS.WEBDAV;
      
      if (sns.equals(ns))
        _r.appendBeginTag(stag);
      else
        _r.appendBeginTag(stag, "xmlns", sns);
      
      if (e.hasAttributes()) {
        // FIXME: namespaces attributes
        final NamedNodeMap attrs = e.getAttributes();
        for (int i = 0, cnt = attrs.getLength(); i < cnt; i++) {
          Attr   a = (Attr)attrs.item(i);
          String n = a.getName();
          String v = a.getValue();
          _r.appendAttribute(n, v);
        }
      }
      
      // FIXME: render children
      if (e.hasChildNodes()) {
        log.error("cannot render structured elements yet: " + e);
        _r.appendBeginTagClose();
      }
      else
        _r.appendBeginTagClose();
    }
    else if (value instanceof Collection) { // eg calendar-home-set
      for (Object sv: (Collection)value)
        this.appendPropertyValueToResponse(sv, ns, _r);
    }
    else {
      log.error("Cannot render property value: " + value);
      this.appendPropertyValueToResponse(value.toString(), ns, _r);
    }
  }
  
  private void appendPropertyToResponse
    (final String propName, Object value, final WOResponse _r)
  {
    int    idx = propName.indexOf('}');
    String ns, tag;

    if (doIndent) _r.appendContentString("        ");
    
    if (idx < 1) {
      ns  = XMLNS.WEBDAV;
      tag = propName;
    }
    else {
      ns  = propName.substring(1, idx);
      tag = propName.substring(idx + 1);
    }
    
    if (ns.equals(XMLNS.WEBDAV))
      _r.appendBeginTag(tag);
    else
      _r.appendBeginTag(tag, "xmlns", ns);
    
    if (value == null) // closed tag
      _r.appendBeginTagClose();
    else if (value instanceof Throwable) {
      // this could (and should) encode more extra info in DAV:error elements
      _r.appendBeginTagClose();
    }
    else {
      _r.appendBeginTagEnd();
      this.appendPropertyValueToResponse(value, ns, _r);
      _r.appendEndTag(tag);
    }
    if (doIndent) _r.appendContentCharacter('\n');
  }
  
  protected void appendDAVResponseToResponse
    (final DAVResponse _dr, final WOResponse _r, final WOContext _ctx)
  {
    final WORequest rq = _ctx.request();
    boolean isBrief = false;
    if (rq != null) {
      // FIXME: Support Prefer return=minimal
      Object v = rq.headerForKey("Brief");
      isBrief = UObject.boolValue(v);
    }
    
    if (doIndent) _r.appendContentString("  ");
    _r.appendBeginTag("response"); _r.appendBeginTagEnd();
    if (doIndent) _r.appendContentCharacter('\n');
    
    /* response URL */
    
    NSURL url = _dr.url();
    if (url != null) {
      if (doIndent) _r.appendContentString("    ");
      _r.appendContentString("<href>");
      _r.appendContentString(url.toString());
      _r.appendContentString("</href>");
      if (doIndent) _r.appendContentCharacter('\n');
    }
    
    if (_dr.status() > 100) {
      int status = _dr.status();
      if (doIndent) _r.appendContentString("    ");
      _r.appendContentString("<status>HTTP/1.1 ");
      _r.appendContentString("" + status);
      _r.appendContentCharacter(' ');
      _r.appendContentString(WOResponse.stringForStatus(status));
      _r.appendContentString("</status>");
      if (doIndent) _r.appendContentString("\n");
    }
    
    /* multistatus */
    
    for (final Integer status: _dr.statusSet()) {
      if (isBrief && status >= 400)
        continue;
      
      final Map<String, Object> propToValue = _dr.propertiesForStatus(status);
      if (propToValue == null || propToValue.size() < 1)
        continue;
      
      this.appendDAVPropStatToResponse(status, propToValue, _r, _ctx);
    }
    
    if (doIndent) _r.appendContentString("  ");
    _r.appendEndTag("response");
    if (doIndent) _r.appendContentCharacter('\n');
  }
  
  protected void appendMultiStatusToResponse
    (final DAVMultiStatus _ms, final WOResponse _r, final WOContext _ctx)
  {
    _r.appendBeginTag("multistatus", "xmlns", XMLNS.WEBDAV);
    _r.appendBeginTagEnd();
    if (doIndent) _r.appendContentCharacter('\n');
    
    for (final DAVResponse dr: _ms.responses())
      this.appendDAVResponseToResponse(dr, _r, _ctx);
    
    _r.appendEndTag("multistatus");
    if (doIndent) _r.appendContentCharacter('\n');
  }
  
  protected Exception renderMultiStatus(DAVMultiStatus _ms, WOContext _ctx) {
    if (_ms == null) {
      log.error("got no multistatus to render??");
      return null;
    }

    /* prepare response object for streamed XML output */
    final WOResponse r = _ctx.response();
    r.setHeaderForKey("text/xml; charset=utf-8", "content-type");
    r.setContentEncoding("utf-8");
    r.setTextCoder(NSXmlEntityTextCoder.sharedCoder,
                   NSXmlEntityTextCoder.sharedCoder);
    
    if (doStream)
      r.enableStreaming(); // start streaming, headers will be written

    /* write body */
    r.appendContentString("<?xml version='1.0' encoding='utf-8'?>\n");
    this.appendMultiStatusToResponse(_ms, r, _ctx);
    
    if (!doStream) {
      log.info("Response: " + r);
      log.debug("  Content: " + r.contentString());
    }
    
    return null;
  }
}
