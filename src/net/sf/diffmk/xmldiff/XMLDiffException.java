// XMLDiffException.java - XMLDiff exception

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

/**
 * <p>Signal XMLDiff exception.</p>
 *
 * <p>This exception is thrown if an error occurs computing an XMLDiff.
 * </p>
 *
 * @see XMLDiff
 *
 * @author Norman Walsh
 * <a href="mailto:Norman.Walsh@Sun.COM">Norman.Walsh@Sun.COM</a>
 *
 * @version 1.0
 */
public class XMLDiffException extends Exception {
  /**
   * The embedded exception if tunnelling, or null.
   */
  private Exception exception = null;

  /**
   * Create a new XMLDiffException.
   *
   * @param message The error or warning message.
   */
  public XMLDiffException (String message) {
    super(message);
    this.exception = null;
  }

  /**
   * Create a new XMLDiffException.
   */
  public XMLDiffException () {
    super("XMLDiff Exception");
    this.exception = null;
  }

  /**
   * Create a new XMLDiffException wrapping an existing exception.
   *
   * <p>The existing exception will be embedded in the new
   * one, and its message will become the default message for
   * the XMLDiffException.</p>
   *
   * @param e The exception to be wrapped in a XMLDiffException.
   */
  public XMLDiffException (Exception e) {
    super();
    this.exception = e;
  }

  /**
   * Create a new XMLDiffException from an existing exception.
   *
   * <p>The existing exception will be embedded in the new
   * one, but the new exception will have its own message.</p>
   *
   * @param message The detail message.
   * @param e The exception to be wrapped in a XMLDiffException.
   */
  public XMLDiffException (String message, Exception e) {
    super(message);
    this.exception = e;
  }

  /**
   * Return a detail message for this exception.
   *
   * <p>If there is an embedded exception, and if the XMLDiffException
   * has no detail message of its own, this method will return
   * the detail message from the embedded exception.</p>
   *
   * @return The error or warning message.
   */
  public String getMessage ()
  {
    String message = super.getMessage();

    if (message == null && exception != null) {
      return exception.getMessage();
    } else {
      return message;
    }
  }

  /**
   * Return the embedded exception, if any.
   *
   * @return The embedded exception, or null if there is none.
   */
  public Exception getException ()
  {
    return exception;
  }

  /**
   * Override toString to pick up any embedded exception.
   *
   * @return A string representation of this exception.
   */
  public String toString ()
  {
    if (exception != null) {
      return exception.toString();
    } else {
      return super.toString();
    }
  }
}
