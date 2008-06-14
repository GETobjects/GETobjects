/*
  Copyright (C) 2006-2007 Helge Hess

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

package org.getobjects.rules;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.eocontrol.EOAndQualifier;
import org.getobjects.eocontrol.EOBooleanQualifier;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.foundation.NSClassLookupContext;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * RuleModelLoader
 * <p>
 * Loads a RuleModel from an XML file.
 * 
 * Sample:
 * <pre><code>
 *   &lt;?xml version="1.0"?&gt;
 *   &lt;model version="1.0"&gt;
 *   
 *     &lt;rule priority="high"&gt;
 *       &lt;qualifier&gt;*true*&lt;/qualifier&gt;
 *       &lt;key&gt;color&lt;/key&gt;
 *       &lt;value&gt;'green'&lt;/value&gt;
 *     &lt;/rule&gt;
 *     
 *     &lt;rule priority="high">
 *       &lt;qualifier&gt;*true*&lt;/qualifier>
 *       &lt;key&gt;color&lt;/key>
 *       &lt;var:value&gt;backgroundColor&lt;/var:value&gt;
 *     &lt;/rule&gt;
 *     
 *     &lt;rule priority="low">
 *       &lt;qualifier&gt;*true*&lt;/qualifier>
 *       &lt;action&gt;color = backgroundColor&lt;/action&gt;
 *     &lt;/rule&gt;
 *     
 *     &lt;rule&gt;*true* => color = 'green'; high&lt;/rule&gt;
 *     
 *   &lt;/model&gt;
 * </code></pre>
 *   
 * Tag Aliases:
 * <pre>
 *   qualifier - q
 *   action    - a
 *   priority  - p
 * </pre>
 * 
 * TBD: multirule
 * 
 * @see RuleModel
 */
public class RuleModelLoader extends NSObject {
  protected static final Log log = LogFactory.getLog("JoRuleModelLoader");

  /* statics */

  protected static DocumentBuilderFactory dbf;
  static {
    dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(false); /* we directly deal with prefixes */
    dbf.setCoalescing(true); /* join adjacent texts */
    dbf.setIgnoringComments(true);
  }
  
  /* ivars */
  
  protected NSClassLookupContext classLookup;
  protected RuleParser           ruleParser;
  protected Exception            lastException;
  
  public RuleModelLoader(NSClassLookupContext _clslookup) {
    this.classLookup = _clslookup;
    this.ruleParser  = new RuleParser(_clslookup);
  }
  public RuleModelLoader() {
    this(NSClassLookupContext.NSSystemClassLookupContext);
  }
  
  /* accessors */
  
  public Exception lastException() {
    return this.lastException;
  }
  
  public void clear() {
    this.lastException = null;
  }

  /* loading the model */
  
  protected RuleModel loadModelFromElement(Element _node) {
    NodeList   children  = _node.getElementsByTagName("rule");
    int        ruleCount = children != null ? children.getLength() : 0;
    List<Rule> rules     = new ArrayList<Rule>(ruleCount);

    /* load regular rules */
    
    for (int i = 0; i < ruleCount; i++) {
      Rule rule = this.loadRuleFromElement((Element)children.item(i));
      if (rule == null) {
        log.error("got no rule for element: " + children.item(i));
        continue;
      }
      
      rules.add(rule);
    }
    
    /* load multi rules */

    if ((children  = _node.getElementsByTagName("multirule")) != null) {
      ruleCount = children.getLength();
      
      for (int i = 0; i < ruleCount; i++) {
        Rule[] mrules = 
          this.loadMultiRulesFromElement((Element)children.item(i));
        
        if (mrules == null) continue;
        
        for (int j = 0; j < mrules.length; j++)
          rules.add(mrules[j]);
      }
    }
    
    return new RuleModel(rules.toArray(new Rule[0]));
  }
  
  protected RuleModel loadModelFromDocument(Document _doc) {
    return this.loadModelFromElement(_doc.getDocumentElement());
  }
  
  public Rule loadRuleFromElement(Element _node) {
    if (_node == null) return null;
    
    boolean debugOn = log.isDebugEnabled();
    
    /* try to load structured rule setup (tags for rule definition) */
    
    EOQualifier sq = this.loadQualifiersOfRule(_node);
    RuleAction  sa = this.loadActionOfRule(_node);
    Integer     sp = this.loadPriorityOfRule(_node);
    String      sk = null;
    if (sa == null) {
      sk = this.joinTrimmedTextsOfElements
        (_node.getElementsByTagName("key"),
         "." /* build keypath (useless) */);
    }
    
    /* check whether its structured */
    
    if (sq == null && sa == null && sk == null) {
      /* not structured */
      String s = _node.getTextContent();
      if (s == null || s.length() == 0) {
        log.error("found rule tag w/o recognisable content");
        return null;
      }
      
      if (sp != null) s = s + "; " + sp; // TODO: check for dups
      Rule rule = this.ruleParser.parseRule(s);
      if (rule == null)
        log.error("could not parse rule: '" + s + "'");
      return rule;
    }
    
    /* treat as a structured rule */
    
    if (debugOn)
      log.debug("load structured rule, sk: " + sk + ", sa: " + sa);
    
    if (sk != null && sa != null)
      log.warn("rule has both, 'key' and 'action' tags. Using 'action'.");
    else if (sk != null) {
      sa = this.loadSplitRuleAction(_node, sk);
      if (debugOn) log.debug("  parsed split rule: " + sa);
    }
    
    if (sq == null) /* no qualifier given, using *true* */
      sq = EOBooleanQualifier.trueQualifier;
    
    if (sp == null) /* no priority given, use normal */
      sp = new Integer(RuleParser.RULE_PRIORITY_NORMAL);
    
    return new Rule(sq, sa, sp.intValue());
  }
  
  public Rule[] loadMultiRulesFromElement(Element _node) {
    if (_node == null) return null;
    
    /* try to load structured rule setup (tags for rule definition) */
    
    RuleAction  sa = this.loadActionOfRule(_node);
    Integer     sp = this.loadPriorityOfRule(_node);
    String      sk = null;
    if (sa == null) {
      sk = this.joinTrimmedTextsOfElements
        (_node.getElementsByTagName("key"),
         "." /* build keypath (useless) */);
    }
    
    /* treat as a structured rule */
    
    if (sk != null && sa != null)
      log.warn("multirule has both, 'key' and 'action' tags. Using 'action'.");
    else if (sk != null)
      sa = this.loadSplitRuleAction(_node, sk);
    
    if (sp == null) /* no priority given, use normal */
      sp = new Integer(RuleParser.RULE_PRIORITY_NORMAL);
    
    /* walk over qualifiers, generate a rule for each */

    NodeList children  = _node.getElementsByTagName("qualifier");
    if (children == null) children  = _node.getElementsByTagName("q");
    
    if (children == null || children.getLength() == 0) {
      log.warn("multirule tag has no qualifiers?");
      return null;
    }

    Rule[] qs = new Rule[children.getLength()];
    for (int i = 0; i < qs.length; i++) {
      EOQualifier q = EOQualifier.qualifierWithQualifierFormat
        (children.item(i).getTextContent(), noArgs);
      
      qs[i] = new Rule(q, sa, sp.intValue());
    }
    return qs;
  }
  
  protected static Object[] noArgs = { };
  
  public EOQualifier loadQualifiersOfRule(Element _node) {
    if (_node == null) return null;
    
    NodeList children  = _node.getElementsByTagName("qualifier");
    if (children == null) children  = _node.getElementsByTagName("q");
    
    if (children == null || children.getLength() == 0)
      return null; /* has no qualifier subelements */
    
    EOQualifier[] qs = new EOQualifier[children.getLength()];
    for (int i = 0; i < qs.length; i++) {
      qs[i] = EOQualifier.qualifierWithQualifierFormat
        (children.item(i).getTextContent(), noArgs);
    }
    
    if (qs.length == 1)
      return qs[0];
    
    return new EOAndQualifier(qs);
  }
  
  public RuleAction loadActionOfRule(Element _node) {
    if (_node == null) return null;
    
    NodeList children = _node.getElementsByTagName("action");
    if (children == null) children  = _node.getElementsByTagName("a");

    if (children == null || children.getLength() == 0)
      return null; /* has no action subelements */
    
    RuleAction[] as = new RuleAction[children.getLength()];
    for (int i = 0; i < as.length; i++) {
      Element child = (Element)children.item(i);
      
      String actionClassName = child.getAttribute("class");
      as[i] = (RuleAction)this.ruleParser.parseAction
        (child.getTextContent(), actionClassName);
    }
    
    return CompoundRuleAction.ruleActionForActionArray(as);
  }
  
  public RuleAction loadSplitRuleAction(Element _node, String _keyPath) {
    if (_node == null) return null;
    
    /* first check for constant values */
    
    NodeList children = _node.getElementsByTagName("value");
    if (children == null || children.getLength() == 0)
      children = _node.getElementsByTagName("v");
    
    if (children != null && children.getLength() > 0) {
      String v = this.joinTrimmedTextsOfElements(children, "");
      // System.err.println("X: " + v + ": " + children);
      return new RuleAssignment(_keyPath, v);
    }
    
    /* check for variable values */

    children = _node.getElementsByTagName("var:value");
    if (children == null || children.getLength() == 0)
      children = _node.getElementsByTagName("var:v");
    
    if (children != null && children.getLength() > 0) {
      String v = this.joinTrimmedTextsOfElements(children, "." /* keypath */);
      return new RuleKeyAssignment(_keyPath, v);
    }
    
    /* didn't find a value */
    log.warn("did not find value tag in rule for keypath: " + _keyPath);
    return null;
  }
  
  public Integer loadPriorityOfRule(Element _node) {
    if (_node == null) return null;
    
    /* check for attribute */
    
    String s = _node.getAttribute("priority");
    if (s != null && s.length() > 0)
      return priorityForString(s);
    
    /* check for subtag */
    
    NodeList children  = _node.getElementsByTagName("priority");
    if (children == null) children  = _node.getElementsByTagName("p");
    
    if (children == null || children.getLength() == 0)
      return null; /* has no action subelements */
    
    if (children.getLength() > 1)
      log.error("multiple priorities given for rule!");
    
    return priorityForString(children.item(0).getTextContent());
  }
  
  public static Integer priorityForString(String _s) {
    if (_s == null)
      return null;
    
    if (Character.isDigit(_s.charAt(0)))
      return new Integer(UObject.intValue(_s));
    
    return RuleParser.parsePriority(_s);
  }
  
  
  /* support */
  
  protected Exception newModelLoadingException(String _reason) {
    // TODO: improve error handling
    return new Exception(_reason);
  }
  
  protected void addError(String _reason) {
    log.error(_reason);
    this.lastException = this.newModelLoadingException(_reason);
  }
  protected void addError(String _reason, Exception _e) {
    log.error(_reason, _e);
    
    // TODO: wrap exception
    this.lastException = _e;
  }
  
  public RuleModel loadModelFromURL(URL _url) {
    boolean isDebugOn = log.isDebugEnabled();
    if (isDebugOn) log.debug("loading model from URL: " + _url);
    
    if (_url == null) {
      this.addError("missing URL parameter for loading model");
      return null;
    }
    
    /* instantiate document builder */
    
    DocumentBuilder db;
    try {
       db = dbf.newDocumentBuilder();
       if (isDebugOn) log.debug("  using DOM document builder:" + db);
    }
    catch (ParserConfigurationException e) {
      this.addError("failed to create docbuilder for parsing URL: " + _url, e);
      return null;
    }
    
    /* load DOM */
    
    Document doc;
    try {
      doc = db.parse(_url.openStream(), _url.toString());
      if (isDebugOn) log.debug("  parsed DOM: " + doc);
    }
    catch (SAXException e) {
      this.addError("XML error when loading model resource: " + _url, e);
      return null;
    }
    catch (IOException e) {
      this.addError("IO error when loading model resource: " + _url, e);
      return null;
    }
    
    /* transform DOM into model */

    RuleModel model = this.loadModelFromDocument(doc);
    
    if (isDebugOn && model != null) {
      log.debug("  model: " + model);
      log.debug("finished model from URL: " + _url);
    }
    if (model == null)
      log.info("failed loading model from URL: " + _url);
    
    return model;
  }

  protected String joinTrimmedTextsOfElements(NodeList _nodes, String _sep) {
    if (_nodes == null || _nodes.getLength() == 0)
      return null;
    
    StringBuilder sb = new StringBuilder(256);
    boolean isFirst = true;
    for (int i = 0; i < _nodes.getLength(); i++) {
      Element node = (Element)_nodes.item(i);
      node.normalize();
      
      String txt = node.getTextContent();
      if (txt == null) continue;
      txt = txt.trim();
      if (txt.length() == 0) continue;
      
      if (isFirst)
        isFirst = false;
      else if (_sep != null)
        sb.append(_sep);
      
      sb.append(txt);
    }
    return sb.length() > 0 ? sb.toString() : null;
  }
}
