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

package org.getobjects.eocontrol.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.getobjects.eocontrol.EOFetchSpecification;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FetchSpecification {

  @Before
  public void setUp() {
    
  }

  @After
  public void tearDown() {
    
  }

  @Test
  public void testBindPatternHints() {
    EOFetchSpecification fs = new EOFetchSpecification();
    fs.setHint("EOCustomQueryExpressionHintKeyBindPattern",
               "%%(tables)s WHERE id = %(id)s");
    
    EOFetchSpecification bfs = fs.fetchSpecificationWithQualifierBindings(this);
    Map<String, Object> bhints = bfs.hints();
    assertNotNull("resolved fetchspec contains no hints!", bhints);
    assertNotNull("missing hint: EOCustomQueryExpressionHint",
                  bhints.get("EOCustomQueryExpressionHintKey"));
    assertEquals("pattern did not resolve",
                 "%(tables)s WHERE id = 10200",
                 bhints.get("EOCustomQueryExpressionHintKey"));
  }
  
  /* support */
  
  public int id() {
    return 10200;
  }
}
