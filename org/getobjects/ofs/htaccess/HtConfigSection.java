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

import java.util.ArrayList;
import java.util.List;

/**
 * HtConfigSection
 * <p>
 * This is a config directive which maintains a list of subdirectives, aka
 * a section. Example:<pre>
 *   &lt;FilesMatch *.gif&gt; ... &lt;/FilesMatch&gt;</pre>
 */
public class HtConfigSection extends HtConfigDirective
  implements IHtConfigContainer
{

  protected ArrayList<IHtConfigNode> directives;

  public HtConfigSection(String _name, String[] _args) {
    super(_name, _args);
    this.directives = new ArrayList<IHtConfigNode>(16);
  }
  
  /* managing nodes */
  
  public void addNode(IHtConfigNode _node) {
    if (_node != null)
      this.directives.add(_node);
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
