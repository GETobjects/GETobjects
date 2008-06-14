/*
  Copyright (C) 2006 Helge Hess

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

package org.getobjects.rules.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.List;

import org.getobjects.rules.RuleContext;
import org.getobjects.rules.RuleModel;
import org.getobjects.rules.RuleModelLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TRuleContext {
  
  protected RuleModel   model1;
  protected RuleContext context1;

  @Before
  public void setUp() {
    this.model1   = loadModel("RuleModel1");
    
    this.context1 = new RuleContext();
    this.context1.setModel(this.model1);
  }

  @After
  public void tearDown() {
    this.context1 = null;
    this.model1   = null;
  }
  
  /* tests */

  @Test public void testSimpleEvals() {
    Object v = this.context1.valueForKey("allowCollapsing");
    assertNotNull("got no value for allowCollapsing", v);
    assertTrue("value is not a Boolean", v instanceof Boolean);
    assertEquals("did expect a different value", Boolean.FALSE, v);
  }
  
  @Test public void testEvalWithContext() {
    this.context1.takeValueForKey("error", "task");
    
    Object v = this.context1.valueForKey("allowCollapsing");
    assertNotNull("got no value for allowCollapsing", v);
    assertTrue("value is not a Boolean", v instanceof Boolean);
    assertEquals("did expect a different value", Boolean.TRUE, v);
  }
  
  @Test public void testKeyValueEvalWithContext() {
    this.context1.takeValueForKey("green", "backgroundColor");
    
    Object v = this.context1.valueForKey("color");
    assertNotNull("got no value for color", v);
    assertEquals("did expect a different value", "green", v);
  }

  @Test public void testPossibleValuesForKey() {
    List<Object> values =
      this.context1.allPossibleValuesForKey("allowCollapsing");

    assertTrue("did not contain false", values.contains(Boolean.FALSE));
    assertFalse("did contain true",  values.contains(Boolean.TRUE));
  }
  
  @Test public void testPossibleValuesForKeyWithContext() {
    this.context1.takeValueForKey("green", "backgroundColor");
    
    List<Object> values = this.context1.allPossibleValuesForKey("color");
    assertNotNull("got no values for color", values);
    assertEquals("did not get expected number of values", 2, values.size());
    assertEquals("did expect a different 0 value", "green", values.get(0));
    assertEquals("did expect a different 1 value", "green", values.get(1));
  }
  
  @Test public void testPossibleValuesForKeyWithQualifier() {
    this.context1.takeValueForKey("edit", "task");
    
    Object v = this.context1.valueForKey("color");
    assertNotNull("got no value for color", v);
    assertEquals("did expect a different value", "red", v);
  }
  
  /* support */
  
  protected RuleModel loadModel(String _model) {
    URL url = TRuleContext.class.getResource(_model + ".xml");
    RuleModelLoader loader = new RuleModelLoader();
    return loader.loadModelFromURL(url);
  }
}
