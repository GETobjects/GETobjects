/*
  Copyright (C) 2006-2008 Helge Hess

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

import java.util.Collection;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSObject;

/**
 * WOCookie
 * <p>
 * Represents an HTTP cookie.
 * <p>
 * Use the addCookie() method of WOResponse to add a setup cookie, eg:<pre>
 *   response.addCookie(new WOCookie("myCookie", "Hello World"));</pre>
 * To check for cookies coming in, use cookieValueForKey() or
 * cookieValuesForKey() of WORequest, eg:<pre>
 *   System.err.println("Cookie: " + _rq.cookieValueForKey("myCookie"));</pre>
 *
 * Remember that browsers limit the size and the number of cookies per site.
 *
 * <p>
 * To pass a cookie between hosts you can use the domain, eg:<pre>
 *   domain=.zideone.com</pre>
 * will deliver the cookie to:<pre>
 *   shop.zideone.com
 *   crm.zideone.com
 *   etc</pre>
 * Note: if the path is not set, the request path will be used! This is usually
 *       not what you want.
 *
 * <p>
 * http://www.ietf.org/rfc/rfc2109.txt<br>
 * http://www.faqs.org/rfcs/rfc2965.html (only implemented by Opera?)
 */
public class WOCookie extends NSObject {

  public enum SameSite {
    None, Strict, Lax
  }

  protected static final Log log = LogFactory.getLog("WOCookie");

  protected String   name;     /* name of cookie, eg 'wosid'   */
  protected String   value;    /* value of cookie, eg '283873' */
  protected String   path;     /* path the cookie is valid for, eg '/MyApp' */
  protected String   domain;   /* domain the cookie is valid for (.com) */
  protected Date     date;
  protected int      timeout;  /* in seconds (-1 == do not expire) */
  protected boolean  isSecure; /* whether cookie requires an HTTPS connection */
  protected SameSite sameSite; /* instruct browser when to send cookie */

  public WOCookie(final String _name, final String _value, final String _path, final String _domain,
                  final Date _date, final boolean _isSecure)
  {
    this.name     = _name;
    this.value    = _value;
    this.path     = _path;
    this.domain   = _domain;
    this.date     = _date;
    this.isSecure = _isSecure;
    this.timeout  = -1; /* do not expire cookie (use date) */
    this.sameSite = SameSite.Lax;
  }

  public WOCookie(final String _name, final String _value, final String _path, final String _domain,
                  final int _timeoutInS, final boolean _isSecure)
  {
    this.name     = _name;
    this.value    = _value;
    this.path     = _path;
    this.domain   = _domain;
    this.timeout  = _timeoutInS;
    this.isSecure = _isSecure;
    this.sameSite = SameSite.Lax;
  }

  public WOCookie(final String _name, final String _value, final String _path, final String _domain,
                  final boolean _isSecure)
  {
    this.name     = _name;
    this.value    = _value;
    this.path     = _path;
    this.domain   = _domain;
    this.isSecure = _isSecure;
    this.timeout  = -1; /* do not expire cookie */
    this.sameSite = SameSite.Lax;
  }

  public WOCookie(final String _name, final String _value) {
    this.name     = _name;
    this.value    = _value;
    this.timeout  = -1; /* do not expire cookie */
    this.isSecure = false;
    this.sameSite = SameSite.Lax;
  }


  /* accessors */

  public void setName(final String _s) {
    this.name = _s;
  }
  public String name() {
    return this.name;
  }

  public void setValue(final String _s) {
    if (_s != null && _s.length() > 0 && _s.charAt(0) == '$')
      log.warn("cookie value may not start with a dollar sign!: " + _s);
    this.value = _s;
  }
  public String value() {
    return this.value;
  }

  /**
   * Sets the cookie-timeout aka the 'Max-Age' attribute of RFC 2109.
   * <ol>
   *   <li>if the value is &lt; 0, we do not generate a Max-Age (hence, no to)
   *   <li>if the value is 0, this tells the browser to expire the cookie
   *   <li>if the value is &gt; 0, the browser will remove the cookie
   * </ol>
   *
   * @param _date
   */
  public void setTimeOut(final int _date) {
    this.timeout = _date;
  }
  public int timeOut() {
    return this.timeout;
  }

  /**
   * Sets the 'expires' date for the cookie, see RFC 2109. This is deprecated,
   * use setTimeOut (Max-Age) instead.
   *
   * @param _date - the Date when the cookie expires
   */
  public void setExpires(final Date _date) {
    this.date = _date;
  }
  public Date expires() {
    return this.date;
  }

  public void setDomain(final String _s) {
    if (_s != null && !_s.startsWith("."))
      log.warn("cookie domain does not start with a dot!: " + _s);
    this.domain = _s;
  }
  public String domain() {
    return this.domain;
  }

  public void setPath(final String _s) {
    this.path = _s;
  }
  public String path() {
    return this.path;
  }

  public void setSameSite(final SameSite _sameSite) {
    if (_sameSite == SameSite.None && !this.isSecure)
      log.warn("'None' directive requires secure attribute!");
    this.sameSite = _sameSite;
  }
  public SameSite sameSite() {
    return this.sameSite;
  }

  /* generating HTTP cookie */

  /**
   * Returns the HTTP response representation of the cookie value, w/o the
   * header name ("cookie").
   *
   * @return the HTTP String representing the WOCookie
   */
  public String headerString() {
    final StringBuilder sb = new StringBuilder(256);

    // TODO: do we need to escape the value?
    sb.append(name());
    sb.append("=");

    final String v = value();
    if (v != null) sb.append(v);
    sb.append("; version=\"1\""); // v1 means RFC 2109

    if (this.path != null) {
      sb.append("; path=");
      sb.append(this.path);
    }
    if (this.domain != null) {
      sb.append("; domain=");
      sb.append(this.domain);
    }

    final int to = timeOut();
    if (to >= 0) {
      sb.append("; Max-Age=");
      sb.append(to);
    }
    if (expires() != null) {
      // TBD: Wdy, DD-Mon-YY HH:MM:SS GMT
      log.warn("WOCookie does not yet support expires, and you should use " +
          "setTimeOut() anyways (aka Max-Age)");
    }
    else if (to == 0) {
      /* A convenience to improve browser compat, straight from:
       *   http://wp.netscape.com/newsref/std/cookie_spec.html
       * This helps Safari3 forget cookies (Max-Age: 0 doesn't seem to affect
       * it).
       */
      sb.append("; expires=Wednesday, 09-Nov-99 23:12:40 GMT");
    }

    if (this.isSecure)
      sb.append("; secure");

    sb.append("; SameSite=");
    sb.append(this.sameSite.toString());

    return sb.toString();
  }


  /* parsing cookies */

  public static WOCookie parseCookieString(final String _s) {
    if (_s == null) return null;

    int idx = _s.indexOf('=');
    if (idx == -1) {
      log.warn("got invalid cookie value: '" + _s + "'");
      return null;
    }

    String name  = _s.substring(0, idx);
    String value = _s.substring(idx + 1);

    // TODO: process escaping

    idx = _s.indexOf(';');
    if (idx == -1)
      return new WOCookie(name, value);

    /* process options */

    final String[] opts = value.substring(idx + 1).split(";");
    value = value.substring(0, idx);

    String  path = null;
    final String domain = null;
    boolean isSecure = false;
    String sameSite = null;

    for (int i = 0; i < opts.length; i++) {
      final String opt = opts[i].trim();
      if (opt.startsWith("domain="))
        name = opt.substring(7);
      else if (opt.startsWith("path="))
        path = opt.substring(5);
      else if (opt.equals("secure"))
        isSecure = true;
      else if (opt.startsWith("SameSite="))
        sameSite = opt.substring(9);
      else
        log.error("unknown cookie option: " + opt + " in: " + _s);
    }

    final WOCookie c = new WOCookie(name, value, path, domain, isSecure);
    if (sameSite != null) {
      if (sameSite.equals("Strict"))
        c.setSameSite(SameSite.Strict);
      else if (sameSite.equals("Lax"))
        c.setSameSite(SameSite.Lax);
      else if (sameSite.equals("None"))
        c.setSameSite(SameSite.None);
      else {
        log.error("unknown SameSite option: " + sameSite);
      }
    }
    return c;
  }


  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    if (this.name != null) {
      _d.append(" name=");
      _d.append(this.name);
    }
    if (this.value != null) {
      _d.append(" value=");
      _d.append(this.value);
    }

    if (this.path != null) {
      _d.append(" path=");
      _d.append(this.path);
    }
    if (this.domain != null) {
      _d.append(" domain=");
      _d.append(this.domain);
    }

    if (this.timeout == 0)
      _d.append(" delete-cookie");
    else if (this.timeout > 0) {
      _d.append(" to=");
      _d.append(this.timeout);
      _d.append("s");
    }

    _d.append(" SameSite=");
    _d.append(this.sameSite.toString());

    if (this.isSecure)
      _d.append(" secure");
  }


  /* utility */

  public static void addCookieInfo
    (final Collection<WOCookie> _cookies, final StringBuilder _sb)
  {
    boolean isFirst = true;

    for (final WOCookie cookie: _cookies) {
      if (isFirst) isFirst = false;
      else _sb.append(",");

      if (cookie.timeOut() == 0) { /* expire */
        _sb.append("-");
        _sb.append(cookie.name());
      }
      else {
        _sb.append(cookie.name());
        final String v = cookie.value();
        if (v != null) {
          _sb.append("=");
          if (v.length() < 12)
            _sb.append(v);
          else {
            _sb.append(v.substring(0, 10));
            _sb.append("..");
          }
        }
      }
    }
  }
}
