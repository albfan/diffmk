// WhitespaceTokenizer.java - Like StringTokenizer but returns the whitespace

package net.sf.diffmk.util;

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

import java.util.Enumeration;

/**
 * <p>Like StringTokenizer but returns the whitespace.</p>
 *
 * @author Norman Walsh
 * <a href="mailto:Norman.Walsh@Sun.COM">Norman.Walsh@Sun.COM</a>
 *
 * @version 1.0
 */
public class WhitespaceTokenizer implements Enumeration {
  private String str = null;

  public WhitespaceTokenizer(String str) {
    this.str = str;
  }

  public boolean hasMoreElements() {
    return str != null && !str.equals("");
  }

  public boolean hasMoreTokens() {
    return hasMoreElements();
  }

  public Object nextElement() {
    return nextToken();
  }

  public String nextToken() {
    if (!hasMoreTokens()) {
      return null;
    }

    int pos = 0;
    boolean whitespace = isWhitespace(str.substring(0,1));
    while (pos < str.length()
	   && whitespace == isWhitespace(str.substring(pos,pos+1))) {
      pos++;
    }

    String token = str.substring(0,pos);
    str = str.substring(pos);

    return token;
  }

  private boolean isWhitespace(String ch) {
    return (ch.equals(" ")
	    || ch.equals("\t")
	    || ch.equals("\r")
	    || ch.equals("\n"));
  }
}
