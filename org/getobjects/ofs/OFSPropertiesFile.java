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
package org.getobjects.ofs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.ResourceBundle;

import org.getobjects.ofs.fs.IOFSFileInfo;

/**
 * OFSPropertiesFile
 * <p>
 * Wraps a Java .properties file, commonly used for localization (as a
 * LocalizableStrings.properties file).
 */
public class OFSPropertiesFile extends OFSJavaObject {
 
  protected ResourceBundle resourceBundle;
  
  /* config file */
  
  public Properties properties() {
    return (Properties)this.object();
  }
  
  public Object primaryLoadObject(IOFSFileInfo _info) throws Exception {
    /* parse it */
    
    InputStream is = this.fileManager.openStreamOnPath(_info.getPath());
    if (is == null) {
      log.error("could not open input stream on OFS node: " + _info);
      return null;
    }
    
    try {
      final Properties lProps = new Properties();
      lProps.load(is);
      return lProps;
    }
    finally {
      if (is != null) {
        try { is.close(); } catch (IOException e) {}
      }
    }
  }
  
  public ResourceBundle resourceBundle() {
    Properties lProps = this.properties();
    if (lProps == null)
      return null;
    
    if (this.resourceBundle == null)
      this.resourceBundle = new FixPropertiesResourceBundle(lProps);
    
    return this.resourceBundle;
  }

  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.object != null) {
      _d.append(" cached=#");
      ((Properties)this.object).size();
    }
  }
  
  
  /* ResourceBundle wrapper */
  
  static class FixPropertiesResourceBundle extends ResourceBundle {
    
    protected Properties props;
    
    public FixPropertiesResourceBundle(Properties _props) {
      this.props = _props;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<String> getKeys() {
      return this.props != null ? (Enumeration)this.props.keys() : null;
    }

    @Override
    protected Object handleGetObject(final String _key) {
      if (_key == null || this.props == null)
        return null;
      
      return this.props.get(_key);
    }
    
  }
}
