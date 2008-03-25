/*
  Copyright (C) 2007 Helge Hess

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
package org.getobjects.appserver.publisher;

import org.getobjects.foundation.NSObject;

/**
 * JoPermission
 * <p>
 * Just a set of standard permission names.
 */
public class JoPermission extends NSObject {

  public static final String AccessContentsInformation    = "Access Contents Information";
  public static final String AddDatabaseMethods           = "Add Database Methods";
  public static final String AddDocumentsImagesAndFiles   = "Add Documents, Images, and Files";
  public static final String AddExternalMethods           = "Add External Methods";
  public static final String AddFolders                   = "Add Folders";
  public static final String AddMailHostObjects           = "Add MailHost Objects";
  public static final String AddPythonScripts             = "Add Python Scripts";
  public static final String AddSiteRoots                 = "Add Site Roots";
  public static final String AddUserFolders               = "Add User Folders";
  public static final String AddVersions                  = "Add Versions";
  public static final String AddVocabularies              = "Add Vocabularies";
  public static final String ChangeDatabaseConnections    = "Change Database Connections";
  public static final String ChangeExternalMethods        = "Change External Methods";
  public static final String ChangeImagesAndFiles         = "Change Images and Files";
  public static final String ChangePythonScripts          = "Change Python Scripts";
  public static final String ChangeVersions               = "Change Versions";
  public static final String ChangeBindings               = "Change Bindings";
  public static final String ChangeConfiguration          = "Change Configuration";
  public static final String ChangePermissions            = "Change Permissions";
  public static final String ChangeProxyRoles             = "Change Proxy Roles";
  public static final String DeleteObjects                = "Delete Objects";
  public static final String ManageAccessRules            = "Manage Access Rules";
  public static final String ManageVocabulary             = "Manage Vocabulary";
  public static final String ManageProperties             = "Manage Properties";
  public static final String ManageUsers                  = "Manage Users";
  public static final String OpenCloseDatabaseConnections = "Open/Close Database Connections";
  public static final String QueryVocabulary              = "Query Vocabulary";
  public static final String SaveDiscardVersionChanges    = "Save/Discard Version Changes";
  public static final String TakeOwnership                = "Take Ownership";
  public static final String TestDatabaseConnections      = "Test Database Connections";
  public static final String UndoChanges                  = "Undo Changes";
  public static final String UseDatabaseMethods           = "Use Database Methods";
  public static final String UseMailHostServices          = "Use MailHost Services";
  public static final String View                         = "View";
  public static final String ViewHistory                  = "View History";
  public static final String ViewManagementScreens        = "View Management Screens";
  public static final String WebDAVAccess                 = "WebDAV Access";
  public static final String WebDAVLockItems              = "WebDAV Lock Items";
  public static final String WebDAVUnlockItems            = "WebDAV Unlock Items";
}
