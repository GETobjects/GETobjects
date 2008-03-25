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

package org.getobjects.rules.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.getobjects.eocontrol.EOKeyComparisonQualifier;
import org.getobjects.eocontrol.EOKeyValueQualifier;
import org.getobjects.rules.Rule;
import org.getobjects.rules.RuleAssignment;
import org.getobjects.rules.RuleKeyAssignment;
import org.getobjects.rules.RuleParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TRuleParser {
  
  protected RuleParser parser;

  @Before
  public void setUp() {
    this.parser = new RuleParser();
  }

  @After
  public void tearDown() {
    this.parser = null;
  }

  @Test public void testSimpleConstRule() {
    Rule rule = this.parser.parseRule
      ("status = \"edited\" => color = 'green'; high");
    
    assertNotNull("could not parse rule",  rule);
    assertNotNull("rule has no qualifier", rule.qualifier());
    assertNotNull("rule has no action",    rule.action());
    assertEquals("priority mismatch", 150, rule.priority());
    
    assertTrue("not a key/value qualifier",
               rule.qualifier() instanceof EOKeyValueQualifier);
    
    assertEquals("assignment not constant",
                 RuleAssignment.class, rule.action().getClass());
  }

  @Test public void testSimpleKeyPathRule() {
    Rule rule = this.parser.parseRule
      ("status = pageStatus => color = pageColor; low");
    
    assertNotNull("could not parse rule",  rule);
    assertNotNull("rule has no qualifier", rule.qualifier());
    assertNotNull("rule has no action",    rule.action());
    assertEquals("priority mismatch", 50,  rule.priority());
    
    assertTrue("not a key comparison qualifier",
               rule.qualifier() instanceof EOKeyComparisonQualifier);
    
    assertEquals("assignment not a key one",
                 RuleKeyAssignment.class, rule.action().getClass());
  }
}
