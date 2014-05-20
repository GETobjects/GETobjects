/*
  Copyright (C) 2014 Helge Hess

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
/**
 * <h3>htaccess</h3>
 * <p>
 * Classes for dealing with htaccess files. We want to preserve as much of the
 * structure of the file as possible so that we can use the parser to edit
 * htaccess files (read/save, not just to interpret them).
 *
 * <h4>HtConfigBuilder</h4>
 * This object 'executes' the directives contained in a HtConfigFile against
 * a given lookup context.<br>
 * The directives are the IHtConfigEvaluation objects contained in the 'eval'
 * subpackage.
 *
 * <h4>HtConfigSection</h4>
 * This is a collection of directives, eg a 
 * &lt;FilesMatch&gt;...&lt;/FilesMatch&gt;.
 *
 * <h4>HtConfigDirective</h4>
 * A parsed directive. Has a name (eg SetHandler) and an array of String[]
 * arguments.<br>
 * The actual processing of the directive is done by an 'IHtConfigEvaluation'
 * objects. Those processing objects are stored in the 'eval' subpackage.
 *
 * <h4>Sample</h4>
 * <p>
 * Configure authentication and provide context info to an
 * OFSDatabaseDataSourceFolder (e.g. contacts.jods/):
 * <pre>
 *   AuthType      WOSession
 *   AuthName      "YoYo"
 *   AuthLoginURL  /yoyo/index
 *   
 *   EOEntity    Persons
 *   EOQualifier "type IS NULL OR (type != 'NSA')"
 *
 *   &lt;LocationMatch "^.+/persons/\d+.*"&gt;
 *     EOQualifier "id = $configObject.nameInContainer"
 *   &lt;/LocationMatch&gt;
 * </pre>
 */
package org.getobjects.ofs.htaccess;
