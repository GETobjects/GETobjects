package org.getobjects.samples.testdav.controllers;

import java.util.Collection;

import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.appserver.publisher.IGoObject;
import org.getobjects.appserver.publisher.IGoSecuredObject;
import org.getobjects.appserver.publisher.annotations.DefaultAccess;
import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.foundation.UObject;
import org.getobjects.foundation.XMLNS;
import org.getobjects.samples.testdav.objects.DAVMultiStatus;
import org.getobjects.samples.testdav.objects.DAVResponse;

@DefaultAccess("allow")
public class DAVCollection extends DAVObject implements IGoObject {
  
  /* Go methods */
  
  @Override
  public Object doPropfind(WOContext _ctx) {
    Object r = super.doPropfind(_ctx);
    
    if (r instanceof DAVMultiStatus) {
      final WORequest      rq = _ctx != null ? _ctx.request() : null;
      final DAVMultiStatus ms = (DAVMultiStatus)r;
      
      if (UObject.intValue(rq.headerForKey("Depth")) == 1)
        this.addResponsesForChildren(ms, _ctx);
    }
    
    return r;
  }
  
  public Collection<String> davChildKeysInContext(WOContext _ctx) {
    // This is very inefficient, but quite OK for the 10 records of our demo :-)
    return null;
  }
  
  public void addResponsesForChildren(DAVMultiStatus _ms, WOContext _ctx) {
    // This is very inefficient, but quite OK for the 10 records of our demo :-)
    // FIXME: This is a bit hackish and only works for Depth:1 ...
    String baseURL = _ctx.request().uri();
    if (!baseURL.endsWith("/"))
      baseURL = baseURL + "/";
    
    final Collection<String> childKeys = this.davChildKeysInContext(_ctx);
    if (childKeys == null)
      return;
    
    for (final String key: childKeys) {
      // We do not acquire, we only want direct children!
      // Note: do not simply use lookupName! This is not secured and the
      //       security of subobjects has not been checked. This is different
      //       to lookupName being called by Go, which does the same we do
      //       here:
      Object o = IGoSecuredObject.Utility.lookupName(this, key, _ctx, false);
      
      final String childURL = baseURL + key;
      
      if (o instanceof DAVObject) {
        final DAVObject davO = (DAVObject)o;
        davO.addSelfResponse(_ms, childURL, _ctx);
        continue;
      }
      
      if (o instanceof WOResponse) {
        WOResponse r = (WOResponse)o;
        DAVResponse dr = new DAVResponse(childURL, r.status());
        // FIXME: add etag, content-type, etc!
        _ms.addResponse(dr);
        continue;
      }
      
      if (o instanceof Throwable) {
        int status = UObject.intValue(
            NSKeyValueCoding.Utility.valueForKey(o, "httpStatus"));
        if (status > 100) {
          DAVResponse dr = new DAVResponse(childURL, status);
          _ms.addResponse(dr);
          continue;
        }
      }
      
      log.error("Cannot deal with child object '" + key + "': " + o);
    }
  }
  
  @Override
  public Object davResourceType() {
    return createElementNS(XMLNS.WEBDAV, "collection");
  }
  
  static int cTagCounter = 0;
  
  static final String propCTag = "{http://calendarserver.org/ns/}getctag";
  @Override
  public Object valueForPropertyInContext(String _propName, WOContext _ctx) {
    if (propCTag.equals(_propName)) {
      // mark collections as changed on every request for full refreshes
      cTagCounter++;
      return "NeverendingStory" + cTagCounter;
    }
      
    return super.valueForPropertyInContext(_propName, _ctx);
  }
  
  /* collection */

  public Object lookupName(String _name, IGoContext _ctx, boolean _acquire) {
    // FIXME: override in subclasses to support the child keys
    return IGoObject.DefaultImplementation
             .lookupName(this, _name, _ctx, _acquire);
  }
}
