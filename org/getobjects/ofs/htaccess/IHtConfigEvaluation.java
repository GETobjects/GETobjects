/*
  Copyright (C) 2008 Helge Hess

  This file is part of JOPE.

  JOPE is free software; you can redistribute it and/or modify it under
  the terms of the GNU Lesser General Public License as published by the
  Free Software Foundation; either version 2, or (at your option) any
  later version.

  JOPE is distributed in the hope that it will be useful, but WITHOUT ANY
  WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
  License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with JOPE; see the file COPYING.  If not, write to the
  Free Software Foundation, 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
*/
package org.getobjects.ofs.htaccess;

import java.util.Map;

/**
 * This is implemented by 'directive evaluation objects', that is object in the
 * 'eval' subpackage, eg <code>FilesMatch</code>.
 */
public interface IHtConfigEvaluation {

  /**
   * Execute the given <code>_directive</code> with the given <code>_cfg</code>
   * dictionary in the given <code>_lookupCtx</code>.
   * 
   * @param _builder   - the HtConfigBuilder which is active
   * @param _directive - the HtConfigDirective to be executed
   * @param _cfg       - the configuration object to be filled/modified/...
   * @param _lookupCtx - the configuration lookup context (eg path/location)
   * @return null if everything went fine, an Exception otherwise
   */
  public Exception evaluateDirective
    (final HtConfigBuilder _builder, HtConfigDirective _directive,
     final Map<String, Object> _cfg, final Object _lookupCtx);

}
