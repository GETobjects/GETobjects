/*
  Copyright (C) 2006 Helge Hess

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.elements.WOString;
import org.getobjects.foundation.NSJavaRuntime;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.XMLNS;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/*
  Abstract class to build JOPE WOElement templates from
  XML DOM structures.
  
  Template builders can be stacked in a processing queue, so that
  unknown elements are produced by the "nextBuilder". When processing
  is done using a stack, the first stack builder is called the
  "templateBuilder" and used to keep global state (eg the template
  builder must be used to create further elements if the same element
  set is required).
  
  WOxElemBuilder stacks are not thread-safe since the template builder
  stores subcomponent creation info in an instance variable.
*/

public abstract class WOxElemBuilder extends NSObject {
  
  protected final Log log = LogFactory.getLog("WOTemplates");
  protected WOxElemBuilder nextBuilder = null;
 
  protected static Map<String, Class> nsToAssocClass =
    new HashMap<String, Class>(16);
  
  /* accessors */
  
  public void setNextBuilder(WOxElemBuilder _builder) {
    this.nextBuilder = _builder;
  }
  public WOxElemBuilder nextBuilder() {
    return this.nextBuilder;
  }
  
  /* top-level method */

  public WOElement buildTemplateFromDocument(Document _doc) {
    return this.buildNode(_doc, this);
  }
  
  /* node-type build dispatcher method ... */
  
  public WOElement buildNode(Node _node, WOxElemBuilder _builder) {
    if (_node == null)
      return null;
    
    switch (_node.getNodeType()) {
      case Node.ELEMENT_NODE:
        return this.buildElement((Element)_node, _builder);
      case Node.TEXT_NODE:
        return this.buildText((Text)_node, _builder);
      case Node.CDATA_SECTION_NODE:
        return this.buildCDATASection((CDATASection)_node, _builder);
      case Node.COMMENT_NODE:
        return this.buildComment((Comment)_node, _builder);
      case Node.DOCUMENT_NODE:
        return this.buildDocument((Document)_node, _builder);
      
      default: {
        if (this.nextBuilder != null)
          return this.nextBuilder.buildNode(_node, _builder);
        
        this.log.error("unsupported node type: " + _node);
      }
    }
    return null;
  }
  
  public List<WOElement> buildNodes(NodeList _list, WOxElemBuilder _builder) {
    if (_list == null)
      return null; // TODO: should we return an empty list as per style?
    
    int             len   = _list.getLength();
    List<WOElement> elems = new ArrayList<WOElement>(len);
    
    for (int i = 0; i < len; i++) {
      Node      node = _list.item(i);
      WOElement elem = this.buildNode(node, _builder);
      
      if (elem != null)
        elems.add(elem);
    }
    
    return elems;
  }

  /* building parts of a DOM ... */

  public WOElement buildDocument(Document _doc, WOxElemBuilder _builder) {
    if (_doc == null)
      return null;
    
    return this.buildElement(_doc.getDocumentElement(), _builder);   
  }

  public WOElement buildElement(Element _node, WOxElemBuilder _b) {
    if (_node == null)
      return null;
    
    if (this.nextBuilder != null)
      this.nextBuilder.buildElement(_node, _b);
    
    this.log.error("failed to build WOx node element: " + _node);
    return null;
  }

  public WOElement buildCharacterData(CharacterData _node, WOxElemBuilder _b) {
    if (_node == null)
      return null;
    
    String s = _node.getData();
    if (s       == null) return null;
    if (s.length() == 0) return null;
    
    return new WOString(WOAssociation.associationWithValue(s),
                        true /* escapeHTML */);
  }

  public WOElement buildText(Text _node, WOxElemBuilder _b) {
    return this.buildCharacterData(_node, _b);   
  }

  public WOElement buildCDATASection(CDATASection _node, WOxElemBuilder _b) {
    return this.buildCharacterData(_node, _b);   
  }

  public WOElement buildComment(Comment _node, WOxElemBuilder _b) {
    return null; /* we do not deliver comments */   
  }
  
  /* associations */
  
  public WOAssociation associationForAttribute(Attr _attr) {
    if (_attr == null)
      return null;
    
    /* get namespace */
    
    String ns = _attr.getNamespaceURI();
    if (ns == null) ns = ""; /* this will get the default */
    
    /* map namespace to Class */
    
    Class assocClass = nsToAssocClass.get(ns);
    if (assocClass == null)
      assocClass = nsToAssocClass.get("");
    if (assocClass == null) {
      this.log.error("could not find association class: " + _attr);
      return null;
    }
    
    /* construct */
    
    WOAssociation assoc = (WOAssociation)
      NSJavaRuntime.NSAllocateObject(assocClass, String.class,_attr.getValue());
    
    return assoc;
  }
  
  public Map<String,WOAssociation> associationsForAttributes(NamedNodeMap _m) {    
    if (_m == null)
      return null;
    
    int len = _m.getLength();
    Map<String,WOAssociation> assocs = new HashMap<String, WOAssociation>(len);
    
    for (int i = 0; i < len; i++) {
      WOAssociation assoc;
      Attr attr;
      
      attr = (Attr)(_m.item(i));
      if ((assoc = this.associationForAttribute(attr)) != null) {
        String n;
        
        if ((n = attr.getLocalName()) == null)
          n = attr.getName();
        assocs.put(n, assoc);
      }
    }
    
    return assocs;
  }
  
  public WOAssociation associationForValue(Object _value) {
    return WOAssociation.associationWithValue(_value);
  }
  public WOAssociation associationForKeyPath(String _keyPath) {
    return WOAssociation.associationWithKeyPath(_keyPath);
  }
  
  /* static initializer */
  {
    // TODO: better move to a config file
    nsToAssocClass.put("",
      org.getobjects.appserver.associations.WOValueAssociation.class);
    nsToAssocClass.put(XMLNS.OD_BIND,
      org.getobjects.appserver.associations.WOKeyPathAssociation.class);
    nsToAssocClass.put(XMLNS.OD_CONST,
      org.getobjects.appserver.associations.WOValueAssociation.class);
    nsToAssocClass.put("OGo:bind",
      org.getobjects.appserver.associations.WOKeyPathAssociation.class);
    nsToAssocClass.put("OGo:value",
      org.getobjects.appserver.associations.WOValueAssociation.class);
    /*
    nsToAssocClass.put(XMLNamespaces.XMLNS_OD_EVALJS,
      org.opengroupware.jope.appserver.WOScriptAssociation.class);
    nsToAssocClass.put("http://www.skyrix.com/od/so-lookup",
      org.opengroupware.jope.appserver.SoLookupAssociation.class);
    nsToAssocClass.put("OGo:script",
      org.opengroupware.jope.appserver.WOScriptAssociation.class);
    nsToAssocClass.put("OGo:url",
      org.opengroupware.jope.appserver.WOResourceURLAssociation.class);
    nsToAssocClass.put("OGo:label",
      org.opengroupware.jope.appserver.WOLabelAssociation.class);
    nsToAssocClass.put("OGo:path",
      org.opengroupware.jope.appserver.SoLookupAssociation.class);
      */
  }
}
