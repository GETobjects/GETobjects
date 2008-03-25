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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Map;

import org.getobjects.eoaccess.EOActiveDataSource;
import org.getobjects.eoaccess.EOActiveRecord;
import org.getobjects.eoaccess.EODatabase;
import org.getobjects.foundation.NSObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TActiveRecord extends EOAccessTest {

  static final String createTable2 =
    "CREATE TABLE jopetest_contact_auto ( pkey INT PRIMARY KEY, " +
    "lastname VARCHAR(255), firstname VARCHAR(255), moddate TIMESTAMP )";
  
  static final String dropTable2 = "DROP TABLE jopetest_contact_auto";
  
  EOActiveRecord donald;
  EOActiveRecord mickey;

  @Before
  public void setUp() {
    this.database = new EODatabase(myurl, null /* model */, null /* classes */);

    this.database.adaptor().performUpdateSQL(createJopeTestContact);
    this.database.adaptor().performUpdateSQL(fillTable1_1);
    this.database.adaptor().performUpdateSQL(fillTable1_2);
    this.database.adaptor().performUpdateSQL(createTable2);
    
    this.dataSource =
      new EOActiveDataSource(this.database, "jopetest_contact");
    
    this.donald = (EOActiveRecord)this.dataSource.findById(1);
    this.mickey = (EOActiveRecord)this.dataSource.findById(2);
  }

  @After
  public void tearDown() {
    this.donald.dispose(); this.donald = null;
    this.mickey.dispose(); this.mickey = null;
    
    this.database.adaptor().performUpdateSQL(dropTable2);
    this.database.adaptor().performUpdateSQL(dropJopeTestContact);
    this.database.dispose();
    this.database = null;
  }
  
  @Test
  public void testIsNotNewAfterFetch() {
    assertFalse("mickey is NOT a new object!", this.mickey.isNew());
    assertFalse("donald is NOT a new object!", this.donald.isNew());
  }
  
  @Test
  public void testSnapshotChangeTracking() {
    /* rename Mickey to Minney */
    this.mickey.takeValueForKey("Minney", "firstname");
    
    Map<String, Object> changes = 
      this.mickey.changesFromSnapshot(this.mickey.snapshot());
    assertNotNull("got no changes record", changes);
    assertFalse("changes record is empty", changes.size() == 0);
    assertTrue("got more than one change", changes.size() == 1);
    assertTrue("change is not firstname", changes.containsKey("firstname"));
    assertEquals("change does not match", "Minney", changes.get("firstname"));
  }

  @Test
  public void testSaveExistingObject() {
    Date date = new Date();
    
    /* rename Mickey to Minney */
    this.mickey.takeValueForKey("Minney", "firstname");
    this.mickey.takeValueForKey(date,     "moddate");
    
    Exception error = this.mickey.save();
    assertNull("saving mickey produced an error: " + error, error);

    Map<String, Object> changes = 
      this.mickey.changesFromSnapshot(this.mickey.snapshot());
    assertTrue("record has changes after save",
               changes == null || changes.size() == 0);
    
    EOActiveRecord minney = (EOActiveRecord)this.dataSource.findById(2);
    assertNotNull("could not refetch updated record", minney);
    assertEquals("change did not apply",
                 "Minney", minney.valueForKey("firstname"));
    
    /* eg MySQL 4.1 returns does not return milliseconds */
    /* Note: Date classes change on retrieval, eg MySQL returns a Timestamp */
    assertEquals("moddate mismatch",
                 date.getTime() / 1000,
                 ((Date)minney.valueForKey("moddate")).getTime() / 1000);
  }

  @Test
  public void testSaveNewObject() {
    EOActiveRecord tick =
      new EOActiveRecord(this.database, "jopetest_contact");
    assertNotNull("could not allocate new tick record", tick);
    
    /* rename Mickey to Minney */
    tick.takeValueForKey("3",    "pkey");
    tick.takeValueForKey("Tick", "firstname");
    tick.takeValueForKey("Duck", "lastname");
    
    Exception error = tick.save();
    assertNull("inserting tick produced an error: " + error, error);

    Map<String, Object> changes = tick.changesFromSnapshot(tick.snapshot());
    assertTrue("record has changes after insert",
               changes == null || changes.size() == 0);
    
    EOActiveRecord reloadTick = (EOActiveRecord)this.dataSource.findById(3);
    assertNotNull("could not refetch inserted record", reloadTick);
    assertEquals("value mismatch",
                 "Tick", reloadTick.valueForKey("firstname"));
    assertEquals("value mismatch",
                 "Duck", reloadTick.valueForKey("lastname"));
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
