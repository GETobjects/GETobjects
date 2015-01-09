package org.getobjects.samples.testdav.controllers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.getobjects.foundation.XMLNS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DAVParsers {
  private DAVParsers() { }

  public static Set<String> extractPropertyNameList(final Element e) {    
    final NodeList propL = e.getElementsByTagNameNS(XMLNS.WEBDAV, "prop");
    if (propL == null || propL.getLength() < 1)
      return null;
    
    Set<String> propNames = new HashSet<String>(16);
    StringBuilder sb = new StringBuilder(32);
    
    // technically only one is allowed
    for (int i = 0, cnt = propL.getLength(); i < cnt; i++) {
      Element  prop         = (Element)propL.item(i);
      NodeList propChildren = prop.getChildNodes();
      
      for (int j = 0, jcnt = propChildren.getLength(); j < jcnt; j++) {
        Node n = propChildren.item(j);
        if (n == null || !(n instanceof Element))
          continue;
        Element pe = (Element)n;
        
        sb.append("{");
        sb.append(pe.getNamespaceURI());
        sb.append("}");
        sb.append(pe.getLocalName());
        propNames.add(sb.toString());
        sb.setLength(0);
      }
    }
    
    return propNames;
  }
  
  public static Set<String> extractPropfindProperties(Document d) {
    final Element root = d != null ? d.getDocumentElement() : null;
    if (root == null)
      return null;    
    
    String t  = root.getLocalName();
    String ns = root.getNamespaceURI();
    if (!"propfind".equals(t) || !XMLNS.WEBDAV.equals(ns))
      return null;
    
    return extractPropertyNameList(root);
  }
  
  public static Map<String, Object> extractPropPatchProperties(Document d) {
    /* Sample:
     * <A:propertyupdate xmlns:A="DAV:" xmlns:D="http://apple.com/ns/ical/">
     * <A:set><A:prop>
     * <D:calendar-order >0</D:calendar-order>
     * </A:prop></A:set></A:propertyupdate>
     */
    String t, ns;
    
    if (d == null)
      return null;
    
    final Element root = d.getDocumentElement();
    if (root == null)
      return null;    
    
    t  = root.getLocalName();
    ns = root.getNamespaceURI();
    if (!"propertyupdate".equals(t) || !XMLNS.WEBDAV.equals(ns))
      return null;
    
    final NodeList propL = root.getElementsByTagNameNS(XMLNS.WEBDAV, "prop");
    if (propL == null || propL.getLength() < 1)
      return null;
    
    Map<String, Object> propValues = new HashMap<String, Object>(16);
    StringBuilder sb = new StringBuilder(32);
    
    // technically only one is allowed
    for (int i = 0, cnt = propL.getLength(); i < cnt; i++) {
      Element  prop         = (Element)propL.item(i);
      NodeList propChildren = prop.getChildNodes();
      
      for (int j = 0, jcnt = propChildren.getLength(); j < jcnt; j++) {
        Node n = propChildren.item(j);
        if (n == null || !(n instanceof Element))
          continue;
        Element pe = (Element)n;
        
        // FIXME: could be structured!
        // Also: as a special feature, calendar-color can have an attribute as
        //       part of the property
        Object value = pe.getTextContent();
        
        sb.append("{");
        sb.append(pe.getNamespaceURI());
        sb.append("}");
        sb.append(pe.getLocalName());
        propValues.put(sb.toString(), value);
        sb.setLength(0);
      }
    }
    
    return propValues;
  }
}
