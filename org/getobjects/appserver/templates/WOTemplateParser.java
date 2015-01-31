/*
  Copyright (C) 2006-2015 Helge Hess

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

import java.net.URL;
import java.util.List;

import org.getobjects.appserver.core.WOElement;

/**
 * WOTemplateParser
 * 
 * Interface which represents a template parser.
 * The only class (and hence the only template syntax) implementing this in Go
 * is WOHtmlParser.
 * <br>
 * The parser is only responsibly for parsing the syntax. It does not even do
 * the WOElement object construction - this is done by the handler of the
 * parser (via the dynamicElementWithName() method).
 * <p>
 * Do not confuse this class with WOTemplate<b>Builder</b>. The builder also
 * deals with the on-disk structure (eg .wo directory vs .xml file) and returns
 * a WOTemplate element.
 * <p>
 * P.S.: StaticCMS has additional parser for some specific XHTML syntax.
 */
public interface WOTemplateParser {

  public void setHandler(WOTemplateParserHandler _handler);
  
  public List<WOElement> parseHTMLData(URL _data);

  public Exception lastException();
  
}
