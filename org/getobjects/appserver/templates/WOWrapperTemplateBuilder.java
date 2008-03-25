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

package org.getobjects.appserver.templates;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOResourceManager;
import org.getobjects.appserver.elements.WOCompoundElement;
import org.getobjects.appserver.elements.WOConditional;
import org.getobjects.appserver.elements.WOGenericContainer;
import org.getobjects.appserver.elements.WOGenericElement;
import org.getobjects.appserver.elements.WOHTMLDynamicElement;
import org.getobjects.appserver.elements.WOHyperlink;
import org.getobjects.appserver.elements.WORepetition;
import org.getobjects.appserver.elements.WOStaticHTMLElement;
import org.getobjects.foundation.NSJavaRuntime;
import org.getobjects.foundation.NSPropertyListParser;
import org.getobjects.foundation.UString;

/**
 * WOWrapperTemplateBuilder
 * <p>
 * This class implements a parser for so called 'wrapper templates', which
 * in turn are WebObjects style templates composed of an HTML template plus
 * a .wod file.
 *
 * <h4>Supported binding prefixes</h4>
 * <pre>
 *   const  - WOValueAssociation
 *   jo     - JoPathAssociation
 *   label  - WOLabelAssociation
 *   ognl   - WOOgnlAssociation
 *   plist  - parse value as plist, then create a WOValueAssociation
 *   q      - WOQualifierAssociation (evaluate the given qualifier)
 *   regex  - WORegExAssociation
 *   rsrc   - WOResourceURLAssociation (lookup URL for a given resource name)
 *   var    - WOKeyPathAssociation
 *   varpat - WOKeyPathPatternAssociation</pre>
 *
 * <h4>Shortcuts</h4>
 * <pre>
 *   &lt;#a&gt;         - WOHyperlink
 *   &lt;#for&gt;       - WORepetition
 *   &lt;#get&gt;       - WOString
 *   &lt;#if&gt;        - WOConditional
 *   &lt;#put&gt;       - WOCopyValue
 *   &lt;#submit&gt;    - WOSubmitButton
 *   &lt;#table-for&gt; - WOTableRepetition</pre>
 * 
 * <h4>Element Wrapper Attributes</h4>
 * <pre>
 *   &lt#tag ... if="condition"&gt;    - WOConditional
 *   &lt#tag ... ifnot="condition"&gt; - WOConditional
 *   &lt#tag ... foreach="list"&gt;    - WORepetition
 * </pre>
 *
 * <h4>WOHTMLParser vs WOWrapperTemplateBuilder</h4>
 * <p>
 * The WOHTMLParser just parses the HTML and asks the WOWrapperTemplateBuilder
 * to build the actual WOElements.
 *
 * <p>
 * THREAD: this class is not threadsafe and uses ivars for temporary storage.
 * <pre>
 * TODO: WOWrapperTemplateBuilder is not a good name for this class because it
 *       has nothing to do with wrappers. The actual resolution of the wrapper
 *       is done in the WOResourceManager / WOComponentDefinition.
 *       This class receives the already-looked-up URLs for the .wod and .html
 *       templates.
 *       Possibly we could also move this class into the WOTemplateBuilder?
 * </pre>
 */
public class WOWrapperTemplateBuilder extends WOTemplateBuilder
  implements WODParserHandler, WOTemplateParserHandler
{
  protected WOTemplate        iTemplate;
  protected Map               wodEntries;
  protected WOResourceManager resourceManager;
  // Note: we only use lookupDynamicElementClass() of the resource manager
  // TBD: consolidate that in an interface?

  /* builder */

  @Override
  public WOTemplate buildTemplate
    (URL _template, URL _wod, WOResourceManager _resourceManager)
  {
    boolean isDebugOn = log.isDebugEnabled();

    if (isDebugOn) log.debug("parsing wrapper template ...");

    this.resourceManager = _resourceManager;

    /* parse wod file */

    if (_wod != null) {
      WODParser wodParser = new WODParser();
      wodParser.setHandler(this);
      this.wodEntries = (Map)wodParser.parse(_wod);
      if (this.wodEntries == null) {
        Exception e = wodParser.lastException();
        if (e != null)
          log.error("could not parse WOD file: " + e.getMessage(), e);
      }
      wodParser.reset();
      if (isDebugOn) log.debug("  parsed wod: " + this.wodEntries);
    }
    else
      if (isDebugOn) log.debug("  no wod to parse.");

    /* parse HTML file */

    WOTemplateParser htmlParser = this.instantiateTemplateParser(_template);
    htmlParser.setHandler(this);

    this.iTemplate = new WOTemplate(null /* URL */, null /* root */);
    List<WOElement> elements = htmlParser.parseHTMLData(_template);

    /* reset temporary state */

    this.wodEntries  = null;
    this.resourceManager = null;

    /* process results */

    if (elements == null) {
      if (htmlParser.lastException() != null)
        log.error("could not parse HTML file", htmlParser.lastException());
      return null;
    }
    if (elements.size() == 0) {
      /* got no result? */
      log.warn("parsed no element from the HTML file?");
      return null;
    }
    if (isDebugOn) log.debug("  parsed elements: " + elements);

    /* build template */

    if (elements.size() == 1)
      this.iTemplate.setRootElement(elements.get(0));
    else
      this.iTemplate.setRootElement(new WOCompoundElement(elements));

    WOTemplate template = this.iTemplate;
    this.iTemplate = null;
    if (isDebugOn) log.debug("  parsed template: " + template);
    return template;
  }

  public WOTemplateParser instantiateTemplateParser(URL _template) {
    // TODO: rather use the WOParser API discovered in Wonder?
    return new WOHTMLParser();
  }

  /* WOD parser */

  public boolean willParseDeclarationData(WODParser _p, char[] _data) {
    return true;
  }
  public void failedParsingDeclarationData
    (WODParser _p, char[] _data, Exception _error)
  {
  }
  public void finishedParsingDeclarationData
    (WODParser _p, char[] _data, Map _decls)
  {
  }

  public WOAssociation makeAssociationWithKeyPath(WODParser _p, String _kp) {
    return _kp != null ? WOAssociation.associationWithKeyPath(_kp) : null;
  }

  public WOAssociation makeAssociationWithValue(WODParser _p, Object _value) {
    return WOAssociation.associationWithValue(_value);
  }

  /**
   * Called by the WODParser once it has parsed the data of a WOD entry
   * like:<pre>
   *   Frame: MyFrame {
   *     title = "Welcome to Hola";
   *   }</pre>
   * The parser stores the result of this method in a Map under the _cname
   * (<code>Frame</code>). This Map is queried after the .wod has been
   * parsed. The parser does not care about the type of the object being
   * returned, it just stores it.
   * 
   * @param _p       - the parser
   * @param _cname   - the name of the element (<code>Frame</code>)
   * @param _entry   - the Map containing the bindings (String-WOAssociation)
   * @param _clsname - the name of the component (<code>MyFrame</code>)
   * @return a WODFileEntry object representing the WOD entry
   */
  public Object makeDefinitionForComponentNamed
    (WODParser _p, String _compname, Map _entry, String _clsname)
  {
    return new WODFileEntry(_compname, _clsname, _entry);
  }


  /* HTML parser callback */

  /**
   * This method builds a
   *   <code>Map&lt;String, WOAssociation&gt;</code>
   * from a
   *   <code>Map&lt;String, String&gt;</code>.
   * 
   * It scans the key for a colon (<code>:</code>). If it does not find one,
   * it creates a value-association, if not, it calls the
   *   <code>WOAssociation.associationForPrefix()</code>
   * to determine an appropriate WOAssociation for the prefix.
   * <p>
   * Example:<pre>
   *   {
   *     var:list = "persons";
   *     var:item = "person";
   *     count = 5;
   *   }</pre>
   * is mapped to:<pre>
   *   {
   *     list  = [WOKeyPathAssociation keypath="persons"];
   *     item  = [WOKeyPathAssociation keypath="person"];
   *     count = [WOValueAssociation value=5];
   *   }</pre>
   * 
   * We also support Project Wonder style value prefixes (~ and $). Those are
   * only processed if the attribute has NO prefix. Eg if you want to have a
   * regular, constant value, you can use the <code>const:</code> prefix.
   * 
   * <p>
   * @param _attrs - a Map&lt;String,String&gt; as parsed from HTML
   * @return a Map&lt;String, WOAssociation&gt;
   */
  public static Map<String, WOAssociation> buildAssociationsForTagAttributes
    (final Map<String, String> _attrs)
  {
    if (_attrs == null) {
      /* we return an empty hash-map for no attributes, because its valid to
       * have none but the elements always expect a (non-null) association
       * hashmap.
       */
      return new HashMap<String, WOAssociation>(0);
    }

    final Map<String,WOAssociation> assocs =
      new HashMap<String,WOAssociation>(_attrs.size());

    for (String k: _attrs.keySet()) {
      String value = _attrs.get(k);
      WOAssociation assoc;
      int           pm;

      pm = k.indexOf(':');
      if (pm == -1) {
        /* No prefix like 'var:', we still want to support Wonder value
         * prefixes, eg '$' for KVC and '~' for OGNL.
         * 
         * This implies that we need to escape the String value, we'll use
         * backslashes.
         */
        char c0 = value.length() > 0 ? value.charAt(0) : 0;
        String prefix = "const";
        if (c0 == '$') { // can be masked using '\', eg '\$'
          char c1 = value.length() > 1 ? value.charAt(1) : 0;
          if (c1 != '(') {
            /* well, we do not convert '$(', because this is usually some
             * prototype const attribute, eg:
             *    before="$('progress').show()"
             * kinda hackish, but we wanted this AND Wonder style bindings ...
             */
            prefix = "var";
            value  = value.substring(1);
          }
          else if (log.isInfoEnabled())
            log.info("found a binding w/o prefix: " + k + ": " + value);
        }
        else if (c0 == '~') {
          prefix = "ognl";
          value  = value.substring(1);
        }
        value = UString.stringByUnescapingWithEscapeChar(value, '\\');
        
        assoc = WOAssociation.associationForPrefix(prefix, k, value);
      }
      else {
        String prefix = k.substring(0, pm);
        k = k.substring(pm + 1);

        assoc = WOAssociation.associationForPrefix(prefix, k, value);
      }

      if (assoc != null)
        assocs.put(k, assoc);
    }
    return assocs;
  }

  protected static Class[] dynElemCtorSignature = {
    String.class,   /* element name */
    Map.class,      /* associations */
    WOElement.class /* template     */
  };

  /**
   * This method constructs a WODynamicElement for the given name. It will first
   * check for an entry with the name in the wod mapping table and otherwise
   * attempt to lookup the name as a class. If that also fails some fallbacks
   * kick in, that is element name aliases (<#if>) and automatic generic
   * elements (<#li>).
   * <p>
   * If the name represents a component, a WOChildComponentReference object
   * will get constructed (not the component itself, components are allocated
   * on demand).
   */
  @SuppressWarnings("unchecked")
  public WOElement dynamicElementWithName
    (String _name, Map<String, String> _attrs, List<WOElement> _children)
  {
    WODFileEntry entry = null;
    Class     cls      = null;
    WOElement content  = null;
    Map       assocs   = null;

    if (_name == null) {
      log().warn("parsed element has no name, attrs: " + _attrs);
      return new WOStaticHTMLElement("[Unnamed dynelement]");
    }

    if (this.wodEntries != null)
      entry = (WODFileEntry)this.wodEntries.get(_name);
    
    if (entry == null) {
      /*
       * Derive element from tag name, eg:
       *
       *   <wo:WOString var:value="abc" const:escapeHTML="1"/>
       *
       * This will attempt to find the class 'WOString'. If it can't find the
       * class, it checks for aliases and HTML tags (generic elements).
       */
      // TODO: I suppose we could also try WOxElemBuilder's!
      boolean addElementName = false;

      assocs = buildAssociationsForTagAttributes(_attrs);

      if ((cls = this.resourceManager.lookupDynamicElementClass(_name))==null) {
        /* Could not resolve tagname as a WODynamicElement class, check for
         * aliases and dynamic HTML tags, like
         * 
         *   <wo:li var:style="current" var:+style="isCurrent" />
         * 
         * Note: we only check for dynamic element classes! The _name could
         *       still be the name of a WOComponent class!
         */
        String clsName;

        if (_name.equals("a")) {
          if (assocs.containsKey("action") ||
              assocs.containsKey("actionClass") ||
              assocs.containsKey("@action") ||
              assocs.containsKey("pageName") ||
              assocs.containsKey("disabled"))
            cls = WOHyperlink.class;
          else {
            cls = WOGenericContainer.class;
            addElementName = true;
          }
        }
        else if ((clsName = elementNameAliasMap.get(_name)) != null) {
          cls = this.resourceManager.lookupDynamicElementClass(clsName);
          if (cls == null) {
            log().error("could not resolve name alias class: " + _name);
            return new WOStaticHTMLElement("[Missing element: " + _name + "]");
          }

          /* Note: WOGenericContainer inherits from WOGenericElement */
          addElementName = WOGenericElement.class.isAssignableFrom(cls);
        }
        /* ELSE: probably a WOComponent. We do not resolve WOComponent classes
         *       during template parsing (anymore).
        else {
          log().info("did not find element in .wod file: " + _name);
          return new WOStaticHTMLElement("[Missing element in wod: "+_name+"]");
        }
        */
      }

      if (addElementName)
        assocs.put("elementName", WOAssociation.associationWithValue(_name));
    }
    else {
      cls = this.resourceManager.lookupDynamicElementClass
        (entry.componentClassName);
      /* if this returns null, its most likely a WOComponent
      if (cls == null) {
        log().debug("did not find class for element in .wod file: " + _name);
        return new WOStaticHTMLElement
          ("[Missing dynelement: " + _name + " / " +
           entry.componentClassName + "]");
      }
      */

      /* Note: its important that we copy the associations since an element can
       *       be used twice! (and we clear the Map during element init)
       */
      assocs = new HashMap(entry.associations);

      /* merge attributes of the tag (eg <#MyStyle color="red" />) */
      if (_attrs != null && _attrs.size() > 0) {
        Map<String, WOAssociation> tagAttrAssocs =
          buildAssociationsForTagAttributes(_attrs);
        if (tagAttrAssocs != null)
          assocs.putAll(tagAttrAssocs);
      }
    }
    if (assocs != null)
      assocs.remove("NAME");

    
    /* narrow down the children */

    if (_children == null || _children.size() == 0)
      ;
    else if (_children.size() == 1)
      content = _children.get(0);
    else
      content = new WOCompoundElement(_children);

    
    /* create element */

    WOElement element = null;

    if (cls == null /* || WOComponent.class.isAssignableFrom(cls) */) {
      /*
       * Note: we cannot use cls.getName (the fully qualified component name),
       *       this will make the template lookup fail because it won't use
       *       the proper resource manager (the first will succeed because
       *       the name is fully qualified).
       * => and in the new code we don't resolve the WOComponent class
       *    anyways.
       */
      String cname = this.iTemplate.addSubcomponent
        (entry != null ? entry.componentClassName : _name, assocs);
      element = new WOChildComponentReference(cname, content);
    }
    else /* if (WOElement.class.isAssignableFrom(cls)) */ {
      /* all classes we find are dyn elems */
      element = (WOElement)NSJavaRuntime.NSAllocateObject
        (cls, dynElemCtorSignature, new Object[] {
            _name, assocs, content
        });
      
      /* special hack */
      element = this.hackNewElement(element, assocs);

      // TODO: maybe we need to remove 'name' or so
      if (assocs != null && element != null) {
        if (assocs.size() > 0 && element instanceof WODynamicElement)
          ((WODynamicElement)element).setExtraAttributes(assocs);
      }
    }
    /*
    else {
      log.error("non-WOElement in template: " +
          cls.getSimpleName() + " (" + cls + ")");
    }
    */
    return element;
  }
  
  /**
   * Add WOConditional support to all elements, eg:<pre>
   *   &lt;#get var:value="label" if="label.isNotEmpty" /&gt;</pre>
   * 
   * @param _element
   * @param _assocs
   * @return
   */
  @SuppressWarnings("unchecked")
  public WOElement hackNewElement(WOElement _element, Map _assocs) {
    if (_element instanceof WOHTMLDynamicElement) {
      
      /* if attribute, wraps the element in a WOConditional */
      if (_assocs.containsKey("if")) {
        Map assocs = new HashMap(1);
        assocs.put("condition", _assocs.remove("if"));
        _element = new WOConditional("if-attr", assocs, _element);
      }
      
      /* ifnot attribute, wraps the element in a WOConditional */
      if (_assocs.containsKey("ifnot")) {
        Map assocs = new HashMap(2);
        assocs.put("condition", _assocs.remove("ifnot"));
        assocs.put("negate", Boolean.TRUE);
        _element = new WOConditional("ifnot-attr", assocs, _element);
      }
      
      /* foreach attribute, wraps the element in a WORepetition */
      if (_assocs.containsKey("foreach")) {
        Map assocs = new HashMap(2);
        assocs.put("list", _assocs.remove("foreach"));
        assocs.put("item", WOAssociation.associationWithKeyPath("item"));
        _element = new WORepetition("foreach-attr", assocs, _element);
      }
    }
    return _element;
  }

  
  public boolean willParseHTMLData(WOTemplateParser _p, char[] _data) {
    return true;
  }
  public void failedParsingHTMLData
    (WOTemplateParser _p, char[] _data, Exception _error)
  {
  }
  public void finishedParsingHTMLData
    (WOTemplateParser _p, char[] _data, List<WOElement> _topLevel)
  {
  }
  

  /* list of aliases */

  @SuppressWarnings("unchecked")
  private static Map<String, String> elementNameAliasMap =
    (Map<String, String>)NSPropertyListParser.parse(
        WOWrapperTemplateBuilder.class, "WOTagAliases.plist");
}
