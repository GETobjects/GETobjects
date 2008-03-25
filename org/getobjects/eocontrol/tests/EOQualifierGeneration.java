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

import static org.junit.Assert.assertEquals;

import org.getobjects.eocontrol.EOQualifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EOQualifierGeneration {

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  @Test
  public void testSimpleKeyValueQualifierInt() {
    EOQualifier q = parse("amount = 10000");
    assertEquals("amount = 10000", q.stringRepresentation());
  }
  
  @Test
  public void testComplexCompoundQualifier() {
    EOQualifier q = parse("a = 1 AND b = 2 OR c = 3 AND f = 4");
    String qs = q.stringRepresentation();
    assertEquals("representation differs",
                 "((a = 1 AND b = 2) OR c = 3) AND f = 4", qs);
  }

  @Test
  public void testComplexArgumentQualifier() {
    EOQualifier q = EOQualifier.qualifierWithQualifierFormat
      ("name = %K AND salary > %d AND startDate %@ endDate",
       "firstname", "5000", "<=");
    
    String qs = q.stringRepresentation();
    assertEquals("representation differs",
                 "name = firstname AND salary > 5000 AND startDate <= endDate",
                 qs);
  }
  
  /* utility */
  
  protected static EOQualifier parse(String _fmt) {
    return EOQualifier.qualifierWithQualifierFormat(_fmt);
  }
}
