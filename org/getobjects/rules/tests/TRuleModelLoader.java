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

import java.net.URL;

import org.getobjects.rules.Rule;
import org.getobjects.rules.RuleModel;
import org.getobjects.rules.RuleModelLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TRuleModelLoader {

  protected RuleModelLoader loader;

  @Before
  public void setUp() {
    this.loader = new RuleModelLoader(); 
  }

  @After
  public void tearDown() {
    this.loader = null;
  }
  
  /* tests */

  @Test public void testLoadRuleModel1() {
    RuleModel model = this.loadModel("RuleModel1");
    assertNotNull("could not parse rule model", model);
    
    Rule[] rulez = model.rules();
    assertNotNull("parsed rule model has no rules set?", rulez);
    assertFalse("parsed rule model has an empty ruleset?", rulez.length == 0);
    assertEquals("rule count mismatch", 8, rulez.length);
    
    // for (Rule r: rulez) System.err.println("R: " + r);
  }
  
  /* support */
  
  protected RuleModel loadModel(String _model) {
    URL url = TRuleModelLoader.class.getResource(_model + ".xml");
    return this.loader.loadModelFromURL(url);
  }
}
