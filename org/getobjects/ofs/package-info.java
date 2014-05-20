/*
  Copyright (C) 2007-2014 Helge Hess

  This file is part of Go JMI.

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
 * <h3>OFS - Object File System</h3>
 * <p>
 * In Zope OFS maps a filesystem on top of the ZODB object database. Go
 * basically does the reverse. It instantiates certain 'controller' objects for
 * files in the filesystem.<br>
 * It does not directly access the filesystem but rather use the IOFSFileManager
 * interface. This allows the system to hook in virtual filesystems (eg expose
 * zip files as filesystem mount points).
 * <p>
 * Conversion of a filesystem file/dir to an object is done by the
 * OFSRestorationFactory. Which per default looks at the type (file/dir) and the
 * filename extension. The latter are key to the default implementation and you
 * would define custom extensions for additional classes.
 * <p>
 * Restored OFS objects do not need to expose their 'child' hierarchy as-is.
 * E.g. an IMAP4 server entry point could expose the IMAP4 folders dynamically.
 * Some container objects even do a mixture (hold some objects in the local FS
 * and retrieve others from an external server, eg a database or an LDAP
 * server).
 */
package org.getobjects.ofs;
