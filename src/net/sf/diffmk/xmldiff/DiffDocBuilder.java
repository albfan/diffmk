/* DiffMk */

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

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.IOException;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Enumeration;

import org.w3c.dom.*;
import org.xml.sax.*;

import javax.xml.parsers.*;

import net.sf.diffmk.xmldiff.NodeDiff;
import net.sf.diffmk.xmldiff.XMLDiffException;

import bmsi.util.Diff;

public class DiffDocBuilder {
  protected PrintStream debugOut = null;
  protected Document doc = null;
  protected DiffMarkup diffMarkup = null;

  public DiffDocBuilder(DiffMarkup dMarkup, PrintStream debugOut) {
    diffMarkup = dMarkup;
    this.debugOut = debugOut;

    DocumentBuilderFactory factory = null;
    DocumentBuilder builder = null;

    factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setValidating(false);
    try {
      builder = factory.newDocumentBuilder();
      doc = builder.newDocument();
    } catch (ParserConfigurationException pce) {
      doc = null;
    }
  }

  public Document getDocument() {
    return doc;
  }

  private boolean sameGI (NodeDiff nd1, NodeDiff nd2) {
    if (nd1.isText() || nd2.isText()) {
      return false;
    }

    Node n1 = nd1.getNode();
    Node n2 = nd2.getNode();

    if (n1.getNodeType() != Node.ELEMENT_NODE
	|| n2.getNodeType() != Node.ELEMENT_NODE) {
      return false;
    }

    if ((n1.getNamespaceURI() == null && n2.getNamespaceURI() == null
	 || (n1.getNamespaceURI() != null
	     && n1.getNamespaceURI().equals(n2.getNamespaceURI())))
	&& n1.getLocalName().equals(n2.getLocalName())) {
      return true;
    } else {
      return false;
    }
  }

  private Vector santize(Vector nodeList) {
    Stack dStack = new Stack();
    Vector newList = new Vector();

    for (int count = 0; count < nodeList.size(); count++) {
      NodeDiff nd = (NodeDiff) nodeList.get(count);
      NodeDiff next = null;
      boolean skip = false;
      int status = nd.getStatus();

      if (count+1 < nodeList.size()) {
	next = (NodeDiff) nodeList.get(count+1);
      }

      if (status == NodeDiff.DELETED && !nd.isText()) {
	skip = true;
      }

      /*
      if (status == NodeDiff.DELETED) {
	skip = true;
      }
      */

      if (!skip) {
	newList.add(nd);
      }
    }

    return newList;
  }

  public void build(Vector nodeList) {
    Node context = null;
    Node newContext = null;
    Node node = null;

    nodeList = santize(nodeList);

    for (int count = 0; count < nodeList.size(); count++) {
      NodeDiff nd = (NodeDiff) nodeList.get(count);
      //      if (!nd.isText() && nd.getNode().getNodeType() == Node.ELEMENT_NODE) {
      //System.err.println("LIST2: " + count + ": " + nd);
	//      }
    }

    Stack eStack = new Stack();

    // Check context...
    for (int count = 0; count < nodeList.size(); count++) {
      NodeDiff nd = (NodeDiff) nodeList.get(count);
      int status = nd.getStatus();

      if (!nd.isText() && nd.getNode().getNodeType() == Node.ELEMENT_NODE) {
	if (nd.isEndTag()) {
	  if (eStack.empty()) {
	    System.err.println("Out of context (empty stack): " + nd);
	  } else {
	    Node ot = (Node) eStack.peek();
	    Node e = nd.getNode();

	    if ((e.getNamespaceURI() == null && ot.getNamespaceURI() == null
		 || (e.getNamespaceURI() != null
		     && e.getNamespaceURI().equals(ot.getNamespaceURI())))
		&& e.getLocalName().equals(ot.getLocalName())) {
	      eStack.pop();
	    } else {
	      System.err.println("Out of context: " + nd);
	    }
	  }
	} else {
	  eStack.push(nd.getNode());
	}
      }
    }

    if (!eStack.empty()) {
      System.err.println("Open stack!?");
    }

    for (int count = 0; count < nodeList.size(); count++) {
      NodeDiff nd = (NodeDiff) nodeList.get(count);
      int status = nd.getStatus();
      Element wrapper = null;
      newContext = null;

      //System.err.println("Building " + nd + " (context: " + context + ")");

      if (nd.isEndTag()) {
	if (context != null && context.getNodeType() == Node.ELEMENT_NODE) {
	  context = context.getParentNode();
	}
      } else {
	if (nd.isText()) {
	  String text = nd.getText();
	  int peek = count+1;
	  while (peek < nodeList.size()) {
	    NodeDiff peekNd = (NodeDiff) nodeList.get(peek);
	    if (peekNd.isText()
		&& (peekNd.getStatus() == status
		    || (peekNd.getText().trim().equals("")
			|| text.equals("")))) {
	      text += peekNd.getText();
	      peek++;
	    } else {
	      peek--;
	      break;
	    }
	  }
	  count = peek;

	  node = doc.createTextNode(text);
	  if (status != NodeDiff.UNCHANGED || !inContext(context, status)) {

	    // FIXME: this should never happen!
	    if (context != null && context.getNodeType() == Node.DOCUMENT_NODE) {
	      context = ((Document) context).getDocumentElement();
	    }

	    wrapper = diffMarkup.getWrapper((Element) context);
	    if (wrapper == null) {
	      //System.err.println("null for " + (Element) context + " with " + node);
	      wrapper = (Element) context;
	    } else {
	      //wrapper.appendChild(node);
	    }
	    wrapper.appendChild(node);

	    if (status == NodeDiff.CHANGED) {
	      diffMarkup.setChanged(wrapper);
	    } else if (status == NodeDiff.INSERTED) {
	      diffMarkup.setAdded(wrapper);
	    } else { // status == NodeDiff.DELETED
	      diffMarkup.setDeleted(wrapper);
	    }

	    node = wrapper;
	  }
	} else {
	  node = clone(nd.getNode(),context);

	  if (status == NodeDiff.UNCHANGED || inContext(context, status)) {
	    if (node.getNodeType() == Node.ELEMENT_NODE) {
	      newContext = node;
	    }
	  } else if (node.getNodeType() == Node.TEXT_NODE
		     && isWhitespace((Text) node)) {
	    // nop;
	  } else if (node.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE
		     || node.getNodeType() == Node.COMMENT_NODE) {
	    // nop;
	  } else {
	    if (node.getNodeType() == Node.ELEMENT_NODE) {
	      wrapper = (Element) node;
	      newContext = wrapper;
	    } else {
	      wrapper = diffMarkup.getWrapper((Element) context);
	      if (wrapper == null) {
		//System.err.println("null2 for " + (Element) context + " with " + node);
		wrapper = (Element) context;
	      } else {
		//wrapper.appendChild(node);
	      }

	      wrapper.appendChild(node);

	    }

	    if (status == NodeDiff.CHANGED) {
	      diffMarkup.setChanged(wrapper);
	    } else if (status == NodeDiff.INSERTED) {
	      diffMarkup.setAdded(wrapper);
	    } else { // status == NodeDiff.DELETED
	      diffMarkup.setDeleted(wrapper);
	    }

	    node = wrapper;
	  }
	}

	if (context == null) {
	  if (node.getNodeType() == Node.ELEMENT_NODE) {
	    Element rootElem = doc.getDocumentElement();
	    if (rootElem != null) {
	      rootElem.appendChild(node);
	    } else {
	      doc.appendChild(node);
	    }
	  } else {
	    System.err.println("Error: attempting to append " + node + " to #document");
	  }
	} else if (node != context) {
	  if (context.getNodeType() == Node.ELEMENT_NODE) {
	    Node parent = node.getParentNode();
	    context.appendChild(node);
	  } else {
	    System.err.println("Error: attempting to append " + node + " to " + context);
	  }
	}

	if (newContext != null) {
	  context = newContext;
	}
      }

      if (debugOut != null) {
	debugOut.println(nd.toString());
      }
    }
  }

  private boolean inContext(Node context, int type) {
    if (type == NodeDiff.UNCHANGED) {
      return true;
    }

    while (context != null && context.getNodeType() == Node.ELEMENT_NODE) {
      switch (type) {
      case NodeDiff.CHANGED:
	if (diffMarkup.isChanged(context)) {
	  return true;
	}
	break;
      case NodeDiff.INSERTED:
	if (diffMarkup.isAdded(context)) {
	  return true;
	}
	break;
      case NodeDiff.DELETED:
	if (diffMarkup.isDeleted(context)) {
	  return true;
	}
	break;
      }
      context = context.getParentNode();
    }

    return false;
  }

  private boolean inScopeNamespace(Node context, String prefix, String uri) {
    String xmlns = "xmlns";

    if (prefix != null && !prefix.equals("")) {
      xmlns = "xmlns:" + prefix;
    }

    while (context != null && context.getNodeType() == Node.ELEMENT_NODE) {
      Element elem = (Element) context;
      if (elem.hasAttribute(xmlns)) {
	// it's either right or it's not
	return elem.getAttribute(xmlns).equals(uri);
      }
      context = context.getParentNode();
    }

    return false;
  }

  private boolean isWhitespace(Text node) {
    String text = node.getData().trim();
    return text.equals("");
  }

  private Node clone(Node onode, Node context) {
    switch (onode.getNodeType()) {
    case Node.ELEMENT_NODE:
      return cloneElement((Element) onode, context);
    case Node.TEXT_NODE:
      return cloneText((Text) onode, context);
    case Node.CDATA_SECTION_NODE:
      return cloneCDATASection((CDATASection) onode, context);
    case Node.PROCESSING_INSTRUCTION_NODE:
      return cloneProcessingInstruction((ProcessingInstruction)onode,context);
    case Node.COMMENT_NODE:
      return cloneComment((Comment) onode, context);
    default:
      System.err.println("Unexpected node type in DiffDocBuilder.clone: " + onode.getNodeType());
      return null;
    }
  }

  private Node cloneElement(Element oelem, Node context) {
    String tagName = oelem.getTagName();
    String prefix = null;
    String uri = oelem.getNamespaceURI();
    String xmlns = "xmlns";
    int pos = tagName.indexOf(":");

    if (pos >= 0) {
      prefix = tagName.substring(0,pos);
      xmlns = "xmlns:" + prefix;
    }

    Node node = doc.createElementNS(uri,tagName);

    if (uri != null && !uri.equals("") && !oelem.hasAttribute(xmlns)) {
      // this element has a namespace; make sure we have it in scope
      if (!inScopeNamespace(context, prefix, uri)) {
	((Element) node).setAttribute(xmlns, uri);
      }
    }

    NamedNodeMap attrNM = oelem.getAttributes();
    for (int count = 0; count < attrNM.getLength(); count++) {
      Attr attr = (Attr) attrNM.item(count);
      uri = attr.getNamespaceURI();
      tagName = attr.getName();
      prefix = null;
      xmlns = "xmlns";
      pos = tagName.indexOf(":");

      if (pos >= 0) {
	prefix = tagName.substring(0,pos);
	if (prefix.equals("xmlns") || prefix.equals("xml")) {
	  // this one doesn't count
	  prefix = null;
	} else {
	  xmlns = "xmlns:" + prefix;
	}
      }

      if (prefix != null
	  && uri != null && !uri.equals("")
	  && !oelem.hasAttribute(xmlns)) {
	// this element has a namespace; make sure we have it in scope
	if (!inScopeNamespace(context, prefix, uri)) {
	  ((Element) node).setAttribute(xmlns, uri);
	}
      }

      ((Element) node).setAttributeNS(uri, tagName, attr.getValue());
    }

    return node;
  }

  private Node cloneText(Text otext, Node context) {
    return doc.createTextNode(otext.getData());
  }

  private Node cloneCDATASection(CDATASection node, Node context) {
    return doc.createCDATASection(node.getData());
  }

  private Node cloneProcessingInstruction(ProcessingInstruction node,
					  Node context) {
    return doc.createProcessingInstruction(node.getTarget(),
					   node.getData());
  }

  private Node cloneComment(Comment node, Node context) {
    return doc.createComment(node.getData());
  }
}
