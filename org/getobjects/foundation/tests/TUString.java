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

package org.getobjects.foundation.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.getobjects.foundation.NSKeyValueStringFormatter;
import org.getobjects.foundation.UString;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TUString {

  public class Contact {
    public String name;
    public int    balance;
    
    public Contact(String _name, int _balance) {
      this.name = _name;
      this.balance = _balance;
    }
  }
  
  protected Contact donald;
  
  @Before
  public void setUp() {
    this.donald = new Contact("Donald", 10000);
  }

  @After
  public void tearDown() {
    this.donald = null;
  }

  @Test
  public void testCapitalizedString() {
    assertEquals("text mismatch",
                 "Mein Text",
                 UString.capitalizedString("mein text"));
    assertEquals("text mismatch",
                 "   Mein /-s Text",
                 UString.capitalizedString("   mein /-s text"));
  }

  @Test
  public void testXMLEscaping() {
    assertEquals("text mismatch",
                 "hello &lt;tag&gt;",
                 UString.stringByEscapingXMLString("hello <tag>"));

    assertEquals("text mismatch",
                 "a &amp; b",
                 UString.stringByEscapingXMLString("a & b"));
  }

  @Test
  public void testFormatting() {
    String fmt;
    
    fmt = "Add %i and %i";
    assertEquals("text mismatch", "Add 1 and 2",
                 NSKeyValueStringFormatter.format(fmt, 1, 2));
    
    fmt = "Add %(0)i and %(1)i";
    assertEquals("text mismatch", "Add 1 and 2",
                 NSKeyValueStringFormatter.format(fmt, 1, 2));
    
    fmt = "Array[%(size)i]: %(0)s and %(1)@";
    assertEquals("text mismatch", "Array[2]: 1 and 2",
                 NSKeyValueStringFormatter.format(fmt, 1, 2));

    fmt = "Customer: %(name)s, balance: %(balance)i";
    assertEquals("text mismatch", "Customer: Donald, balance: 10000",
                 NSKeyValueStringFormatter.format(fmt, this.donald));
    
    fmt = "Customer: %(name)s, %(firstname)s";
    assertNull("did not raise on missing attribute!",
               NSKeyValueStringFormatter.format(fmt, this.donald));
    
    fmt = "Customer: %(name)s, %(firstname)s";
    assertEquals("did not raise on missing attribute!",
                 "Customer: Donald, <null>",
                 NSKeyValueStringFormatter.format(fmt, this.donald, false));
  }
}
