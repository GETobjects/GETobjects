/*
  Copyright (C) 2006-2007 Helge Hess

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

package org.getobjects.samples.HelloWorld;

import org.getobjects.appserver.core.WOActionResults;
import org.getobjects.appserver.core.WOComponent;

/**
 * A small component which demonstrates the WOFileUpload template element. The
 * most important thing for file-upload is to remember to set the
 * "multipart/form-data" enctype in the WOForm so that the browser actually
 * transmits the data.
 * <p>
 * WOFileUpload requires the Apache commons-fileupload-1.2.jar.
 */
public class UploadPage extends WOComponent {
  
  public byte[] data; // this value is bound to WOFileUplaod
  
  /* accessors */
  
  /**
   * Just returns the length of the uploaded data. This is done as a method
   * because byte[] is currently unsupported in KVC.
   */
  public int dataLength() {
    return this.data != null ? this.data.length : -1;
  }
  
  /* actions */

  /**
   * This action method does not do anything. Its triggered when the WOForm is
   * submitted (in direct action mode). Because the method is POST, the bindings
   * of the page will get pushed into the component (that is the WOFileUpload
   * will fill the 'data' ivar).
   * <br>
   * Then, when the page is rerendered the template will be able to render the
   * now filled data ivar.
   */
  public WOActionResults postAction() {
    return this;
  }
}
