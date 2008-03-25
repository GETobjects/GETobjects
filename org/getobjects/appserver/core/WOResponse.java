/*
  Copyright (C) 2006-2007 Helge Hess

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

package org.getobjects.appserver.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * WOResponse
 * <p>
 * Represents a HTTP response which will usually get send to the browser. 
 * 
 * <p>
 * THREAD: a response is not synchronized and can only be used from one
 *         thread w/o additional locking.
 */
public class WOResponse extends WOMessage implements WOActionResults {
  
  protected static int initialBufferSize = 4096;

  protected WORequest request = null;
  protected int       status  = WOMessage.HTTP_STATUS_OK;
  
  public WOResponse() {
    super();
    this.init(null /* WORequest */);
  }
  public WOResponse(WORequest _rq) {
    super();
    this.init(_rq);
  }
  
  protected void init(WORequest _rq) {
    super.init(_rq != null ? _rq.httpVersion() : null,
               null /* headers  */,
               null /* contents */,
               null /* userInfo */);
  
    this.request = _rq; // TODO: do we need to know that?
  
    this.outputStream = new ByteArrayOutputStream(initialBufferSize);
    
    /* preconfigure for HTML */
    if (this.headers == null || !this.headers.containsKey("content-type"))
      this.setHeaderForKey("text/html", "content-type");
  }
  
  /* accessors */
  
  public void setStatus(int _status) {
    this.status = _status;
  }
  public int status() {
    return this.status;
  }
  
  public WORequest request() {
    return this.request;
  }
  
  public void disableClientCaching() {
    // TODO: add expires header
    // TODO: maybe add some etag which changes always?
    // TODO: check whether those are correct
    this.setHeaderForKey("no-cache", "cache-control");
    this.setHeaderForKey("no-cache", "pragma");
  }
  
  /* WOActionResults */
  
  /**
   * This method just returns this. Its implemented to support the
   * WOActionResults interface.
   * 
   * @return the WOResponse object (the method receiver)
   */
  public WOResponse generateResponse() {
    return this;
  }
  
  
  /* streaming */
  
  /**
   * Enables streaming output. Be sure you know what you do before attempting
   * this :-)
   * <p>
   * Remember that all HTTP headers must be set before you start the streaming
   * (including content-length and content-type!).
   * <p>
   * The method works by acquiring the Servlet OutputStream of the WORequest
   * associated with this response.
   * 
   * @return true if streaming could be enabled, false otherwise
   */
  public boolean enableStreaming() {
    /* before streaming can be enabled, all HTTP headers need to be setup! */
    if (this.isStreaming())
      return true;
    
    if (this.request == null) {
      if (log.isInfoEnabled())
        log.info("missing request to enable streaming on response: " + this);
      return false;
    }
    
    /* setup HTTP headers/status on ServletResponse prior writing! */
    
    if (!this.request.prepareForStreaming(this)) {
      if (log.isInfoEnabled())
        log.info("could not prepare request for streaming: " + this.request);
      return false;
    }
    
    /* select streaming output stream */
    
    OutputStream os = this.request.outputStream();
    if (os == null) {
      log.info("cannot enable streaming because the WORequest " +
               "has no output stream: " + this.request);
      return false;
    }
    
    /* write existing content to stream */
    
    byte[] lContent = this.content();
    this.outputStream = null;
    
    if (lContent != null && lContent.length > 0) {
      try {
        os.write(lContent);
      }
      catch (IOException e) {
        this.lastException = e;
        return false;
      }
    }
    
    /* configure new stream */
    
    this.outputStream = os;
    return true;
  }
}
