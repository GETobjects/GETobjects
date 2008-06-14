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

package org.getobjects.appserver.templates;

import java.util.List;
import java.util.Map;

import org.getobjects.appserver.core.WOElement;

public interface WOTemplateParserHandler {
  
  /* parsing state notifications */

  public boolean willParseHTMLData(WOTemplateParser _parser, char[] _data);
  
  public void failedParsingHTMLData
    (WOTemplateParser _parser, char[] _data, Exception _error);
  
  public void finishedParsingHTMLData
    (WOTemplateParser _parser, char[] _data, List<WOElement> _topLevel);
  
  /* factory */
  
  public WOElement dynamicElementWithName
    (String _name, Map<String, String> _attrs, List<WOElement> _children);
}
