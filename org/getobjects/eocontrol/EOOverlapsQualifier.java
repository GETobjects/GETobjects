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
package org.getobjects.eocontrol;

import java.util.Calendar;
import java.util.Date;

import org.getobjects.foundation.NSTimeRange;

/**
 * EOOverlapsQualifier
 * <p>
 * Important: startKey/endKey must be ordered (start < end).
 */
public class EOOverlapsQualifier extends EOQualifier
  implements EOQualifierEvaluation, EOExpressionEvaluation
{
  // TBD: check semantics and implementation
  protected EOKey       startKey;
  protected EOKey       endKey;
  protected EOKey       rangeKey;
  protected NSTimeRange range;
  
  public EOOverlapsQualifier
    (final EOKey _startKey, final EOKey _endKey, final NSTimeRange _range)
  {
    this.startKey = _startKey;
    this.endKey   = _endKey;
    this.range    = _range;
  }
  
  /* accessors */

  public String startKey() {
    return this.startKey != null ? this.startKey.key() : null;
  }
  public String endKey() {
    return this.endKey != null ? this.endKey.key() : null;
  }

  public EOKey startKeyExpression() {
    return this.startKey;
  }
  public EOKey endKeyExpression() {
    return this.endKey;
  }
  public NSTimeRange range() {
    return this.range;
  }
  
  /* rewrite as plain qualifier */
  
  public EOQualifier rewriteAsPlainQualifier() {
    if (this.rangeKey != null) {
      log.error("range keys cannot be rewritten to plain qualifiers: " + this);
      return null;
    }
    if (this.range == null) /* no range to check */
      return null;
    
    /* remember, this requires proper ordering of the range keys */
    EOKeyValueQualifier q1 = null;
    EOKeyValueQualifier q2 = null;
    
    if (this.startKey != null) {
      /* To overlap the beginning of the appointment must be before the end
       * of the range. If its greater, the range is before the apt.
       * The end of the range is EXCLUSIVE.
       */
      q1 = new EOKeyValueQualifier(
          this.startKey,
          EOQualifier.ComparisonOperation.LESS_THAN,
          this.range.toDate());
    }
    if (this.endKey != null) {
      /* To overlap the end of the appointment must be after the start
       * of the range. If its smaller, the range is after the apt.
       * The beginning of the range is INCLUSIVE.
       */
      q2 = new EOKeyValueQualifier(
          this.endKey,
          EOQualifier.ComparisonOperation.GREATER_THAN_OR_EQUAL,
          this.range.fromDate());
    }
    
    if (q1 == null && q2 == null) return null; /* is this reasonable? */
    if (q2 == null) return q1;
    if (q1 == null) return q2;
    return new EOAndQualifier(q1, q2);
  }  
  
  /* evaluation */

  public boolean evaluateWithObject(final Object _object) {
    if (_object == null)
      return false;
    
    if (this.range == null)
      return false;
    
    Object startValue =
      this.startKey != null ? this.startKey.valueForObject(_object) : null;
    Object endValue =
      this.endKey != null ? this.endKey.valueForObject(_object) : null;
    NSTimeRange rangeValue = (NSTimeRange)
      (this.rangeKey != null ? this.rangeKey.valueForObject(_object) : null);
    
    if (startValue == null && endValue == null && rangeValue == null)
      return false;
    
    if (rangeValue != null)
      return rangeValue.overlaps(this.range);
    
    if (startValue != null) {
      // TBD: check start/end ordering and reverse when necessary?
      if (startValue instanceof Calendar)
        rangeValue = new NSTimeRange((Calendar)startValue, (Calendar)endValue);
      else if (startValue instanceof Date)
        rangeValue = new NSTimeRange((Date)startValue, (Date)endValue);
    }
    else {
      if (endValue instanceof Calendar)
        rangeValue = new NSTimeRange(null, (Calendar)endValue);
      else if (endValue instanceof Date)
        rangeValue = new NSTimeRange(null, (Date)endValue);
    }

    if (rangeValue != null)
      return rangeValue.overlaps(this.range);
    
    return false;
  }

  public Object valueForObject(final Object _object) {
    return this.evaluateWithObject(_object);
  }

}
