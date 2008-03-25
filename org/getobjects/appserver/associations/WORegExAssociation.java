/*
  Copyright (C) 2007-2008 Helge Hess

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
package org.getobjects.appserver.associations;

import java.util.regex.Pattern;

import org.getobjects.appserver.core.WOAssociation;

/**
 * WORegExAssociation
 * <p>
 * Returns a pattern object for a given regex.
 * <p>
 * This is roughly the same like:
 * <pre>ognl:value="Pattern.compile('MY PATTERN')"</pre>
 * But the regex association is a constant association (the Pattern object
 * never changes), which allows additional caching.
 * <p>
 * This can be used in combination with the WOConditional 'match' binding,
 * eg:<pre>
 *   &lt;#if regex:match="^/person.*" value="request.uri"&gt;</pre>
 * Which is just like:<pre> 
 *   &lt;#if match="^/person.*" value="request.uri"&gt;</pre>
 * with the difference that the first one caches the RegEx Pattern object.
 * <br>
 * Well, actually WOConditional optimizes the case above :-) If the match
 * binding is a constant value association, WOConditional will convert it to
 * a WORegExAssociation. 
 */
public class WORegExAssociation extends WOAssociation {
  
  protected Pattern pattern;
  
  public WORegExAssociation(final String _regex) {
    // TBD: we could support options?
    this.pattern = _regex != null ? Pattern.compile(_regex) : null;
  }
  
  /* accessors */

  @Override
  public String keyPath() {
    return this.pattern.pattern();
  }

  /* reflection */
  
  @Override
  public boolean isValueConstant() {
    return true;
  }
  
  @Override
  public boolean isValueSettable() {
    return false;
  }
  
  @Override
  public boolean isValueConstantInComponent(final Object _cursor) {
    return true;
  }
  
  @Override
  public boolean isValueSettableInComponent(final Object _cursor) {
    return false;
  }
  
  /* values */
  
  @Override
  public String stringValueInComponent(Object _cursor) {
    return (this.pattern == null) ? null : this.pattern.pattern();
  }
  
  @Override
  public Object valueInComponent(Object _cursor) {
    return this.pattern;
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    _d.append(" pattern=");
    _d.append(this.pattern);
  }
}
