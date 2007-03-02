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

import org.w3c.dom.*;

public class NodeDiff extends Object {
  public static final int UNCHANGED = 0;
  public static final int CHANGED = 1;
  public static final int INSERTED = 2;
  public static final int DELETED = 3;

  public static boolean ignoreWhitespace = false;

  protected static DiffMarkup diffMarkup = new DiffMarkup();

  protected String text = null;
  protected Node node = null;
  protected boolean endTag = false;
  protected int status = UNCHANGED;

  public NodeDiff (Node node, boolean endTag) {
    super();
    this.node = node;
    this.endTag = endTag;

    //if (node.getNodeType() != Node.ELEMENT_NODE
    //&& node.getNodeType() != Node.TEXT_NODE) {
    //System.err.println("add: " + node.getNodeType());
    //}
  }

  public NodeDiff (String text) {
    super();
    this.text = text;
  }

  public boolean isText() {
    return text != null;
  }

  public Node getNode() {
    return node;
  }

  public String getText() {
    return text;
  }

  public boolean isEndTag() {
    return endTag;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public boolean equals(Object obj) {
    NodeDiff oObj = (NodeDiff) obj;

    //System.err.println("diff: " + node + " =?= " + oObj.getNode());

    //if ((node.getNodeType() != Node.ELEMENT_NODE
    //	 && node.getNodeType() != Node.TEXT_NODE)
    //	|| (oObj.getNode().getNodeType() != Node.ELEMENT_NODE
    //&& oObj.getNode().getNodeType() != Node.TEXT_NODE)) {
    //System.err.println("compare: " + node.getNodeType() + " with " + oObj.getNode().getNodeType());
    //}

    if (text != null || oObj.isText()) {
      if (text != null && oObj.isText()) {
	String otherText = oObj.getText();
	return text.equals(otherText);
      } else {
	return false;
      }
    }

    Node oNode = oObj.node;

    if (oNode.getNodeType() != node.getNodeType()) {
      return false;
    }

    if (node.getNodeType() == Node.ELEMENT_NODE) {
      Element elem = (Element) node;
      Element oElem = (Element) oNode;

      String ns1 = elem.getNamespaceURI();
      String ns2 = oElem.getNamespaceURI();

      if ((ns1 == null && ns2 != null)
	  || (ns1 != null && ns2 == null)) {
	return false;
      }

      if (ns1 != null && !ns1.equals(ns2)) {
	return false;
      }

      if (endTag != oObj.endTag) {
	return false;
      }

      if (!elem.getLocalName().equals(oElem.getLocalName())) {
	return false;
      }

      NamedNodeMap attr = elem.getAttributes();
      NamedNodeMap oAttr = oElem.getAttributes();

      int attrCount = 0;
      int oAttrCount = 0;

      for (int count = 0; count < attr.getLength(); count++) {
	Attr a = (Attr) attr.item(count);
	String ans = a.getNamespaceURI();
	String aname = a.getLocalName();
	String value = a.getValue();

	if (!diffMarkup.ignoreAttribute(elem.getNamespaceURI(), elem.getLocalName(),
					ans, aname)) {
	  attrCount++;
	}
      }

      for (int count = 0; count < oAttr.getLength(); count++) {
	Attr a = (Attr) oAttr.item(count);
	String ans = a.getNamespaceURI();
	String aname = a.getLocalName();
	String value = a.getValue();

	if (!diffMarkup.ignoreAttribute(elem.getNamespaceURI(), elem.getLocalName(),
					ans, aname)) {
	  oAttrCount++;
	}
      }

      if (attrCount != oAttrCount) {
	return false;
      }

      for (int count = 0; count < attr.getLength(); count++) {
	Attr a = (Attr) attr.item(count);
	String ans = a.getNamespaceURI();
	String aname = a.getLocalName();
	String value = a.getValue();

	if (!diffMarkup.ignoreAttribute(elem.getNamespaceURI(), elem.getLocalName(),
					ans, aname)) {
	  if (!oElem.hasAttributeNS(ans, aname)) {
	    return false;
	  }
	  if (!value.equals(oElem.getAttributeNS(ans, aname))) {
	    return false;
	  }
	}
      }

      return true;
    } else if (node.getNodeType() == Node.TEXT_NODE) {
      Text text = (Text) node;
      Text oText = (Text) oNode;

      String text1 = text.getData();
      String text2 = oText.getData();

      if (ignoreWhitespace) {
	text1 = text1.trim();
	text2 = text2.trim();
      }

      return text1.equals(text2);
    } else if (node.getNodeType() == Node.CDATA_SECTION_NODE) {
      CDATASection text = (CDATASection) node;
      CDATASection oText = (CDATASection) oNode;

      String text1 = text.getData();
      String text2 = oText.getData();

      if (ignoreWhitespace) {
	text1 = text1.trim();
	text2 = text2.trim();
      }

      //System.err.println("CDATA: " + text1);

      return text1.equals(text2);
    } else {
      //System.err.println("Unexpected Node type: " + node.getNodeType());
      return false;
    }
  }

  public int hashCode() {
    if (text != null) {
      if (ignoreWhitespace) {
	return text.trim().hashCode();
      } else {
	return text.hashCode();
      }
    }

    if (node.getNodeType() == Node.ELEMENT_NODE) {
      Element elem = (Element) node;
      return elem.getTagName().hashCode();
    } else if (node.getNodeType() == Node.TEXT_NODE) {
      Text text = (Text) node;
      if (ignoreWhitespace) {
	return text.getData().trim().hashCode();
      } else {
	return text.getData().hashCode();
      }
    } else if (node.getNodeType() == Node.CDATA_SECTION_NODE) {
      CDATASection cdata = (CDATASection) node;
      return cdata.getData().hashCode();
    } else {
      return super.hashCode();
    }
  }

  public String toString() {
    String string = "";

    if (status == UNCHANGED) {
      string = "= ";
    } else if (status == CHANGED) {
      string = "c ";
    } else if (status == INSERTED) {
      string = "i ";
    } else if (status == DELETED) {
      string = "d ";
    } else {
      string = "? ";
    }

    if (text != null) {
      String trimText = text;
      if (ignoreWhitespace) {
	trimText = text.trim();
      }
      return string += "[" + trimText + "]";
    }

    if (node.getNodeType() == Node.ELEMENT_NODE) {
      Element elem = (Element) node;
      if (endTag) {
	string += "</" + elem.getTagName() + ">";
	return string;
      } else {
	String tag = "<" + elem.getTagName();
	NamedNodeMap attributes = elem.getAttributes();
	for (int count = 0; count < attributes.getLength(); count++) {
	  Attr attr = (Attr) attributes.item(count);
	  String name = attr.getName();
	  String value = attr.getValue();
	  tag += " " + name + "=" + "\"" + value + "\"";
	}
	string += tag + ">";
	return string;
      }
    } else if (node.getNodeType() == Node.TEXT_NODE) {
      Text text = (Text) node;
      String trimText = text.getData();
      if (ignoreWhitespace) {
	trimText = trimText.trim();
      }
      return string + "[" + trimText + "]";
    } else {
      return string + node.toString();
    }
  }
}
