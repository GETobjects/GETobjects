/*
  Copyright (C) 2007 Helge Hess

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
package org.getobjects.appserver.core;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import org.getobjects.foundation.UString;

/*
 * WOExternalResourceManager
 *
 * TBD: document
 * TBD: implement ;-)
 *
 * Pattern:
 *   templateDirPattern: $home/templates/$name
 *   stringsDirPattern:  $home/translations/$name
 *   wwwDirPattern:      /var/www/$name
 *
 * OGo resource pathes:
 *   /usr/share/opengroupware.org-1.1/templates/BundleName/*.[html|wod|wox]
 *   /usr/share/opengroupware.org-1.1/translations/Language.lproj/BundleName.str
 *   /usr/share/opengroupware.org-1.1/www/Language.lproj/*.[gif|css|...]
 * Schema:
 *   PREFIX/APPNAME-APPVERSION/templates/BUNDLE/RESOURCE
 *   PREFIX/APPNAME-APPVERSION/translations/BUNDLE/RESOURCE
 *   PREFIX/APPNAME-APPVERSION/www/LANGUAGE.lproj/RESOURCE
 * Themes
 *   PREFIX/APPNAME-APPVERSION/templates/Themes/THEME/BUNDLE/RESOURCE
 */
public class WOExternalResourceManager extends WOResourceManager {

  protected String[] prefixList;
  protected String   templateDirPattern;
  protected String   stringsDirPattern;
  protected String   wwwDirPattern;

  protected File resourceDir;
  protected File wwwDir;

  public WOExternalResourceManager(final boolean _enableCaching) {
    super(_enableCaching);
  }

  /* labels */

  @Override
  public ResourceBundle stringTableWithName
    (String _name, final String _fwname, final String[] _langs)
  {
    if (_name == null) _name = "LocalizableStrings";

    // TBD
    System.err.println("asked for stringtable " + _name + " in " + _fwname +
        " langs: " + UString.componentsJoinedByString(_langs, ","));
    return null;
  }

  /* internal resources */

  @Override
  public URL urlForResourceNamed(final String _name, final String[] _langs) {
    System.err.println("asked for resource " + _name +
        " langs: " + UString.componentsJoinedByString(_langs, ","));
    // TBD
    return null;
  }

  /* external resources */

  @Override
  public String urlForResourceNamed
    (final String _name, final String _fwname, final List<String> _langs, final WOContext _ctx)
  {
    System.err.println("asked for URL " + _name + " in " + _fwname +
        " langs: " + UString.componentsJoinedByString(_langs, ","));
    // TBD
    return null;
  }

  /* class lookup */

  @Override
  public Class lookupClass(final String _name) {
    /* we cannot discover classes, only resources */
    return null;
  }

  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
  }
}
