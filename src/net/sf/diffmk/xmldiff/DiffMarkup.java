/* NodeDiff */

package net.sf.diffmk.xmldiff;

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

import java.util.Hashtable;
import java.util.Enumeration;

import org.w3c.dom.*;

public class DiffMarkup {
  protected Hashtable elemContext = new Hashtable();
  protected Hashtable rootElements = new Hashtable();
  protected Hashtable ignoreAttributes = new Hashtable();
  protected Hashtable verbatimElements = new Hashtable();

  public DiffMarkup () {
  }

  public void setChanged(Element node) {
      //node.setAttribute(attrName, changed);
      node.setAttributeNS("http://diffmk.sf.net/ns/diff","diffmk:change","changed");
  }

  public void setAdded(Element node) {
      //node.setAttribute(attrName, added);
      node.setAttributeNS("http://diffmk.sf.net/ns/diff","diffmk:change","added");
  }

  public void setDeleted(Element node) {
      //node.setAttribute(attrName, deleted);
      node.setAttributeNS("http://diffmk.sf.net/ns/diff","diffmk:change","deleted");
      // FIXME: add a switch for id attribute name
      if (node.hasAttribute("id")) {
          node.setAttribute("id", "DEL." + node.getAttribute("id"));
      }
      if (node.hasAttribute("xml:id")) {
          node.setAttribute("xml:id", "DEL." + node.getAttribute("xml:id"));
      }
  }

  private String getChangedAttribute(Node node) {
      if (node == null || node.getNodeType() != Node.ELEMENT_NODE) {
          return null;
      }
      
      Element elem = (Element) node;
      
      if (elem.hasAttributeNS("http://diffmk.sf.net/ns/diff", "diffmk:changed")) {
          return elem.getAttributeNS("http://diffmk.sf.net/ns/diff", "diffmk:changed");
      } else {
          return null;
      }
  }
  
  public boolean isChanged(Node node) {
      return "changed".equals(getChangedAttribute(node));
  }

  public boolean isAdded(Node node) {
      return "added".equals(getChangedAttribute(node));
  }

  public boolean isDeleted(Node node) {
      return "deleted".equals(getChangedAttribute(node));
  }

  public void clear(Element node) {
    node.removeAttributeNS("http://diffmk.sf.net/ns/diff", "diffmk:changed");
  }

  public Element getWrapper(Element context) {
      if (context == null) {
          System.err.print("The sky is falling!");
          return null;
      }
    Document doc = context.getOwnerDocument();
    return doc.createElementNS("http://diffmk.sf.net/ns/diff", "diffmk:wrapper");
  }

  public String getWrapper(String contextNS, String context) {
    String key = "{" + contextNS + "}" + context;
    System.out.println("getWrapper(" + contextNS + "," + context + ")");

    if (elemContext.containsKey(key)) {
      String elemName = (String) elemContext.get(key);
      if (elemName.equals("*")) {
	return null;
      } else {
	return elemName;
      }
    } else if (elemContext.containsKey("*")) {
      return (String) elemContext.get("*");
    } else {
      return null;
    }
  }

  public Element _old_getWrapper(Element context) {
    String elemName = null;
    String nsURI = context.getNamespaceURI();

    if (nsURI == null) {
      nsURI = "";
    }

    String key = "{" + nsURI + "}" + context.getLocalName();

    if (elemContext.containsKey(key)) {
      elemName = (String) elemContext.get(key);
    } else {
      if (elemContext.containsKey("*")) {
	elemName = (String) elemContext.get("*");
      }
    }

    //System.err.println("key for " + context + ": " + key + " " + elemName);

    if (elemName == null) {
      return null;
    }

    if (elemName.equals("*")) {
      return null;
    }

    Document doc = context.getOwnerDocument();

    // elemName = "{uri}localName"
    String uri = elemName.substring(1);
    int pos = uri.lastIndexOf("}");
    String localName = uri.substring(pos+1);
    uri = uri.substring(0, pos);

    // System.out.println(elemName + " => " + uri + " :: " + localName);

    if ("".equals(uri)) {
      return doc.createElement(localName);
    } else {
      return doc.createElementNS(uri, localName);
    }
  }

  public String _old_getWrapper(String contextNS, String context) {
    String key = "{" + contextNS + "}" + context;

    if (elemContext.containsKey(key)) {
      String elemName = (String) elemContext.get(key);
      if (elemName.equals("*")) {
	return null;
      } else {
	return elemName;
      }
    } else if (elemContext.containsKey("*")) {
      return (String) elemContext.get("*");
    } else {
      return null;
    }
  }
  
  public void setDefaultWrapper(String elemNS, String elemName) {
    elemContext.put("*", "{" + elemNS + "}" + elemName);
  }

  public void setWrapper(String elemNS, String elemName,
			 String contextNS, String context) {
    //System.err.println("Wrapper for {" + contextNS + "}" + context + " is " +
    //		    "{" + elemNS + "}" + elemName);
    elemContext.put("{" + contextNS + "}" + context,
		    "{" + elemNS + "}" + elemName);
  }

  public void setNullWrapper(String contextNS, String context) {
    //System.err.println("Null wrapper for {" + contextNS + "}" + context);
    elemContext.put("{" + contextNS + "}" + context, "*");
  }

  public void addRoot(String uri, String localName) {
    String root = "{}";
    if (uri != null) {
      root = "{" + uri + "}";
    }
    root = root + localName;
    rootElements.put(root, localName);
  }

  public boolean hasRoot(String uri, String localName) {
    String root = "{}";
    if (uri != null) {
      root = "{" + uri + "}";
    }
    root = root + localName;
    return rootElements.containsKey(root);
  }

  public void addIgnoreAttribute(String elementNS, String element,
				 String attrNS, String attr) {
    if (elementNS == null) {
      elementNS = "";
    }

    if (attrNS == null) {
      attrNS = "";
    }

    //System.out.println("Add Ignore " + "{" + elementNS + "}" + element + "|"
    //+ "{" + attrNS + "}" + attr);

    ignoreAttributes.put("{" + elementNS + "}" + element + "|"
			 + "{" + attrNS + "}" + attr, "i");
  }

  public boolean ignoreAttribute(String elementNS, String element,
				 String attrNS, String attr) {
    if (elementNS == null) {
      elementNS = "";
    }

    if (attrNS == null) {
      attrNS = "";
    }

    //System.out.println("Ignore? " + "{" + elementNS + "}" + element + "|"
    //+ "{" + attrNS + "}" + attr);

    String ekey = "{" + elementNS + "}" + element;
    String ekeyAny = "{" + elementNS + "}" + "*";
    String akey = "{" + attrNS + "}" + attr;
    String akeyAny = "{" + attrNS + "}" + "*";

    //    System.out.println("Ignore? " + attr + " on " + element);

    boolean testIgnore =
           (   ignoreAttributes.containsKey(ekey + "|" + akey)
	    || ignoreAttributes.containsKey(ekey + "|" + akeyAny)
	    || ignoreAttributes.containsKey(ekey + "|{}*")

	    || ignoreAttributes.containsKey(ekeyAny + "|" + akey)
	    || ignoreAttributes.containsKey(ekeyAny + "|" + akeyAny)
	    || ignoreAttributes.containsKey(ekeyAny + "|{}*")

	    || ignoreAttributes.containsKey("{}*|" + akey)
	    || ignoreAttributes.containsKey("{}*|" + akeyAny)
	    || ignoreAttributes.containsKey("{}*|{}*"));

    //System.out.println("Ignore " + attr + " on " + element + ": " + testIgnore);

    return testIgnore;
  }

  public void addVerbatim(String elementNS, String element) {
    if (elementNS == null) {
      elementNS = "";
    }

    //System.out.println("Verbatim: " + "{" + elementNS + "}" + element);

    verbatimElements.put("{" + elementNS + "}" + element, "v");
  }

  public boolean verbatimElement(String elementNS, String element) {
    if (elementNS == null) {
      elementNS = "";
    }

    //System.out.println("Verbatim? " + "{" + elementNS + "}" + element);
    return verbatimElements.containsKey("{" + elementNS + "}" + element);
  }
}
