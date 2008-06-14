/*
  Copyright (C) 2007 Helge Hess

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
package org.getobjects.jmi;

import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.publisher.IGoLocation;
import org.getobjects.foundation.NSJavaRuntime;
import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.ofs.IGoFolderish;
import org.getobjects.ofs.OFSBaseObject;

public abstract class JMIComponent extends WOComponent {

  public Object[] parentObjects;
  public Object   item;

  public Object clientObject() {
    return this.context().clientObject();
  }
  
  public Object clientJoClass() {
    Object co = this.clientObject();
    
    if (co instanceof OFSBaseObject)
      return ((OFSBaseObject)co).joClassInContext(this.context());
    
    return NSKeyValueCoding.Utility.valueForKey(co, "joClass"); 
  }
  
  public Object activeContainer() {
    Object co = this.clientObject();
    if (co == null) // lookup in traversal path
      return null;
    
    if (co instanceof IGoFolderish)
      return co;
    
    if (co instanceof IGoLocation)
      return ((IGoLocation)co).container();
    
    // TODO: lookup in traversal path
    return null;
  }
  
  public String activeMethod() {
    String[] path = this.context().goTraversalPath().path();
    if (path == null || path.length == 0)
      return null;
    return path[path.length - 1];
  }
  
  public String titleOrName() {
    Object co = this.clientObject();
    String s = NSJavaRuntime.stringValueForKey(co, "NSFileSubject");
    if (s == null || s.length() == 0)
      s = NSJavaRuntime.stringValueForKey(co, "NSFileName");
    return s;
  }
  
  public String viewURL() {
    Object co = this.clientObject();
    
    if (NSJavaRuntime.boolValueForKey(co, "isFolderish")) {
      /* eg: /apps/tasks/-manage => /apps/tasks/ */
      return ".";
    }

    /* eg: /apps/tasks/index.html/-manage => /apps/tasks/index.html */
    return "../" + NSJavaRuntime.stringValueForKey(co, "nameInContainer");
  }
  
  public Object[] parentObjectArray() {
    if (this.parentObjects != null)
      return this.parentObjects;
    
    Object[] objs = this.context().goTraversalPath().objectTraversalPath();
    if (objs == null || objs.length < 2)
      return null;
    
    /* eg: company => index.html => -manage, cut off last two */
    this.parentObjects = new Object[objs.length - 2];
    System.arraycopy(objs, 0, this.parentObjects, 0, this.parentObjects.length);
    
    return this.parentObjects;
  }
  
  public String imageResourcePrefix() {
    return "/-ControlPanel/Products/jmi/";
  }
  
  public String itemIconURL() {
    // TODO: retrieve Icon from product
    //if (this.item instanceof OGoPubWebSite)
    //  return "/-products/jmi/dtree-globe-19x18.gif";
    
    // TODO: well, the OGo icons are a bit dark
    // return "/-products/jmi/folder-open-19x16.gif";
    
    return this.imageResourcePrefix() + 
      (NSJavaRuntime.boolValueForKey(this.item, "isFolderish")
       ? "dtree-folder-18x18.gif" : "dtree-page-18x18.gif");
  }
  public String itemIconOpenURL() {
    // TODO: do in product.plist
    if (this.item == null)
      return null;
    
    // TODO: retrieve Icon from product
    //if (this.item instanceof OGoPubWebSite)
    //  return "/-products/jmi/dtree-globe-19x18.gif";
    
    return this.imageResourcePrefix() + 
    (NSJavaRuntime.boolValueForKey(this.item, "isFolderish")
     ? "dtree-folderopen-18x18.gif" : "dtree-page-18x18.gif");
  }
  
  /* pasteboard */
  
  public JMIPasteboardContents pasteboardContents() {
    String s = this.context().request().cookieValueForKey("jmi.pasteboard");
    return JMIPasteboardContents.contentsForString(s);
  }
  public boolean hasPasteboardContents() {
    return this.pasteboardContents() != null;
  }
}
