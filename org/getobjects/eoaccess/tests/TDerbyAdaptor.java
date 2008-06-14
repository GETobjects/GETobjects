/*
  Copyright (C) 2007 Helge Hess

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
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.getobjects.eoaccess.EOAdaptor;
import org.getobjects.eoaccess.EOAdaptorChannel;
import org.getobjects.eoaccess.derby.EODerbyAdaptor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TDerbyAdaptor {

  static final String url = "jdbc:derby:testdb; create=true";
  
  /* Note: we need to quote all IDs, otherwise Derby won't find them in the
   *       INSERT. The INSERT quotes all record key IDs. (it calls
   *       sqlStringForSchemaObjectName() when converting the ID into the
   *       INSERT statement)
   */
  static final String createTableUmlauts =
    "CREATE TABLE \"umlauts\" ( \"id\" INT, \"text\" VARCHAR(255) )";
  static final String dropTableUmlauts = "DROP TABLE \"umlauts\"";

  EOAdaptor        adaptor;
  EOAdaptorChannel channel;
  
  @Before
  public void setUp() {
    EODerbyAdaptor.setSystemDirectory("/tmp/jope-derby-test/");
    
    this.adaptor = EOAdaptor.adaptorWithURL(url);
    this.channel = this.adaptor.openChannel();

    /* cleanup after failed tests */
    this.channel.performUpdateSQL(dropTableUmlauts);
  }
  
  @After
  public void tearDown() {
    this.channel.performUpdateSQL(dropTableUmlauts);
    
    this.channel.dispose();
    this.channel = null;
    this.adaptor.dispose();
    this.adaptor = null;
  }
  
  /* tests */
  
  @Test
  public void testCreateTable() {
    int result = this.channel.performUpdateSQL(createTableUmlauts);
    assertEquals("failed to create table", result, 0);
  }
  
  @Test
  public void testCreateAndInsertTable() {
    int result = this.channel.performUpdateSQL(createTableUmlauts);
    assertEquals("failed to create table", result, 0);
    
    Map<String, Object> record = new HashMap<String, Object>(16);
    record.put("id",    1);
    record.put("text", "Hello World!");
    
    assertTrue("failed to insert record",
        this.channel.insertRow("umlauts", record));
  }
}
