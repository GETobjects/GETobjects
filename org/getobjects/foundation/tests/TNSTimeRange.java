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
package org.getobjects.foundation.tests;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.getobjects.foundation.NSTimeRange;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TNSTimeRange {

  
  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  @Test
  public void testNowOverlap() {
    NSTimeRange r1 = new NSTimeRange(new Date(), 60 * 60 * 1 /* 1h */);
    assertEquals("now not contained in now range",
        r1.contains(new Date()), true);
  }

  @Test
  public void testEmptyRange() {
    NSTimeRange r;
    Date now = new Date();
    
    r = new NSTimeRange((Date)null, (Date)null);
    assertEquals("range is not empty", r.isEmpty(), true);

    r = new NSTimeRange((Date)null, 0);
    assertEquals("range is not empty", r.isEmpty(), true);

    r = new NSTimeRange(now, now);
    assertEquals("range is not empty", r.isEmpty(), true);
    
    r = new NSTimeRange(now, 0);
    assertEquals("range is not empty", r.isEmpty(), true);
    
    r = new NSTimeRange(0, 0);
    assertEquals("range is not empty", r.isEmpty(), true);

    r = new NSTimeRange(new Date(), 60 * 60 * 1 /* 1h */);
    assertEquals("range is empty", r.isEmpty(), false);
  }
}
