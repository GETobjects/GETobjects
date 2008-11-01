/*
  Copyright (C) 2008 Helge Hess

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
package org.getobjects.weextensions;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.Date;
import java.util.Map;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.FopFactory;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.foundation.NSXmlEntityTextCoder;
import org.getobjects.foundation.UObject;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * WEApplyFOP
 * <p>
 * This elements renders its template into a String buffer, which is then
 * parsed as a set of XSL:FO (formatting objects) tags.
 * The template is embedded in a proper "root" XSL:FO tag, the default
 * namespace is defined to be be the XSL/Format namespace. Which means that
 * you can use the XSL:FO tags w/o a prefix (just &lt;block&gt; instead of
 * &lt;fo:block&gt;, etc).
 * 
 * <p>
 * Performance:
 * This approach is not exactly the fasted way to produce the PDF. The element
 * first renders all XSL:FO instructions into a single String. Since, for
 * large reports, the instructions can become pretty huge, this takes a lot
 * of memory.
 * The String is then parsed as XML by FOP and the PDF is written into a
 * temporary byte buffer. So the PDF is also held in memory before it gets
 * delivered.
 * 
 * <p>
 * Example:<pre>
 *   &lt;wo:WEApplyFOP&gt;
 *     &lt;layout-master-set&gt;
 *       &lt;simple-page-master master-name="a4"
 *           page-height="29.7cm"  page-width="21cm"
 *           margin-top="1cm"      margin-bottom="2cm"
 *           margin-left="2.5cm"   margin-right="2.5cm">
 *         &lt;region-body region-name="page-content"/&gt;
 *       &lt;/simple-page-master&gt;
 *     &lt;/layout-master-set&gt;
 *     
 *     &lt;page-sequence master-reference="a4"&gt;
 *       &lt;flow flow-name="page-content"&gt;
 *         &lt;block font-size="18pt"
 *              font-family="sans-serif" line-height="24pt"
 *              space-after.optimum="15pt"
 *              color="white" background-color="blue"
 *              text-align="center" padding-top="3pt"
 *              font-variant="small-caps"&gt;
 *           &lt;wo:str value="$title" /&gt;
 *         &lt;/block&gt;
 *       &lt;/flow&gt;
 *     &lt;/page-sequence&gt;
 *   &lt;/wo:WEApplyFOP&gt;</pre>
 * 
 * <p>
 * Bindings:
 * <pre>
 *   data        [out] - byte[] - receives the rendering result 
 *   response    [out] - WOResponse - receives the rendering result 
 *   mimeType    [in]  - String - output format, eg 'application/pdf'
 *   disposition [in]  - String - content-disposition
 *   filename    [in]  - String - content-disposition filename
 *   error       [out] - Exception - any FOP error</pre>
 */
public class WEApplyFOP extends WODynamicElement {
  
  protected static FopFactory fopFactory = FopFactory.newInstance();
  
  protected WOAssociation data;
  protected WOAssociation response;
  protected WOAssociation mimeType;
  protected WOAssociation disposition;
  protected WOAssociation filename;
  protected WOAssociation error;

  /* FOUserAgent info */
  protected WOAssociation baseURL;
  protected WOAssociation producer;
  protected WOAssociation creator;
  protected WOAssociation author;
  protected WOAssociation creationDate;
  protected WOAssociation title;
  protected WOAssociation keywords;
  protected WOAssociation targetResolution;
  
  protected WOElement     template;

  public WEApplyFOP
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.data             = grabAssociation(_assocs, "data");
    this.response         = grabAssociation(_assocs, "response");
    this.mimeType         = grabAssociation(_assocs, "mimeType");
    this.disposition      = grabAssociation(_assocs, "disposition");
    this.filename         = grabAssociation(_assocs, "filename");
    this.response         = grabAssociation(_assocs, "response");

    this.baseURL          = grabAssociation(_assocs, "baseURL");
    this.producer         = grabAssociation(_assocs, "producer");
    this.creator          = grabAssociation(_assocs, "creator");
    this.author           = grabAssociation(_assocs, "author");
    this.creationDate     = grabAssociation(_assocs, "creationDate");
    this.title            = grabAssociation(_assocs, "title");
    this.keywords         = grabAssociation(_assocs, "keywords");
    this.targetResolution = grabAssociation(_assocs, "targetResolution");
    
    this.template = _template;
  }
  
  /* generate response */

  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    final Object cursor = _ctx != null ? _ctx.cursor() : null;

    String format = this.mimeType != null
      ? this.mimeType.stringValueInComponent(cursor)
      : null;
    if (format == null) format = "application/pdf";
    
    final int defaultOutputCapacity = 64 * 1024; // 64k
    
    /* first generate the XSL:FO instructions contained in the template */
    
    String fo = this.renderFOInContext(_ctx);
    
    
    /* convert the FO response into an XML input stream */

    InputSource xmlInput = new InputSource(new StringReader(fo));
    fo = null; /* release memory early, can be quite large! */
    
    
    /* The iText book says browsers do not deal well with missing
     * content-lengths, so we can't stream the PDF directly.
     * Technically it would probably be best to stream to a file and then
     * deliver that on the socket using sendfile().
     */
    ByteArrayOutputStream bas =
      new ByteArrayOutputStream(defaultOutputCapacity);


    /* generate PDF */
    
    Exception lError  = null;
    byte[]    result = null;
    
    synchronized (fopFactory) { /* FOP is not thread safe?! */
      
      /* configure user agent */

      FOUserAgent ua = fopFactory.newFOUserAgent();
      String v;
      
      // TBD: kinda crappy
      if (this.baseURL != null) {
        if ((v = this.baseURL.stringValueInComponent(cursor)) != null)
          ua.setBaseURL(v);
      }
      if (this.producer != null) {
        if ((v = this.producer.stringValueInComponent(cursor)) != null)
          ua.setProducer(v);
      }
      if (this.creator != null) {
        if ((v = this.creator.stringValueInComponent(cursor)) != null)
          ua.setCreator(v);
      }
      if (this.author != null) {
        if ((v = this.author.stringValueInComponent(cursor)) != null)
          ua.setAuthor(v);
      }
      if (this.creationDate != null) {
        Date d = (Date)this.creationDate.valueInComponent(cursor);
        if (d != null) ua.setCreationDate(d);
      }
      if (this.title != null) {
        if ((v = this.title.stringValueInComponent(cursor)) != null)
          ua.setTitle(v);
      }
      if (this.keywords != null) {
        if ((v = this.keywords.stringValueInComponent(cursor)) != null)
          ua.setKeywords(v);
      }
      if (this.targetResolution != null) {
        Object o = this.targetResolution.stringValueInComponent(cursor);
        if (o instanceof String)
          o = new Double(Double.parseDouble((String)o));
        if (o instanceof Number)
          ua.setTargetResolution(((Number)o).floatValue());
        else if (o != null)
          delog.error("unexpected targetResolution value: " + o);
      }


      /* process content using FOP */
      
      try {
        ContentHandler fop =
          fopFactory.newFop(format, ua, bas).getDefaultHandler();

        XMLReader reader = XMLReaderFactory.createXMLReader();
        reader.setContentHandler(fop);
        reader.parse(xmlInput);

        fop    = null;
        reader = null;
      }
      catch (Exception e) {
        lError = e;
        if (this.error == null)
          delog.error("FOP exception during rendering: " + this, e);
      }
    }

    /* extract result array */
    
    if (lError == null)
      result = bas.toByteArray();
    
    /* release memory early, can be quite large! */
    bas = null;
    
    
    /* handle result */
    
    if (this.error != null)
      this.error.setValue(lError, cursor);
        
    if (this.data != null)
      this.data.setValue(result, cursor);
    
    if (this.data == null || this.response != null) {
      if (lError != null && this.error == null && result == null) {
        _r.appendContentString("<h2>FOP Error</h2><pre>");
        _r.appendContentHTMLString(lError.toString());
        _r.appendContentString("</pre>");
      }
      else {
        WOResponse or = this.response != null
          ? new WOResponse(_ctx != null ? _ctx.request() : null)
          : _r; /* render directly */

        String d = this.contentDispositionInContext(_ctx);
        if (d != null) or.setHeaderForKey(d, "content-disposition");

        or.setHeaderForKey("close",            "connection");
        or.setHeaderForKey(format,             "content-type");
        or.setHeaderForKey("" + result.length, "content-length");
        or.setContent(result);

        if (this.response != null)
          this.response.setValue(or, cursor);

        or = null;
      }
    }
    
    result = null;
  }
  
  /**
   * Returns the content-disposition for the response, for example:<pre>
   *   inline; filename=test.pdf</pre>
   * 
   * @param _ctx - the WOContext
   * @return a content-disposition value, or null
   */
  public String contentDispositionInContext(final WOContext _ctx) {
    if (this.disposition == null && this.filename == null)
      return null;
    
    String d = null;
    String f = null;
      
    if (this.disposition != null)
      d = this.disposition.stringValueInComponent(_ctx.cursor());
    if (this.filename != null)
      f = this.filename.stringValueInComponent(_ctx.cursor());

    if (UObject.isEmpty(d)) d = "inline";
    
    if (UObject.isNotEmpty(f))
      d += "; filename=" + f; // TBD: quoting?
    
    return d;
  }
  
  /**
   * This method renders the elements template into a separate response,
   * and then extracts that content as a String.
   * <p>
   * Note: we also embed the template content in the XSL:FO root-tag for
   * convenience.
   * 
   * @param _ctx - the active WOContext
   * @return the String containing the rendering instructions
   */
  public String renderFOInContext(final WOContext _ctx) {
    // TBD: would be better to stream to a temporary file?
    WOResponse foResponse = new WOResponse(_ctx != null ? _ctx.request() :null);
    foResponse.setContentEncoding("utf-8");
    foResponse.setHeaderForKey("text/xml", "Content-Type");
    foResponse.setTextCoder
      (NSXmlEntityTextCoder.sharedCoder, NSXmlEntityTextCoder.sharedCoder);

    foResponse.appendContentString(
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
    foResponse.appendContentString(
        "<root xmlns=\"http://www.w3.org/1999/XSL/Format\">");
    
    this.template.appendToResponse(foResponse, _ctx);
    
    foResponse.appendContentString("</root>");
    
    return foResponse.contentString();
  }
  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    WODynamicElement.appendBindingsToDescription(_d, 
        "data",        this.data,
        "mimeType",    this.mimeType,
        "disposition", this.disposition,
        "filename",    this.filename,
        "response",    this.response,
        "error",       this.error
    );
  }
  
}
