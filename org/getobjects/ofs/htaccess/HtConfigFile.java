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
package org.getobjects.ofs.htaccess;

import java.util.ArrayList;
import java.util.List;

import org.getobjects.foundation.NSObject;


/**
 * HtConfigFile
 * <p>
 * Toplevel container. Actually its wrapped into a &lt;Directory&gt;
 * section.
 */
public class HtConfigFile extends NSObject
  implements IHtConfigContainer, IHtConfigNode
{
  
  // TBD: would be nice to store some source name
  protected ArrayList<IHtConfigNode> directives;
  
  public HtConfigFile() {
    this.directives = new ArrayList<IHtConfigNode>(64);
  }
  
  /* managing nodes */

  public void addNode(IHtConfigNode _directive) {
    if (_directive != null)
      this.directives.add(_directive);
  }

  public List<IHtConfigNode> nodes() {
    return this.directives;
  }
  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.directives == null)
      _d.append(" no-directives");
    else if (this.directives.size() == 0)
      _d.append(" empty-directives");
    else if (this.directives.size() == 1) {
      _d.append(" directive=");
      _d.append(this.directives.get(0));
    }
    else {
      _d.append(" #directives=");
      _d.append(this.directives.size());
    }
  }
}
