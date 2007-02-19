/* DiffMkConfig */

package net.sf.diffmk;

/* Copyright (C) 2001, 2002 Sun Microsystems, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Enumeration;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.w3c.dom.Element;

import javax.xml.parsers.*;

import net.sf.diffmk.xmldiff.DiffMarkup;

public class DiffMkConfig extends DefaultHandler {
  public static final String namespace = "http://www.sun.com/xml/diffmk";

  private static Hashtable namespaceMap = new Hashtable();

  private String doctype = null;
  private Hashtable doctypes = null;

  public DiffMkConfig() {
    doctypes = new Hashtable();
  }

  public boolean parse(String xmlFile) throws IOException {
    SAXParserFactory spf = SAXParserFactory.newInstance();
    spf.setNamespaceAware(true);
    spf.setValidating(false);

    try {
      SAXParser parser = spf.newSAXParser();
      parser.parse(xmlFile, this);
      return true;
    } catch (SAXException se) {
      System.err.println("SAX exception parsing: " + xmlFile);
      System.err.println(se);
    } catch (ParserConfigurationException pce) {
      System.err.println("Parser config exception parsing: " + xmlFile);
      System.err.println(pce);
    }

    return false;
  }

  public boolean doctype(String name) {
    return doctypes.containsKey(name);
  }

  public DiffMarkup markup(String doctype) {
    return (DiffMarkup) doctypes.get(doctype);
  }

  public String guessDoctype(Element element) {
    for (Enumeration denum = doctypes.keys();
	 denum.hasMoreElements() ; ) {
      String key = (String) denum.nextElement();
      DiffMarkup diffMarkup = (DiffMarkup) doctypes.get(key);
      if (diffMarkup.hasRoot(element.getNamespaceURI(),
			     element.getLocalName())) {
	return key;
      }
    }
    return "";
  }

  // ----------------------------------------------------------------------
  // Implement the SAX ContentHandler interface

  /** <p>The SAX <code>setDocumentLocator</code> method. Does nothing.</p> */
  public void setDocumentLocator (Locator locator) {
    // nop;
  }

  /** <p>The SAX <code>startDocument</code> method. Does nothing.</p> */
  public void startDocument () throws SAXException {
    // nop;
  }

  /** <p>The SAX <code>endDocument</code> method. Does nothing.</p> */
  public void endDocument () throws SAXException {
    // nop;
  }

  /**
   * <p>The SAX2 <code>startElement</code> method.</p> 
   *
   * <p>The catalog parser is selected based on the namespace of the
   * first element encountered in the catalog.</p>
   */
  public void startElement (String namespaceURI,
			    String localName,
			    String qName,
			    Attributes atts)
    throws SAXException {

    if (namespace.equals(namespaceURI)) {
      if (localName.equals("doctype")) {
	doctype = atts.getValue("name");

	DiffMarkup diffMarkup = new DiffMarkup(atts.getValue("attribute"),
					       atts.getValue("changed"),
					       atts.getValue("added"),
					       atts.getValue("deleted"));

	doctypes.put(doctype, diffMarkup);
      } else if (localName.equals("wrapper")) {
	DiffMarkup diffMarkup = (DiffMarkup) doctypes.get(doctype);
	String parentNS = "";
	String elementNS = "";
	String parent = atts.getValue("parent");
	String element = atts.getValue("element");

 	if (parent != null && parent.indexOf(":") > 0) {
	  String prefix = parent.substring(0, parent.indexOf(":"));
	  parent = parent.substring(parent.indexOf(":")+1);
	  parentNS = (String) namespaceMap.get(prefix);
	  if (parentNS == null) {
	    System.err.println("Error: configuration file is not namespace valid.");
	    System.err.println("Missing declaration for prefix \"" + prefix + "\".");
	    System.exit(2);
	  }
	}

	if (element != null && element.indexOf(":") > 0) {
	  String prefix = element.substring(0, element.indexOf(":"));
	  element = element.substring(element.indexOf(":")+1);
	  elementNS = (String) namespaceMap.get(prefix);
	  if (elementNS == null) {
	    System.err.println("Error: configuration file is not namespace valid.");
	    System.err.println("Missing declaration for prefix \"" + prefix + "\".");
	    System.exit(2);
	  }
	}

	if (parent == null || parent.equals("")) {
	  diffMarkup.setDefaultWrapper(elementNS, element);
	} else if (element == null || element.equals("")) {
	  diffMarkup.setNullWrapper(parentNS, parent);
	} else {
	  diffMarkup.setWrapper(elementNS, element, parentNS, parent);
	}
      } else if (localName.equals("root")) {
	DiffMarkup diffMarkup = (DiffMarkup) doctypes.get(doctype);
	String name = atts.getValue("name");
	String prefix = null;
	String lName = name;
	String uri = "";

	if (name.indexOf(":") > 0) {
	  prefix = name.substring(0, name.indexOf(":"));
	  lName = name.substring(name.indexOf(":")+1);
	  uri = (String) namespaceMap.get(prefix);
	  if (uri == null) {
	    System.err.println("Error: configuration file is not namespace valid.");
	    System.err.println("Missing declaration for prefix \"" + prefix + "\".");
	    System.exit(2);
	  }
	}

	diffMarkup.addRoot(uri, name);
      } else if (localName.equals("ignoreattribute")) {
	DiffMarkup diffMarkup = (DiffMarkup) doctypes.get(doctype);
	String elementNS = "";
	String element = atts.getValue("element");
	String attrNS = "";
	String attr = atts.getValue("name");

	if (element != null && element.indexOf(":") > 0) {
	  String prefix = element.substring(0, element.indexOf(":"));
	  element = element.substring(element.indexOf(":")+1);
	  elementNS = (String) namespaceMap.get(prefix);
	  if (elementNS == null) {
	    System.err.println("Error: configuration file is not namespace valid.");
	    System.err.println("Missing declaration for prefix \"" + prefix + "\".");
	    System.exit(2);
	  }
	}

	if (attr != null && attr.indexOf(":") > 0) {
	  String prefix = attr.substring(0, attr.indexOf(":"));
	  attr = attr.substring(attr.indexOf(":")+1);
	  attrNS = (String) namespaceMap.get(prefix);
	  if (attrNS == null) {
	    System.err.println("Error: configuration file is not namespace valid.");
	    System.err.println("Missing declaration for prefix \"" + prefix + "\".");
	    System.exit(2);
	  }
	}

	if (element == null || element.equals("")) {
	  element = "*";
	}

	if (attr == null || attr.equals("")) {
	  attr = "*";
	}

	diffMarkup.addIgnoreAttribute(elementNS, element, attrNS, attr);
      } else if (localName.equals("verbatim")) {
	DiffMarkup diffMarkup = (DiffMarkup) doctypes.get(doctype);
	String elementNS = "";
	String element = atts.getValue("element");

	if (element != null && element.indexOf(":") > 0) {
	  String prefix = element.substring(0, element.indexOf(":"));
	  element = element.substring(element.indexOf(":")+1);
	  elementNS = (String) namespaceMap.get(prefix);
	  if (elementNS == null) {
	    System.err.println("Error: configuration file is not namespace valid.");
	    System.err.println("Missing declaration for prefix \"" + prefix + "\".");
	    System.exit(2);
	  }
	}

	diffMarkup.addVerbatim(elementNS, element);
      } else if (localName.equals("diffmk")) {
	// nop;
      } else {
	System.out.println("Unexpected configuration element: " + localName);
      }
    }
  }

  /** <p>The SAX2 <code>endElement</code> method. Does nothing.</p> */
  public void endElement (String namespaceURI,
			  String localName,
			  String qName) throws SAXException {
    // nop;
  }

  /** <p>The SAX <code>characters</code> method. Does nothing.</p> */
  public void characters (char ch[], int start, int length)
    throws SAXException {
    // nop;
  }

  /** <p>The SAX <code>ignorableWhitespace</code> method. Does nothing.</p> */
  public void ignorableWhitespace (char ch[], int start, int length)
    throws SAXException {
    // nop;
  }

  /** <p>The SAX <code>processingInstruction</code> method. Does nothing.</p> */
  public void processingInstruction (String target, String data)
    throws SAXException {
    // nop;
  }

  /** <p>The SAX <code>startPrefixMapping</code> method. Does nothing.</p> */
  public void startPrefixMapping (String prefix, String uri)
    throws SAXException {
    namespaceMap.put(prefix, uri);
  }

  /** <p>The SAX <code>endPrefixMapping</code> method. Does nothing.</p> */
  public void endPrefixMapping (String prefix)
    throws SAXException {
    namespaceMap.remove(prefix);
  }

  /** <p>The SAX <code>skippedentity</code> method. Does nothing.</p> */
  public void skippedEntity (String name)
    throws SAXException {
    // nop;
  }
}
