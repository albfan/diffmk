/* Serialize */

package net.sf.diffmk.xmlutil;

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

import java.io.PrintWriter;
import org.w3c.dom.*;

public class Serializer {
  protected PrintWriter out = null;

  public Serializer(PrintWriter outWriter) {
    out = outWriter;
  }

  public void serialize(Document doc, String preamble) {
    if (out == null) {
      // what's the point?
      return;
    }

    out.print(preamble);
    dump(doc);
    out.println("");
  }

  protected void dump(Node node) {
    if (node == null) {
      return;
    }

    switch (node.getNodeType()) {
    case Node.DOCUMENT_NODE:
      dumpDocument((Document) node);
      break;
    case Node.ELEMENT_NODE:
      dumpElement((Element) node);
      break;
    case Node.TEXT_NODE:
      dumpText((Text) node);
      break;
    case Node.CDATA_SECTION_NODE:
      dumpCDATASection((CDATASection) node);
      break;
    case Node.COMMENT_NODE:
      dumpComment((Comment) node);
      break;
    case Node.PROCESSING_INSTRUCTION_NODE:
      dumpProcessingInstruction((ProcessingInstruction) node);
      break;
    case Node.DOCUMENT_TYPE_NODE:
      dumpDocumentType((DocumentType) node);
      break;
    default:
      System.err.println("Unexpected node type in serializer: " + node.getNodeType());
    }
  }

  protected void dumpDocument(Document node) {
    Node child = node.getFirstChild();
    while (child != null) {
      dump(child);
      child = child.getNextSibling();
    }
  }

  protected void dumpElement(Element elem) {
    out.print("<");
    out.print(elem.getTagName());

    NamedNodeMap attributes = elem.getAttributes();
    for (int count = 0; count < attributes.getLength(); count++) {
      Attr attr = (Attr) attributes.item(count);
      String quote = "\"";
      String value = attr.getValue();

      out.print(" " + attr.getName() + "=" + quote);
      out.print(value);
      out.print(quote);
    }

    out.print(">");

    Node child = elem.getFirstChild();
    while (child != null) {
      dump(child);
      child = child.getNextSibling();
    }

    out.print("</");
    out.print(elem.getTagName());
    out.print(">");
  }

  protected void dumpText(Text node) {
    out.print(escape(node.getData()));
  }

  protected void dumpCDATASection(CDATASection node) {
    out.print("<![CDATA[");
    out.print(node.getData());
    out.print("]]>");
  }

  protected void dumpComment(Comment node) {
    out.print("<!--");
    out.print(node.getData());
    out.print("-->");
  }

  protected void dumpProcessingInstruction(ProcessingInstruction pi) {
    out.print("<?");
    out.print(pi.getTarget());
    if (pi.getData() != null) {
      out.print(" ");
      out.print(escape(pi.getData()));
    }
    out.print("?>");
  }

  protected void dumpDocumentType(DocumentType node) {
    //nop
  }

  private String escape(String str) {
    str = stringReplace(str, "&", "&amp;");
    str = stringReplace(str, "<", "&lt;");
    str = stringReplace(str, ">", "&gt;");
    return str;
  }

  /**
   * <p>Replace one string with another.</p>
   *
   */
  private String stringReplace(String str,
				      String oldStr,
				      String newStr) {

    String result = "";
    int pos = str.indexOf(oldStr);

    //    System.out.println(str + ": " + oldStr + " => " + newStr);

    while (pos >= 0) {
      //      System.out.println(str + " (" + pos + ")");
      result += str.substring(0, pos);
      result += newStr;
      str = str.substring(pos+1);

      pos = str.indexOf(oldStr);
    }

    return result + str;
  }
}
