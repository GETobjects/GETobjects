/*
  Copyright (C) 2008 Helge Hess <helge.hess@opengroupware.org>

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
package org.getobjects.samples.HelloCSV;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.getobjects.appserver.core.WOComponent;
import org.getobjects.foundation.csv.CSVParser;
import org.getobjects.foundation.csv.CSVStrategy;


/**
 * Main is the 'default' component of a regular Go application. This is what
 * you get if you enter the root /App URL.
 */
public class Main extends WOComponent {
  
  public byte[]  data; // this value is bound to WOFileUplaod

  public boolean ignoreLeadingWS  = true;
  public boolean interpretUnicode = false;
  public boolean ignoreEmptyLines = true;
  
  public String delimiter    = ",";
  public String encapsulator = "\"";
  public String commentStart = "";
  
  public String charset = "Cp850";
  
  public String error;
  
  /* actions */

  /**
   * This is triggered when the WOForm is submitted (in direct action mode).
   * Because the method is POST, the bindings of the page will get pushed into
   * the component (that is the WOFileUpload will fill the 'data' ivar).
   * <p>
   * The most important thing for file-upload is to remember to set the
   * "multipart/form-data" enctype in the WOForm so that the browser actually
   * transmits the data.
   */
  public Object postAction() {
    char cdelim, cencap, ccom;
    
    if (this.data == null || this.data.length == 0)
      return null; /* stay on page */
    
    /* setup CSV parsing strategy */
    
    cdelim = this.delimiter.length()    > 0 ? this.delimiter.charAt(0)    : 0;
    cencap = this.encapsulator.length() > 0 ? this.encapsulator.charAt(0) : 0;
    ccom   = this.commentStart.length() > 0 ? this.commentStart.charAt(0) : 0;
    
    CSVStrategy csvConfig = new CSVStrategy(
        cdelim, /* delimiter */
        cencap, /* encapsulator */
        ccom,   /* commentStart */
        this.ignoreLeadingWS,   /* ignore leading WS */
        this.interpretUnicode,  /* do not interpret unicode */
        this.ignoreEmptyLines); /* ignore empty lines */
    
    /* parse */
    
    InputStream fis    = new ByteArrayInputStream(this.data);
    Reader reader = null;

    try {
      reader = new InputStreamReader(fis, this.charset);
    }
    catch (UnsupportedEncodingException e1) {
      this.error = "Unsupported file encoding";
      return null;
    }


    CSVParser   parser = new CSVParser(reader, csvConfig);
    
    String[][] lines = null;
    try {
      lines = parser.getAllValues();
      fis.close();
    }
    catch (Exception e) {
      this.error = e.getMessage();
      return null;
    }
    
    /* push result into session, so that it sticks */
    this.session().takeValueForKey(lines, "lines");
    
    return this;
  }
}
