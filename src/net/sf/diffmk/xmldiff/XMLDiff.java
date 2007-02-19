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
import java.io.IOException;
import java.io.PrintStream;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Enumeration;

import org.w3c.dom.*;
import org.xml.sax.*;

import javax.xml.parsers.*;

import net.sf.diffmk.xmldiff.NodeDiff;
import net.sf.diffmk.xmldiff.XMLDiffException;
import net.sf.diffmk.util.WhitespaceTokenizer;

import bmsi.util.Diff;

public class XMLDiff {
  protected PrintStream msgOut = null;
  protected PrintStream debugOut = null;

  protected boolean nsAware = true;
  protected boolean validate = true;
  protected boolean diffElements = false;
  protected boolean diffText = true;
  protected boolean diffWords = false;

  protected Diff.change script = null;
  protected NodeDiff nodes1[] = null;
  protected NodeDiff nodes2[] = null;
  protected DiffMarkup diffMarkup = null;

  protected Document doc1 = null;
  protected Document doc2 = null;

  protected Vector newNodeList = new Vector();
  protected int doc2Pos = 0;

  protected int verbose = 0;

  public XMLDiff(PrintStream msgOut, PrintStream debugOut) {
    this.msgOut = msgOut;
    this.debugOut = debugOut;
  }

  public XMLDiff(PrintStream msgOut, PrintStream debugOut, int verbose) {
    this.msgOut = msgOut;
    this.debugOut = debugOut;
    this.verbose = verbose;
  }

  public void setDiffMarkup(DiffMarkup dm) {
    diffMarkup = dm;
    NodeDiff.setDiffMarkup(dm);
  }

  public void setNamespaceAware(boolean aware) {
    nsAware = aware;
  }

  public void setValidating(boolean validating) {
    validate = validating;
  }

  public void setDiffElements(boolean diff) {
    diffElements = diff;
  }

  public void setDiffText(boolean diff) {
    diffText = diff;
  }

  public void setDiffWords(boolean diff) {
    diffWords = diff;
  }

  public Diff.change getScript() {
    return script;
  }

  public Document getDocument1() {
    return doc1;
  }

  public Document getDocument2() {
    return doc2;
  }

  public Document getNewDocument(DiffMarkup diffMarkup) {
    DiffDocBuilder builder = new DiffDocBuilder(diffMarkup,debugOut);

    for (int count = 0; count < newNodeList.size(); count++) {
      NodeDiff nd = (NodeDiff) newNodeList.get(count);
      //      if (!nd.isText() && nd.getNode().getNodeType() == Node.ELEMENT_NODE) {
      //System.err.println("LIST: " + count + ": " + nd);
	//      }
    }

    builder.build(newNodeList);
    return builder.getDocument();
  }

  public NodeDiff[] getNodes1() {
    return nodes1;
  }

  public NodeDiff[] getNodes2() {
    return nodes2;
  }

  public void computeDiff(String file1, String file2)
    throws XMLDiffException {

    DocumentBuilderFactory factory = null;
    DocumentBuilder builder = null;

    factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(nsAware);
    factory.setValidating(validate);
    try {
      builder = factory.newDocumentBuilder();
    } catch (ParserConfigurationException pce) {
      throw new XMLDiffException(pce);
    }

    try {
      message("Loading "+file1+"...");
      doc1 = builder.parse(file1);
      message("Loading "+file2+"...");
      doc2 = builder.parse(file2);
    } catch (SAXException se) {
      throw new XMLDiffException(se);
    } catch (IOException ioe) {
      throw new XMLDiffException(ioe);
    }

    computeDiff(doc1, doc2);
  }

  public void computeDiff(Document doc1, Document doc2) {
    this.doc1 = doc1;
    this.doc2 = doc2;

    message("Calculating node list for first file...");
    Vector nodesV1 = inorder_nodes(doc1.getDocumentElement());
    message(nodesV1.size() + " nodes");

    // Now convert the vectors into arrays of DiffObj
    nodes1 = new NodeDiff[nodesV1.size()];

    for (int count = 0; count < nodesV1.size(); count++) {
      nodes1[count] = (NodeDiff) nodesV1.get(count);
      if (debugOut != null) {
	debugOut.println("1f: " + count + ": " + nodes1[count].toString());
      }
    }

    message("Calculating node list for second file...");
    Vector nodesV2 = inorder_nodes(doc2.getDocumentElement());
    message(nodesV2.size() + " nodes");

    // Now convert the vectors into arrays of DiffObj
    nodes2 = new NodeDiff[nodesV2.size()];

    for (int count = 0; count < nodesV2.size(); count++) {
      nodes2[count] = (NodeDiff) nodesV2.get(count);
      if (debugOut != null) {
	debugOut.println("2f: " + count + ": " + nodes2[count].toString());
      }
    }

    message("Calculating differences; this can take a (long) while...");

    //    for (int count = 0; count < nodesV1.size(); count++) {
    //      System.err.println("1: " + nodes1[count]);
    //    }

    //    for (int count = 0; count < nodesV2.size(); count++) {
    //      System.err.println("2: " + nodes2[count]);
    //    }

    Diff diff = new Diff(nodes1,nodes2);

    //    System.err.println("diffing done?");

    script = diff.diff_2(false);
  }

  private Vector inorder_nodes(Node node) {
    Vector nodes = new Vector();
    NodeDiff ndNode = null;

    if (node.getNodeType() == Node.ELEMENT_NODE) {
      ndNode = new NodeDiff(node,false);
      nodes.add(ndNode);

      Node child = node.getFirstChild();
      while (child != null) {
	nodes.addAll(inorder_nodes(child));
	child = child.getNextSibling();
      }
      ndNode = new NodeDiff(node,true);
      nodes.add(ndNode);
    } else if (node.getNodeType() == Node.TEXT_NODE
	       || node.getNodeType() == Node.CDATA_SECTION_NODE) {
      Text textNode = null;

      if (node.getNodeType() == Node.TEXT_NODE) {
	textNode = (Text) node;
      } else {
	Document doc = node.getOwnerDocument();
	textNode = doc.createTextNode(((CDATASection) node).getData());
      }

      // What about verbatim?
      boolean verbatim = false;
      Node parent = node.getParentNode();
      while (!verbatim && parent != null) {
	if (parent.getNodeType() == Node.ELEMENT_NODE) {
	  verbatim = verbatim || diffMarkup.verbatimElement(parent.getNamespaceURI(),
							    parent.getLocalName());
	}
	parent = parent.getParentNode();
      }

      if (!verbatim && diffWords) {
	WhitespaceTokenizer st = new WhitespaceTokenizer(textNode.getData());
	while (st.hasMoreTokens()) {
	  String token = st.nextToken();
	  ndNode = new NodeDiff(token);
	  nodes.add(ndNode);
	}
      } else {
	ndNode = new NodeDiff(node,false);
	nodes.add(ndNode);
      }
    } else {
      //System.err.println("inorder_node: " + node);
      ndNode = new NodeDiff(node,false);
      nodes.add(ndNode);
    }

    return nodes;
  }

  /** Divide SCRIPT into pieces by calling HUNKFUN and
      print each piece with PRINTFUN.
      Both functions take one arg, an edit script.

      PRINTFUN takes a subscript which belongs together (with a null
      link at the end) and prints it.  */
  public void update() {
    Diff.change next = script;

    while (next != null) {
      Diff.change t, end;

      /* Find a set of changes that belong together.  */
      t = next;
      end = hunkfun(next);

      /* Disconnect them from the rest of the changes,
	 making them a hunk, and remember the rest for next iteration.  */
      next = end.link;
      end.link = null;

      /* Print this hunk.  */
      update_hunk(t);

      /* Reconnect the script so it will all be freed properly.  */
      end.link = next;
    }

    while (doc2Pos < nodes2.length) {
      if (debugOut != null) {
	debugOut.println("3: = " + nodes2[doc2Pos]);
      }
      newNodeList.add(nodes2[doc2Pos]);
      doc2Pos++;
    }
  }

  /** Called with the tail of the script
      and returns the last link that belongs together with the start
      of the tail. */
  protected Diff.change hunkfun(Diff.change hunk) {
    return hunk;
  }

  protected int first0, last0, first1, last1, deletes, inserts;

  /** Look at a hunk of edit script and report the range of lines in each file
      that it applies to.  HUNK is the start of the hunk, which is a chain
      of `struct change'.  The first and last line numbers of file 0 are stored
      in *FIRST0 and *LAST0, and likewise for file 1 in *FIRST1 and *LAST1.
      Note that these are internal line numbers that count from 0.

      If no lines from file 0 are deleted, then FIRST0 is LAST0+1.

      Also set *DELETES nonzero if any lines of file 0 are deleted
      and set *INSERTS nonzero if any lines of file 1 are inserted.
      If only ignorable lines are inserted or deleted, both are
      set to 0.  */
  protected void analyze_hunk(Diff.change hunk) {
    int f0, l0 = 0, f1, l1 = 0, show_from = 0, show_to = 0;
    int i;
    Diff.change next;

    show_from = show_to = 0;

    f0 = hunk.line0;
    f1 = hunk.line1;

    for (next = hunk; next != null; next = next.link) {
      l0 = next.line0 + next.deleted - 1;
      l1 = next.line1 + next.inserted - 1;
      show_from += next.deleted;
      show_to += next.inserted;
    }

    first0 = f0;
    last0 = l0;
    first1 = f1;
    last1 = l1;

    deletes = show_from;
    inserts = show_to;
  }

  protected void delete_node(int pos) {
    if (debugOut != null) {
      debugOut.println("3: d " + nodes1[pos]);
    }

    if (nodes1[pos].isText()) {
      String text = nodes1[pos].getText();
      if (text.trim().equals("")) {
	// nop; don't mark changes in whitespace
      } else {
	nodes1[pos].setStatus(NodeDiff.DELETED);
      }
    } else {
      nodes1[pos].setStatus(NodeDiff.DELETED);
    }

    newNodeList.add(nodes1[pos]);
  }

  protected void insert_node(int pos) {
    if (debugOut != null) {
      debugOut.println("3: i " + nodes2[pos]);
    }

    //nodes2[pos].setStatus(NodeDiff.INSERTED);
    if (nodes2[pos].isText()) {
      String text = nodes2[pos].getText();
      if (text.trim().equals("")) {
	// nop; don't mark changes in whitespace
      } else {
	nodes2[pos].setStatus(NodeDiff.INSERTED);
      }
    } else {
      nodes2[pos].setStatus(NodeDiff.INSERTED);
    }

    newNodeList.add(nodes2[pos]);

    doc2Pos = pos+1;
  }

  protected void change_node(int pos) {
    int nodeType;
    boolean change = true;

    if (nodes2[pos].isText()) {
      nodeType = Node.TEXT_NODE;

      // Wait, if this is a text node and it's purely whitespace and we're
      // ignoring whitespace, then it isn't changed at all!
      // FIXME: check ignorewhitespace

      if (nodes2[pos].getText().trim().equals("")) {
	if (debugOut != null) {
	  debugOut.println("3: ! " + nodes2[pos]);
	}
	nodes2[pos].setStatus(NodeDiff.UNCHANGED);
	newNodeList.add(nodes2[pos]);
	doc2Pos = pos+1;
	return;
      }
    } else {
      nodeType = nodes2[pos].getNode().getNodeType();
    }

    if ((nodeType == Node.TEXT_NODE && diffText)
	|| (nodeType == Node.ELEMENT_NODE && diffElements)) {
      if (debugOut != null) {
	debugOut.println("3: c " + nodes2[pos]);
      }

      nodes2[pos].setStatus(NodeDiff.CHANGED);
      newNodeList.add(nodes2[pos]);
      doc2Pos = pos+1;
    }
  }

  protected boolean isomorphic(int first0, int first1, int len) {
    boolean theSame = true;
    for (int count = 0; count < len && theSame; count++) {
      NodeDiff dDiff = nodes1[first0+count];
      NodeDiff iDiff = nodes2[first1+count];

      if (dDiff.isText() || iDiff.isText()) {
	theSame = (dDiff.isText() == iDiff.isText());
      } else {
	if (dDiff.getNode().getNodeType() != iDiff.getNode().getNodeType()) {
	  theSame = false;
	} else {
	  if (dDiff.getNode().getNodeType() == Node.ELEMENT_NODE) {
	    if (dDiff.isEndTag() != iDiff.isEndTag()) {
	      theSame = false;
	    } else {
	      Node dNode = dDiff.getNode();
	      Node iNode = iDiff.getNode();

	      String dNS = dNode.getNamespaceURI();
	      String iNS = iNode.getNamespaceURI();

	      if (dNS == null || iNS == null) {
		theSame = (dNS == null && iNS == null);
	      } else {
		theSame = dNS.equals(iNS);
	      }

	      theSame = dNode.getLocalName().equals(iNode.getLocalName());
	    }
	  }
	}
      }
    }

    return theSame;
  }

  /** Print a hunk of a normal diff.
      This is a contiguous portion of a complete edit script,
      describing changes in consecutive lines.  */
  protected void update_hunk (Diff.change hunk) {
    /* Determine range of line numbers involved in each file.  */
    analyze_hunk(hunk);
    if (deletes == 0 && inserts == 0)
      return;

    if (debugOut != null) {
      debugOut.println("--- block of changes ---");
      debugOut.println("--- (" + first0 + "," + last0 + ")-(" + first1 + "," + last1 + ")");
    }

    // f1 is the first line of doc2 that needs to be modified...
    while (doc2Pos < first1) {
      if (debugOut != null) {
	debugOut.println("3: = " + nodes2[doc2Pos]);
      }
      newNodeList.add(nodes2[doc2Pos]);
      doc2Pos++;
    }

    // If we have a sequence of changed nodes and:
    // 1. The sequences are the same length and
    // 2. Considering the sequence of nodes pairwise,
    // 2a. Each node is a text node or
    // 2b. Each node is a start tag for the same element or
    // 2c. Each node is an end tag for the same element, then:
    // Mark the sequence of nodes as changed instead of
    // an insert followed by a delete.
    if ((last1 - first1) == (last0 - first0)
	&& isomorphic(first0, first1, last1-first1+1)) {
      for (int count = first1; count <= last1; count++) {
	change_node(count);
      }
      return;
    }

    /* Update the nodes that the second file has.  */
    if (inserts != 0) {
      for (int i = first1; i <= last1; i++) {
	insert_node (i);
      }
    }

    /* Update the nodes that the first file has.  */
    if (deletes != 0) {
      for (int i = first0; i <= last0; i++) {
	delete_node (i);
      }
    }
  }

  // ============================================================

  protected void message(String message) {
    if (verbose > 0) {
      msgOut.println(message);
    }
  }

  protected void message(String message, int level) {
    if (verbose >= level) {
      msgOut.println(message);
    }
  }
}
