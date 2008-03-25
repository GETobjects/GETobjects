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
package org.getobjects.samples.HelloPDF;

import java.io.ByteArrayOutputStream;

import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODirectAction;
import org.getobjects.appserver.core.WOResponse;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * A WODirectAction object is pretty much like a servlet, it can accept
 * requests and is responsible for producing a result. The result can be
 * either a WOResponse or a WOComponent or anything else which can be
 * rendered by JOPE.
 * <p>
 * In this example we directly generate a WOResponse with the
 * application/pdf content type.
 * 
 * @author helge
 */
public class PDFAction extends WODirectAction {

  public PDFAction(WOContext _ctx) {
    super(_ctx);
  }
  
  /**
   * Add some content to the PDF. This is using iText methods to add some text
   * and a few tables to the given Document.
   * Note how you can reuse higher-level objects once they got added to the
   * document (iText does not keep a higher level structure but directly
   * generates the PDF content).
   * 
   * @param _itext - the iText Document object
   * @throws DocumentException
   */
  void addContent(Document _itext) throws DocumentException {
    
    /* a small paragraph */
    
    _itext.add(new Paragraph("Hello World"));
    
    /* a simple table (default width is 80%) */
    
    PdfPTable table = new PdfPTable(3);
    table.setSpacingBefore(10);
    table.setSpacingAfter(10);
    
    PdfPCell th = new PdfPCell(new Paragraph("Title"));
    th.setColspan(3);
    table.addCell(th);
    
    for (int i = 0; i < 8; i++) {
      for (int j = 0; j < 3; j++)
        table.addCell("" + i + "/" + j);
    }
    
    _itext.add(table);
    
    /* readd table with full width */
    
    table.setWidthPercentage(100);
    _itext.add(table);
    
    /* readd with align */
    
    table.setWidthPercentage(60);
    table.setHorizontalAlignment(Element.ALIGN_LEFT);
    _itext.add(table);
    
    /* and make a big table to enforce a pagebreak */

    for (int i = 0; i < 32; i++) {
      for (int j = 0; j < 3; j++)
        table.addCell("" + i + "/" + j);
    }
    _itext.add(table);
  }

  
  /**
   * This method is called when you invoke the direct action like this:<pre>
   *   /HelloPDF/wa/PDFAction/default</pre>
   *   
   * @return returns the WOResponse with the PDF or an Exception
   */
  @Override
  public Object defaultAction() {
    
    /* The book says browsers do not deal well with missing content-lengths,
     * so we can't stream the PDF directly.
     * Technically it would probably be best to stream to a file and then
     * deliver that on the socket using sendfile().
     */
    ByteArrayOutputStream bas = new ByteArrayOutputStream(16000);
    
    
    /* generate PDF */
    
    Document itDoc = new Document();
    try {
      PdfWriter.getInstance(itDoc, bas);
    }
    catch (DocumentException e) {
      daLog.error("got no PdfWriter instance?", e);
      return e;
    }
    
    itDoc.open();
    
    try {
      this.addContent(itDoc);
    }
    catch (DocumentException e) {
      daLog.error("failed to add PDF paragraph", e);
      return e;
    }
    
    itDoc.close();
    
    
    /* generate response */
    
    WOResponse r = this.context().response();
    
    r.setHeaderForKey("application/pdf",           "content-type");
    r.setHeaderForKey("inline; filename=test.pdf", "content-disposition");
    r.setContent(bas.toByteArray());
    r.setHeaderForKey("" + bas.size(), "content-length");
    
    return r;
  }

}
