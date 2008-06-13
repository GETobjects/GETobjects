/*
  Copyright (C) 2008 Helge Hess

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
  License along with JOPE; see the file COPYING.  If not, write to the
  Free Software Foundation, 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
*/
package org.getobjects.appserver.core;

import java.util.List;

import org.getobjects.foundation.NSObject;

/**
 * WOErrorReport
 * <p>
 * This object is used to capture errors found during form processing. For
 * example if the user entered a number in a date field. The WODateFormatter
 * would raise an exception, which can then be handled by a WOErrorReport for
 * the whole page.
 * <p>
 * Error reports are managed as a stack on the WOContext.
 */
public class WOErrorReport extends NSObject {
  
  protected WOErrorReport       parentReport;
  protected List<WOErrorReport> subReports;
  protected List<WOErrorItem>   errors;
  
  public WOErrorReport() {
  }

  /* accessors */
  
  public void setParentReport(final WOErrorReport _parent) {
    this.parentReport = _parent;
  }
  public WOErrorReport parentReport() {
    return this.parentReport;
  }
  
  
  /* errors */
  
  public List<WOErrorItem> errors() {
    return this.errors;
  }
  public boolean hasErrors() {
    return (this.errors == null || this.errors.size() == 0) ? true : false;
  }
  
  
  /* items */
  
  public class WOErrorItem extends NSObject {
    
    protected String    elementID;
    protected String    name;
    protected Exception exception;
    protected Object    value;
    
    
    /* accessors */

    public String elementID() {
      return this.elementID;
    }
    public String name() {
      return this.name;
    }
    public Exception exception() {
      return this.exception;
    }
    public Object value() {
      return this.value;
    }
    
  }
}
