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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.getobjects.eoaccess.EORawSQLValue;
import org.getobjects.eocontrol.EOAndQualifier;
import org.getobjects.eocontrol.EOKeyComparisonQualifier;
import org.getobjects.eocontrol.EOKeyValueQualifier;
import org.getobjects.eocontrol.EOOrQualifier;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.eocontrol.EOQualifierVariable;
import org.getobjects.eocontrol.EOSQLQualifier;
import org.getobjects.eocontrol.EOQualifier.ComparisonOperation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class QualifierParser {

  @Before
  public void setUp() {
    
  }

  @After
  public void tearDown() {
    
  }
  
  /* tests */

  @Test public void testSimpleKeyValueQualifierInt() {
    this.testKeyValueQualifier("amount = 10000", "amount", new Integer(10000));
  }
  
  @Test public void testSimpleKeyValueQualifierString() {
    this.testKeyValueQualifier("name = 'Duck'",     "name", "Duck");
    this.testKeyValueQualifier("name like 'Duck*'", "name", "Duck*");
    this.testKeyValueQualifier("name < 'Duck'",     "name", "Duck");
    this.testKeyValueQualifier("name = null",       "name", null);
  }

  @Test public void testSimpleAndQualifier() {
    EOQualifier q = parse("firstname = 'Donald' AND lastname = 'Duck'");
    assertNotNull("could not parse qualifier", q);
    
    assertTrue("did not parse and AND qualifier",
               q instanceof EOAndQualifier);
    EOAndQualifier aq = (EOAndQualifier)q;
    
    assertEquals("length does not match", 2, aq.qualifiers().length);
  }

  @Test public void testComplexCompoundQualifier() {
    // should be: ((a = 1 AND b = 2) OR c = 3) AND f = 4
    EOQualifier q = parse("a = 1 AND b = 2 OR c = 3 AND f = 4");
    assertNotNull("could not parse qualifier", q);
    
    assertTrue("did not parse and AND qualifier",
               q instanceof EOAndQualifier);
    EOAndQualifier aq = (EOAndQualifier)q;
    
    assertEquals("length of top-level does not match",
                 2, aq.qualifiers().length);
  }
  
  @Test public void testComplexArgumentParsing() {
    EOQualifier q = EOQualifier.qualifierWithQualifierFormat
      ("name = %K AND salary > %d AND startDate %@ endDate",
       "firstname", "5000", "<=");
    assertNotNull("could not parse qualifier", q);
    
    assertTrue("did not parse an AND qualifier",
               q instanceof EOAndQualifier);
    EOAndQualifier aq = (EOAndQualifier)q;
    
    assertTrue("first qualifier is not a key comparison",
               aq.qualifiers()[0] instanceof EOKeyComparisonQualifier);
    assertTrue("second qualifier is not a key/value qualifier",
               aq.qualifiers()[1] instanceof EOKeyValueQualifier);
  }
  
  @Test public void testQualifierWithOneVariables() {
    EOQualifier q = EOQualifier.qualifierWithQualifierFormat
      ("lastname = $lastname");
    assertNotNull("could not parse qualifier", q);
    
    List<String> keys = q.bindingKeys();
    assertNotNull("no bindings in qualifier?",  keys);
    //System.err.println("K " + keys);
    //System.err.println("Q " + q);
    
    assertTrue("missing lastname in bindings",  keys.contains("lastname"));
  }

  @Test public void testQualifierWithSomeVariables() {
    EOQualifier q = EOQualifier.qualifierWithQualifierFormat
      ("lastname = $lastname AND firstname = $firstname OR salary > $salary");
    assertNotNull("could not parse qualifier", q);
    
    List<String> keys = q.bindingKeys();
    assertNotNull("no bindings in qualifier?",  keys);
    //System.err.println("K " + keys);
    //System.err.println("Q " + q);
    
    assertTrue("missing lastname in bindings",  keys.contains("lastname"));
    assertTrue("missing firstname in bindings", keys.contains("firstname"));
    assertTrue("missing salary in bindings",    keys.contains("salary"));
  }
  
  @Test public void testQualifierWithParenthesis() {
    EOQualifier q = EOQualifier.qualifierWithQualifierFormat
      ("name = 'Duck' AND (balance = 1 OR balance = 2\n OR balance = 3)");
    assertNotNull("could not parse qualifier", q);

    assertTrue("did not parse an AND qualifier",
               q instanceof EOAndQualifier);
    EOAndQualifier aq = (EOAndQualifier)q;
    
    assertTrue("first qualifier is not a key/value qualifier",
               aq.qualifiers()[0] instanceof EOKeyValueQualifier);
    assertTrue("second qualifier is not an OR qualifier",
               aq.qualifiers()[1] instanceof EOOrQualifier);
  }
  
  @Test public void testSimpleBoolKeyValueQualifier() {
    EOQualifier q = EOQualifier.qualifierWithQualifierFormat("isArchived");
    assertNotNull("could not parse qualifier", q);

    assertTrue("did not parse a key/value qualifier",
               q instanceof EOKeyValueQualifier);
  }
  
  @Test public void testBoolKeyValueAndFrontQualifier() {
    EOQualifier q = EOQualifier.qualifierWithQualifierFormat
      ("isArchived AND code > 3");
    assertNotNull("could not parse qualifier", q);

    assertTrue("did not parse an AND qualifier",
               q instanceof EOAndQualifier);
    EOAndQualifier aq = (EOAndQualifier)q;
    
    assertTrue("first qualifier is not a key/value qualifier",
        aq.qualifiers()[0] instanceof EOKeyValueQualifier);
    assertTrue("second qualifier is not a key/value qualifier",
        aq.qualifiers()[1] instanceof EOKeyValueQualifier);
  }
  
  @Test public void testBoolKeyValueAndBackQualifier() {
    EOQualifier q = EOQualifier.qualifierWithQualifierFormat
      ("code > 3 AND isArchived");
    assertNotNull("could not parse qualifier", q);

    assertTrue("did not parse an AND qualifier",
               q instanceof EOAndQualifier);
    EOAndQualifier aq = (EOAndQualifier)q;
    
    assertTrue("first qualifier is not a key/value qualifier",
        aq.qualifiers()[0] instanceof EOKeyValueQualifier);
    assertTrue("second qualifier is not a key/value qualifier",
        aq.qualifiers()[1] instanceof EOKeyValueQualifier);
  }
  
  @Test public void testBoolKeyValueAndParenQualifier() {
    EOQualifier q = EOQualifier.qualifierWithQualifierFormat
      ("(isArchived) AND code > 3 AND (isUsed)");
    assertNotNull("could not parse qualifier", q);

    assertTrue("did not parse an AND qualifier",
               q instanceof EOAndQualifier);
    EOAndQualifier aq = (EOAndQualifier)q;
    
    assertTrue("first qualifier is not a key/value qualifier",
        aq.qualifiers()[0] instanceof EOKeyValueQualifier);
    assertTrue("second qualifier is not a key/value qualifier",
        aq.qualifiers()[1] instanceof EOKeyValueQualifier);
    assertTrue("third qualifier is not a key/value qualifier",
        aq.qualifiers()[2] instanceof EOKeyValueQualifier);
  }
  
  @Test public void testSQLQualifier() {
    EOQualifier q = EOQualifier.qualifierWithQualifierFormat
      ("SQL[lastname = $lastname AND balance = $balance]");
    assertNotNull("could not parse qualifier", q);

    assertTrue("did not parse a SQL qualifier",
               q instanceof EOSQLQualifier);
    
    Object[] parts = ((EOSQLQualifier)q).parts();
    assertNotNull("qualifier contains no parts?", parts);
    assertEquals("number of parts does not match", 4, parts.length);
    
    assertTrue("first part is not a SQL value: " + parts[0].getClass(),
        parts[0] instanceof EORawSQLValue);
    
    assertTrue("second part is not a variable?",
        parts[1] instanceof EOQualifierVariable);
    assertEquals("lastname", ((EOQualifierVariable)parts[1]).key());

    assertTrue(parts[2] instanceof EORawSQLValue);
    
    assertTrue(parts[3] instanceof EOQualifierVariable);
    assertEquals("balance", ((EOQualifierVariable)parts[3]).key());
  }
  
  @Test public void testSkyPubQualifier() {
    String qs =
      "(isBanner=\'YES\') AND (showOnFrontPage=\'YES\') AND " +
      "(NSFileName hasPrefix: \'news_\')";
    EOQualifier q = EOQualifier.qualifierWithQualifierFormat(qs);
    assertNotNull("could not parse qualifier", q);

    assertTrue("did not parse an AND qualifier",
               q instanceof EOAndQualifier);
    EOAndQualifier aq = (EOAndQualifier)q;
    
    assertTrue("first qualifier is not a key/value qualifier",
               aq.qualifiers()[0] instanceof EOKeyValueQualifier);
    assertTrue("second qualifier is not a key/value qualifier",
               aq.qualifiers()[1] instanceof EOKeyValueQualifier);
    assertTrue("third qualifier is not a key/value qualifier",
               aq.qualifiers()[2] instanceof EOKeyValueQualifier);
    
    EOKeyValueQualifier kvq = (EOKeyValueQualifier)aq.qualifiers()[0];
    assertEquals("key is incorrect",   "isBanner", kvq.key());
    assertEquals("value is incorrect", "YES",      kvq.value());
    assertEquals("operation mismatch",
        EOQualifier.ComparisonOperation.EQUAL_TO, kvq.operation());
    
    kvq = (EOKeyValueQualifier)aq.qualifiers()[2];
    assertEquals("key is incorrect",   "NSFileName", kvq.key());
    assertEquals("value is incorrect", "news_",      kvq.value());
    assertEquals("operation mismatch", "hasPrefix:", kvq.extendedOperation());
  }
  
  @Test public void testSkyPubQualifierWithNot() {
    String qs = "(NSFileName!='index.html')";
    EOQualifier q = parse(qs);
    assertNotNull("could not parse qualifier", q);
    
    testKeyValueQualifier(qs, "NSFileName", "index.html");
    
    EOKeyValueQualifier kvq = (EOKeyValueQualifier)q;
    assertEquals("operation did not match", ComparisonOperation.NOT_EQUAL_TO,
                 kvq.operation());
  }
  
  /* support */

  protected void testKeyValueQualifier(String _qs, String _k, Object _v) {
    EOQualifier q = parse(_qs);
    assertNotNull("could not parse qualifier", q);

    assertTrue("did not parse a key/value qualifier: " + q.getClass(),
               q instanceof EOKeyValueQualifier);
    EOKeyValueQualifier kvq = (EOKeyValueQualifier)q;
    
    assertEquals("key of qualifier does not match",   _k, kvq.key());
    assertEquals("value of qualifier does not match", _v, kvq.value());
  }
  
  /* utility */
  
  protected static EOQualifier parse(String _fmt) {
    org.getobjects.eocontrol.EOQualifierParser parser =
      new org.getobjects.eocontrol.EOQualifierParser
        (_fmt.toCharArray(), new Object[0] /* args */);
    
    EOQualifier qualifier = parser.parseQualifier();
    // TODO: check for errors
    return qualifier;
  }
}
