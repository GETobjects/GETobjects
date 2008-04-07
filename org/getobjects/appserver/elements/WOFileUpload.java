/*
  Copyright (C) 2006-2008 Helge Hess

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

package org.getobjects.appserver.elements;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.fileupload.FileItem;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;

/**
 * WOFileUpload
 * <p>
 * Renders a file upload form element.
 * 
 * <p>
 * Bindings:<pre>
 *   filePath        [io]  - object
 *   data            [io]  - byte[]
 *   string          [in]  - String
 *   size            [io]  - int
 *   contentType     [out] - String
 *   deleteAfterPush [in]  - bool
 *   inputStream     [in]  - InputStream</pre>
 *   
 * Bindings (WOInput):<pre>
 *   id         [in]  - string
 *   name       [in]  - string
 *   value      [io]  - object
 *   readValue  [in]  - object (different value for generation)
 *   writeValue [out] - object (different value for takeValues)
 *   disabled   [in]  - boolean</pre>
 * <p>
 * Note: the 'value' binding contains the raw, adaptor specific value. For
 *       example Apache Commons FileUpload FileItem object.
 */
public class WOFileUpload extends WOInput {
  // TBD: document more
  
  protected WOAssociation filePath;
  protected WOAssociation data;
  protected WOAssociation string;
  protected WOAssociation size;
  protected WOAssociation contentType;
  protected WOAssociation deleteAfterPush;
  protected WOAssociation inputStream;

  public WOFileUpload
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.filePath        = grabAssociation(_assocs, "filePath");
    this.data            = grabAssociation(_assocs, "data");
    this.string          = grabAssociation(_assocs, "string");
    this.size            = grabAssociation(_assocs, "size");
    this.contentType     = grabAssociation(_assocs, "contentType");
    this.deleteAfterPush = grabAssociation(_assocs, "deleteAfterPush");
    this.inputStream     = grabAssociation(_assocs, "inputStream");
    
    if (this.data == null && this.string == null && this.inputStream == null)
      log.info("no data retrieval binding is set!");
    else if (this.data != null) {
      if (this.string != null)
        log.warn("data AND a string bindings are set ...");
      if (this.inputStream != null)
        log.warn("data AND a inputStream bindings are set ...");
      this.string      = null;
      this.inputStream = null;
    }
    else if (this.string != null) {
      if (this.inputStream != null)
        log.warn("string AND a inputStream bindings are set ...");
      this.inputStream = null;
    }
  }
  
  /* process requests */

  @Override
  public void takeValuesFromRequest(final WORequest _rq, final WOContext _ctx) {
    final Object cursor = _ctx.cursor();

    String formName  = this.elementNameInContext(_ctx);
    Object formValue = _rq.formValueForKey(formName);
    
    if (this.writeValue != null)
      this.writeValue.setValue(formValue, cursor);
    
    if (formValue == null) {
      if (this.filePath != null) this.filePath.setValue(null, cursor);
      if (this.data     != null) this.data.setValue(null, cursor);
    }
    else if (formValue instanceof String) {
      /* this happens if the enctype is not multipart/formdata */
      if (this.filePath != null)
        this.filePath.setStringValue((String)formValue, cursor);
    }
    else if (formValue instanceof FileItem) {
      FileItem fileItem = (FileItem)formValue;
      
      if (this.size != null)
        this.size.setValue(fileItem.getSize(), cursor);
      
      if (this.contentType != null)
        this.contentType.setStringValue(fileItem.getContentType(), cursor);
      
      if (this.filePath != null)
        this.filePath.setStringValue(fileItem.getName(), cursor);
      
      /* process content */
      
      if (this.data != null)
        this.data.setValue(fileItem.get(), cursor);
      else if (this.string != null) {
        // TODO: we could support a encoding get-binding
        this.string.setStringValue(fileItem.getString(), cursor);
      }
      else if (this.inputStream != null) {
        try {
          this.inputStream.setValue(fileItem.getInputStream(), cursor);
        }
        catch (IOException e) {
          log.error("failed to get input stream for upload file", e);
          this.inputStream.setValue(null, cursor);
        }
      }
      
      /* delete temporary file */
      
      if (this.deleteAfterPush != null) {
        if (this.deleteAfterPush.booleanValueInComponent(cursor))
          fileItem.delete();
      }
    }
    else
      log.warn("cannot process WOFileUpload value: " + formValue);
  }
  
  
  /* generate response */

  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    if (_ctx.isRenderingDisabled())
      return;

    _r.appendBeginTag("input",
        "type", "file",
        "name", this.elementNameInContext(_ctx));

    final Object cursor = _ctx.cursor(); 
    String lid = this.eid!=null ? this.eid.stringValueInComponent(cursor):null;
    if (lid != null) _r.appendAttribute("id", lid);
    
    if (this.readValue != null) {
      String s = this.readValue.stringValueInComponent(cursor);
      if (s != null)
        _r.appendAttribute("value", s);
    }
    
    if (this.disabled != null) {
      if (this.disabled.booleanValueInComponent(cursor))
        _r.appendAttribute("disabled", "disabled"); // TBD: empty values?!
    }

    if (this.coreAttributes != null)
      this.coreAttributes.appendToResponse(_r, _ctx);
    
    this.appendExtraAttributesToResponse(_r, _ctx);
    // TODO: otherTagString
    
    _r.appendBeginTagClose(_ctx.closeAllElements());
  }
}
