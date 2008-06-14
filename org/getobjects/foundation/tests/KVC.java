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

package org.getobjects.foundation.tests;

import static org.junit.Assert.assertEquals;

import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.foundation.NSKeyValueCodingAdditions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class KVC {

  public class ValueObject {
    int color                           = 5;
    public boolean primitiveTypeBoolean = false;
    public Boolean classBoolean         = Boolean.FALSE;

    public void setColor(int v) {
      this.color = v;
    }

    public int color() {
      return this.color;
    }

    public void setClassBoolean(Boolean _tf) {
      this.classBoolean = _tf;
    }

    public Object testPath() {
      return this;
    }
  }

  ValueObject o = null;

  @Before
  public void setUp() {
    this.o = new ValueObject();
  }

  @After
  public void tearDown() {
    this.o = null;
  }

  @Test
  public void testValueForKey() {
    assertEquals(NSKeyValueCoding.Utility.valueForKey(this.o, "color"), 5);
    
    assertEquals(NSKeyValueCoding.Utility.valueForKey(null, "key"), null);
  }

  @Test
  public void testTakeValueForKey() {
    NSKeyValueCoding.Utility.takeValueForKey(this.o, new Integer(10), "color");
    assertEquals(NSKeyValueCoding.Utility.valueForKey(this.o, "color"), 10);

    NSKeyValueCoding.Utility.takeValueForKey(null, "a", "key");
  }

  @Test
  public void testValueForKeyPath() {
    assertEquals(NSKeyValueCodingAdditions.Utility
        .valueForKeyPath(this.o, "color"), 5);
    assertEquals(NSKeyValueCodingAdditions.Utility
        .valueForKeyPath(this.o, "testPath.color"), 5);

    assertEquals(NSKeyValueCodingAdditions.Utility
        .valueForKeyPath(null, "testPath.color"), null);
  }
  
  @Test
  public void testTakeValueForKeyPath() {
    NSKeyValueCodingAdditions.Utility.takeValueForKeyPath
      (this.o, new Integer(10), "testPath.color");
    
    assertEquals(NSKeyValueCodingAdditions.Utility.valueForKeyPath
      (this.o, "color"), 10);

    // check against null
    NSKeyValueCodingAdditions.Utility.takeValueForKeyPath(null, "a", "key");   
  }
  
  @Test
  public void testTakeValueForKeyWithStringArgumentForPrimitiveBooleanType() {
    NSKeyValueCoding.Utility.takeValueForKey(this.o, "YES",
                                             "primitiveTypeBoolean");
    assertEquals(NSKeyValueCoding.Utility.
        valueForKey(this.o, "primitiveTypeBoolean"), true);
    NSKeyValueCoding.Utility.takeValueForKey(this.o, "false",
                                             "primitiveTypeBoolean");
    assertEquals(NSKeyValueCoding.Utility.
        valueForKey(this.o, "primitiveTypeBoolean"), false);
    NSKeyValueCoding.Utility.takeValueForKey(this.o, "1",
                                             "primitiveTypeBoolean");
    assertEquals(NSKeyValueCoding.Utility.
        valueForKey(this.o, "primitiveTypeBoolean"), true);
  }
  
  @Test
  public void testTakeValueForKeyWithStringArgumentForClassBoolean() {
    NSKeyValueCoding.Utility.takeValueForKey(this.o, "YES",
                                             "classBoolean");
    assertEquals(NSKeyValueCoding.Utility.
        valueForKey(this.o, "classBoolean"), Boolean.TRUE);
    NSKeyValueCoding.Utility.takeValueForKey(this.o, "0",
                                             "classBoolean");
    assertEquals(NSKeyValueCoding.Utility.
        valueForKey(this.o, "classBoolean"), Boolean.FALSE);
    NSKeyValueCoding.Utility.takeValueForKey(this.o, "true",
                                             "classBoolean");
    assertEquals(NSKeyValueCoding.Utility.
        valueForKey(this.o, "classBoolean"), Boolean.TRUE);
  }
}
