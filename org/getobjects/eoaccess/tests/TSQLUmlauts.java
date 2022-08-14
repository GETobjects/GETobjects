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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.getobjects.eoaccess.EOAdaptor;
import org.getobjects.eoaccess.EOAdaptorChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/*
 * Needs:
 *   CREATE DATABASE jopetest CHARACTER SET 'utf8';
 *   GRANT ALL PRIVILEGES ON *.* TO jopetest@localhost
 *     IDENTIFIED BY 'jopetest' WITH GRANT OPTION;
 *   CREATE TABLE umlauts ( id INT, text VARCHAR(255) ) CHARACTER SET 'utf8';
 */
public class TSQLUmlauts {

  static final String url =
    "jdbc:mysql://localhost/jopetest?user=jopetest&password=jopetest";
    //"&useUnicode=true&characterEncoding=utf8&characterSetResults=utf8";
  EOAdaptor        adaptor;
  EOAdaptorChannel channel;

  static char[] char1 = new char[] { 72, 101, 223 };
  static char[] char2 = new char[] { 77, 252, 108, 108, 101, 114 };
  static String text1 = new String(char1);
  static String text2 = new String(char2);

  @Before
  public void setUp() {
    this.adaptor = EOAdaptor.adaptorWithURL(url);
    this.channel = this.adaptor.openChannel();
    this.channel.performUpdateSQL("DELETE FROM umlauts");

    // Not necessary:
    //  this.channel.performUpdateSQL("SET character_set_client = 'utf8'");
    //  this.channel.performUpdateSQL("SET character_set_connection = 'utf8'");
    //  this.channel.performUpdateSQL("SET character_set_results = 'utf8'");
    //  this.channel.performUpdateSQL("SET character_set_server = 'utf8'");
  }

  @After
  public void tearDown() {
    this.channel.dispose();
    this.channel = null;
    this.adaptor.dispose();
    this.adaptor = null;
  }

  /* tests */

  @Test
  public void testUmlautsReadWrite1() {
    testUmlautsReadWrite(42, text1, char1);
  }

  @Test
  public void testUmlautsReadWrite2() {
    testUmlautsReadWrite(43, text2, char2);
  }

  /* support */

  protected void printChars(final String _prefix, final char[] _chars) {
    System.err.print(_prefix);
    if (_chars == null || _chars.length == 0) {
      System.err.println("no chars to print ...");
      return;
    }
    for (int i = 0; i < _chars.length; i++)
      System.err.print(" " + (int)_chars[i]);
    System.err.println("");
  }

  protected void testUmlautsReadWrite(final int i, final String _txt, final char[] _chrs) {
    /* first insert a record */

    final Map<String, Object> record = new HashMap<>(2);
    record.put("id", Integer.valueOf(i));
    record.put("text", _txt);
    assertTrue("insert failed", this.channel.insertRow("umlauts", record));

    //this.printChars("original-string: ", _txt.toCharArray());

    /* then retrieve it */

    final List<Map<String, Object>> records =
      this.channel.performSQL("SELECT text FROM umlauts WHERE id = " + i);
    assertNotNull("error performing select",   records);
    assertTrue("did not find inserted record", records.size() == 1);

    /* check field */

    final String text = (String)records.get(0).get("text");
    //this.printChars("result-string:   ", text.toCharArray());

    assertTrue("got no value for 'text' column", text != null);
    assertTrue("'text' column is empty",         text.length() > 0);

    assertEquals("'text' value length does not match: " +
                 text.length() + " / " + _txt.length(),
                 text.length(), _txt.length());
    assertEquals("'text' values do not match", text, _txt);
  }
}
