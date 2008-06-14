/*
  Copyright (C) 2006-2007 Helge Hess

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

package org.getobjects.appserver.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.getobjects.appserver.publisher.GoResource;

/**
 * WOResourceRequestHandler
 * <p>
 * This class is used for delivering static resource files, like images or
 * stylesheet. Usually you would want to deliver resource files using Apache
 * or some other frontend Apache server. 
 */
public class WOResourceRequestHandler extends WORequestHandler {
  // TBD: document
  // TBD: rewrite to use GoObjects for content handling?

  public WOResourceRequestHandler(WOApplication _app) {
    super(_app);
  }
  
  protected int expirationIntervalForMimeType(String _mimeType) {
    return 3600 /* 1 hour */;
  }

  @Override
  public WOResponse handleRequest(WORequest _rq, WOContext _ctx, WOSession _s) {
    boolean isFavIcon = "/favicon.ico".equals(_rq.uri());
    
    /* decode URL */
    
    String[] handlerPath = _rq.requestHandlerPathArray();
    if (!isFavIcon && (handlerPath == null || handlerPath.length < 1)) {
      log.error("URL resource path too short / missing.");
      return null;
    }
    
    List<String> langList = _ctx.languages();
    String[] langs = null;
    String   resourceName;
    
    if (isFavIcon)
      resourceName = "favicon.ico";
    else if (handlerPath.length > 1) {
      resourceName = handlerPath[1];
      langList     = new ArrayList<String>(langList);
      langList.add(0, handlerPath[0]);
    }
    else
      resourceName = handlerPath[0];
    
    resourceName = "www/" + resourceName;
    langs        = langList.toArray(new String[0]);

    if (log.isDebugEnabled())
      log.debug("lookup resource: " + resourceName);
    
    /* find resource manager and deliver */
    
    WOResourceManager rm = _ctx.application().resourceManager();
    WOResponse        r  = _ctx.response();

    if (log.isDebugEnabled())
      log.debug("lookup resource using manager: " + rm);
    
    InputStream is = rm.inputStreamForResourceNamed(resourceName, langs);
    if (is == null) {
      log.error("did not find resource: " + resourceName);
      r.setStatus(WOMessage.HTTP_STATUS_NOT_FOUND);
      return r;
    }
    
    String mimeType = GoResource.mimeTypeForPath(resourceName);
    if (mimeType == null) {
      mimeType = "application/octet-stream";
      log.warn("could not determine content-type of resource: " + resourceName);
    }
    r.setHeaderForKey(mimeType, "content-type");
    
    /* setup caching headers */
    
    Date              now = new Date();
    GregorianCalendar cal = new GregorianCalendar();
    
    //cal.setTime(new Date(con.getLastModified()));
    //_r.setHeaderForKey(WOMessage.httpFormatDate(cal), "last-modified");
    
    cal.setTime(now);
    r.setHeaderForKey(WOMessage.httpFormatDate(cal), "date");
    
    cal.add(Calendar.SECOND, this.expirationIntervalForMimeType(mimeType));
    r.setHeaderForKey(WOMessage.httpFormatDate(cal), "expires");
    
    
    // TODO: retrieve resource metadata (content-type, length)
    
    try {
      r.enableStreaming();
      byte[] buffer = new byte[4096]; // TODO: adjust buffer size?
      
      while (true) {
        int len = is.read(buffer);
        if (len == -1)
          break;
        
        if (r.appendContentData(buffer, len) != null) {
          log.error("failed to write resource data", r.lastException());
          break;
        }
      }
    }
    catch (IOException e) {
      log.error("failed to read resource data", e);
    }
    finally {
      if (is != null) {
        try {
          is.close();
        }
        catch (IOException e) {
          log.warn("could not close input stream of resource " + resourceName +
                   ": " + is, e);
        }
      }
    }
    
    return r;
  }

}
