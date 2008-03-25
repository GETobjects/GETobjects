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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.getobjects.eoaccess.EOAdaptor;
import org.getobjects.eoaccess.EOAdaptorChannel;
import org.getobjects.eoaccess.EOAttribute;
import org.getobjects.eoaccess.EOEntity;
import org.getobjects.eoaccess.EOModel;
import org.getobjects.eoaccess.mysql.EOMySQLChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TSchemaReflection extends EOAccessTest {

  static final String createTable1 =
    "CREATE TABLE jope_schema_check ( pkey INT PRIMARY KEY, ch VARCHAR(42) )";
  static final String dropTable1 =
    "DROP TABLE jope_schema_check";
  
  EOAdaptor        adaptor;
  EOAdaptorChannel channel;

  @Before
  public void setUp() {
    this.adaptor = EOAdaptor.adaptorWithURL(myurl);
    this.channel = this.adaptor.openChannel();

    //this.channel.performUpdateSQL(dropTable1);
    this.channel.performUpdateSQL(createTable1);
    this.channel.performUpdateSQL(createJopeTestNotes);
    this.channel.performUpdateSQL(createJopeTestContact);
}

  @After
  public void tearDown() {
    this.channel.performUpdateSQL(dropJopeTestContact);
    this.channel.performUpdateSQL(dropTable1);
    this.channel.performUpdateSQL(dropJopeTestNotes);
    
    this.channel.dispose();
    this.channel = null;
    this.adaptor.dispose();
    this.adaptor = null;
  }
  
  @Test
  public void testDescribeTables() {
    String[] tableNames = this.channel.describeTableNames();
    assertNotNull("adaptor returned no table names", tableNames);
    
    List<String> l = Arrays.asList(tableNames);
    assertTrue("did not find jope_schema_check table",
               l.contains("jope_schema_check"));
  }
  
  @Test
  public void testAdaptorModelFetch() {
    EOModel model = this.adaptor.model();
    assertNotNull("could not fetch model from adaptor", model);
    assertNotNull("missing jope_schema_check table",
                  model.entityNamed("jope_schema_check"));
    assertFalse("fetched model is a pattern?", model.isPatternModel());
  }

  @Test
  public void testAdaptorModelResolveDerivedNames() throws Exception {
    EOModel model = EOModel.loadModel
      (this.getClass().getResource("ModelWithDerivedNames.xml"));
    
    this.adaptor.setModelPattern(model);
    
    model = this.adaptor.model();
    assertNotNull("could not resolve model in adaptor", model);
    assertFalse("resolved model is still a pattern?", model.isPatternModel());
  }

  @Test
  public void testAdaptorModelResolveAttrNamePats() throws Exception {
    EOModel model = EOModel.loadModel
      (this.getClass().getResource("ModelWithAttrNamePatterns.xml"));
    
    this.adaptor.setModelPattern(model);
    
    model = this.adaptor.model();
    assertNotNull("could not resolve model in adaptor", model);
    assertFalse("resolved model is still a pattern?", model.isPatternModel());

    assertNotNull("missing jopetest_notes entity",
                  model.entityNamed("jopetest_notes"));

    EOEntity e = model.entityNamed("Contact");
    assertNotNull("missing Contact entity", e);
    assertNotNull("missing Contact.pkey",      e.attributeNamed("pkey"));
    assertNotNull("missing Contact.moddate",   e.attributeNamed("moddate"));
    assertNotNull("missing Contact.lastname",  e.attributeNamed("lastname"));
    assertNotNull("missing Contact.firstname", e.attributeNamed("firstname"));
    
    EOAttribute a = e.attributeNamed("moddate");
    assertNotNull("no exttype for moddate?", a.externalType());
  }
  
  //@Test
  public void testSomeMySQLStuff() {
    if (!(this.channel instanceof EOMySQLChannel))
      return;
    
    EOMySQLChannel mchannel = (EOMySQLChannel)this.channel;
    
    List<Map<String,Object>> rows =
      mchannel._fetchMySQLColumnsOfTable("jope_schema_check");
    System.err.println("COLS: " + rows);
    
    System.err.println("STATEMENT: " +
        mchannel._fetchMySQLTableCreateStatementForTable("jope_schema_check"));
    
    System.err.println("STATUS: " + mchannel._fetchMySQLStatus(null));
    System.err.println("VARS: " + mchannel._fetchMySQLVariables(false, null));
    
    System.err.println("MODEL: " +
        mchannel.describeModelWithTableNames(mchannel.describeTableNames()));
  }
}
