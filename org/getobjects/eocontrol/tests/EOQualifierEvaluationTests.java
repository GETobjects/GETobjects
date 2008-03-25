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

package org.getobjects.eocontrol.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.eocontrol.EOQualifierEvaluation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EOQualifierEvaluationTests {
  
  protected Map<String, Object> duckMap;
  protected Map<String, Object> mouseMap;

  @Before
  public void setUp() {
    this.duckMap  = new HashMap<String, Object>(2);
    this.mouseMap = new HashMap<String, Object>(2);
    
    this.duckMap.put("name",  "Duck");
    this.mouseMap.put("name", "Mouse");

    this.duckMap.put("salary", new Integer(100));
    this.mouseMap.put("salary", new Integer(1000));

    this.duckMap.put("isDuck",  Boolean.TRUE);
    this.mouseMap.put("isDuck", Boolean.FALSE);
  }

  @After
  public void tearDown() {
    this.duckMap  = null;
    this.mouseMap = null;
  }

  @Test
  public void testSimpleKeyValueQualifierString() {
    EOQualifierEvaluation q = parse("name = 'Duck'");
    
    assertTrue("duck name did not match", q.evaluateWithObject(this.duckMap));
    assertFalse("mouse name did match", q.evaluateWithObject(this.mouseMap));
  }
  
  @Test
  public void testSimpleKeyValueQualifierIntLessThan() {
    EOQualifierEvaluation q = parse("salary < 500");
    assertTrue("salary match failed", q.evaluateWithObject(this.duckMap));
    assertFalse("salary match failed", q.evaluateWithObject(this.mouseMap));
  }

  @Test
  public void testSimpleKeyValueQualifierIntLessThanEdge() {
    EOQualifierEvaluation q = parse("salary < 1000");
    assertTrue("salary match failed", q.evaluateWithObject(this.duckMap));
    assertFalse("salary match failed", q.evaluateWithObject(this.mouseMap));
  }

  @Test
  public void testSimpleKeyValueQualifierIntLessThanOrEqual() {
    EOQualifierEvaluation q = parse("salary <= 1000");
    assertTrue("salary match failed", q.evaluateWithObject(this.duckMap));
    assertTrue("salary match failed", q.evaluateWithObject(this.mouseMap));
  }
  
  @Test
  public void testPrefixExtOpColon() {
    EOQualifierEvaluation q = parse("name hasPrefix: 'Du'");
    assertTrue("name match failed", q.evaluateWithObject(this.duckMap));
    assertFalse("name matched", q.evaluateWithObject(this.mouseMap));
  }
  
  @Test
  public void testPrefixExtOp() {
    EOQualifierEvaluation q = parse("name hasPrefix 'Du'");
    assertTrue("name match failed", q.evaluateWithObject(this.duckMap));
    assertFalse("name matched", q.evaluateWithObject(this.mouseMap));
  }
  
  @Test
  public void testDynamicLookupExtOp() {
    EOQualifierEvaluation q = parse("name startsWith 'Du'");
    assertTrue("name match failed", q.evaluateWithObject(this.duckMap));
    assertFalse("name matched", q.evaluateWithObject(this.mouseMap));
  }
  
  @Test
  public void testBoolBoolComparison() {
    EOQualifierEvaluation q = parse("isDuck = YES");
    assertTrue("bool match failed", q.evaluateWithObject(this.duckMap));
    assertFalse("bool matched", q.evaluateWithObject(this.mouseMap));
  }
  
  @Test
  public void testBoolStringComparison() {
    EOQualifierEvaluation q = parse("isDuck = 'YES'");
    assertTrue("bool match failed", q.evaluateWithObject(this.duckMap));
    assertFalse("bool matched", q.evaluateWithObject(this.mouseMap));
  }
  
  /* utility */
  
  protected static EOQualifierEvaluation parse(String _fmt) {
    return (EOQualifierEvaluation)
      EOQualifier.qualifierWithQualifierFormat(_fmt);
  }
}
