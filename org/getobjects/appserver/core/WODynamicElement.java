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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSKeyValueStringFormatter;

/**
 * WODynamicElement
 * <p>
 * This is the abstract superclass for _stateless_ and reentrant parts of a
 * template. Subclasses MUST NOT store any processing state in instance
 * variables because they can be accessed concurrently by multiple threads and
 * even by one thread.
 * <p>
 * This element also tracks 'extra bindings'. Those are bindings which where
 * not explicitly grabbed (removed from the '_assocs' ctor Map) by subclasses.
 * What is done with those extra bindings depends on the subclass, usually
 * they are added to the HTML tag which is managed by the dynamic element.
 * For example extra-attrs in a WOHyperlink are (usually) added to the &lt;a&gt;
 * tag.
 * <p>
 * Further 'extra bindings' can be <code>%(key)s</code> style patterns. Example:
 * <pre>
 *   &lt:wo:span id="employee-%(person.id)s"&gt;...&lt;wo:span&gt;
 *   &lt;a onclick="alert('clicked %(person.name)s');"&gt;</pre>
 * Those patterns are resolved using the NSKeyValueStringFormatter.format()
 * function.
 */
public abstract class WODynamicElement extends WOElement {
  // TBD: pregenerate HTML for constant extra attributes (otherTagString?)
  protected static final Log delog = LogFactory.getLog("WODynamicElement");
  
  protected WOAssociation   otherTagString;
  protected String[]        extraKeys;
  protected WOAssociation[] extraValues;

  public WODynamicElement
    (String _name, Map<String,WOAssociation> _assocs, WOElement _template)
  {
  }
  
  /* helpers */
  
  public static WOAssociation grabAssociation
    (final Map<String,WOAssociation> _assocs, final String _name)
  {
    if (_assocs == null)
      return null;
    
    final WOAssociation assoc = _assocs.get(_name);
    if (assoc == null)
      return null;
    
    _assocs.remove(_name);
    return assoc;
  }
  
  /* accessors */
  
  public Log log() {
    return delog;
  }
  
  /**
   * Usually called by the WOWrapperTemplateBuilder to apply bindings which
   * did not get grabbed in the constructor of the element.
   * 
   * @param _attrs - the bindings map (often empty)
   */
  public void setExtraAttributes(Map<String, WOAssociation> _attrs) {
    if (delog.isDebugEnabled())
      delog.debug("setting extra attributes: " + _attrs);
    
    this.extraKeys   = null;
    this.extraValues = null;
    
    this.otherTagString = grabAssociation(_attrs, "otherTagString");
    
    int extraCount;
    if (_attrs != null && (extraCount = _attrs.size()) > 0) {
      this.extraKeys   = new String[extraCount];
      this.extraValues = new WOAssociation[extraCount];
      
      int i = 0;
      for (String key: _attrs.keySet()) {
        this.extraKeys[i]   = key;
        if ((this.extraValues[i] = _attrs.get(key)) == null)
          delog.warn("missing association for extra binding: " + key);
        i++;
      }
    }
  }
  
  
  /* response */
  
  /**
   * Calls the broader appendExtraAttributesToResponse() with the cursor()
   * of the context as the pattern object.
   * 
   * @param _response - the WOResponse
   * @param _ctx      - the WOContext
   */
  public void appendExtraAttributesToResponse
    (final WOResponse _response, final WOContext _ctx)
  {
    if (this.extraKeys == null)
      return;
    
    this.appendExtraAttributesToResponse(_response, _ctx, null /* patobject */);
  }
  
  /**
   * The method walks over all 'extraKeys'. If the key starts with a '%'
   * sign, the value of the key is treated as pattern for the
   * NSKeyValueStringFormatter.format() function.
   * 
   * @param _r - the WOResponse
   * @param _c - the WOContext
   * @param _patObject - the pattern object, usually the active WOComponent
   */
  public void appendExtraAttributesToResponse
    (final WOResponse _r, final WOContext _c, Object _patObject)
  {
    if (this.extraKeys == null)
      return;
    
    /* we could probably improve the speed of the pattern processor ... */
    final Object cursor = _c != null ? _c.cursor() : null;
    if (_patObject == null) _patObject = cursor;
    for (int i = 0; i < this.extraKeys.length; i++) {
      String v = this.extraValues[i].stringValueInComponent(cursor);
      if (v == null)
        continue;
      
      if (this.extraKeys[i].charAt(0) == '%') {
        v = NSKeyValueStringFormatter.format(v, _patObject);
        _r.appendAttribute(this.extraKeys[i].substring(1), v);
      }
      else
        _r.appendAttribute(this.extraKeys[i], v);
    }
  }
  
  
  /* description */
  
  /**
   * Utility function to add WOAssociation ivar info to a description string.
   * Example:<pre>
   *   this.appendAssocToDescription(_d, "id", this.idBinding);</pre>
   * 
   * @param _d    - the String to add the description to
   * @param _name - name of the binding
   * @param _a    - WOAssociation object used as the binding value
   */
  public static void appendBindingToDescription
    (final StringBuilder _d, final String _name, final WOAssociation _a)
  {
    if (_a == null)
      return;
    
    _d.append(' ');
    _d.append(_name);
    _d.append('=');

    // TODO: make output even smarter ;-)
    if (!_a.isValueConstant()) {
      _d.append(_a);
      return;
    }
    
    /* constant assocs */
    
    Object v = _a.valueInComponent(null);
    
    if (v != null) {
      _d.append('"');
      if (v instanceof String) {
        if (((String)v).length() > 79)
          v = ((String)v).substring(0, 77) + "...";
      }
      _d.append(v);
      _d.append('"');
    }
    else
      _d.append(" null");
  }
  public static void appendBindingsToDescription
    (final StringBuilder _d, final Object ... _nameAssocPairs)
  {
    if (_nameAssocPairs == null)
      return;
    for (int i = 1; i < _nameAssocPairs.length; i += 2) {
      appendBindingToDescription(_d,
          (String)_nameAssocPairs[i - 1], (WOAssociation)_nameAssocPairs[i]);
    }
  }
  
  public void appendAssocToDescription
    (final StringBuilder _d, final String _name, final WOAssociation _a)
  {
    appendBindingToDescription(_d, _name, _a);
  }
  public void appendAssocsToDescription
    (final StringBuilder _d, final Object ... _nameAssocPairs)
  {
    if (_nameAssocPairs == null)
      return;
    for (int i = 1; i < _nameAssocPairs.length; i += 2) {
      appendBindingToDescription(_d,
          (String)_nameAssocPairs[i - 1], (WOAssociation)_nameAssocPairs[i]);
    }
  }
}
