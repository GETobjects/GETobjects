/*
  Copyright (C) 2007-2009 Helge Hess

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
package org.getobjects.foundation;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Represents a URL. This mostly exists because java.net.URL can't store
 * relative URLs.
 */
public class NSURL extends NSObject {
  // FIXME: finish me up

  protected URL    parsedAbsoluteURL;
  protected String url;

  public NSURL(final String _url) {
    this.url = _url;
  }
  public NSURL(final URL _url) {
    this.parsedAbsoluteURL = _url;
  }

  /* accessors */

  public String scheme() {
    if (!parseIfNecessary())
      return null;
    return this.parsedAbsoluteURL.getProtocol();
  }

  public String host() {
    if (!parseIfNecessary())
      return null;
    return this.parsedAbsoluteURL.getHost();
  }

  public int port() {
    if (!parseIfNecessary())
      return -1;
    return this.parsedAbsoluteURL.getPort();
  }

  public String path() {
    if (!parseIfNecessary()) {
      // FIXME: strip of query parameters and such?
      return this.url;
    }
    return this.parsedAbsoluteURL.getPath();
  }

  public URL URL() {
    parseIfNecessary();
    return this.parsedAbsoluteURL;
  }

  /* parsing */

  public boolean parseIfNecessary() {
    if (this.parsedAbsoluteURL != null)
      return true;
    if (this.url == null)
      return false;

    try {
      this.parsedAbsoluteURL = new URL(this.url);
      return true;
    }
    catch (MalformedURLException e) {
      return false;
    }
  }

  /* string */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    _d.append(" url=");
    _d.append(toString());
  }

  @Override
  public String toString() {
    if (this.url != null)
      return this.url;

    if (this.parsedAbsoluteURL != null) {
      this.url = this.parsedAbsoluteURL.toString();
      return this.url;
    }

    return null;
  }
}
