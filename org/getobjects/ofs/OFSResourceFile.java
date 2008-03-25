/*
  Copyright (C) 2007-2008 Helge Hess

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
package org.getobjects.ofs;


/**
 * OFSResourceFile
 * <p>
 * Superclass for file objects which are returned as-is, eg images or generic
 * files.
 */
public class OFSResourceFile extends OFSBaseObject {

  // TBD: replace that with a generic facility?! (must allow custom types)
  public static final String[] extToMIME = {
    "html",       "text/html",
    "xhtml",      "application/xhtml+xml",
    
    "txt",        "text/plain",
    "css",        "text/css",
    "js",         "text/javascript",
    "make",       "text/x-makefile",
    "pl",         "text/x-perl",
    
    "gif",        "image/gif",
    "ico",        "image/x-icon",
    "jpg",        "image/jpeg",
    "png",        "image/png",
    
    "pdf",        "application/pdf",
    "sh",         "application/x-sh",
    "sed",        "application/x-sed",
    "gz",         "application/x-gzip",
    "zip",        "application/zip",
    "xul",        "application/vnd.mozilla.xul+xml",
    "csv",        "application/csv",
    
    "xtmpl",      "skyrix/xtmpl",
    "sfm",        "skyrix/form",
  };

  public String mimeTypeForExtension(String _ext) {
    if (_ext == null || _ext.length() == 0)
      return null;
    
    for (int i = 1; i < extToMIME.length; i += 2) {
      if (_ext.equals(extToMIME[i - 1]))
        return extToMIME[i];
    }
    
    int idx = _ext.indexOf('.');
    if (idx != -1)
      return this.mimeTypeForExtension(_ext.substring(idx + 1));
    
    return null;
  }

  public String defaultDeliveryMimeType() {
    String ext = this.pathExtension();
    if (ext == null)
      return null;
    
    if ((ext = this.mimeTypeForExtension(ext)) != null)
      return ext;
    
    return super.defaultDeliveryMimeType();
  }
}
