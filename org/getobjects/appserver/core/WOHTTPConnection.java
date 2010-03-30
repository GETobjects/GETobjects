/*
  Copyright (C) 2010 Marcus Mueller

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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UData;
import org.getobjects.foundation.UObject;
import org.getobjects.foundation.UString;

public class WOHTTPConnection extends NSObject {

  protected static final Log log = LogFactory.getLog("WOHTTPConnection");

  protected String  host;
  protected int     port;
  protected int     receiveTimeout  = 30 * 1000; // milliseconds
  protected int     sendTimeout     = 10 * 1000; // milliseconds
  protected int     readTimeout     = 0;
  protected boolean followRedirects = true;
  protected boolean keepAlive       = true;

  protected HttpURLConnection urlConnection;
  protected WORequest request; // last used request, attached to response


  public WOHTTPConnection(String _host, int _port) {
    this.host = _host;
    this.port = _port;
  }

  public WOHTTPConnection(String _host) {
    this(_host, 80);
  }

  public WOHTTPConnection(URL _url) {
    this.host = _url.getHost();
    this.port = _url.getPort();
    if (!_url.getProtocol().toLowerCase().equals("http")) {
      log.warn("given URL not for HTTP connection: " + _url);
    }
  }

  // Accessors

  public void setReceiveTimeout(int _receiveTimeout) {
    this.receiveTimeout = _receiveTimeout;
  }

  public int receiveTimeout() {
    return this.receiveTimeout;
  }

  public void setReadTimeout(int _readTimeout) {
    this.readTimeout = _readTimeout;
  }

  // NOTE: this cast is for conformance with the WebObjects specification -
  // I don't think it'll do any harm
  @SuppressWarnings("cast")
  public long readTimeout() {
    return (long)this.readTimeout;
  }

  public void setSendTimeout(int _sendTimeout) {
    this.sendTimeout = _sendTimeout;
  }

  public int sendTimeout() {
    return this.sendTimeout;
  }

  public void setKeepAliveEnabled(boolean _keepAlive) {
    this.keepAlive = _keepAlive;
  }

  public boolean keepAliveEnabled() {
    return this.keepAlive;
  }

  public void setFollowRedirects(boolean _followRedirects) {
    this.followRedirects = _followRedirects;
  }

  public boolean followRedirects() {
    return this.followRedirects;
  }

  // Request

  public boolean sendRequest(WORequest _rq) {
    if (_rq == null) return false;

    try {
      URL     url        = new URL("http", this.host, this.port, _rq.uri());
      boolean needsSetup = true;

      if (this.urlConnection != null &&
          this.urlConnection.getURL().equals(url))
      {
        needsSetup = false;
      }

      if (needsSetup) {
          this.urlConnection = (HttpURLConnection)url.openConnection();
          // According to WO53 docs, this is indeed the first sendTimeout
          this.urlConnection.setConnectTimeout(this.sendTimeout);
          this.urlConnection.setAllowUserInteraction(false);
          this.urlConnection.setDoInput(true);
      }

      // set properties and headers
      this.urlConnection.setRequestMethod(_rq.method());
      this.urlConnection.setInstanceFollowRedirects(this.followRedirects);
      for (String headerKey : _rq.headerKeys()) {
        List<String> headers = _rq.headersForKey(headerKey);
        if (UObject.isNotEmpty(headers)) {
          String headerProp = UString.componentsJoinedByString(headers, " , ");
          this.urlConnection.setRequestProperty(headerKey, headerProp);
        }
      }

      String connectionProp = this.keepAlive ? "keep-alive" : "close";
      this.urlConnection.setRequestProperty("connection", connectionProp);

      byte[] content = _rq.content();
      boolean hasContent = UObject.isNotEmpty(content);
      // NOTE: setting doOutput(true) has the effect of also setting
      // the request method to POST? WTF?
      this.urlConnection.setDoOutput(hasContent);
      if (hasContent) {
        // send content body
        OutputStream os = this.urlConnection.getOutputStream();
        os.write(content);
        os.close();
      }
      else {
        this.urlConnection.connect();
      }

      // save request for response
      this.request = _rq;
      return true;
    }
    catch (Exception e) {
      log.error("sendRequest() failed: " + e);
      return false;
    }
  }

  // Response

  protected static void attachToResponse(HttpURLConnection _urlConnection,
      WOResponse _r)
  {
    String httpVersion = null;
    int    status      = WOMessage.HTTP_STATUS_INTERNAL_ERROR;

    // retrieve status and HTTP version
    String resp = _urlConnection.getHeaderField(0);
    if (resp != null) {
      String[] fields = resp.split(" ");

      if (fields.length >= 2) {
        try {
          httpVersion = fields[0];
          status      = Integer.parseInt(fields[1]);
        }
        catch (Exception e) {
          try {
            status = _urlConnection.getResponseCode();
          }
          catch (IOException e1) {
          }
        }
      }
    }

    // apply status
    _r.setStatus(status);
    // kinda hackish
    _r.httpVersion = httpVersion;

    for (int i = 1; _urlConnection.getHeaderField(i) != null; i++) {
      String headerProp = _urlConnection.getHeaderField(i);
      String headerKey  = _urlConnection.getHeaderFieldKey(i).toLowerCase();
      String[] headerValues = headerProp.split(" , ");
      if (headerValues.length == 1) {
        _r.setHeaderForKey(headerProp, headerKey);
      }
      else {
        List<String> header = new ArrayList<String>(headerValues.length);
        for (String value : headerValues) {
          header.add(value.trim());
        }
        _r.setHeadersForKey(header, headerKey);
      }
    }
  }

  public WOResponse readResponse() {
    WOResponse r = new WOResponse(this.request);
    try {
      this.urlConnection.setReadTimeout(this.readTimeout);

      // read content body

      InputStream is  = this.urlConnection.getInputStream();
      // TODO: in order to properly support receiveTimeout, we'd need
      // to implement stream loading here and check the receiveTimeout
      // exhaustion for all successive read operations.
      byte[] content = UData.loadContentFromStream(is);
      r.setContent(content);

      attachToResponse(this.urlConnection, r);

      String connValue = r.headerForKey("connection");
      if (UObject.isNotEmpty(connValue)) {
        if (connValue.toLowerCase().equals("close")) {
          this.urlConnection.disconnect();
          // NOTE: it's ok to do this here, as this is the last step and
          // a disconnected urlConnection cannot be reused anyways
          this.urlConnection = null;
        }
      }
    }
    catch (Exception e) {
      if (this.urlConnection != null) {
        attachToResponse(this.urlConnection, r);
      }
      else {
        r.setStatus(WOMessage.HTTP_STATUS_INTERNAL_ERROR);
      }
      log.error("readResponse() failed: " + e);
    }
    return r;
  }
}
