/* XMLContext */

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
import org.xml.sax.*;

public class XMLContext {
  public static final int BEFORE = 0;
  public static final int AFTER = 1;
  public static final int APPEND = 2;
  public static final int BEGINNING = 3;

  protected Node node = null;
  protected int place = -1;

  public XMLContext() {
    // nop;
  }

  public Node getNode() {
    return node;
  }

  public Node getParentNode() {
    adjustContext();

    if (place == BEFORE || place == AFTER) {
      return node.getParentNode();
    } else { // APPEND
      return node;
    }
  }

  public int getPlace() {
    return place;
  }

  public void setContext(Node contextNode, int contextPlace) {
    node = contextNode;
    place = contextPlace;
  }

  public void insertNode(Node newNode) {
    adjustContext();

    if (place == BEFORE) {
      Node parent = node.getParentNode();
      parent.insertBefore(newNode, node);
    } else if (place == APPEND) {
      node.appendChild(newNode);
    }

    node = newNode;

    if (newNode.getNodeType() == Node.ELEMENT_NODE) {
      place = APPEND;
    } else {
      place = AFTER;
    }
  }

  public void skip(Node node) {
    adjustContext();
    place = AFTER;
    this.node = node;
  }

  protected void adjustContext() {
    if (place == AFTER) {
      if (node.getNextSibling() != null) {
	node = node.getNextSibling();
	place = BEFORE;
      } else {
	place = APPEND;
	node = node.getParentNode();
      }
    } else if (place == BEGINNING) {
      if (node.getFirstChild() != null) {
	node = node.getFirstChild();
	place = BEFORE;
      } else {
	place = APPEND;
      }
    }
  }
}



