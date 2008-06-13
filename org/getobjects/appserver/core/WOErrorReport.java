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

import java.util.ArrayList;
import java.util.List;

import org.getobjects.foundation.NSException;
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
  protected WOErrorReportTrampoline trampoline;
  
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
  
  public List<WOErrorItem> errorsForElementID(final String _eid) {
    if (this.errors == null || _eid == null)
      return null;
    
    List<WOErrorItem> errs = null;
    for (WOErrorItem item: this.errors) {
      if (item.elementID == null) continue;
      if (_eid.equals(item.elementID)) {
        if (errs == null)
          errs = new ArrayList<WOErrorItem>(4);
        errs.add(item);
      }
    }
    return errs;
  }
  public List<WOErrorItem> errorsForName(final String _name) {
    if (this.errors == null || _name == null)
      return null;
    
    List<WOErrorItem> errs = null;
    for (WOErrorItem item: this.errors) {
      if (item.name == null) continue;
      if (_name.equals(item.name)) {
        if (errs == null)
          errs = new ArrayList<WOErrorItem>(4);
        errs.add(item);
      }
    }
    return errs;
  }
  public WOErrorItem errorForElementID(final String _eid) {
    if (this.errors == null || _eid == null)
      return null;
    
    for (WOErrorItem item: this.errors) {
      if (item.elementID != null && _eid.equals(item.elementID))
        return item;
    }
    return null;
  }
  public WOErrorItem errorForName(final String _name) {
    if (this.errors == null || _name == null)
      return null;
    
    for (WOErrorItem item: this.errors) {
      if (item.name != null && _name.equals(item.name))
        return item;
    }
    return null;
  }
  
  public void addErrorItem(final WOErrorItem _item) {
    if (_item == null)
      return;
    
    if (this.errors == null)
      this.errors = new ArrayList<WOErrorItem>(16);
    this.errors.add(_item);
  }
  
  public void addError
    (String _eid, String _name, Object _value, Exception _error)
  {
    WOErrorItem item = new WOErrorItem();
    item.elementID = _eid;
    item.name      = _name;
    item.value     = _value;
    item.exception = _error;
    this.addErrorItem(item);
  }

  public void addError(final String _name, final Object _value) {
    this.addError(null, _name, _value, null);
  }
  
  public void addError(final String _name, final Object _value, String _error) {
    this.addError(null, _name, _value, new NSException(_error));
  }
  
  /**
   * Marks a field with the given name as invalid. This only generates a new,
   * empty erroritem if no error is registered for the field yet.
   * 
   * @param _name - name of the field, eg 'startdate'
   */
  public void markField(final String _name) {
    if (this.errorForName(_name) == null)
      this.addError(null, _name, null, null);
  }
  /**
   * Marks a field with the given name as invalid. This only generates a new
   * erroritem if no error is registered for the field yet.
   * 
   * @param _name  - name of the field, eg 'startdate'
   * @param _value - buggy value of the field, eg 'murks'
   */
  public void markField(final String _name, final Object _value) {
    if (this.errorForName(_name) == null)
      this.addError(null, _name, _value, null);
  }
  
  
  /**
   * Used to navigate to errors using NSKeyValueCoding.
   * Example:<pre>
   *   &lt;div .haserrs="errors.on.amount"&gt;</pre>
   * 
   * @return a trampoline object which can resolve errors
   */
  public WOErrorReportTrampoline on() {
    if (this.trampoline == null)
      this.trampoline = new WOErrorReportTrampoline(this);
    return this.trampoline;
  }

  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    if (this.errors == null)
      _d.append(" no-errors");
    else {
      _d.append(" errors=[");
      
      for (WOErrorItem item: this.errors)
        _d.append(item);
      
      _d.append("]");
    }
    super.appendAttributesToDescription(_d);
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
    
    /* description */
    
    @Override
    public void appendAttributesToDescription(final StringBuilder _d) {
      if (this.elementID != null) {
        _d.append(" eid=");
        _d.append(this.elementID);
      }
      if (this.name != null) {
        _d.append(" name=");
        _d.append(this.name);
      }
      if (this.value != null) {
        _d.append(" value=");
        _d.append(this.value);
      }
      if (this.exception != null) {
        _d.append(" error=");
        _d.append(this.exception);
      }
    }
  }
  
  /* helpers */
  
  public class WOErrorReportTrampoline extends NSObject {
    
    protected WOErrorReport report;
    
    public WOErrorReportTrampoline(final WOErrorReport _report) {
      this.report = _report;
    }
    
    @Override
    public Object valueForKey(final String _key) {
      Object item = this.report.errorForElementID(_key);
      if (item == null) item = this.report.errorsForName(_key);
      return item;
    }
    
    @Override
    public void takeValueForKey(Object _value, String _key) {
      // do nothing
    }
    
    /* description */
    
    @Override
    public void appendAttributesToDescription(final StringBuilder _d) {
      if (this.report != null) {
        _d.append(" report=");
        _d.append(this.report);
      }
      else
        _d.append(" no-report");
    }
  }
}
