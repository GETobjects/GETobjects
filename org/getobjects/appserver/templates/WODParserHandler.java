/*
  Copyright (C) 2006-2007 Helge Hess

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

package org.getobjects.appserver.templates;

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;

/**
 * WODParserHandler
 * <p>
 * This is the callback interface provided by the WODParser class.
 * The most prominent implementation is WOWrapperTemplateBuilder.
 */
public interface WODParserHandler {
  
  /**
   * Called by the WODParser if it starts parsing a given string.
   * 
   * @param _p    - the parser instance
   * @param _data - the data which shall be parsed
   * @return true if the parsing should be done, false to abort
   */
  public boolean willParseDeclarationData(WODParser _p, char[] _data);
  
  public void    finishedParsingDeclarationData
                   (WODParser _p, char[] _data, Map _decls);
  public void    failedParsingDeclarationData
                   (WODParser _p, char[] _data, Exception _error);
  
  /**
   * This is called by the WODParser to create an association for a constant
   * value.
   * <p>
   * The value can be a String, a Number, a property list object, or some
   * other basic stuff ;-)
   *  
   * @param _p     - a reference to the WOD parser
   * @param _value - the value which has been parsed
   * @return a WOAssociation (most likely a WOValueAssocation)
   */
  public WOAssociation makeAssociationWithValue(WODParser _p, Object _value);
  
  /**
   * This is called by the WODParser to create an association for a dynamic
   * value (a keypath binding).
   * <p>
   * The value is the string containing the keypath.
   *  
   * @param _p  - a reference to the WOD parser
   * @param _kp - the String containing the keypath (eg person.lastname)
   * @return a WOAssociation (most likely a WOKeyPathAssocation)
   */
  public WOAssociation makeAssociationWithKeyPath(WODParser _p, String _kp);
  
  /**
   * Called by the WODParser once it has parsed the data of a WOD entry
   * like:<pre>
   *   Frame: MyFrame {
   *     title = "Welcome to Hola";
   *   }</pre>
   * The parser stores the result of this method in a Map under the _cname
   * (<code>Frame</code>). This Map is queried after the .wod has been
   * parsed. The parser does not care about the type of the object being
   * returned, it just stores it.
   * <p>
   * WOWrapperTemplateBuilder returns a WODFileEntry object.
   * 
   * @param _p       - the parser
   * @param _cname   - the name of the element (<code>Frame</code>)
   * @param _entry   - the Map containing the bindings (String-WOAssociation)
   * @param _clsname - the name of the component (<code>MyFrame</code>)
   * @return an object representing the WOD entry
   */
  public Object makeDefinitionForComponentNamed
                  (WODParser _p, String _cname, Map _entry, String _clsname);
}
