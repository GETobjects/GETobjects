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

package org.getobjects.rules;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.eocontrol.EOBooleanQualifier;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.eocontrol.EOQualifierParser;
import org.getobjects.foundation.NSClassLookupContext;
import org.getobjects.foundation.NSJavaRuntime;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.NSPropertyListParser;
import org.getobjects.foundation.UObject;
import org.getobjects.foundation.UString;

// TODO: properly report errors in last-exception!
// TODO: improve performance
// TODO: parse assignment class? (eg "a = b; (BoolAssignment)")

public class RuleParser extends NSObject {
  protected static Log log = LogFactory.getLog("JoRuleParser");
  
  public static int RULE_PRIORITY_IMPORTANT = 1000;
  public static int RULE_PRIORITY_VERYHIGH  = 200;
  public static int RULE_PRIORITY_HIGH      = 150;
  public static int RULE_PRIORITY_NORMAL    = 100;
  public static int RULE_PRIORITY_LOW       = 50;
  public static int RULE_PRIORITY_VERYLOW   = 5;
  public static int RULE_PRIORITY_FALLBACK  = 0;
  
  protected NSClassLookupContext classLookup;

  public RuleParser(NSClassLookupContext _clslookup) {
    this.classLookup = _clslookup;
  }
  public RuleParser() {
    this(NSClassLookupContext.NSSystemClassLookupContext);
  }

  /* parsing */
  
  public Rule parseRule(String _s) {
    if (_s == null || _s.length() == 0) return null;
    
    int idx = UString.indexOfStringBySkippingQuotes(_s, "=>", "\"'", '\\');
    if (idx < 3) {
      log.info("could not parse rule: '" + _s + "'");
      return null;
    }
    
    /* parse qualifier */
    
    EOQualifier q = this.parseQualifier(_s.substring(0, idx).trim());
    if (q == null) {
      log.info("could not parse qualifier of rule: '" + _s + "'");
      return null;
    }
    
    /* scan for priority separator */
    
    String remainder = _s.substring(idx + 2);
    idx = UString.indexOfStringBySkippingQuotes(remainder, ";", "\"", '\\');
    
    /* parse priority */
    
    int priority = RULE_PRIORITY_NORMAL;
    if (idx != -1)
      priority = parsePriority(remainder.substring(idx + 1).trim());
    
    /* parse action / assignment */
    
    Object action = this.parseAction(remainder.substring(0, idx), null /*cls*/);
    if (action == null) {
      log.info("could not parse action of rule: '" + _s + "'");
      return null;
    }
    
    /* return rule */
    return new Rule(q, action, priority);
  }
  
  public EOQualifier parseQualifier(String _s) {
    if (_s == null || _s.length() == 0) return null;
    
    if ("*true*".equals(_s))
      return EOBooleanQualifier.trueQualifier;
    if ("*false*".equals(_s))
      return EOBooleanQualifier.falseQualifier;
    
    EOQualifierParser p =
      new EOQualifierParser(_s.toCharArray(), null /* args */);
    
    return p.parseQualifier();
  }
  
  public static int parsePriority(String _s) {
    if (_s == null || _s.length() == 0)
      return RULE_PRIORITY_NORMAL;
    
    char c0 = _s.charAt(0);
    if (Character.isDigit(c0) || c0 == '-')
      return UObject.intValue(_s);
    
    if ("important".equals(_s)) return RULE_PRIORITY_IMPORTANT;
    if ("very high".equals(_s)) return RULE_PRIORITY_VERYHIGH;
    if ("high".equals(_s))      return RULE_PRIORITY_HIGH;
    if ("normal".equals(_s))    return RULE_PRIORITY_NORMAL;
    if ("default".equals(_s))   return RULE_PRIORITY_NORMAL;
    if ("low".equals(_s))       return RULE_PRIORITY_LOW;
    if ("very low".equals(_s))  return RULE_PRIORITY_VERYLOW;
    if ("fallback".equals(_s))  return RULE_PRIORITY_FALLBACK;
    
    log.warn("unknown rule priority: '" + _s + "'");
    return RULE_PRIORITY_NORMAL;
  }
  
  protected static final Class[] AssiCtorSig = { String.class, Object.class };
  
  public Object parseAction(String _s, String _forceClassName) {
    if (_s == null || _s.length() == 0)
      return null;
    
    Class enforcedClass = null;
    if (_s.charAt(0) == '(') {
      // TODO: parse an Assignment type cast
      int sidx = _s.indexOf(')');
      if (sidx == -1)
        log.error("typecast is not closed: " + _s);
      else {
        String clsname = _s.substring(1, sidx);
        enforcedClass = this.classLookup.lookupClass(clsname);
      }
    }
    else if (_forceClassName != null)
      enforcedClass = this.classLookup.lookupClass(_forceClassName);
    
    int idx = UString.indexOfStringBySkippingQuotes(_s, "=", "\"'", '\\');
    if (idx < 1) {
      log.info("could not parse assignment: '" + _s + "'");
      return null;
    }
    
    String key    = _s.substring(0, idx).trim();
    String valstr = _s.substring(idx + 1).trim();
    
    Class assignmentClass = 
      enforcedClass != null ? enforcedClass : RuleKeyAssignment.class;
    Object value = valstr;
    
    /* parse value */
    
    if (enforcedClass == null && valstr.length() > 0) {
      char c0 = valstr.charAt(0);
      
      if (c0 == '"' || c0 == '\'') { /* quoted string */
        if (valstr.charAt(valstr.length() - 1) != c0) {
          log.error("string value of assignment misses a closing quote");
          value = valstr.substring(1);
        }
        else
          value = valstr.subSequence(1, valstr.length() - 1);
      }
      else if (Character.isDigit(c0) || c0 == '-') { /* number */
        value = UObject.intValue(valstr);
      }
      else if (c0 == '{' || c0 == '(') { /* property list */
        NSPropertyListParser p = new NSPropertyListParser();
        value = p.parse(valstr);
        if (value == null)
          log.error("could not parse plist of assignment: " + valstr);
      }
      else {
        if      (valstr.equals("true"))  value = Boolean.TRUE;
        else if (valstr.equals("false")) value = Boolean.FALSE;
        else if (valstr.equals("YES"))   value = Boolean.TRUE;
        else if (valstr.equals("NO"))    value = Boolean.FALSE;
        else if (valstr.equals("null"))  value = null;
        else if (valstr.equals("nil"))   value = null;
      }
      
      if (value != valstr)
        assignmentClass = RuleAssignment.class;
    }
    
    /* construct */
    
    return NSJavaRuntime.NSAllocateObject
      (assignmentClass, AssiCtorSig, new Object[] { key, value } );
  }
}
