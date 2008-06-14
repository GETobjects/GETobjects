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

package org.getobjects.appserver.templates;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.getobjects.appserver.core.WOResourceManager;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * WOxTemplateBuilder
 * <p>
 * This class parses templates from Go XML files (.wox templates).
 * <p>
 * Note: this is not finished yet?
 */
public class WOxTemplateBuilder extends WOTemplateBuilder {
  // TBD: complete me
  
  static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
  
  /* entry points */
  
  public WOTemplate buildTemplate
    (URL _template, URL _bindings, WOResourceManager _rm)
  {
    if (_bindings != null)
      log.warn("got bindings for WOx template?");

    return this.buildTemplate(_template);
  }
  
  public WOTemplate buildTemplate(URL _url) {
    InputStream in;
    
    try {
      in = _url.openStream();
    }
    catch (IOException e) {
      log.error("could not open stream to read WOx from URL: " + _url);
      return null;
    }

    Document d;
    if ((d = this.parseDOM(in, _url.toString())) == null)
      return null;
    
    return this.buildTemplate(d, null);
  }
  
  public WOTemplate buildTemplate(String _template) {
    if (_template == null)
      return null;
    
    ByteArrayInputStream in = null;
    try {
      in = new ByteArrayInputStream(_template.getBytes("utf8"));
    }
    catch (UnsupportedEncodingException uee) {
      log.error("could not convert template to UTF-8", uee);
      return null;
    }
    if (in == null)
      return null;

    Document d;
    if ((d = this.parseDOM(in, "<string>")) == null)
      return null;
    
    return this.buildTemplate(d, null);
  }
  
  
  /* primary template building class */
  
  public WOTemplate buildTemplate(Document _doc, URL _url) {
    /*
     * TODO:
     * WOxElemBuilder builder = this.builderForDocument(_doc);
     * root = builder.buildTemplateFromDocument(_doc);
     * template = new WOTemplate(_url, ..., root);
     */
    System.err.println("TODO: build template from DOM: " + _doc);
    // TODO: implement me
    return null;
  }
  
  
  /* XML processing */
  
  protected DocumentBuilder createDocumentBuilder() {
    try {
       return dbf.newDocumentBuilder();
    }
    catch (ParserConfigurationException e) {
      // TODO: add some log
      return null;
    }
  }
  
  protected Document parseDOM(InputStream _in, String _systemId) {
    DocumentBuilder db;
    
    if ((db = this.createDocumentBuilder()) == null)
      return null;

    try {
      return db.parse(_in, _systemId);
    }
    catch (SAXException e) {
      // TODO: add some log
      return null;
    }
    catch (IOException e) {
      // TODO: add some log
      return null;
    }
  }
}
