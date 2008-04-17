/*
  Copyright (C) 2006-2008 Helge Hess

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
package org.getobjects.appserver.elements;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.foundation.NSKeyValueStringFormatter;
import org.getobjects.foundation.UString;

/**
 * WOString
 * <p>
 * Just a plain, dynamic string. The 'value' can be an arbitrary object which is
 * then formatted using a java.text.Format.
 * <p>
 * 
 * Sample:<pre>
 *   ComponentName: WOString {
 *     value = name;
 *   }</pre>
 * 
 * Renders:
 *   The element renders the given value, possibly after applying conversions.
 * 
 * <p>
 * Bindings:
 * <pre>
 *   value          [in] - object
 *   valueWhenEmpty [in] - object
 *   escapeHTML     [in] - boolean (set to false to avoid HTML escaping)
 *   insertBR       [in] - boolean (replace newlines with &lt;br/&gt; tags)
 *   %value         [in] - string (pattern in %(keypath)s syntax)
 *   dateformat     [in] - a dateformat
 *   numberformat   [in] - a numberformat
 *   formatterClass [in] - Class or class name of a formatter to use
 *   formatter      [in] - java.text.Format used to format the value or the
 *                         format for the formatterClass</pre>
 * <p>
 * If additional bindings are given, the text will get embedded into a tag
 * (defaults to 'span' if no other 'elementName' binding is given).
 * <br>
 * Note: the tag is only rendered if there is content.
 */
public class WOString extends WOHTMLDynamicElement {
  protected Log log = LogFactory.getLog("WOString");
  
  protected WOAssociation value;
  protected WOAssociation valuePattern;
  protected WOAssociation valueWhenEmpty;
  protected WOAssociation escapeHTML;
  protected WOAssociation insertBR;
  protected WOFormatter   formatter;

  public WOString(String _name, Map<String, WOAssociation> _assocs,
                  WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.value          = grabAssociation(_assocs, "value");
    this.valuePattern   = grabAssociation(_assocs, "%value");
    this.escapeHTML     = grabAssociation(_assocs, "escapeHTML");
    this.valueWhenEmpty = grabAssociation(_assocs, "valueWhenEmpty");
    this.insertBR       = grabAssociation(_assocs, "insertBR");
    
    this.formatter = WOFormatter.formatterForAssociations(_assocs);
  }
  
  public WOString(WOAssociation _value, boolean _escapeHTML) {
    super(null /* name */, null /* assocs */, null /* template */);
    
    this.value      = _value;
    this.escapeHTML =
      WOAssociation.associationWithValue(new Boolean(_escapeHTML)); 
  }
  
  /* some convenience accessors for from-code creation */
  
  public WOString(Object _value) {
    this(WOAssociation.associationWithValue(_value), true /* escapeHTML */);
  }
  
  
  /* generate response */

  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    // method is pretty long, maybe we want to split it up
    if (_ctx.isRenderingDisabled())
      return;
    
    final boolean isDebugOn = this.log.isDebugEnabled();
    final Object  cursor = _ctx.cursor();
    Object  v        = null;
    boolean doEscape = true;
    String  s;
    
    if (isDebugOn) this.log.debug("append, cursor: " + cursor);
    
    if (this.value != null) {
      if ((v = this.value.valueInComponent(cursor)) == null) {
        if (isDebugOn)
          this.log.debug("  value binding return no object: " + this.value);
      }
      if (this.valuePattern != null) {
        String pat = this.valuePattern.stringValueInComponent(cursor);
        v = pat != null ? NSKeyValueStringFormatter.format(pat, v) : null;
      }
    }
    else if (this.valuePattern != null) {
      String pat = this.valuePattern.stringValueInComponent(cursor);
      v = pat != null ? NSKeyValueStringFormatter.format(pat, cursor) : null;
    }
    else {
      if (isDebugOn) this.log.debug("  no value binding: " + cursor);
      return;
    }
    
    /* valueWhenEmpty */
    
    if (v != null) {
      if (v instanceof String) {
        if (((String)v).length() == 0)
          v = null;
      }
    }
    if (this.valueWhenEmpty != null) {
      if (v == null) // Note: length==0 is checked above
        v = this.valueWhenEmpty.valueInComponent(cursor);
    }
    
    /* format value */
    
    if (this.formatter != null) {
      try {
        s = this.formatter.stringForObjectValue(v, _ctx);
      }
      catch (NullPointerException e) {
        log().error("exception during formatting of value: " + v +
            "\n  formatter: " + this.formatter +
            "\n  context:   " + _ctx, e);
        s = v.toString();
      }
    }
    else if (v != null)
      s = v.toString();
    else
      s = null;

    /* is escaping required? */
    
    if (this.escapeHTML != null) {
      if (!this.escapeHTML.booleanValueInComponent(cursor))
        doEscape = false;
    }

    if (isDebugOn) this.log.debug("  escape: " + doEscape);
    
    /* insertBR processing */
    
    if (this.insertBR != null && s != null) {
      if (this.insertBR.booleanValueInComponent(cursor)) {
        s = this.handleInsertBR(s, doEscape);
        doEscape = false;
      }
    }
    
    /* append */
    
    if (s != null && s.length() > 0) {
      if (this.extraKeys != null && this.extraKeys.length > 0)
        this.appendWrappedStringToResponse(_r, _ctx, s, doEscape);
      else if (doEscape)
        _r.appendContentHTMLString(s);
      else
        _r.appendContentString(s);
    }
    else if (isDebugOn)
      this.log.debug("  no content to render.");
  }
  
  /**
   * Rewrites \n characters to &lt;br /&gt; tags. Note: the result must not be
   * escaped in subsequent processing.
   * 
   * @param s        - String to rewrite
   * @param doEscape - whether the parts need to be escaped
   * @return the rewritten string
   */
  public String handleInsertBR(String s, boolean doEscape) {
    if (s == null)
      return null;
    
    /* Note: we can't use replace() because we need to escape the individual
     *       parts.
     */
    String[] lines = s.split("[\\n]");
    StringBuilder sb = new StringBuilder(lines.length * 80 + 16);
    boolean isFirst = true;

    for (String line: lines) {
      if (isFirst)
        isFirst = false;
      else
        sb.append("<br />");
      if (doEscape)
        line = UString.stringByEscapingHTMLString(line);
      sb.append(line);
    }
    doEscape = false;
    return sb.toString();
  }
  
  /**
   * Embeds the String in an HTML tag, &lt;span&gt; per default.
   * 
   * @param _r       - the WOResponse to add to
   * @param _ctx     - the WOContext in which the operation runs
   * @param s        - the already formatted string to render
   * @param doEscape - whether to use appendContentHTMLString() or not
   */
  public void appendWrappedStringToResponse
    (final WOResponse _r, final WOContext _ctx, String s, boolean doEscape)
  {
    /* this allows you to attach attributes to a text :-) */
    final Object cursor = _ctx.cursor();
    final int attrCount = this.extraKeys != null ? this.extraKeys.length : 0;
    String elementName = null;
    
    /* first scan for elementName */
    
    for (int i = 0; i < attrCount; i++) {
      if ("elementName".equals(this.extraKeys[i])) {
        elementName = this.extraValues[i].stringValueInComponent(cursor);
        break;
      }
    }
    if (elementName == null) elementName = "span";
    
    /* start tag */
    
    _r.appendBeginTag(elementName);
    
    /* render attributes */
    
    for (int i = 0; i < attrCount; i++) {
      if ("elementName".equals(this.extraKeys[i]))
        continue;
      
      _r.appendAttribute(this.extraKeys[i],
          this.extraValues[i].stringValueInComponent(cursor));
    }
    
    /* finish start tag */
    
    _r.appendBeginTagEnd();
    
    /* render content */
    
    if (doEscape)
      _r.appendContentHTMLString(s);
    else
      _r.appendContentString(s);
    
    /* end tag */
    
    _r.appendEndTag(elementName);
  }
  
  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    this.appendAssocToDescription(_d, "value",          this.value);
    this.appendAssocToDescription(_d, "valuePattern",   this.valuePattern);
    this.appendAssocToDescription(_d, "valueWhenEmpty", this.valueWhenEmpty);
    this.appendAssocToDescription(_d, "escapeHTML",     this.escapeHTML);
    this.appendAssocToDescription(_d, "insertBR",       this.insertBR);
    
    if (this.formatter != null) {
      _d.append(" formatter=");
      _d.append(this.formatter);
    }
  }
}
