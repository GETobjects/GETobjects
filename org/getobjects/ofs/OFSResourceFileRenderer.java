/*
  Copyright (C) 2007 Helge Hess

  This file is part of JOPE.

  JOPE is free software; you can redistribute it and/or modify it under
  the terms of the GNU Lesser General Public License as published by the
  Free Software Foundation; either version 2, or (at your option) any
  later version.

  JOPE is distributed in the hope that it will be useful, but WITHOUT ANY
  WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
  License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with JOPE; see the file COPYING.  If not, write to the
  Free Software Foundation, 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
*/
package org.getobjects.ofs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOMessage;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.publisher.IJoObjectRenderer;
import org.getobjects.appserver.publisher.JoInternalErrorException;
import org.getobjects.foundation.NSObject;

/**
 * OFSResourceFileRenderer
 * <p>
 * This renderer directly streams the content of an OFSResourceFile object to
 * the WOResponse.
 */
public class OFSResourceFileRenderer extends NSObject
  implements IJoObjectRenderer
{
  protected static final Log log = LogFactory.getLog("JoOFS");

  /* control rendering */
  
  public boolean canRenderObjectInContext(Object _object, WOContext _ctx) {
    return _object instanceof OFSResourceFile;
  }
  
  /* rendering */

  /**
   * Stream the given OFSResourceFile object to the response.
   */
  public Exception renderObjectInContext(Object _object, WOContext _ctx) {
    if (_object == null)
      return new JoInternalErrorException("got no object to render");
    
    /* retrieve basic info */
    
    OFSResourceFile doc = (OFSResourceFile)_object;

    String mimeType = doc.defaultDeliveryMimeType();
    
    /* start response */
    
    WOResponse r = _ctx.response();
    r.setStatus(WOMessage.HTTP_STATUS_OK);
    r.setHeaderForKey(mimeType, "content-type");
    
    /* setup caching headers */
    
    Date              now = new Date();
    GregorianCalendar cal = new GregorianCalendar();
    
    cal.setTime(doc.lastModified());
    r.setHeaderForKey(WOMessage.httpFormatDate(cal), "last-modified");
    
    cal.setTime(now);
    r.setHeaderForKey(WOMessage.httpFormatDate(cal), "date");
    
    // TBD: document
    if (mimeType.startsWith("image/"))
      cal.add(Calendar.HOUR, 1);
    else if (mimeType.startsWith("text/css"))
      cal.add(Calendar.MINUTE, 10);
    else
      cal.add(Calendar.SECOND, 5);
    r.setHeaderForKey(WOMessage.httpFormatDate(cal), "expires");
    
    /* transfer content */
    
    r.setHeaderForKey("" + doc.size(), "content-length");
    
    /* remember that no headers can be set after streaming got activated */
    // TODO: this should be ensured by WOResponse
    if (!r.enableStreaming())
      log.warn("could not enable streaming for doc: " + doc);

    InputStream is = doc.openStream();
    if (is == null)
      return new JoInternalErrorException("could not open resource stream");
      
    r.resetLastException();
    try {
      byte[] buffer = new byte[0xFFFF];
      for (int len; (len = is.read(buffer)) != -1; )
        r.appendContentData(buffer, len);
    }
    catch (IOException e) {
      return new JoInternalErrorException
        ("failed to read from resource stream");
    }
    finally {
      try {
        if (is != null) is.close();
      }
      catch (IOException e) {
        log.warn("could not close input stream");
      }
    }
    
    return r.lastException(); /* WOResponse might have catched an issue */
  }
}
