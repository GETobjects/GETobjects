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

import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.associations.WOCheckRoleAssociation;
import org.getobjects.appserver.associations.WOKeyAssociation;
import org.getobjects.appserver.associations.WOKeyPathAssociation;
import org.getobjects.appserver.associations.WOKeyPathPatternAssociation;
import org.getobjects.appserver.associations.WOLabelAssociation;
import org.getobjects.appserver.associations.WONegateAssociation;
import org.getobjects.appserver.associations.WOOgnlAssociation;
import org.getobjects.appserver.associations.WOQualifierAssociation;
import org.getobjects.appserver.associations.WORegExAssociation;
import org.getobjects.appserver.associations.WOResourcePatternAssociation;
import org.getobjects.appserver.associations.WOValueAssociation;
import org.getobjects.appserver.publisher.GoPathAssociation;
import org.getobjects.foundation.NSJavaRuntime;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.NSPropertyListParser;
import org.getobjects.foundation.UObject;

/**
 * WOAssociation
 * <p>
 * Abstract superclass for 'association' classes. Associations define how
 * dynamic elements (stateless, non-WOComponent template elements) pull and
 * push their 'bindings'.
 * <p>
 * The most common subclasses are <code>WOKeyPathAssociation</code>, which
 * pushes/pulls values into/from the current component in the context,
 * and <code>WOValueAssociation</code>, which just wraps a constant value in
 * the WOAssociation API.
 * <br>
 * But in addition there are associations which evaluate OGNL expressions,
 * which resolve their value as localization keys or which resolve string
 * patterns in a certain context. etc etc
 */
public abstract class WOAssociation extends NSObject
  implements IWOAssociation
{
  protected static final Log log = LogFactory.getLog("WOAssociations");

  /**
   * Factory method which returns an association which pulls/pushes values
   * from the current component using KVC (key/value coding).<br>
   * Per default this is the WOKeyPathAssociation class.
   *
   * @param _keyPath - the keypath (eg "person.address.city")
   * @return the WOAssociation, usually a WOKeyPathAssociation
   */
  public static WOAssociation associationWithKeyPath(final String _keyPath) {
    if (_keyPath != null && _keyPath.indexOf('.') < 0) {
      // TBD: we could even have a few hardcoded assoc keys, eg 'item' to save
      //      objects
      return new WOKeyAssociation(_keyPath);
    }

    return new WOKeyPathAssociation(_keyPath);
  }

  /**
   * Factory method which returns an association that wraps a constant in the
   * association interface. The constants are usually - but not necessarily -
   * simple objects, like Strings or Numbers or property list objects (Map,
   * List).
   * <br>
   * Per default this is the WOValueAssociation class.
   *
   * @param _value - the value, eg a String or a Number or a Map
   * @return the WOAssociation, usually a WOValueAssociation
   */
  public static WOAssociation associationWithValue(final Object _value) {
    return new WOValueAssociation(_value);
  }


  /* accessors */

  /**
   * Returns a KVC keypath associated with the WOAssociation. Or nothing if the
   * association is unrelated with KVC.
   *
   * @return the keypath of the association
   */
  public abstract String keyPath();


  /* reflection */

  /**
   * Returns true if the association always returns the same value. This can be
   * used by dynamic elements to cache the value (and discard the association
   * wrapper).
   *
   * @return true if the value of the association never changes, false otherwise
   */
  public boolean isValueConstant() {
    return false;
  }

  /**
   * Returns true if the association accepts new values. Eg a constant
   * association obviously doesn't accept new values. A KVC association to a
   * target which does not have a <code>set</code> accessor could also return
   * false (but currently does not ...).
   *
   * @return true if the value of the association can be set, false otherwise
   */
  public boolean isValueSettable() {
    return true;
  }

  /**
   * Returns true if the association always returns the same value for the
   * specified cursor (usually a component). This can be used by dynamic
   * elements to cache the value.
   *
   * @return true if the value of the association does not change
   */
  public boolean isValueConstantInComponent(final Object _cursor) {
    return this.isValueConstant();
  }

  /**
   * Returns true if the association can accept new values for the given cursor
   * (usually a WOComponent). A KVC association to a target which does not have
   * a <code>set</code> accessor could also return false (but currently does
   * not ...).
   *
   * @return true if the value of the association can be set, false otherwise
   */
  public boolean isValueSettableInComponent(final Object _cursor) {
    return this.isValueSettable();
  }


  /* values */

  public void setValue(final Object _value, final Object _cursor) {
    /* Note: crappy name due to WO compatibility */
  }

  public Object valueInComponent(Object _cursor) {
    return null;
  }


  /* specific values */

  public void setBooleanValue(final boolean _value, final Object _cursor) {
    this.setValue(_value ? Boolean.TRUE : Boolean.FALSE, _cursor);
  }
  public boolean booleanValueInComponent(final Object _cursor) {
    return UObject.boolValue(this.valueInComponent(_cursor));
  }

  public void setIntValue(final int _value, final Object _cursor) {
    this.setValue(new Integer(_value), _cursor);
  }
  public int intValueInComponent(final Object _cursor) {
    return UObject.intValue(this.valueInComponent(_cursor));
  }

  public void setStringValue(final String _value, final Object _cursor) {
    this.setValue(_value, _cursor);
  }
  public String stringValueInComponent(final Object _cursor) {
    Object v = this.valueInComponent(_cursor);

    if (v == null)
      return null;

    if (v instanceof String)
      return (String)v;

    return v.toString();
  }


  /* prefix registry */

  protected static ConcurrentHashMap<String, Class> prefixToAssoc =
    new ConcurrentHashMap<String, Class>(16);

  public static Class getAssociationClassForPrefix(final String _prefix) {
    return _prefix != null ? prefixToAssoc.get(_prefix) : null;
  }
  public static void registerAssociationClassForPrefix
    (final String _prefix, final Class _class)
  {
    if (_prefix == null || _prefix.length() == 0) {
      log.error("got no prefix in assoc-class registration!");
      return;
    }

    if (_class != null)
      prefixToAssoc.put(_prefix, _class);
    else
      prefixToAssoc.remove(_prefix);
  }

  /**
   * Create an WOAssociation object for the given namespace prefix. This is
   * called when the parser encounters element attributes in the HTML file,
   * its not called from the WOD parse.
   * <p>
   * Prefixes:
   * <ul>
   *   <li>const  - WOAssociation.associationWithValue
   *   <li>jo     - GoPathAssociation
   *   <li>label  - WOLabelAssociation
   *   <li>ognl   - WOOgnlAssociation
   *   <li>plist  - WOAssociation.associationWithValue
   *   <li>q      - WOQualifierAssociation
   *   <li>regex  - WORegExAssociation
   *   <li>role   - WOCheckRoleAssociation
   *   <li>rsrc   - WOResourceURLAssociation
   *   <li>var    - WOAssociation.associationWithKeyPath
   *   <li>varpat - WOKeyPathPatternAssociation
   *   <li>not    - WONegateAssociation on WOKeyPathAssociation
   * </ul>
   * If no prefix matches, associationWithValue will be used.
   *
   * @param _prefix - the parsed prefix which denotes the association class
   * @param _name   - the name of the binding (not relevant in this imp)
   * @param _value  - the value which needs to be put into the context
   */
  public static WOAssociation associationForPrefix
    (final String _prefix, final String _name, final String _value)
  {
    WOAssociation assoc = null;

    char c = _prefix.charAt(0);

    switch (c) {
      case 'c':
        if (_prefix.equals("const"))
          assoc = associationWithValue(_value);
        break;

      case 'j':
        if (_prefix.equals("jo"))
          assoc = new GoPathAssociation(_value);
        break;

      case 'l':
        if (_prefix.equals("label") || _prefix.equals("loc"))
          assoc = new WOLabelAssociation(_value);
        break;

      case 'n':
        if (_prefix.equals("not"))
          // TBD: inspect value for common _static_ values, eg 'true'?
          assoc = new WONegateAssociation(new WOKeyPathAssociation(_value));
        break;

      case 'o':
        if (_prefix.equals("ognl"))
          assoc = new WOOgnlAssociation(_value);
        break;

      case 'p':
        if (_prefix.equals("plist")) {
          /* Allow arrays like this: list="(a,b,c)",
           * required because we can't specify plists in .html
           * template attributes. (we might want to change that?)
           */
          NSPropertyListParser parser = new NSPropertyListParser();
          Object v = parser.parse(_value);
          if (v == null) {
            log.warn("could not parse plist value of association: " + _value,
                     parser.lastException());
            assoc = null;
          }
          else
            assoc = associationWithValue(v);
        }
        break;

      case 'q':
        if (_prefix.equals("q"))
          assoc = new WOQualifierAssociation(_value);
        break;

      case 'r':
        if (_prefix.equals("rsrc"))
          assoc = new WOResourceURLAssociation(_value);
        else if (_prefix.equals("rsrcpat"))
          assoc = new WOResourcePatternAssociation(_value);
        else if (_prefix.equals("regex"))
          assoc = new WORegExAssociation(_value);
        else if (_prefix.equals("role"))
          assoc = new WOCheckRoleAssociation(_value);
        break;

      case 'v':
        if (_prefix.equals("var")) {
          if (_value == null || _value.length() == 0) {
            log.error("got var: association w/o keypath: " + _name);
            assoc = associationWithValue
              ("[binding '"+ _name + "' has no value]");
          }
          else
            assoc = associationWithKeyPath(_value);
        }
        else if (_prefix.equals("varpat"))
          assoc = new WOKeyPathPatternAssociation(_value);
        break;
    }

    if (assoc == null) {
      Class clazz = prefixToAssoc.get(_prefix);
      if (clazz != null)
        assoc = (WOAssociation)NSJavaRuntime.NSAllocateObject(clazz, _value);
    }

    if (assoc == null) /* default to value assoc */
      assoc = associationWithValue(_value);

    return assoc;
  }

}
