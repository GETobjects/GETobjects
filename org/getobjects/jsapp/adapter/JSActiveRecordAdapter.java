/*
 * Copyright (C) 2008 Helge Hess
 *
 * This file is part of Go.
 *
 * Go is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2, or (at your option) any later version.
 *
 * Go is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JOPE; see the file COPYING. If not, write to the Free Software
 * Foundation, 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.getobjects.jsapp.adapter;

import org.getobjects.eoaccess.EOActiveRecord;
import org.getobjects.eoaccess.EOEntity;
import org.mozilla.javascript.Scriptable;

public class JSActiveRecordAdapter extends JSEntityObjectAdapter {
  private static final long serialVersionUID = 1L;

  public JSActiveRecordAdapter() {
  }

  public JSActiveRecordAdapter
    (Scriptable _scope, Object _javaObject, Class _type)
  {
    super(_scope, _javaObject, _type);
  }

  public JSActiveRecordAdapter
    (Scriptable _scope, Object _javaObject, Class _type, boolean _isAdapter)
  {
    super(_scope, _javaObject, _type, _isAdapter);
  }


  /* accessors */
  
  @Override
  public String getClassName() {
    return "JSActiveRecordAdapter"; 
  }
  
  
  /* entity */
  
  public EOEntity entity() {
    return ((EOActiveRecord)this.javaObject).entity();
  }
}
