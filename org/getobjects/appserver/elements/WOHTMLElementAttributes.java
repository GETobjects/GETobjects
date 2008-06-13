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
package org.getobjects.appserver.elements;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.foundation.UObject;
import org.getobjects.foundation.UString;

/**
 * WOHTMLElementAttributes
 * <p>
 * An element used to generate the core set of attributes which can be assigned
 * to an HTML tag. Currently 'style' and 'class'.
 * <p>
 * The element has support for dynamic creation of the contents of a style or
 * class attribute.
 * Contents of 'style' attributes are managed by bindings
 * which start using a '!'.
 * Contents of the CSS class are controlled by
 * bindings starting with a dot (.).
 * <p>
 * Example:<pre>
 * &lt;#div !display='none' style="color: red;"&gt;</pre>
 * produces<pre>
 * &lt;div style="color: red; display: none;"&gt;</pre>
 * A very common usecase is dynamically adding classes, eg:<pre>
 * &lt;#li var:.selected="isSelectedPage"&gt;Customers&lt;/#li&gt;</pre>
 */
public class WOHTMLElementAttributes extends WODynamicElement {
  protected static final Log log = LogFactory.getLog("WOHTMLElementAttributes");
  
  protected static final char stylePrefix = '!';
  protected static final char classPrefix = '.';
  
  final protected WOAssociation style;
  final protected WOAssociation clazz;
  protected Map<String, WOAssociation> dynStyles;  /* all !style bindings */
  protected Map<String, WOAssociation> dynClasses; /* all .class bindings */
  
  public WOHTMLElementAttributes
    (String _name, Map<String, WOAssociation> _assoc, WOElement _t)
  {
    super(_name, _assoc, _t);
    
    this.style = grabAssociation(_assoc, "style");
    this.clazz = grabAssociation(_assoc, "class");
    
    if (_assoc.size() > 0) {
      
      for (String key: _assoc.keySet()) {
        final char c0 = key != null && key.length() > 1 ? key.charAt(0) : 0;
        
        if (c0 == stylePrefix) { /* eg: !display="none" */
          if (this.dynStyles == null)
            this.dynStyles = new HashMap<String, WOAssociation>(16);
          this.dynStyles.put(key.substring(1), _assoc.get(key));
        }
        else if (c0 == classPrefix) { /* eg .even */
          if (this.dynClasses == null)
            this.dynClasses = new HashMap<String, WOAssociation>(16);
          this.dynClasses.put(key.substring(1), _assoc.get(key));
        }
      }
      
      /* not sure whether we can modify the hash concurrently */
      if (this.dynStyles != null) {
        for (String key: this.dynStyles.keySet())
          _assoc.remove(stylePrefix + key);
      }
      if (this.dynClasses != null) {
        for (String key: this.dynClasses.keySet())
          _assoc.remove(classPrefix + key);
      }
    }
    /*
    if (this.dynStyles != null) {
      System.err.println("SETUP: " + this.dynStyles);
      System.err.println("    A: " + _assoc);
    }
    */
    
    if (_t != null)
      log().warn("WOElementAttributes element got assigned a template: " + _t);
  }
  
  /**
   * This element checks whether the associations Map contains any dynamic
   * class or style bindings. If yes, it creates a new WOHTMLElementAttributes
   * instances to generate those, if not, it returns null.
   * 
   * @param _name  - the name of the new element (no internal use)
   * @param _assoc - the associations to be scanned for class/tyle
   * @return a dynamic element to generate the class/style attributes
   */
  public static WOHTMLElementAttributes buildIfNecessary
    (final String _name, final Map<String, WOAssociation> _assoc)
  {
    if (_assoc == null || _assoc.size() == 0)
      return null;

    for (String key: _assoc.keySet()) {
      final char c0 = key != null && key.length() > 1 ? key.charAt(0) : 0;
      
      if (c0 < 1) continue;

      if (c0 == stylePrefix || c0 == classPrefix) 
        return new WOHTMLElementAttributes(_name, _assoc, null);
      
      if ((c0 == 's' && key.equals("style")) ||
          (c0 == 'c' && key.equals("class")))
        return new WOHTMLElementAttributes(_name, _assoc, null);
    }
    
    return null;
  }

  
  /* supporting methods */
  
  /**
   * This evaluates each !style binding and returns the values in a Map.
   * For example:<pre>
   *   !color = "red";
   *   !font  = "bold";
   *   !high  = null;</pre>
   * Will be returned as<pre>{ color = "red"; font = "bold"; }</pre>
   * 
   * @param _cursor - the component for the evaluation of bindings
   * @return a Map containing the bindings, or null if none were found
   */
  protected Map<String, Object> extractExtStyles(final Object _cursor) {
    Map<String, Object> styles = null;
    
    for (String cssStyleName: this.dynStyles.keySet()) {
      Object v = this.dynStyles.get(cssStyleName).valueInComponent(_cursor);
      if (v == null) continue;
      
      if (styles == null) styles = new HashMap<String, Object>(4);
      styles.put(cssStyleName, v);
    }
    return styles;
  }
  
  /**
   * This method evaluates the .class bindings. The value of a .class binding
   * is a BOOLEAN, eg:<pre>
   *   .selected = isPageSelected</pre>
   * This will only ADD the 'selected' class if the isPageSelected binding
   * returns <code>true</code>.
   * <p>
   * If the binding returns <code>null</code>, the code will not do anything
   * with the class. If it returns <code>false</code> it will actually REMOVE
   * the class from the list.
   * 
   * @param _cursor - the component for the evaluation of bindings
   * @return a List of CSS classnames
   */
  protected List<String> extractExtClasses(final Object _cursor) {
    List<String> classes = null;
    
    for (String cssClassName: this.dynClasses.keySet()) {
      /* extract the value of the .class binding */
      Object v = this.dynClasses.get(cssClassName).valueInComponent(_cursor);
      if (v == null) {
        /* Note how 'null' and 'false' are different. 'null' just doesn't add
         * the class, but 'false' actually removes the class.
         * TBD: would need to work with the base classes to be useful.
         */
        continue;
      }
      
      /* this is to detect bugs */
      boolean isOk;
      if (v instanceof Boolean)
        isOk = (Boolean)v;
      else if (v instanceof Integer)
        isOk = ((Integer)v).intValue() != 0;
      else if (v instanceof String) {
        String s = (String)v;
        
        // TBD: kinda stupid. Just to support the warning ...
        if (s.equals("true"))
          isOk = true;
        else if (s.equals("false"))
          isOk = false;
        else if (s.equals("1"))
          isOk = true;
        else if (s.equals("0"))
          isOk = false;
        else if (s.equals("YES"))
          isOk = true;
        else if (s.equals("NO"))
          isOk = false;
        else if (s.length() > 0) {
          log.warn("unexpected String in CSS class '" + cssClassName +
              "' boolean binding: " + 
              this.dynClasses.get(cssClassName) + ", value: " + v);
          isOk = UObject.boolValue(v);
        }
        else
          isOk = false;
      }
      else
        isOk = true;
      
      if (isOk) {
        if (classes == null || !classes.contains(cssClassName)) {
          if (classes == null)
            classes = new ArrayList<String>(4);
          
          classes.add(cssClassName);
        }
      }
      else if (classes != null) {
        // TBD: not so useful unless it works on the full set of names?
        classes.remove(cssClassName);
      }
    }
    return classes;
  }
  
  @SuppressWarnings("unchecked")
  protected Collection<String> listForClasses(final Object _classes) {
    if (_classes == null)
      return null;
    
    if (_classes instanceof Collection) {
      final List<String> list = new ArrayList<String>(4);
      for (Object c: (Collection<Object>)_classes) {
        String s = this.stringForClass(c);
        if (s == null) continue;
        list.add(s);
      }
      return list;
    }
    
    if (_classes instanceof String) {
      /* simple parser, eg 'record even' */
      final List<String> list = new ArrayList<String>(4);
      for (String className: ((String)_classes).split(" ")) {
        className = className.trim();
        if (className.length() == 0) continue;
        list.add(className);
      }
      return list;
    }

    log().error("do not know what to do about class value: " + _classes);
    return null;
  }
  protected String stringForClass(final Object _class) {
    return _class != null ? _class.toString() : null;
  }
  
  @SuppressWarnings("unchecked")
  protected Map<String, ?> mapForStyles(final Object _styles) {
    if (_styles == null)
      return null;
    
    if (_styles instanceof Map)
      return (Map<String, ?>)_styles;
    
    if (_styles instanceof Entry) {
      Entry e = ((Entry)_styles);
      Map<String, Object> vals = new HashMap<String, Object>(1);
      vals.put(e.getKey().toString(), e.getValue());
      return vals;
    }
    
    if (_styles instanceof Collection) {
      // Collection: set of key/value pairs?
      Map<String, Object> vals = null;
      
      for (Object styleEntry: (Collection<Object>)_styles) {
        Map<String, ?> sm = this.mapForStyles(styleEntry);
        if (sm == null || sm.size() == 0)
          continue;
        
        if (vals == null)
          vals = (Map<String, Object>)sm;
        else
          vals.putAll(sm);
      }
      return vals;
    }

    if (_styles instanceof String)
      return this.parseCssStyles((String)_styles);

    log().error("do not know what to do about style value: " + _styles);
    return null;
  }
  
  /**
   * Simple CSS style parser. Just splits on ';' and ':'. Example:<pre>
   *   display: none; color: red;</pre>
   * will result in:<pre>
   *   { "display" = "none"; "color": "red"; }</pre>
   * 
   * @param _styles - a String containing a 'style' value string
   * @return a Map containing the styles
   */
  protected Map<String, ?> parseCssStyles(String _styles) {
    if (_styles != null) _styles = _styles.trim();
    if (_styles == null || _styles.length() == 0)
      return null;
    
    /* simple parser ... */
    String[] pairs = _styles.split(";"); /* display: none; color: red; */
    Map<String, String> styles = new HashMap<String, String>(pairs.length);
    for (String pair: pairs) {
      pair = pair.trim();
      if (pair.length() == 0) continue;
      
      int idx = pair.indexOf(':');
      if (idx < 0) {
        log().error("did not find colon in style value pair: " + _styles);
        continue;
      }
      
      String key = pair.substring(0, idx).trim();
      String vs  = pair.substring(idx + 1).trim();
      
      if (key.length() == 0 || vs.length() == 0) {
        log().error("found invalid style value pair: " + _styles);
        continue;
      }
      
      // TBD: should we attempt to parse the value?
      styles.put(key, vs);
    }
    
    return styles;
  }
  
  /**
   * This method renders a few special object values, depending on the style
   * key.
   * <ul>
   *   <li>'padding', 'margin', 'border':
   *     the value can be a Map with 'top', 'right', 'bottom', 'left' keys
   *   <li>Collection's are rendered as Strings with ", " between the values
   *   <li>'visibility', 'display', 'clear':
   *     the value can be a Boolean, it will render as 'visible'/'hidden',
   *     'block'/'none' and 'both'/'none'
   * </ul> 
   * 
   * @param _key   - name of the style element, eg 'visibility'
   * @param _value - value to render
   * @param _sb    - the output buffer
   */
  public void appendStyleValue(String _key, Object _value, StringBuilder _sb) {
    if (_value instanceof String) {
      _sb.append(_value);
      return;
    }
      
    if (_value instanceof Map) {
      Map map = (Map)_value;
      if (_key.equals("margin") || _key.equals("border") ||
          _key.equals("padding")) {
        /* margin: 0 0 0 0; */
        Object t = map.get("top");
        Object r = map.get("right");
        Object b = map.get("bottom");
        Object l = map.get("left");
        if (t == null) t = "0";
        if (r == null) r = "0";
        if (b == null) b = "0";
        if (l == null) l = "0";
        _sb.append(t);
        _sb.append(' ');
        _sb.append(r);
        _sb.append(' ');
        _sb.append(b);
        _sb.append(' ');
        _sb.append(l);
      }
      else {
        log().warn("not sure what to do with Map value for style '" + _key +
            "': " + _value);
      }
    }
    
    if (_value instanceof Collection) {
      boolean isColFirst = true;
      for (Object o: (Collection)_value) {
        if (isColFirst) isColFirst = false;
        else _sb.append(", ");
        
        _sb.append(o);
      }
      return;
    }
    
    if (_value instanceof Boolean) {
      if (_key.equals("visibility"))
        _sb.append(((Boolean)_value).booleanValue() ? "visible" : "hidden");
      else if (_key.equals("display"))
        _sb.append(((Boolean)_value).booleanValue() ? "block" : "none");
      else if (_key.equals("clear"))
        _sb.append(((Boolean)_value).booleanValue() ? "both" : "none");
      else
        _sb.append(_value);
      return;
    }
    
    if (_value instanceof Number) {
      _sb.append(_value);
      return;
    }
    
    log().warn("not sure what to do with value for style '" + _key +
        "': " + _value);
    _sb.append(_value);
    return;
  }
  

  /* generate response */
  
  @SuppressWarnings("unchecked")
  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    if (_ctx.isRenderingDisabled())
      return;
    
    // TBD: allow maps and lists as values!!! (style={display:a,urks:b)
    Object cursor = _ctx.cursor();
    
    String styleString = null;
    String classString = null;
    
    
    /* calculate style */
    
    Object baseStyle = null;
    Object extStyle  = null;
    
    if (this.style != null)
      baseStyle = this.style.valueInComponent(cursor);
    if (this.dynStyles != null)
      extStyle = this.extractExtStyles(cursor);
    
    if (extStyle != null && baseStyle == null) {
      baseStyle = extStyle;
      extStyle  = null;
    }
    
    if (extStyle != null) {
      /* complex, merge with basestyle */
      Map baseMap = this.mapForStyles(baseStyle);
      Map extMap  = this.mapForStyles(extStyle);
      if (extMap != null) baseMap.putAll(extMap);
      baseStyle = baseMap;
    }
    
    if (baseStyle instanceof String) {
      /* simple case, just the base style */
      styleString = (String)baseStyle;
    }
    else if (baseStyle instanceof Map) {
      // TBD: style values can be lists! (eg: serif, sans-serif)
      StringBuilder sb = new StringBuilder(256);
      Map<String, Object> map = (Map<String, Object>)baseStyle;
      boolean isFirst = true;
      for (String key: map.keySet()) {
        Object v = map.get(key);
        if (v == null) // TBD: might imply a meaning for some style names?
          continue;
        
        if (isFirst) isFirst = false;
        else sb.append(' ');
        
        sb.append(key);
        sb.append(": ");
        this.appendStyleValue(key, v, sb);
        sb.append(";");
      }
      
      if (sb.length() > 0)
        styleString = sb.toString();
    }
    else if (baseStyle != null)
      log().error("cannot render 'style' attribute value: " + baseStyle);
    
    
    /* calculate class */
    
    Object baseClass = null;
    Object extClass  = null;
    
    if (this.clazz != null)
      baseClass = this.clazz.valueInComponent(cursor);
    if (this.dynClasses != null)
      extClass = this.extractExtClasses(cursor);
    
    if (extClass != null && baseClass == null) {
      baseClass = extClass;
      extClass = null;
    }
    
    if (extClass != null) {
      /* merge classes */
      Collection<String> baseSet = this.listForClasses(baseClass);
      Collection<String> extSet  = this.listForClasses(extClass);
      if (extSet != null) {
        for (String s: extSet) {
          if (!baseSet.contains(s))
            baseSet.add(s);
        }
      }
      baseClass = baseSet;
    }
    
    if (baseClass instanceof String)
      classString = (String)baseClass;
    else if (baseClass instanceof Collection)
      classString = UString.componentsJoinedByString((Collection)baseClass," ");
    else if (baseClass != null)
      log().error("cannot render 'class' attribute value: " + baseClass);
    
    
    /* generate content to WOResponse*/
    
    if (classString != null && classString.length() > 0)
      _r.appendAttribute("class", classString);
    if (styleString != null && styleString.length() > 0)
      _r.appendAttribute("style", styleString);
  }
  

  /* description */

  @Override
  public void appendAssocToDescription
    (final StringBuilder _d, final String _name, final WOAssociation _a)
  {
    super.appendAssocToDescription(_d, _name, _a);
    
    if (this.style != null)
      this.appendAssocToDescription(_d, "style", this.style);
    if (this.clazz != null)
      this.appendAssocToDescription(_d, "class", this.clazz);
  }
}
