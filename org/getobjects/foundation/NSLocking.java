/*
  Copyright (C) 2006-2025 Helge Hess and Contributors.

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

package org.getobjects.foundation;

public interface NSLocking {
  
  /* number of milliseconds in each of the units */
  static long OneSecond  = 1000;
  static long OneMinute  = 60000;
  static long OneHour    = 360000;
  static long OneDay     = OneHour * 24;
  static long OneWeek    = OneDay  * 7;
  static long OneYear    = OneDay  * 365;
  static long OneCentury = OneYear * 100;
  
  public void lock();
  public void unlock();
}

/*
  Local Variables:
  c-basic-offset: 2
  tab-width: 8
  End:
*/
