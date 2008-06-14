/*
 * Copyright (C) 2007 Helge Hess <helge.hess@opengroupware.org>
 * 
 * This file is part of Go.
 * 
 * Go is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2, or (at your option) any later version.
 * 
 * Go is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Go; see the file COPYING. If not, write to the Free Software
 * Foundation, 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.getobjects.foundation;

/*
 * XMLNamespaces
 * 
 * A collection of predefined XML namespace constants. Lets try to keep it
 * in-sync with SOPE/sope-xml/XMLNamespaces.h.
 */
public abstract class XMLNS {
  
  public static final String PRE_SXOD  = "http://www.skyrix.com/od/";
  public static final String OD_BIND   = PRE_SXOD + "binding";
  public static final String OD_CONST  = PRE_SXOD + "constant";
  public static final String OD_ACTION = PRE_SXOD + "action";
  public static final String OD_EVALJS = PRE_SXOD + "javascript";
  
  public static final String PRE_OGoOD = "http://www.opengroupware.org/ns/";
  public static final String OGo_OGNL  = PRE_OGoOD + "ognl";
  
  public static final String PRE_W3C = "http://www.w3.org/";
  public static final String XMLNS  = PRE_W3C + "2000/xmlns/";
  public static final String XHTML  = PRE_W3C + "1999/xhtml";
  public static final String HTML40 = PRE_W3C + "TR/REC-html40";
  public static final String XLINK  = PRE_W3C + "1999/xlink";
  public static final String XSLT   = PRE_W3C + "1999/XSL/Transform";
  public static final String XSL_FO = PRE_W3C + "1999/XSL/Format";
  public static final String XFORMS = PRE_W3C + "2001/06/xforms";
  public static final String SVG    = PRE_W3C + "2000/svg";
  public static final String MATHML = PRE_W3C + "1998/Math/MathML";
  
  public static final String RDF =
    PRE_W3C + "1999/02/22-rdf-syntax-ns#";

  public static final String XUL =
    "http://www.mozilla.org/keymaster/gatekeeper/there.is.only.xul";

  public static final String WML12 =
    "http://www.wapforum.org/DTD/wml_1.2.xml";

  public static final String XUPDATE = "http://www.xmldb.org/xupdate";

  public static final String WEBDAV = "DAV:";

  public static final String XCAL_01 =
    "http://www.ietf.org/internet-drafts/draft-ietf-calsch-many-xcal-01.txt";

  public static final String RELAXNG_STRUCTURE =
    "http://relaxng.org/ns/structure/1.0";

  public static final String XINCLUDE = PRE_W3C + "2001/XInclude";

  public static final String KUPU =
    "http://kupu.oscom.org/namespaces/dist";

  /* Microsoft related namespaces */

  public static final String MS_OFFICE_WORDML =
    "http://schemas.microsoft.com/office/word/2003/wordml";

  public static final String MS_OFFICE_OFFICE =
    "urn:schemas-microsoft-com:office:office";

  public static final String MS_OFFICE_WORD =
    "urn:schemas-microsoft-com:office:word";

  public static final String MS_HOTMAIL =
    "http://schemas.microsoft.com/hotmail/";

  public static final String MS_HTTPMAIL = "urn:schemas:httpmail:";

  public static final String MS_EXCHANGE =
    "http://schemas.microsoft.com/exchange/";
  
  public static final String MS_EX_CALENDAR = "urn:schemas:calendar:";
  public static final String MS_EX_CONTACTS = "urn:schemas:contacts:";

  /* WebDAV related namespaces */

  public static final String WEBDAV_APACHE =
    "http://apache.org/dav/props/";
  public static final String CADAVER_PROPS =
    "http://webdav.org/cadaver/custom-properties/";
  public static final String NAUTILUS_PROPS =
    "http://services.eazel.com/namespaces";

  /* OpenOffice.org namespaces */

  public static final String OOo_UCB_WEBDAV =
    "http://ucb.openoffice.org/dav/props/";

  public static final String OOo_MANIFEST =
    "http://openoffice.org/2001/manifest";

  public static final String PRE_OOo     = "http://openoffice.org/2000/";
  public static final String OOo_OFFICE  = PRE_OOo + "office";
  public static final String OOo_TEXT    = PRE_OOo + "text";
  public static final String OOo_META    = PRE_OOo + "meta";
  public static final String OOo_STYLE   = PRE_OOo + "style";
  public static final String OOo_TABLE   = PRE_OOo + "table";
  public static final String OOo_DRAWING = PRE_OOo + "drawing";
  public static final String OOo_CHART   = PRE_OOo + "chart";
  public static final String OOo_DRAW3D  = PRE_OOo + "dr3d";
  public static final String OOo_FORM    = PRE_OOo + "form";
  public static final String OOo_SCRIPT  = PRE_OOo + "script";
  public static final String OOo_DATASTYLE    = PRE_OOo + "datastyle";
  public static final String OOo_PRESENTATION = PRE_OOo + "presentation";

  public static final String DublinCore = "http://purl.org/dc/elements/1.1/";

  public static final String PROPRIETARY_SLOX = "SLOX:";

  /* Zope */

  public static final String Zope_TAL   ="http://xml.zope.org/namespaces/tal";
  public static final String Zope_METAL ="http://xml.zope.org/namespaces/metal";

  /* SOAP */

  public static final String SOAP_ENVELOPE =
    "http://schemas.xmlsoap.org/soap/envelope/";
  public static final String SOAP_ENCODING =
    "http://schemas.xmlsoap.org/soap/encoding/";

  public static final String XMLSchema = PRE_W3C + "1999/XMLSchema";
  public static final String XMLSchemaInstance1999 =
    PRE_W3C + "1999/XMLSchema-instance";
  public static final String XMLSchemaInstance2001 =
    PRE_W3C + "2001/XMLSchema-instance";

  /* Novell */

  public static final String Novell_NCSP_Types =
    "http://schemas.novell.com/2003/10/NCSP/types.xsd";
  public static final String Novell_NCSP_Methods =
    "http://schemas.novell.com/2003/10/NCSP/methods.xsd";

  /* XML vCards */

  public static final String VCARD_XML_03 =
    "http://www.ietf.org/internet-drafts/draft-dawson-vcard-xml-dtd-03.txt";

  /* ATOM */

  public static final String ATOM_2005 = PRE_W3C + "2005/Atom";

  /* Google */

  public static final String GOOGLE_2005 =
    "http://schemas.google.com/g/2005";

  public static final String GOOGLE_CAL_2005 =
    "http://schemas.google.com/gCal/2005";

  public static final String OPENSEARCH_RSS =
    "http://a9.com/-/spec/opensearchrss/1.0/";

  /* GroupDAV */

  public static final String GROUPDAV = "http://groupdav.org/";
  
  /* CalDAV */

  public static final String CALDAV = "urn:ietf:params:xml:ns:caldav";
  
  /* Apple CalServer */
  
  public static final String AppleCalServer =
    "http://apple.com/ns/calendarserver/";

  public static final String AppleCalApp = "com.apple.ical:";
  
  /* Adobe */
  
  public static final String MXML_2006 = "http://www.adobe.com/2006/mxml";
}
