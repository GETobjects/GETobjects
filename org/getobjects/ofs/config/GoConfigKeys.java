/*
  Copyright (C) 2008-2014 Helge Hess <helge.hess@opengroupware.org>

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
package org.getobjects.ofs.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getobjects.foundation.NSObject;

/**
 * Those keys are set in the configuration context by the configuration files.
 * The keys imply standard semantics which can be implemented by OFS objects.
 */
public class GoConfigKeys {
  private GoConfigKeys() {} // do not instantiate
  
  /* config processing */

  public static final String AccessFileName     = "accessfilename";
  
  public static final String AllowOverride      = "allowoverride";
  public static final String AllowOverride_None = "none";
  public static final String AllowOverride_All  = "all";
  
  public static final String Environment        = "env";
  public static final Object Environment_Remove = new Object();
  
  /* sections */

  public static final String Files              = "files";
  public static final String FilesMatch         = "filesmatch";
  public static final String Directory          = "directory";
  public static final String DirectoryMatch     = "directorymatch";
  public static final String Location           = "location";
  public static final String LocationMatch      = "locationmatch";
  public static final String AliasMatchName     = "aliasmatchname";
  
  /* server configuration */

  public static final String DocumentRoot       = "documentroot";
  public static final String Options            = "options";
  
  /* authentication */

  public static final String AuthType           = "authtype";
  public static final String AuthName           = "authname";
  
  /* authorization */

  public static final String Require            = "require";
  public static final String Require_ValidUser  = "valid-user";
  public static final String Require_User       = "user";
  public static final String Require_Group      = "group";
  public static final String Satisfy            = "satisfy";
  public static final String Satisfy_Any        = "any";
  public static final String Satisfy_All        = "all";
  public static final String Allow              = "allow";
  public static final String Deny               = "deny";
  public static final String Order              = "order";
  
  /* Go */

  public static final String Bindings           = "bindings";
  public static final String EODatabase         = "eodatabase";
  public static final String EOEntity           = "eoentity";
  public static final String EOQualifier        = "eoqualifier";
  public static final String EOMergeQualifier   = "eomergequalifier";
  public static final String EOSortOrdering     = "eosortordering";
  public static final String EOAdaptorURL       = "eoadaptorurl";

  
  public static class KeyMatchEntry extends NSObject {
    
    protected final Pattern pattern;
    protected final String  replacement;
    
    public KeyMatchEntry(final Pattern _pattern, final String  _replacement) {
      this.pattern     = _pattern;
      this.replacement = _replacement;
    }

    public String match(final String _name) {
      if (this.pattern == null || _name == null)
        return null;
      
      Matcher matcher = this.pattern.matcher(_name);
      
      // TBD: we could support group references in the name, eg $1, $2 etc
      
      if (matcher.matches())
        return this.replacement;
      
      return null;
    }
    
    /* description */

    @Override
    public void appendAttributesToDescription(final StringBuilder _d) {
      super.appendAttributesToDescription(_d);
      
      if (this.pattern != null) {
        _d.append(" pat=");
        _d.append(this.pattern);
      }
      else
        _d.append(" no-pattern?");
      
      if (this.replacement != null) {
        _d.append(" replace=");
        _d.append(this.replacement);
      }
      else
        _d.append(" no-replacement?");
    }
  }
}
