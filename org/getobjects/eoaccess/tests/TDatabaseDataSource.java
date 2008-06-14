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

package org.getobjects.eoaccess.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.getobjects.eoaccess.EOActiveDataSource;
import org.getobjects.eoaccess.EOActiveRecord;
import org.getobjects.eoaccess.EODatabase;
import org.getobjects.eoaccess.EOEntity;
import org.getobjects.eoaccess.EOModel;
import org.getobjects.eocontrol.EOFetchSpecification;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.foundation.NSObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TDatabaseDataSource extends EOAccessTest {

  static final String createTable2 =
    "CREATE TABLE jopetest_contact_auto ( pkey INT PRIMARY KEY, " +
    "lastname VARCHAR(255), firstname VARCHAR(255) )";
  static final String dropTable1 =
    "DROP TABLE jopetest_contact";
  static final String dropTable2 =
    "DROP TABLE jopetest_contact_auto";
  
  EOModel              model;
  EODatabase           databaseWithModel;
  EOActiveDataSource dataSourceWithModel;

  @Before
  public void setUp() throws Exception {
    this.database = new EODatabase(myurl, null /* model */, null /* classes */);
    
    URL murl = this.getClass().getResource("ModelWithDerivedNames.xml");
    this.model = EOModel.loadModel(murl);
    this.databaseWithModel =
      new EODatabase(myurl, this.model /* model */, null /* classes */);
    
    this.database.adaptor().performUpdateSQL(createGopeTestContact);
    this.database.adaptor().performUpdateSQL(fillTable1_1);
    this.database.adaptor().performUpdateSQL(fillTable1_2);
    this.database.adaptor().performUpdateSQL(createTable2);
    this.database.adaptor().performUpdateSQL(createGopeTestNotes);
    this.database.adaptor().performUpdateSQL(fillTable2_1);
    
    this.dataSource =
      new EOActiveDataSource(this.database, "jopetest_contact");
    this.dataSourceWithModel =
      new EOActiveDataSource(this.databaseWithModel, "Contact");
  }

  @After
  public void tearDown() {
    this.database.adaptor().performUpdateSQL(dropGopeTestNotes);
    this.database.adaptor().performUpdateSQL(dropTable2);
    this.database.adaptor().performUpdateSQL(dropTable1);
    this.database.dispose();
    this.database = null;
    
    this.databaseWithModel.dispose();
    this.databaseWithModel = null;
    
    this.dataSource          = null;
    this.dataSourceWithModel = null;
  }
  
  /* tests */
  
  @Test public void testEntityFetch() {
    EOEntity entity;
    
    entity = this.dataSource.entity();
    assertNotNull("could not retrieve entity from datasource", entity);
    
    assertNotNull("missing attribute", entity.attributeNamed("firstname"));
    assertNotNull("missing attribute", entity.attributeNamed("lastname"));
  }
  
  @Test public void testFetchWithoutFetchSpecification() {
    this.dataSource.setFetchSpecification(null);
    
    List objects = this.dataSource.fetchObjects();
    assertNotNull("error fetching objects", objects);
    assertEquals("objects count mismatch", 2, objects.size());
    
    //System.err.println("O: " + objects);
  }
  
  @Test public void testDisableFetch() {
    this.dataSource.setFetchEnabled(false);
    List objects = this.dataSource.fetchObjects();
    assertNotNull("error fetching objects", objects);
    assertEquals("got objects from disabled fetch", 0, objects.size());
    
    this.dataSource.setFetchEnabled(true);
  }
  
  @Test public void testFetchWithSimpleQualifier() {
    EOQualifier qualifier = EOQualifier.qualifierWithQualifierFormat
      ("lastname = 'Mouse'");
    
    EOFetchSpecification fs = new EOFetchSpecification
      ("jopetest_contact", qualifier, null /* sort orderings */);
    this.dataSource.setFetchSpecification(fs);
    
    List objects = this.dataSource.fetchObjects();
    assertNotNull("error fetching objects", objects);
    assertEquals("objects count mismatch", 1, objects.size());
    
    this.assertMickey(objects.get(0));
  }
  
  @Test public void testFetchWithRelShipQualifierMatch() {
    EOQualifier qualifier = EOQualifier.qualifierWithQualifierFormat
      ("lastname = 'Mouse' AND toNotes.notetext LIKE '*hello*'");
    
    EOFetchSpecification fs = new EOFetchSpecification
      ("Contact" /* entity */, qualifier, null /* sort orderings */);
    this.dataSourceWithModel.setFetchSpecification(fs);
    
    List objects = this.dataSourceWithModel.fetchObjects();
    assertNotNull("error fetching objects", objects);
    assertEquals("objects count mismatch", 1, objects.size());
    
    this.assertMickey(objects.get(0));
  }
  
  @Test public void testFetchWithRelShipQualifierNoMatch() {
    EOQualifier qualifier = EOQualifier.qualifierWithQualifierFormat
      ("lastname = 'Mouse' AND toNotes.notetext LIKE '*No Hello*'");
    
    EOFetchSpecification fs = new EOFetchSpecification
      ("Contact" /* entity */, qualifier, null /* sort orderings */);
    this.dataSourceWithModel.setFetchSpecification(fs);
    
    List objects = this.dataSourceWithModel.fetchObjects();
    assertNotNull("error fetching objects", objects);
    assertEquals("objects count mismatch", 0, objects.size());
  }

  @Test public void testIntPrimaryKeyFinder() {
    this.assertMickey(this.dataSource.findById(2));
  }
  
  @Test public void testUpdateObject() {
    EOActiveRecord mickey = (EOActiveRecord)this.dataSource.findById(2);
    assertFalse("mickey is NOT a new object!", mickey.isNew());
    
    /* rename Mickey to Minney */
    mickey.takeValueForKey("Minney", "firstname");
    
    Exception error = this.dataSource.updateObject(mickey);
    assertNull("saving mickey produced an error: " + error, error);
    
    Map<String, Object> changes = mickey.changesFromSnapshot(mickey.snapshot());
    assertTrue("record has changes after save",
               changes == null || changes.size() == 0);
    
    EOActiveRecord minney = (EOActiveRecord)this.dataSource.findById(2);
    assertNotNull("could not refetch updated record", minney);
    assertEquals("change did not apply",
                 "Minney", minney.valueForKey("firstname"));
  }
  
  @Test
  public void testInsertObject() {
    EOActiveRecord tick = new EOActiveRecord();
    assertNotNull("could not allocate new tick record", tick);
    
    /* rename Mickey to Minney */
    tick.takeValueForKey("3",    "pkey");
    tick.takeValueForKey("Tick", "firstname");
    tick.takeValueForKey("Duck", "lastname");
    
    Exception error = this.dataSource.insertObject(tick);
    assertNull("inserting tick produced an error: " + error, error);
    
    /* check for change status */

    Map<String, Object> changes = tick.changesFromSnapshot(tick.snapshot());
    assertTrue("record has changes after insert",
               changes == null || changes.size() == 0);
    
    /* check whether we can refetch it */
    
    EOActiveRecord reloadTick = (EOActiveRecord)this.dataSource.findById(3);
    assertNotNull("could not refetch inserted record", reloadTick);
    assertEquals("value mismatch",
                 "Tick", reloadTick.valueForKey("firstname"));
    assertEquals("value mismatch",
                 "Duck", reloadTick.valueForKey("lastname"));
  }
  
  @Test
  public void testModelFindRawRows() {
    this.dataSourceWithModel.setFetchSpecificationByName("count");
    
    EOFetchSpecification fs = this.dataSourceWithModel.fetchSpecification();
    assertNotNull("could not set named fspec", fs);
    assertNotNull("missing count fetch-spec of Contact", fs);
    assertNotNull(fs.hints());
    assertNotNull(fs.hints().get("EOCustomQueryExpressionHintKey"));
    
    Object object = this.dataSourceWithModel.find();
    assertNotNull("error fetching object", object);
  }
  
  @Test
  public void testModelFindRawRowsShortCut() {
    Object object = this.dataSourceWithModel.find("count");
    assertNotNull("error fetching object", object);
  }
  
  @Test
  public void testModelFetchObjectsWithNamedSpec() {
    this.dataSourceWithModel.setFetchSpecificationByName("allDucks");
    
    List objects = this.dataSourceWithModel.fetchObjects();
    //System.err.println("DUCKS: " + objects);
    assertNotNull("error fetching objects", objects);
    assertEquals("objects count mismatch", 1, objects.size());
  }
  
  @Test
  public void testModelFetchObjectsWithPartialNamedSpec() {
    this.dataSourceWithModel
      .setFetchSpecificationByName("allFirstnamesOfTheDucks");
    
    List objects = this.dataSourceWithModel.fetchObjects();
    //System.err.println("DUCKS: " + objects);
    assertNotNull("error fetching objects: " + 
                  this.dataSourceWithModel.lastException(),
                  objects);
    assertEquals("objects count mismatch", 1, objects.size());
  }
  
  @Test
  public void testModelFetchObjectsWithPartialSQLSpec() {
    this.dataSourceWithModel
      .setFetchSpecificationByName("allFirstnamesOfTheDucksSQL");
    
    List objects = this.dataSourceWithModel.fetchObjects();
    //System.err.println("DUCKS: " + objects);
    assertNotNull("error fetching objects", objects);
    assertEquals("objects count mismatch", 1, objects.size());
  }
  
  @Test
  public void testModelFetchObjectsWithPartialSQLPatternSpec() {
    this.dataSourceWithModel
      .setFetchSpecificationByName("allFirstnamesOfTheDucksSQLPat");
    
    List objects = this.dataSourceWithModel.fetchObjects();
    //System.err.println("DUCKS: " + objects);
    assertNotNull("error fetching objects", objects);
    assertEquals("objects count mismatch", 1, objects.size());
  }
  
  /* support */
  
  protected void assertMickey(Object _o) {
    assertNotNull("not mickey, given object is null", _o);
    
    NSObject eo = (NSObject)_o;
    assertEquals("lastname mismatch",
                 "Mouse", eo.valueForKey("lastname")); 
    assertEquals("firstname mismatch",
                 "Mickey", eo.valueForKey("firstname")); 
  }
}
