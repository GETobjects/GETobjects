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

import org.getobjects.eoaccess.EOActiveDataSource;
import org.getobjects.eoaccess.EODatabase;
import org.getobjects.foundation.NSObject;

public abstract class EOAccessTest extends NSObject {

  static final String myurl = 
    "jdbc:mysql://localhost/jopetest?user=jopetest&password=jopetest";
  static final String pgurl = 
    "jdbc:postgresql://localhost/jopetest?user=jopetest&password=jopetest";
  
  /* SQL */
  
  static final String createGopeTestContact =
    "CREATE TABLE jopetest_contact ( pkey INT PRIMARY KEY, " +
    "lastname VARCHAR(255), firstname VARCHAR(255), moddate TIMESTAMP )";

  static final String dropGopeTestContact = "DROP TABLE jopetest_contact";

  static final String createGopeTestNotes =
    "CREATE TABLE jopetest_notes ( id INT PRIMARY KEY, " +
    "notetext VARCHAR(255) NOT NULL, contact_id INT)";
  static final String dropGopeTestNotes =
    "DROP TABLE jopetest_notes";

  /* Note: with MySQL we can't send two in one by using ';' separation! */
  static final String fillTable1_1 =
    "INSERT INTO jopetest_contact ( pkey, firstname, lastname ) VALUES (" +
    "1, 'Donald', 'Duck' )";
  static final String fillTable1_2 =
    "INSERT INTO jopetest_contact ( pkey, firstname, lastname ) VALUES (" +
    "2, 'Mickey', 'Mouse' )";
  static final String fillTable2_1 =
    "INSERT INTO jopetest_notes ( id, notetext, contact_id ) VALUES (" +
    "1, 'Hello World', 2 )";
  
  
  /* ivars */

  EODatabase           database;
  EOActiveDataSource dataSource;
}
