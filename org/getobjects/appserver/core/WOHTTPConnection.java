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
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Socket;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UData;
import org.getobjects.foundation.UObject;
import org.getobjects.foundation.UString;

public class WOHTTPConnection extends NSObject implements HostnameVerifier {

  protected static final Log log = LogFactory.getLog("WOHTTPConnection");

  protected static final Authenticator sharedURLAuthorityBasedAuthenticator =
    new URLAuthorityBasedAuthenticator();

  protected URL     url;
  protected int     receiveTimeout   = 30 * 1000; // milliseconds
  protected int     sendTimeout      = 10 * 1000; // milliseconds
  protected int     readTimeout      = 0;
  protected boolean followRedirects  = true;
  protected boolean keepAlive        = true;
  protected boolean isSSL            = false;
  protected boolean allowInsecureSSL = false;
  protected Authenticator authenticator = sharedURLAuthorityBasedAuthenticator;

  protected HttpURLConnection urlConnection;
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

  /**
   * (GETobjects extension):
   * Sets the authenticator to use for this WOHTTPConnection, if authentication
   * is required.
   */
  public void setAuthenticator(Authenticator _authenticator) {
    this.authenticator = _authenticator;
  }

  /**
   * (GETobjects extension):
   * Returns the authenticator currently in use for this connection.
   * The default is to use the URLAuthorityBasedAuthenticator.
   *
   * @see org.getobjects.appserver.core.WOHTTPConnection.URLAuthorityBasedAuthenticator
   */
  public Authenticator authenticator() {
    return this.authenticator;
  }

  // Setup

  protected void setupURL() {
    String protocol = this.url.getProtocol().toLowerCase();
    if (protocol.equals("https"))
      this.isSSL = true;
    else if (!protocol.equals("http"))
      log.warn("given URL not for HTTP connection: " + this.url);
  }

  /**
   * (GETobjects extension):
   * Called, whenever a new urlConnection is setup.
   *
   * Subclasses may override or extend the steps taken.
   */
  protected void setupHttpURLConnection(HttpURLConnection _conn) {
    // According to WO53 documentation, this is indeed the first sendTimeout
    _conn.setConnectTimeout(this.sendTimeout);
    _conn.setAllowUserInteraction(false);
    _conn.setDoInput(true);
  }

  /**
   * (GETobjects extension):
   * Called, whenever a new urlConnection is setup and only if it's a
   * HttpsURLConnection.
   *
   * This method is called in addition to
   * setupHttpURLConnection() and should contain refinement necessary for
   * SSL purposes, only.
   *
   * Subclasses may override or extend the steps taken.
   */
  protected void setupHttpsURLConnection(HttpsURLConnection _conn) {
    _conn.setHostnameVerifier(this);

    if (this.allowInsecureSSL) {
      try {
        _conn.setSSLSocketFactory(new GullibleSSLSocketFactory(null));
      }
      catch (GeneralSecurityException e) {
        e.printStackTrace();
      }
    }
  }

  // SSL

  public boolean verify(String _hostname, SSLSession _session) {
    // TODO: tie this to allowInsecureSSL and really validate hostname?
    return true;
  }

  // Request

  public boolean sendRequest(WORequest _rq) {
    if (_rq == null) return false;

    try {
      URL     rqURL      = new URL(this.url, _rq.uri());
      boolean needsSetup = true;

      if (this.urlConnection != null &&
          this.urlConnection.getURL().equals(rqURL))
      {
        needsSetup = false;
      }

      if (needsSetup) {
        this.urlConnection = (HttpURLConnection)rqURL.openConnection();

        this.setupHttpURLConnection(this.urlConnection);
        if (this.urlConnection instanceof HttpsURLConnection) {
          this.setupHttpsURLConnection((HttpsURLConnection)this.urlConnection);
        }
      }

      // unfortunately there's no better API...
      Authenticator.setDefault(this.authenticator());

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


  // SSL Helpers

  /**
   * GullibleSSLSocketFactory provides SSL sockets which accept all server
   * certificates.
   *
   * Apart from performing encryption, no 'trust' checking is done on any
   * certificate involved. Connections using these sockets are vulnerable
   * to man in the middle attacks!
   */
  protected class GullibleSSLSocketFactory extends SSLSocketFactory {

    /**
     * GullibleTrustManager doesn't implement any trust management at all.
     */
    protected class GullibleTrustManager implements X509TrustManager {

      public X509Certificate[] getAcceptedIssuers() {
          return new X509Certificate[0];
      }

      public void checkClientTrusted(X509Certificate[] _certs, String _authType)
      {
      }

      public void checkServerTrusted(X509Certificate[] _certs, String _authType)
      {
      }
    }

    protected SSLSocketFactory factory;

    public GullibleSSLSocketFactory(String _unused)
    throws GeneralSecurityException
    {
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, new TrustManager[] { new GullibleTrustManager() }, null);
        this.factory = ctx.getSocketFactory();
    }

    public Socket createSocket(InetAddress _host, int _port) throws IOException
    {
      return this.factory.createSocket(_host, _port);
    }

    public Socket createSocket(String _host, int _port) throws IOException {
      return this.factory.createSocket(_host, _port);
    }

    public Socket createSocket(String _host, int _port, InetAddress localHost,
        int localPort) throws IOException
    {
      return this.factory.createSocket(_host, _port, localHost, localPort);
    }

    public Socket createSocket(InetAddress _address, int _port,
        InetAddress _localAddress, int _localPort) throws IOException
    {
      return this.factory.createSocket(_address, _port, _localAddress,
          _localPort);
    }

    public Socket createSocket(Socket _socket, String _host, int _port,
        boolean _autoClose) throws IOException
    {
      return this.factory.createSocket(_socket, _host, _port, _autoClose);
    }

    public String[] getDefaultCipherSuites() {
      return this.factory.getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
      return this.factory.getSupportedCipherSuites();
    }
  }

  // Authenticator

  /**
   * This authenticator uses the "authority" part of a URL to provide
   * the required authentication, i.e. for a URL like this:
   * <code>
   * URL url = new URL("http://foo:bar@example.org");
   * </code>
   * this authenticator would authenticate as user "foo" with password "bar".
   */
  protected static class URLAuthorityBasedAuthenticator extends Authenticator {
    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
      String auth = getRequestingURL().getAuthority();
      if (auth != null) {
        int idx = auth.indexOf("@");
        if (idx != -1) {
          auth = auth.substring(0, idx);
          idx  = auth.indexOf(":");
          if (idx != -1) {
            String user     = auth.substring(0, idx);
            String password = auth.substring(idx + 1);
            return new PasswordAuthentication(user, password.toCharArray());
          }
          return new PasswordAuthentication(auth, "".toCharArray());
        }
        return new PasswordAuthentication("", "".toCharArray());
      }
      return super.getPasswordAuthentication();
    }
  }
}
