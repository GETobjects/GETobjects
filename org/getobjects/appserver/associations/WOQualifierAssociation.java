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
package org.getobjects.appserver.associations;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.eocontrol.EOQualifierEvaluation;

/**
 * WOQualifierAssociation
 * <p>
 * An association which returns a boolean value depending on a qualifier.
 * <p>
 * This association is exposed under the 'q:' prefix in the wrapper template
 * builder.
 * <p>
 * Sample:<pre>
 *   &lt;wo:if q:condition="lastname LIKE 'Duck*'"&gt;</pre>
 * Note: WOConditional implements the 'q' binding as a shortcut:<pre>
 *   &lt;wo:if q="lastname LIKE 'Duck*'"&gt;</pre>
 */
public class WOQualifierAssociation extends WOAssociation {
  
  protected EOQualifier qualifier;

  public WOQualifierAssociation(EOQualifier _q) {
    this.qualifier = _q;
  }
  public WOQualifierAssociation(String _q) {
    this(EOQualifier.qualifierWithQualifierFormat(_q));
  }
  
  /* accessors */
  
  public EOQualifier qualifier() {
    return this.qualifier;
  }

  @Override
  public String keyPath() {
    return this.qualifier.stringRepresentation();
  }

  /* reflection */
  
  @Override
  public boolean isValueConstant() {
    return false;
  }
  
  @Override
  public boolean isValueSettable() {
    return false;
  }
  
  @Override
  public boolean isValueConstantInComponent(final Object _cursor) {
    return false;
  }
  
  @Override
  public boolean isValueSettableInComponent(final Object _cursor) {
    return false;
  }
  
  /* values */
  
  @Override
  public boolean booleanValueInComponent(final Object _cursor) {
    EOQualifier q = this.qualifier.qualifierWithBindings(_cursor, true);
    return ((EOQualifierEvaluation)q).evaluateWithObject(_cursor);
  }
  
  @Override
  public Object valueInComponent(final Object _cursor) {
    return this.booleanValueInComponent(_cursor) ? Boolean.TRUE : Boolean.FALSE;
  }

  @Override
  public int intValueInComponent(final Object _cursor) {
    return this.booleanValueInComponent(_cursor) ? 1 : 0;
  }

  @Override
  public String stringValueInComponent(final Object _cursor) {
    return this.booleanValueInComponent(_cursor) ? "true" : "false";
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    _d.append(" q=");
    _d.append(this.qualifier);
  }
}
