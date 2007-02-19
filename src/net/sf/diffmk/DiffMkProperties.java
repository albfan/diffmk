// DiffMkProperties.java - Access DiffMk.properties

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

import java.lang.ClassNotFoundException;
import java.lang.IllegalAccessException;
import java.lang.InstantiationException;
import java.lang.SecurityException;
import java.lang.ClassCastException;
import java.net.URL;
import java.net.MalformedURLException;

import java.io.*;
import java.util.*;

/**
 * <p>DiffMkProperties provides an interface to the DiffMk.properties
 * file.</p>
 *
 * @author Norman Walsh
 * <a href="mailto:Norman.Walsh@Sun.COM">Norman.Walsh@Sun.COM</a>
 *
 * @version 1.0
 */

public class DiffMkProperties {
  /** Flag to ignore missing property files and/or properties */
  private static boolean ignoreMissingProperties = false;

  /** Holds the resources after they are loaded from the file. */
  private static ResourceBundle resources;

  /** The name of the properties file. */
  private static String propertyFile = "DiffMk.properties";

  /** The location of the propertyFile */
  private static URL propertyFileURI = null;

  /** Default catalog files list. */
  private static String defaultConfigFile = "diffmk.xml";

  /** Default validating. */
  private static boolean defaultValidating = false;

  /** Default namespace aware. */
  private static boolean defaultNamespaceAware = true;

  /** Default verbosity. */
  private static int defaultVerbose = 1;

  /** Default diff mode. */
  private static String defaultDiff = "both";

  /** Default words mode. */
  private static boolean defaultWords = false;

  /** Default ignore whitespace setting. */
  private static boolean defaultIgnoreWhitespace = true;

  /**
   * <p>Load the properties from the propertyFile and build the
   * resources from it.</p>
   */
  private synchronized static void readProperties() {
    try {
      propertyFileURI = DiffMkProperties.class.getResource("/"+propertyFile);
      InputStream in =
	DiffMkProperties.class.getResourceAsStream("/"+propertyFile);
      if (in==null) {
	if (!ignoreMissingProperties) {
	  System.err.println("Cannot find "+propertyFile + " on CLASSPATH.");
	}
	return;
      }
      resources = new PropertyResourceBundle(in);
    } catch (MissingResourceException mre) {
      if (!ignoreMissingProperties) {
	System.err.println("Cannot read "+propertyFile);
      }
    } catch (java.io.IOException e) {
      if (!ignoreMissingProperties) {
	System.err.println("Failure trying to read "+propertyFile);
      }
    }
  }

  /**
   * <p>Tell the CatalogManager how to handle missing properties.</p>
   *
   * <p>If ignore is true, missing or unreadable property files will
   * not be reported. Otherwise, a message will be sent to System.err.
   * </p>
   */
  public static void ignoreMissingProperties(boolean ignore) {
    ignoreMissingProperties = ignore;
  }

  /**
   * <p>Obtain the config file setting from the properties.</p>
   *
   * @return The config file from the propertyFile or the
   * defaultConfigFile.
   */
  public static String configFile  () {
    if (resources==null) readProperties();

    if (resources==null) return defaultConfigFile;

    try {
      return resources.getString("config");
    } catch (MissingResourceException e) {
      return defaultConfigFile;
    }
  }

  /**
   * <p>Obtain the verbosity setting from the properties.</p>
   *
   * @return The verbosity level from the propertyFile or the
   * defaultVerbosity.
   */
  public static int verbose () {
    if (resources==null) readProperties();

    if (resources==null) return defaultVerbose;

    try {
      String verbStr = resources.getString("verbose");
      try {
	return Integer.parseInt(verbStr.trim());
      } catch (Exception e) {
	System.err.println("Cannot parse verbose: \"" + verbStr + "\"");
	return defaultVerbose;
      }
    } catch (MissingResourceException e) {
      return defaultVerbose;
    }
  }

  /**
   * <p>Obtain the diff setting from the properties.</p>
   *
   * @return The diff setting from the propertyFile or the
   * defaultDiff.
   */
  public static String diff () {
    if (resources==null) readProperties();

    if (resources==null) return defaultDiff;

    try {
      return resources.getString("diff");
    } catch (MissingResourceException e) {
      return defaultDiff;
    }
  }

  /**
   * <p>Obtain the words setting from the properties.</p>
   *
   * @return The words setting from the propertyFile or the
   * defaultWords.
   */
  public static boolean words () {
    if (resources==null) readProperties();

    if (resources==null) return defaultWords;

    try {
      String allow = resources.getString("words");
      return (allow.equalsIgnoreCase("true")
	      || allow.equalsIgnoreCase("yes")
	      || allow.equalsIgnoreCase("1"));
    } catch (MissingResourceException e) {
      return defaultWords;
    }
  }

  /**
   * <p>Obtain the validating setting from the properties.</p>
   *
   * <p>In the properties, a value of 'yes', 'true', or '1' is considered
   * true, anything else is false.</p>
   *
   * @return The validating setting from the propertyFile or the
   * defaultValidating.
   */
  public static boolean validating () {
    if (resources==null) readProperties();

    if (resources==null) return defaultValidating;

    try {
      String allow = resources.getString("validating");
      return (allow.equalsIgnoreCase("true")
	      || allow.equalsIgnoreCase("yes")
	      || allow.equalsIgnoreCase("1"));
    } catch (MissingResourceException e) {
      return defaultValidating;
    }
  }

  /**
   * <p>Obtain the namespace aware setting from the properties.</p>
   *
   * <p>In the properties, a value of 'yes', 'true', or '1' is considered
   * true, anything else is false.</p>
   *
   * @return The namespace aware setting from the propertyFile or the
   * defaultValidating.
   */
  public static boolean namespaceAware () {
    if (resources==null) readProperties();

    if (resources==null) return defaultNamespaceAware;

    try {
      String allow = resources.getString("namespaceaware");
      return (allow.equalsIgnoreCase("true")
	      || allow.equalsIgnoreCase("yes")
	      || allow.equalsIgnoreCase("1"));
    } catch (MissingResourceException e) {
      return defaultNamespaceAware;
    }
  }

  /**
   * <p>Obtain the ignorewhitespace setting from the properties.</p>
   *
   * <p>In the properties, a value of 'yes', 'true', or '1' is considered
   * true, anything else is false.</p>
   *
   * @return The ignorewhitespace setting from the propertyFile or the
   * defaultValidating.
   */
  public static boolean ignoreWhitespace () {
    if (resources==null) readProperties();

    if (resources==null) return defaultIgnoreWhitespace;

    try {
      String allow = resources.getString("ignorewhitespace");
      return (allow.equalsIgnoreCase("true")
	      || allow.equalsIgnoreCase("yes")
	      || allow.equalsIgnoreCase("1"));
    } catch (MissingResourceException e) {
      return defaultIgnoreWhitespace;
    }
  }
}


