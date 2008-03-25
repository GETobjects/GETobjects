/*
  Copyright (C) 2007 Helge Hess <helge.hess@opengroupware.org>
  Copyright (C) 2007 Marcus Mueller <znek@mulle-kybernetik.com>

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

package org.getobjects.appserver.templates;

import java.net.URL;

import org.getobjects.foundation.NSException;

public class WOHTMLParseException extends NSException {
  private static final long serialVersionUID = 1L;
  
  protected String error;
  protected int    line;
  protected String context;
  protected URL    url;
  
  public WOHTMLParseException
    (String _error, int _line, String _ctx, URL _url)
  {
    super(messageForParseException(_error, _line, _ctx, _url));
    
    this.line    = _line;
    this.error   = _error;
    this.context = _ctx;
    this.url     = _url;
  }
  
  /* accessors */
  
  public int line() {
    return this.line;
  }
  public URL url() {
    return this.url;
  }
  
  public String context() {
    return this.context;
  }
  
  public String errorString() {
    return this.error;
  }
  
  /* message */
  
  protected static String messageForParseException
    (String _error, int _line, String _ctx, URL _url)
  {
    StringBuilder sb = new StringBuilder(256);
    
    if (_url != null) {
      sb.append(_url.getFile());
      if (_line > 0) {
        sb.append(":");
        sb.append(_line);
        sb.append(": ");
      }
    }
    else if (_line > 0) {
      sb.append("in:");
      sb.append(_line);
      sb.append(": ");
    }
    
    sb.append(_error != null ? _error : "unknown-error");

    if (_ctx != null && _ctx.length() > 0) {
      sb.append(": context|");
      sb.append(_ctx);
      sb.append("|");
    }
    return sb.toString();
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.url != null)
      _d.append(" url=" + this.url);
    if (this.line > 0)
      _d.append(" line=" + this.line);

    if (this.error != null)
      _d.append(" error=" + this.error);
    
    if (this.context != null) {
      _d.append(" context|");
      _d.append(this.context);
      _d.append("|");
    }
  }
}