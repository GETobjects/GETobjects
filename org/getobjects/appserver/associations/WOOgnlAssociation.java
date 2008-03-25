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

package org.getobjects.appserver.associations;

import java.util.Map;

import ognl.Ognl;
import ognl.OgnlException;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.ognl.NSOgnlException;
import org.getobjects.ognl.WOFramework;

/**
 * WOOgnlAssociation
 * <p>
 * The OGNL association evaluates its string contents as an OGNL expression.
 * OGNL is the "Object Graph Navigation Language" which is documented at:
 * <p>
 * http://www.ognl.org/
 * <p>
 * In practice those bindings allow you to call arbitrary Java methods and
 * evaluate arbitrary Java expressions.
 * <p>
 * Example:
 * <pre><#WOConditional ognl:condition="name.startsWith('/en')"></pre>
 * This will call the startsWith() method on the 'name' of the current
 * component.
 * <p>
 * Note that NSKeyValueCoding objects are exposed as OGNL pathes. Eg you
 * can retrieve the name of a company using a regular KVC path inside the
 * OGNL expression: "person.company.name".
 * <p>
 * This association is parsed using either the 'ognl:' namespace prefix or
 * using the '~' in .wod files, eg:
 * <pre>IsEnglish: WOConditional { condition = ~name.startsWith('/en') }
 * <p>
 * The OGNL expressions are parsed and then cached when the association is
 * instantiated. 
 */
public class WOOgnlAssociation extends WOAssociation {
  
  static {
    WOFramework.setup(); /* setup OGNL for JOPE usage */
  }
  
  protected String ognlString; /* keep for debugging */
  protected Object ognl;
  
  public WOOgnlAssociation(Object _ognl) {
    super();
    this.ognl = _ognl;
  }

  public WOOgnlAssociation(String _expression) {
    super();
    
    this.ognlString = _expression;
    try {
      this.ognl = Ognl.parseExpression(this.ognlString);
    }
    catch (OgnlException e) {
      log.warn("could not parse OGNL expression: " + this.ognlString);
    }
  }
  
  /* accessors */
  
  public boolean isValid() {
    return this.ognl != null;
  }

  @Override
  public String keyPath() {
    return this.ognlString != null ? ("~" + this.ognlString) : null;
  }

  /* reflection */
  
  @Override
  public boolean isValueConstant() {
    if (this.ognl == null)
      return true;
     
    try {
      return Ognl.isConstant(this.ognl);
    }
    catch (OgnlException e) {
      log.error("failed to determine whether value is constant: " + this, e);
      return false;
    }
  }
  
  @Override
  public boolean isValueSettable() {
    if (this.ognl == null)
      return false;
    
    try {
      return Ognl.isSimpleNavigationChain(this.ognl);
    }
    catch (OgnlException e) {
      log.warn("could not check OGNL for navchain value: " + this.ognl, e);
      return this.isValueSettable();
    }
  }
  
  @Override
  public boolean isValueConstantInComponent(Object _cursor) {
    if (this.ognl == null)
      return true;
    
    try {
      return Ognl.isConstant(this.ognl, this.makeOgnlContext(_cursor));
    }
    catch (OgnlException e) {
      log.warn("could not check OGNL for const value: " + this.ognl, e);
      return this.isValueConstant();
    }
  }
  
  @Override
  public boolean isValueSettableInComponent(Object _cursor) {
    if (this.ognl == null)
      return false;
    
    // TBD: check whether this is really correct
    try {
      return Ognl.isSimpleNavigationChain(this.ognl,
          this.makeOgnlContext(_cursor));
    }
    catch (OgnlException e) {
      log.warn("could not check OGNL for navchain value: " + this.ognl, e);
      return this.isValueSettable();
    }
  }
  
  /* values */
  
  protected Map makeOgnlContext(Object _cursor) {
    return Ognl.createDefaultContext(_cursor);
  }
  
  @Override
  public void setValue(Object _value, Object _cursor) {
    if (_cursor == null)
      return;
    
    try {
      Ognl.setValue(this.ognl, this.makeOgnlContext(_cursor), _cursor, _value);
    }
    catch (OgnlException e) {
      throw new NSOgnlException
        ("error setting association value", this.ognlString, e);
    }
  }
  
  @Override
  public Object valueInComponent(Object _cursor) {
    if (_cursor == null)
      return null;
    
    try {
      return Ognl.getValue(this.ognl, this.makeOgnlContext(_cursor), _cursor);
    }
    catch (OgnlException e) {
      throw new NSOgnlException
        ("error retrieving association value for: " + _cursor,
            this.ognlString, e);
    }
    catch (NullPointerException e) {
      throw new NSOgnlException
        ("null-error retrieving association value for: " + _cursor,
         this.ognlString, e);
    }
  }
  
  /* specific values */
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.ognlString != null) {
      _d.append(" ognl=");
      _d.append(this.ognlString);
    }
    else
      _d.append(" no-string");
    
    if (this.ognl != null)
      _d.append(" parsed");
    else
      _d.append(" invalid-expression");
  }
}
