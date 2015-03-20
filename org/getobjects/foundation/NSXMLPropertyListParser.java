/*
  Copyright (C) 2009 Marcus Mueller

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

package org.getobjects.foundation;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class NSXMLPropertyListParser extends NSObject  {
  protected static Log log = LogFactory.getLog("NSXMLPropertyListParser");

  /**
   * http://www.apple.com/DTDs/PropertyList-1.0.dtd
   */
  static final String propertyList10DTDString = ""
      + "<!ENTITY % plistObject \"(array | data | date | dict | real | integer"
      + " | string | true | false )\" >"
      + "<!ELEMENT plist %plistObject;>"
      + "<!ATTLIST plist version CDATA \"1.0\" >"
      + "<!ELEMENT array (%plistObject;)*>"
      + "<!ELEMENT dict (key, %plistObject;)*>"
      + "<!ELEMENT key (#PCDATA)>"
      + "<!ELEMENT string (#PCDATA)>"
      + "<!ELEMENT data (#PCDATA)>"
      + "<!ELEMENT date (#PCDATA)>"
      + "<!ELEMENT true EMPTY>"
      + "<!ELEMENT false EMPTY>"
      + "<!ELEMENT real (#PCDATA)>";

  public static enum Tag {
    PLIST, ARRAY, DICT, KEY, STRING, DATA, DATE, TRUE, FALSE, REAL, INTEGER
  }

  class NSXMLPlistHandler extends DefaultHandler {

    public Object        rootObject;
    public List<Object>  objectStack;
    public Tag           currentTag;

    protected DateFormat fixedTZFormat;
    protected DateFormat freeTZFormat;

    protected Tag tagForTagName(final String name) {
      if (name == "plist")   return Tag.PLIST;
      if (name == "array")   return Tag.ARRAY;
      if (name == "dict")    return Tag.DICT;
      if (name == "key")     return Tag.KEY;
      if (name == "string")  return Tag.STRING;
      if (name == "data")    return Tag.DATA;
      if (name == "date")    return Tag.DATE;
      if (name == "true")    return Tag.TRUE;
      if (name == "false")   return Tag.FALSE;
      if (name == "real")    return Tag.REAL;
      if (name == "integer") return Tag.INTEGER;
      return null;
    }

    public Object rootObject() {
      return this.rootObject;
    }

    public void pushObject(Object _obj) {
      if (this.rootObject == null)
        this.rootObject = _obj;
      this.objectStack.add(_obj);
    }

    public Object popObject() {
      final int count = this.objectStack.size();
      if (count == 0) return null;
      final Object obj = this.objectStack.get(count - 1);
      this.objectStack.remove(count - 1);
      return obj;
    }

    @Override
    public void startDocument() throws SAXException {
      this.objectStack = new ArrayList<Object>(10);

      this.fixedTZFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      this.fixedTZFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

      this.freeTZFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    }

    @Override
    public void endDocument() throws SAXException {
      this.rootObject = popObject();
    }

    @Override
    public void startElement
      (final String uri, final String localName, String name,
       final Attributes attributes) throws SAXException
    {
      name            = name.intern();
      this.currentTag = tagForTagName(name);

      if (this.currentTag == null) {
        log.warn("unknown plist tag '" + name + "' - can't parse");
      }
      else {
        if (this.currentTag == Tag.PLIST) {
          // do nothing
        }
        else if (this.currentTag == Tag.TRUE)
          pushObject(Boolean.TRUE);
        else if (this.currentTag == Tag.FALSE)
          pushObject(Boolean.FALSE);
        else
          pushObject(this.currentTag);
      }
    }

    @Override
    public void endElement(String uri, String localName, String name)
      throws SAXException
    {
      if (name.equals("plist")) return;

      // reduce ... connect to appropriate collection type upon closure
      if (name.equals("array")) {
        List<Object> a;
        int          last, i;

        last = this.objectStack.size() - 1;
        i    = last;

        // traverse stack upwards till array
        while (this.objectStack.get(i) != Tag.ARRAY)
          i--;

        if ((last - i) > 0) {
          List<Object> values = this.objectStack.subList(i + 1, last + 1);
          a = new ArrayList<Object>(values);
        }
        else
          a = new ArrayList<Object>();

        final int stopAt = i - 1;

        // diminish obsolete entries
        for (i = last; i > stopAt; i--)
          this.objectStack.remove(i);
        pushObject(a);
      }
      else if (name.equals("dict")) {
        Map<Object,Object> d;
        int                last, i;

        last = this.objectStack.size() - 1;
        i    = last;

        // traverse stack upwards till dictionary
        while (this.objectStack.get(i) != Tag.DICT)
          i--;

        int stopAt = i - 1;

        if ((last - i) == 1)
          d = new HashMap<Object,Object>();
        else {
          d = new HashMap<Object,Object>((last - i) / 2);
          // set values and keys
          while (i < last) {
            d.put(this.objectStack.get(i + 1), this.objectStack.get(i + 2));
            i += 2;
          }
        }

        // diminish obsolete entries
        for (i = last; i > stopAt; i--)
          this.objectStack.remove(i);
        pushObject(d);
      }
      // any other possible container?
      else if (name.equals("string")  ||
               name.equals("key")     ||
               name.equals("integer") ||
               name.equals("real")    ||
               name.equals("data")    ||
               name.equals("date"))
      {
        Tag tag  = tagForTagName(name);
        int last = this.objectStack.size() - 1;
        int i    = last;

        // traverse stack upwards till tag
        while (this.objectStack.get(i) != tag)
          i--;

        String s;

        if ((last - i) != 1)
        {
          int stopAt = i - 1;

          StringBuilder sb = new StringBuilder();
          for (i += 1; i <= last; i++)
            sb.append(this.objectStack.get(i));

          // diminish obsolete entries
          for (i = last; i > stopAt; i--)
            this.objectStack.remove(i);
          s = sb.toString();
        }
        else {
          this.objectStack.remove(i);
          s = (String)this.objectStack.get(i);
          this.objectStack.remove(i);
        }

        if (tag == Tag.STRING || tag == Tag.KEY)
          pushObject(s);
        else if (tag == Tag.INTEGER)
          pushObject(Long.parseLong(s));
        else if (tag == Tag.REAL)
          pushObject(Double.parseDouble(s));
        else if (tag == Tag.DATE) {
          DateFormat fmt;

          if (s.length() == 20 && s.endsWith("Z")) {
            final StringBuffer sb = new StringBuffer(s);
            sb.setCharAt(10, ' '); // replace "T"
            sb.deleteCharAt(sb.length() - 1); // remove trailing "Z"
            s = sb.toString();
            fmt = this.fixedTZFormat;
          }
          else {
            fmt = this.freeTZFormat;
          }

          try {
            final Object obj = fmt.parseObject(s);
            pushObject(obj);
          }
          catch (ParseException e) {
            log.error("Error parsing date '" + s + "': " + e);
          }
        }
        else if (tag == Tag.DATA) {
          pushObject(UData.dataByDecodingBase64(s));
        }
      }
    }

    @Override
    public void characters(final char[] ch, final int start, final int length)
      throws SAXException
    {
      pushObject(new String(ch, start, length));
    }

    @Override
    public InputSource resolveEntity(final String publicId, final String sysId)
      throws IOException, SAXException
    {
      if (sysId.equals("http://www.apple.com/DTDs/PropertyList-1.0.dtd")) {
        return new InputSource(
                     getClass().getResourceAsStream("PropertyList-1.0.dtd"));
      }
      log.error("resolve: " + publicId + ", " + sysId);
      return null;
    }
  }


  public Object parse(InputStream _in) {
    if (_in == null)
      return null;

    final SAXParserFactory factory = SAXParserFactory.newInstance();

    try {
      final NSXMLPlistHandler handler = new NSXMLPlistHandler();
      final XMLReader         reader  = factory.newSAXParser().getXMLReader();
      final InputSource       input   = new InputSource(_in);
      reader.setContentHandler(handler);
      reader.setEntityResolver(handler);
      try {
        reader.setFeature("http://xml.org/sax/features/validation", false);
        reader.setFeature(
            "http://xml.org/sax/features/external-parameter-entities", false);
      }
      catch (Exception e) {
        log.error("Couldn't turn validation off: " + e);
      }
      reader.parse(input);
      return handler.rootObject();
    }
    catch (ParserConfigurationException e) {
      log.error("error during parser instantiation: " + e);
    }
    catch (SAXException e) {
      log.error("error during parsing: " + e);
    }
    catch (IOException e) {
      log.error("error during parsing: " + e);
    }
    return null;
  }

  protected static final Class[] urlTypes = {
    InputStream.class
  };

  /**
   * This method calls getContent() on the given URL and parses the result as
   * a property list.
   *
   * @param _url - the URL to parse from
   * @return a plist object, or null on error
   */
  public Object parse(final URL _url) {
    if (_url == null)
      return null;

    if (log.isDebugEnabled())
      log.debug("parse URL: " + _url);

    Object o = null;
    try {
      o = _url.getContent(urlTypes);
    }
    catch (IOException e) {
      log.error("failed to read from URL: " + _url, e);
    }
    if (o == null)
      return null;

    if (o instanceof InputStream)
      return this.parse((InputStream)o);

    log.error("don't know how to deal with URL content: " + o.getClass());
    return null;
  }

}
