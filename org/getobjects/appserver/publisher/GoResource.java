/*
  Copyright (C) 2007-2008 Helge Hess

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
package org.getobjects.appserver.publisher;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOMessage;
import org.getobjects.appserver.core.WOResponse;

/**
 * GoResource
 * <p>
 * This GoObject takes a URL and delivers its contents as a resource. Eg its
 * used by GoProduct to deliver public product resources.
 * <p>
 * Note: this object supports nested queries. Eg /www/images/myimage.gif.
 */
public class GoResource extends WOElement implements IGoObject {
  protected static final Log log = LogFactory.getLog("GoResource");

  protected URL url;

  public GoResource(final URL _url) {
    this.url = _url;
  }

  /* generate response */

  public static String mimeTypeForPath(final String _path) {
    // TODO: fix this crap and use some real MIME/ext object

    final int idx = _path.lastIndexOf('.');
    if (idx < 0) {
      log.error("could not detect MIME type for path: " + _path);
      return null;
    }

    String mimeType = null;
    final String ext = _path.substring(idx + 1);
    if (ext.equals("css"))
      mimeType = "text/css";
    else if (ext.equals("txt"))
      mimeType = "text/plain";
    else if (ext.equals("js"))
      mimeType = "text/javascript";
    else if (ext.equals("gif"))
      mimeType = "image/gif";
    else if (ext.equals("png"))
      mimeType = "image/png";
    else if (ext.equals("jpg") || ext.equals("jpeg"))
      mimeType = "image/jpeg";
    else if (ext.equals("html"))
      mimeType = "text/html";
    else if (ext.equals("woff"))
      mimeType = "font/woff";
    else if (ext.equals("woff2"))
      mimeType = "font/woff2";
    else
      log.error("MIME type unknown for extension '" + ext + "': " + _path);

    return mimeType;
  }

  protected int expirationIntervalForMimeType(final String _mimeType) {
    return 3600 /* 1 hour */;
  }

  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    URLConnection con = null;
    try {
      con = this.url.openConnection();
    }
    catch (final IOException coe) {
      log.warn("could not open connection to url: " + this.url);
      _r.setStatus(WOMessage.HTTP_STATUS_NOT_FOUND);
      return;
    }

    /* open stream */

    InputStream is  = null;
    try {
      is = con.getInputStream();
    }
    catch (final IOException ioe) {
      log.warn("could not open stream to url: " + this.url);
      _r.setStatus(WOMessage.HTTP_STATUS_NOT_FOUND);
      return;
    }

    /* transfer */

    try {
      String mimeType = con.getContentType();
      if (mimeType == null || "content/unknown".equals(mimeType))
        mimeType = mimeTypeForPath(this.url.getPath());

      if (mimeType == null)
        mimeType = "application/octet-stream";

      _r.setHeaderForKey(mimeType, "content-type");
      _r.setHeaderForKey("" + con.getContentLength(), "content-length");

      /* setup caching headers */

      final Date              now = new Date();
      final GregorianCalendar cal = new GregorianCalendar();

      cal.setTime(new Date(con.getLastModified()));
      _r.setHeaderForKey(WOMessage.httpFormatDate(cal), "last-modified");

      cal.setTime(now);
      _r.setHeaderForKey(WOMessage.httpFormatDate(cal), "date");

      cal.add(Calendar.SECOND, expirationIntervalForMimeType(mimeType));
      _r.setHeaderForKey(WOMessage.httpFormatDate(cal), "expires");

      /* start streaming */

      _r.enableStreaming();

      final byte[] buffer = new byte[0xFFFF];
      for (int len; (len = is.read(buffer)) != -1; )
        _r.appendContentData(buffer, len);
    }
    catch (final IOException e) {
      log.error("IO error trying to deliver resource: " + this.url, e);
      _r.setStatus(WOMessage.HTTP_STATUS_INTERNAL_ERROR);
    }
    finally {
      try {
        if (is != null) is.close();
      }
      catch (final IOException e) {
        log.warn("could not close URL input stream: " + this.url, e);
      }
    }
  }

  /* recursive lookup */

  @Override
  public Object lookupName(final String _name, final IGoContext _ctx, final boolean _acquire) {
    /* only process names, don't try to be smart */
    if ("/".equals(_name) || "..".equals(_name) || ".".equals(_name))
      return null;

    URL nurl = null;
    try {
      // ugly hack
      String s = this.url.toExternalForm();
      if (!s.endsWith("/")) s += "/";
      nurl = new URI(s + _name).toURL();
    }
    catch (final MalformedURLException | URISyntaxException e) {
      return e;
    }
    if (nurl == null) return null;

    return new GoResource(nurl);
  }

  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);

    if (this.url != null)
      _d.append(" url=" + this.url);
  }
}
