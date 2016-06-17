/*
  Copyright (C) 2007-2015 Helge Hess

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

import java.net.URI;

import org.getobjects.appserver.publisher.IGoCallable;
import org.getobjects.appserver.publisher.IGoContext;

/**
 * WORedirect
 * <p>
 * An action result which can be used to trigger an HTTP redirect (302 Status
 * plus Location header).
 *
 * <p>
 * Note: this only inherits from WOComponent for legacy reasons, no real
 * point in that?!
 */
public class WORedirect extends WOComponent implements IGoCallable {
  // TODO: support parameters, direct actions etc etc (WOLinkGenerator?)

  protected int    status = WOMessage.HTTP_STATUS_FOUND;
  protected int    timeout; /* for meta refresh */
  protected String url;

  public WORedirect(final String _url) {
    super();
    this.setUrl(_url);
  }
  public WORedirect(final String _url, final WOContext _ctx) {
    super();
    this.initWithContext(_ctx); // kinda hackish
    this.setUrl(_url); /* needs context, thus last place */
  }
  public WORedirect() {
    super();
  }

  /* accessors */

  /**
   * Contrary to WebObjects, this will convert a URI into a proper
   * URL as is required by the HTTP standard.
   *
   * @param _url String representing a relative (URI) or absolute URL
   */
  public void setUrl(String _url) {
    if (_url == null || _url.length() == 0) {
      this.url = null;
      return;
    }

    if (_url.indexOf("://") < 0) {
      /* a relative url without a scheme, needs to be turned into proper URL */
      final WOContext ctx   = this.context();
      if (ctx != null) {
        final WORequest rq    = ctx.request();
        String          rqUrl = rq.url();

        if (rqUrl == null) {
          this.log().warn("request doesn't provide info about URL, falling " +
                          "back to URIs which isn't proper according to RFCs!");
          rqUrl = rq.uri();
        }
        final URI base  = URI.create(rqUrl);
        final URI redir = URI.create(_url);
        _url      = base.resolve(redir).toASCIIString();
      }
    }

    this.url = _url;
  }
  public String url() {
    return this.url;
  }

  public int status() {
    return this.status < 1 ? WOMessage.HTTP_STATUS_FOUND : this.status;
  }
  public int timeout() {
    return this.timeout < 0 ? 0 : this.timeout;
  }

  /* some WOComponent infrastructure sanity */

  @Override
  public WOElement template() {
    /* we never have a template */
    return null;
  }


  /* WOActionResults */

  /**
   * Returns a new WOResponse object representing the redirect.
   */
  @Override
  public WOResponse generateResponse() {
    final String location = this.url();
    if (location == null || location.length() == 0) {
      return null; // TBD: we could also redirect to the componentActionURL()?
    }

    final WOContext  ctx = this.context();
    WOResponse r;

    r = ctx != null ? ctx.response() : new WOResponse();
    this.appendToResponse(r, ctx);
    return r;
  }


  /* generate response */

  /**
   * Since WORedirect is a WOComponent, we need to mark it as 'frameless', so
   * that it does not get embedded into a Frame.joframe ...
   */
  public boolean isFrameless() {
    return true;
  }

  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    final String location = this.url();
    if (location == null || location.length() == 0) {
      log().warn("WORedirect has no URL assigned.");
      return;
    }

    if (_r.isStreaming()) {
      /* we could try to be clever here and encode a JavaScript to do the
       * redirect during the streaming?
       */
      log().warn("invoked WORedirect on a streaming response!");
    }

    if ("xjson".equals(F("redir"))) {
      /* hack, XMLHttpRequest or Prototype attempt to resolve 302 responses
       * even when we override the on302 handler. Sigh.
       */
      _r.setStatus(WOMessage.HTTP_STATUS_OK);
      _r.setHeaderForKey("{ redirect: '" + location + "' }", "X-JSON");

      /* Hm, we set the meta-refresh as the body. Just to be sure. */
      this.appendMetaRefreshToResponse(_r, _ctx);
    }
    else {
      _r.setStatus(this.status());
      _r.setHeaderForKey(location, "location");
      _r.removeHeadersForKey("content-type");

      /* reset content, could contain a partial response */
      _r.setContent(new byte[0]);
    }

    // TBD: we could play clever here and generate a META REFRESH and/or
    //      a JavaScript if the element is used in a specific page section
  }

  public void appendMetaRefreshToResponse(WOResponse _r, final WOContext _ctx) {
    // <meta http-equiv="refresh" content="5; URL=http://de.selfhtml.org/">

    final StringBuilder content = new StringBuilder(128);
    content.append(this.timeout());
    content.append("; URL=");
    content.append(this.url());

    _r.appendBeginTag("meta", "http-equiv", "refresh", "content", content);
    _r.appendBeginTagClose(_ctx.closeAllElements());
  }


  /* request processing */

  @Override
  public void takeValuesFromRequest(final WORequest _rq, final WOContext _ctx) {
    /* we handle no requests, we have no template */
  }
  @Override
  public Object invokeAction(final WORequest _rq, final WOContext _ctx) {
    /* we handle no requests, we have no template */
    return null;
  }


  /* IGoCallable */

  public boolean isCallableInContext(IGoContext _ctx) {
    return true;
  }
  public Object callInContext(Object _object, IGoContext _ctx) {
    return this;
  }
}
