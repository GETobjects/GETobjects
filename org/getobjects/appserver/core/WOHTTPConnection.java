/*
  Copyright (C) 2010, 2016 Marcus Mueller

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

import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Authentication;
import org.eclipse.jetty.client.api.AuthenticationStore;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BasicAuthentication;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UObject;

public class WOHTTPConnection extends NSObject {

  protected static final Log log = LogFactory.getLog("WOHTTPConnection");

  protected URL     url;
  protected int     receiveTimeout   = 30 * 1000; // milliseconds
  protected int     sendTimeout      = 10 * 1000; // milliseconds
  protected int     readTimeout      = 0;
  protected boolean followRedirects  = true;
  protected boolean keepAlive        = true;
  protected boolean isSSL            = false;
  protected boolean allowInsecureSSL = false;

  protected HttpClient httpClient;
  protected ContentResponse contentResponse;
  protected WORequest request; // last used request, attached to response


  /**
   * Creates an 'http' connection to _host on _port.
   */
  public WOHTTPConnection(String _host, int _port) {
    try {
      this.url = new URL("http://" + _host + (_port != 80 ? (":" + _port)
                                                          : ""));
    }
    catch (MalformedURLException e) { // won't happen
      e.printStackTrace();
    }
    this.setupURL();
  }

  /**
   * Creates an 'http' connection to _host on port 80.
   */
  public WOHTTPConnection(String _host) {
    this(_host, 80);
  }

  /**
   * Creates either an 'http' connection or an 'https' connection
   * to the host specified in _url.
   */
  public WOHTTPConnection(URL _url) {
    try {
      this.url = new URL(_url, "/");
    }
    catch (MalformedURLException e) { // won't happen
      log.error(e);
    }
    this.setupURL();
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
  public long readTimeout() {
    return this.readTimeout;
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

  /**
   * (GETobjects extension):
   * If true, allow connections to SSL sites whose certs are not trusted by a
   * certificate authority provided in the Java keystore.
   * If false, only allow SSL connections to SSL sites whose certificates
   * are trusted by the Java keystore.
   *
   * NOTE: the keystore which the JavaVM uses can be set upon launch
   *       as the 'javax.net.ssl.trustStore' system property value.
   */
  public void setAllowInsecureHttpsConnection(boolean _tf) {
    this.allowInsecureSSL = _tf;
  }

  /**
   * (GETobjects extension):
   * If true, no trust is implied when performing an SSL connection.
   * The default is 'false' which means only trusted connections can be made.
   */
  public boolean allowInsecureHttpsConnection() {
    return this.allowInsecureSSL;
  }
 
  // Setup

  protected void setupURL() {
    final String protocol = this.url.getProtocol().toLowerCase();
    if (protocol.equals("https"))
      this.isSSL = true;
    else if (!protocol.equals("http"))
      log.warn("given URL not for HTTP connection: " + this.url);
  }

  protected Authentication getAuthentication(URL _rqURL) {
    return AllRealmsBasicAuthentication.createForURL(_rqURL);
  }

  // Request

  public boolean sendRequest(WORequest _rq) {
    if (_rq == null) return false;

    try {
      final URL     rqURL      = new URL(this.url, _rq.uri());
      final boolean needsSetup = (this.httpClient == null);

      if (needsSetup) {
        if (this.isSSL) {
          final SslContextFactory sslCtxFactory =
              new SslContextFactory(this.allowInsecureHttpsConnection());
          this.httpClient = new HttpClient(sslCtxFactory);
        }
        else {
          this.httpClient = new HttpClient();
        }

        final Authentication auth = getAuthentication(rqURL);
        if (auth != null) {
          final AuthenticationStore authStore =
              this.httpClient.getAuthenticationStore();
          authStore.addAuthentication(auth);
        }
        this.httpClient.start();
      }

      // httpClient provides its copy of "user-agent" to all requests it
      // creates - which also includes requests created while following
      // redirects

      String userAgent = _rq.headerForKey("user-agent");
      if (UObject.isNotEmpty(userAgent)) {
        this.httpClient.setUserAgentField(
            new HttpField("user-agent", userAgent));
      }

      final Request rq = this.httpClient.newRequest(rqURL.toString());
      
      // set properties and headers
      rq.method(_rq.method());
      rq.followRedirects(this.followRedirects());
      
      for (String headerKey : _rq.headerKeys()) {
        // filter "user-agent", see above
        if ("user-agent".equals(headerKey.toLowerCase()))
          continue;
        final List<String> headers = _rq.headersForKey(headerKey);
        if (UObject.isNotEmpty(headers)) {
          for (String header : headers)
            rq.header(headerKey, header);
        }
      }
      final String connectionProp = this.keepAlive ? "keep-alive" : "close";
      rq.header("connection", connectionProp);

      for (WOCookie cookie : _rq.cookies()) {
        final HttpCookie httpCookie =
            new HttpCookie(cookie.name(), cookie.value());
        httpCookie.setPath(cookie.path());
        httpCookie.setDomain(cookie.domain());
        httpCookie.setMaxAge(cookie.timeOut());
        httpCookie.setSecure(cookie.isSecure);
      }

      rq.timeout(sendTimeout() + receiveTimeout() + readTimeout(), TimeUnit.MILLISECONDS);
 
      final byte[] content = _rq.content();
      final boolean hasContent = (content != null) && (content.length > 0);
      if (hasContent) {
        rq.content(new BytesContentProvider(content));
      }

      // save request for response
      this.request = _rq;
      this.contentResponse = rq.send();
      return true;
    }
    catch (Exception e) {
      log.error("sendRequest() failed: " + e);
      return false;
    }
  }

  // Response

  public WOResponse readResponse() {
    final WOResponse r = new WOResponse(this.request);
    r.removeHeadersForKey("content-type");
    try {
      r.setStatus(this.contentResponse.getStatus());

      final HttpFields headers = this.contentResponse.getHeaders();
      for (HttpField header : headers) {
        final List<String> allHeaders = r.headersForKey(header.getName());
        allHeaders.add(header.getValue());
        r.setHeadersForKey(allHeaders, header.getName());
      }
      final byte[] content = this.contentResponse.getContent();
      r.setContent(content);

      final String connValue = r.headerForKey("connection");
      if (UObject.isNotEmpty(connValue)) {
        if (connValue.toLowerCase().equals("close")) {
          this.httpClient.stop();
          this.httpClient = null;
        }
      }
    }
    catch (Exception e) {
      r.setStatus(WOMessage.HTTP_STATUS_INTERNAL_ERROR);
      log.error("readResponse() failed: " + e);
    }
    this.contentResponse = null;
    return r;
  }
  
  protected static class AllRealmsBasicAuthentication
    extends BasicAuthentication
  {
    public AllRealmsBasicAuthentication(URI uri, String user, String password){
      super(uri, null, user, password);
    }

    public static AllRealmsBasicAuthentication createForURL(URL url) {
      String auth = url.getAuthority();
      if (auth == null)
        return null;
    
      String user = "";
      String password = "";
      int idx = auth.indexOf("@");
      if (idx != -1) {
        auth = auth.substring(0, idx);
        idx  = auth.indexOf(":");
        if (idx != -1) {
          user     = auth.substring(0, idx);
          password = auth.substring(idx + 1);
        }
        else {
          user = auth;
        }
      }
      URI uri = null;
      try {
        uri = url.toURI();
      }
      catch (URISyntaxException e) {} // never happens
      return new AllRealmsBasicAuthentication(uri, user, password);
    }
    
    @Override
    public boolean matches(String type, URI uri, String realm) {
      if (!"basic".equalsIgnoreCase(type))
        return false;
      return true;
    }
  }
}
