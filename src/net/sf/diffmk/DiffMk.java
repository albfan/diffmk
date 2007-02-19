/* DiffMk */

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
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.FileInputStream;
import java.util.Vector;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.w3c.dom.*;

import javax.xml.parsers.*;

import net.sf.diffmk.util.CommandOpts;
import net.sf.diffmk.xmlutil.Serializer;
import net.sf.diffmk.DiffMkConfig;
import net.sf.diffmk.DiffMkProperties;
import net.sf.diffmk.xmldiff.XMLDiff;
import net.sf.diffmk.xmldiff.XMLDiffException;
import net.sf.diffmk.xmldiff.NodeDiff;
import net.sf.diffmk.ui.UI;

import bmsi.util.Diff;

public class DiffMk {
  protected static PrintWriter xmlOut = null;
  protected static PrintStream debugOut = null;
  protected static DiffMkConfig config = new DiffMkConfig();
  protected static String configFile = "";
  protected static CommandOpts options = new CommandOpts();

  public static String version = "XML DiffMK Java 1.0 beta 1";

  public static void main(String[] args) throws IOException {
    options.addOption("debugout", CommandOpts.STRING);
    options.addOption("verbose", CommandOpts.INTEGER);
    options.addOption("quiet", CommandOpts.BOOLEAN);
    options.addOption("help", CommandOpts.BOOLEAN);
    options.addOption("diff", CommandOpts.STRING);
    options.addOption("words", CommandOpts.BOOLEAN);
    options.addOption("output", CommandOpts.STRING);
    options.addOption("ignorewhitespace", CommandOpts.BOOLEAN);
    options.addOption("doctype", CommandOpts.STRING);
    options.addOption("namespaceaware", CommandOpts.BOOLEAN);
    options.addOption("validating", CommandOpts.BOOLEAN);
    options.addOption("config", CommandOpts.STRING);
    options.addOption("ui", CommandOpts.BOOLEAN);

    options.setOption("debugout", "");
    options.setOption("verbose", 1);
    options.setOption("quiet", false);
    options.setOption("help", false);
    options.setOption("diff", "text");
    options.setOption("words", false);
    options.setOption("output", "");
    options.setOption("ignorewhitespace", true);
    options.setOption("doctype", "");
    options.setOption("namespaceaware", true);
    options.setOption("validating", true);
    options.setOption("config", "");
    options.setOption("ui", false);

    int pos = options.parseArgs(args);
    String file1 = null;
    String file2 = null;
    String xmlOutFilename = null;

    if (options.getBooleanOption("help")) {
      usage("");
      System.exit(1);
    }

    if (pos < args.length) {
      file1 = args[pos++];
    }

    if (pos < args.length) {
      file2 = args[pos++];
    }

    if (pos < args.length && !options.argumentSpecified("output")) {
      xmlOutFilename = args[pos++];
    } else {
      xmlOutFilename = options.getStringOption("output");
    }

    if (pos < args.length) {
      usage("Too many filenames.");
      System.exit(2);
    }

    configFile             = (options.argumentSpecified("config")
			      ? options.getStringOption("config")
			      : DiffMkProperties.configFile());

    boolean validating     = (options.argumentSpecified("validating")
			      ? options.getBooleanOption("validating")
			      : DiffMkProperties.validating());

    boolean namespaceAware = (options.argumentSpecified("namespaceaware")
			      ? options.getBooleanOption("namespaceaware")
			      : DiffMkProperties.namespaceAware());

    int verbose            = (options.argumentSpecified("verbose")
			      ? options.getIntegerOption("verbose")
			      : DiffMkProperties.verbose());

    String diffType        = (options.argumentSpecified("diff")
			      ? options.getStringOption("diff")
			      : DiffMkProperties.diff());

    boolean diffWords      = (options.argumentSpecified("words")
			      ? options.getBooleanOption("words")
			      : DiffMkProperties.words());

    boolean ignoreWhitespace = (options.argumentSpecified("ignorewhitespace")
				? options.getBooleanOption("ignorewhitespace")
				: DiffMkProperties.ignoreWhitespace());

    // check the options...
    if (!diffType.equals("text")
	&& !diffType.equals("element")
	&& !diffType.equals("both")) {
      usage("Unknown diff type: " + diffType);
      System.exit(2);
    }

    boolean quiet = options.getBooleanOption("quiet");
    if (quiet) {
      verbose = 0;
    }

    if (verbose > 0) {
      System.out.println("Loading DiffMk config: " + configFile);
    }

    try {
      if (!config.parse(configFile)) {
	System.exit(2);
      }
    } catch (IOException ioe) {
      System.err.println("Failed to read config file: " + configFile);
      System.exit(2);
    }

    String diffDoctype = options.getStringOption("doctype");

    if (options.getBooleanOption("ui")) {
      UI ui = new UI();
      if (file1 != null) { ui.oldFile = file1; }
      if (file2 != null) { ui.newFile = file2; }
      if (xmlOutFilename != null) { ui.diffFile = xmlOutFilename; }

      ui.diffTypeElement = diffType.equals("element");
      ui.diffTypeText = diffType.equals("text");
      ui.diffTypeBoth = diffType.equals("both");

      ui.wordOpt = diffWords;
      ui.nsOpt = namespaceAware;
      ui.ignOpt = ignoreWhitespace;
      ui.valOpt = validating;
      ui.verbosity = verbose;

      if (diffDoctype != null) {
	ui.doctype = diffDoctype;
      }

      ui.show();
    } else {
      doDiff(file1, file2, xmlOutFilename, diffDoctype, diffType,
	     diffWords, namespaceAware, ignoreWhitespace, validating,
	     verbose);
    }
  }

  public static void doDiff(String file1,
			    String file2,
			    String xmlOutFilename,
			    String diffDoctype,
			    String diffType,
			    boolean diffWords,
			    boolean namespaceAware,
			    boolean ignoreWhitespace,
			    boolean validating,
			    int verbose) {
    int pos;

    if (xmlOutFilename == null) {
      System.err.println("You must specify an output filename.");
      System.exit(2);
    }

    if (diffDoctype != null && !diffDoctype.equals("")
	&& !config.doctype(diffDoctype)) {
      System.err.println("No doctype '" + diffDoctype + "' identified in");
      System.err.println(configFile);
      System.exit(2);
    }

    DocumentBuilderFactory factory = null;
    DocumentBuilder builder = null;

    Document doc1 = null;
    Document doc2 = null;

    factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(namespaceAware);
    factory.setValidating(validating);
    try {
      builder = factory.newDocumentBuilder();
    } catch (ParserConfigurationException pce) {
      System.err.println("Parser configuration exception: " + pce);
      System.exit(2);
    }

    try {
      if (verbose > 0) {
	System.out.println("Loading "+file1+"...");
      }
      doc1 = builder.parse(file1);
    } catch (SAXException se) {
      System.err.println("SAX exception loading " + file1);
      System.err.println(se);
      System.exit(2);
    } catch (IOException ioe) {
      System.err.println("I/O exception: " + file1);
      System.err.println(ioe);
      System.exit(2);
    }

    try {
      if (verbose > 0) {
	System.out.println("Loading "+file2+"...");
      }
      doc2 = builder.parse(file2);
    } catch (SAXException se) {
      System.err.println("SAX exception loading " + file2);
      System.err.println(se);
      System.exit(2);
    } catch (IOException ioe) {
      System.err.println("I/O exception: " + file2);
      System.err.println(ioe);
      System.exit(2);
    }

    // Setup the output file
    try {
      FileOutputStream xmlStream = new FileOutputStream(xmlOutFilename);
      OutputStreamWriter xmlWriter = new OutputStreamWriter(xmlStream, "utf-8");
      xmlOut = new PrintWriter(xmlWriter);
    } catch (Exception e) {
      System.out.println("Failed to setup output file: " + e);
    }

    // Setup the debug output file
    String xmlDebugFilename = options.getStringOption("debugout");
    if (xmlDebugFilename != null
	&& !xmlDebugFilename.equals("")) {
      if (xmlDebugFilename.equals("-")) {
	debugOut = System.out;
      } else {
	try {
	  FileOutputStream debugStream = new FileOutputStream(xmlDebugFilename);
	  debugOut = new PrintStream(debugStream);
	} catch (Exception e) {
	  System.out.println("Failed to setup debug output file: " + e);
	}
      }
    }

    if (diffDoctype == null || diffDoctype.equals("") && doc2 != null) {
      Element elem = doc2.getDocumentElement();
      diffDoctype = config.guessDoctype(elem);
      if (!config.doctype(diffDoctype)) {
	System.err.println("No doctype identified in " + configFile);
	System.err.print("for '" + elem.getLocalName() + "'");
	if (elem.getNamespaceURI() != null) {
	  System.err.print(" in the namespace '" + elem.getNamespaceURI());
	  System.err.println("'");
	} else {
	  System.err.println("");
	}
	System.exit(2);
      }
    }

    if (!config.doctype(diffDoctype)) {
      System.err.println("No doctype '" + diffDoctype + "' identified in");
      System.err.println(configFile);
      System.exit(2);
    }

    if (verbose > 0) {
      System.out.println("Using '" + diffDoctype + "' configuration.");
    }

    XMLDiff diff = new XMLDiff(System.out, debugOut, verbose);
    diff.setDiffMarkup(config.markup(diffDoctype));
    diff.setNamespaceAware(namespaceAware);
    diff.setValidating(validating);
    diff.setDiffElements(diffType.equals("element")
			 || diffType.equals("both"));
    diff.setDiffText(diffType.equals("text")
		     || diffType.equals("both")
		     || diffWords);
    diff.setDiffWords(diffWords);

    NodeDiff.ignoreWhitespace = ignoreWhitespace;
    diff.computeDiff(doc1, doc2);
    diff.update();

    FileInputStream f1Stream = null;
    String preamble = "";
    try {
      f1Stream = new FileInputStream(file2);
      int fByte = f1Stream.read();
      for (int count = 0; fByte >= 0 && count < 8192; count++) {
	preamble += (char) fByte;
	fByte = f1Stream.read();
      }
    } catch (Exception fe) {
      // nop
    }

    pos = preamble.indexOf("<!DOCTYPE");
    if (pos >= 0) {
      int gtpos = preamble.indexOf(">", pos);
      int lbpos = preamble.indexOf("[", pos);

      if (lbpos < 0
	  || (gtpos > 0 && gtpos < lbpos)) {
	// no internal subset
	preamble = preamble.substring(0, gtpos+1);
      } else {
	// internal subset
	pos = preamble.indexOf("]>");
	if (pos >= 0) {
	  preamble = preamble.substring(0, pos+2);
	} else {
	  try {
	    char c1 = 0;
	    char c2 = preamble.charAt(preamble.length()-1);
	    int fByte = f1Stream.read();
	    boolean done = false;
	    while (fByte >= 0 && !done) {
	      c1 = c2;
	      c2 = (char) fByte;
	      preamble += c2;
	      done = (c1 == ']' && c2 == '>');
	      fByte = f1Stream.read();
	    }
	  } catch (Exception fe) {
	    // nop
	  }
	}
      }
    } else {
      preamble = "";
    }

    try {
      f1Stream.close();
    } catch (Exception e) {
      System.out.println("Failed to close input file: " + e);
    }

    Serializer serializer = new Serializer(xmlOut);
    serializer.serialize(diff.getNewDocument(config.markup(diffDoctype)),
			 preamble);

    xmlOut.close();
  }

  public static void dump(Node node) {
    if (node == null) {
      return;
    } else if (node.getNodeType() == Node.DOCUMENT_NODE) {
      Node child = node.getFirstChild();
      while (child != null) {
	dump(child);
	child = child.getNextSibling();
      }
    } else if (node.getNodeType() == Node.ELEMENT_NODE) {
      Element elem = (Element) node;
      xmlOut.print("<");
      xmlOut.print(elem.getTagName());

      NamedNodeMap attributes = elem.getAttributes();
      for (int count = 0; count < attributes.getLength(); count++) {
	Attr attr = (Attr) attributes.item(count);
	String quote = "\"";
	String value = attr.getValue();

	xmlOut.print(" " + attr.getName() + "=" + quote);
	xmlOut.print(value);
	xmlOut.print(quote);
      }

      xmlOut.print(">");

      Node child = elem.getFirstChild();
      while (child != null) {
	dump(child);
	child = child.getNextSibling();
      }

      xmlOut.print("</");
      xmlOut.print(elem.getTagName());
      xmlOut.print(">");
    } else if (node.getNodeType() == Node.TEXT_NODE) {
      xmlOut.print(((Text) node).getData());
    } else if (node.getNodeType() == Node.COMMENT_NODE) {
      xmlOut.print("<!--");
      xmlOut.print(((Comment) node).getData());
      xmlOut.print("-->");
    } else if (node.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
      ProcessingInstruction pi = (ProcessingInstruction) node;
      xmlOut.print("<?");
      xmlOut.print(pi.getTarget());
      if (pi.getData() != null) {
	xmlOut.print(" ");
	xmlOut.print(pi.getData());
      }
      xmlOut.print("?>");
    } else if (node.getNodeType() == Node.DOCUMENT_TYPE_NODE) {
      // nop
    } else {
      System.err.println("??? UNEXPECTED NODE TYPE ???" + node + " " + node.getNodeType());
    }
  }

  private static void usage(String message) {
    String[] usage
      = {version,
	 "",
	 "Usage:",
	 "com.sun.xtc.diffmk.DiffMk [options] file1.xml file2.xml [output.xml]",
	 "",
	 "Where options are:",
	 "--quiet                Turn off all messages (--verbose 0)",
	 "--output file          Send diff output to 'file'",
	 "                       (or specify output.xml as the last file)",
	 "--debugout file        Send debugging information to 'file'",
	 "",
	 "--[no]validating       Enable or disable validation",
	 "--[no]namespaceaware   Enable or disable namespace awareness",
	 "--[no]ignorewhitespace Enable or disable whitespace trimming",
	 "--[no]words            Enable or disable word diffing",
	 "--verbose number       Select verbosity (>'number'=more verbose)",
	 "--diff difftype        Select diff type (element|text|both)",
	 "--config file          Load DiffMk config from 'file'",
	 "--doctype typename     Select doctype 'typename'",
	 "",
	 "Options can be abbreviated to the shortest unique string.",
	 "(In other words, --di is sufficient for --diff.)",
	 "",
	 "Default values for validating, namespaceaware, verbose, diff,",
	 "and config are read from the DiffMk.properties file. The default",
	 "doctype 'typename' can usually be inferred from the documents being",
	 "compared.",
	 ""};

    System.out.println(message);
    System.out.println("");
    for (int count = 0; count < usage.length; count++) {
      System.out.println(usage[count]);
    }
  }
}
