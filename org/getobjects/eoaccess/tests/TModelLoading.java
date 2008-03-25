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

package org.getobjects.eoaccess.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.getobjects.eoaccess.EOEntity;
import org.getobjects.eoaccess.EOModel;
import org.getobjects.eoaccess.EOModelLoader;
import org.getobjects.eoaccess.EORelationship;
import org.getobjects.eocontrol.EOFetchSpecification;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TModelLoading {
  
  protected EOModelLoader loader;

  @Before
  public void setUp() {
    this.loader = new EOModelLoader(); 
  }

  @After
  public void tearDown() {
    this.loader = null;
  }
  
  @Test public void testModelWithDerivedNames() {
    EOModel model = this.loadModel("ModelWithDerivedNames");
    assertNotNull("could not load model", model);
    assertNull("error in loading model", this.loader.lastException());
    
    EOEntity contact = model.entityNamed("Contact");
    EOEntity notes   = model.entityNamed("notes");
    assertNotNull("missing Contact entity", contact);
    assertNotNull("missing notes entity",   notes);
    
    assertTrue("Contact is not a pattern", contact.isPatternEntity());
    assertTrue("notes is not a pattern",   notes.isPatternEntity());
    
    /* fetch specs */

    EOFetchSpecification fs = contact.fetchSpecificationNamed("count");
    assertNotNull("missing count fetch-spec of Contact", fs);
    assertNotNull(fs.hints());
    assertNotNull(fs.hints().get("EOCustomQueryExpressionHintKey"));
    
    fs = contact.fetchSpecificationNamed("allDucks");
    assertNotNull("missing allDucks fetch-spec of Contact", fs);
    //System.err.println("D: " + fs);
    
    /* relationships */
    
    EORelationship toNotes = contact.relationshipNamed("toNotes");
    assertNotNull("missing toNotes relship in Contact entity", toNotes);

    EORelationship toContact = notes.relationshipNamed("toContact");
    assertNotNull("missing toContact relship in notes entity", toContact);
  }

  @Test public void testModelWithAttrNamePatterns() {
    EOModel model = this.loadModel("ModelWithAttrNamePatterns");
    assertNotNull("could not load model", model);
    assertNull("error in loading model", this.loader.lastException());

    EOEntity contact = model.entityNamed("Contact");
    assertNotNull("missing Contact entity", contact);
    assertNotNull("missing jopetest_notes entity",
                  model.entityNamed("jopetest_notes"));
  }
  
  /* support */
  
  protected EOModel loadModel(String _model) {
    URL url = TModelLoading.class.getResource(_model + ".xml");
    return this.loader.loadModelFromURL(url);
  }
}
