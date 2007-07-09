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
import net.sf.diffmk.DiffMkProperties;
import net.sf.diffmk.xmldiff.XMLDiff;
import net.sf.diffmk.xmldiff.XMLDiffException;
import net.sf.diffmk.xmldiff.NodeDiff;
import net.sf.diffmk.ui.UI;

import bmsi.util.Diff;

public class DiffMk {
  protected static PrintWriter xmlOut = null;
  protected static PrintStream debugOut = null;
  protected static CommandOpts options = null;

  public static String version = "DiffMK V3.0 alpha 1";

  private boolean validating = false;
  private int verbose = 0;
  private String diffType = "text";
  private boolean diffWords = false;
  private boolean ignoreWhitespace = false;
  private boolean ui = false;
  private String outputFile = "";
  private String debugOutputFile = "";
  private String originalXMLFile = "";
  private String changedXMLFile = "";
  
  public static void main(String[] args) {
      DiffMk diffmk = new DiffMk();
      diffmk.parseCommandLine(args);
      if (diffmk.getUI()) {
          diffmk.ui();
      }
     
      diffmk.runClean();
  }

  public DiffMk() {
      options = new CommandOpts();
      options.addOption("debugout", CommandOpts.STRING);
      options.addOption("verbose", CommandOpts.INTEGER);
      options.addOption("help", CommandOpts.BOOLEAN);
      options.addOption("diff", CommandOpts.STRING);
      options.addOption("words", CommandOpts.BOOLEAN);
      options.addOption("output", CommandOpts.STRING);
      options.addOption("ignorewhitespace", CommandOpts.BOOLEAN);
      options.addOption("validating", CommandOpts.BOOLEAN);
      options.addOption("ui", CommandOpts.BOOLEAN);

      options.setOption("debugout", debugOutputFile);
      options.setOption("verbose", verbose);
      options.setOption("help", false);
      options.setOption("diff", diffType);
      options.setOption("words", diffWords);
      options.setOption("output", outputFile);
      options.setOption("ignorewhitespace", ignoreWhitespace);
      options.setOption("validating", verbose > 0);
      options.setOption("ui", ui);
  }
  
  public void parseCommandLine(String[] args) {
      int pos = options.parseArgs(args);

      if (options.getBooleanOption("help")) {
          usage("");
          System.exit(1);
      }

      if (pos < args.length) {
          setOriginalInput(args[pos++]);
      }

      if (pos < args.length) {
          setChangedInput(args[pos++]);
      }

      if (pos < args.length && !options.argumentSpecified("output")) {
          setOutput(args[pos++]);
      } else {
          setOutput(options.getStringOption("output"));
      }

      if (pos < args.length) {
          throw new IllegalArgumentException("Too many filenames.");
      }

      setValidating(options.getBooleanOption("validating"));
      setVerbosity(options.getIntegerOption("verbose"));
      setDiffType(options.getStringOption("diff"));
      setDiffWords(options.getBooleanOption("words"));
      setIgnoreWhitespace(options.getBooleanOption("ignorewhitespace"));
      setUI(options.getBooleanOption("ui"));
  }

  public void ui() {
      UI ui = new UI();
      ui.oldFile = getOriginalInput();
      ui.newFile = getChangedInput();
      ui.diffFile = getOutput();

      ui.diffTypeElement = diffType.equals("element");
      ui.diffTypeText = diffType.equals("text");
      ui.diffTypeBoth = diffType.equals("both");

      ui.wordOpt = diffWords;
      ui.ignOpt = ignoreWhitespace;
      ui.valOpt = validating;
      ui.verbOpt = (verbose > 0);

      ui.show(this);
  }


  public void runClean() {
      try {
          // normalize "" and null
          outputFile = "".equals(outputFile) ? null : outputFile;
          originalXMLFile = "".equals(originalXMLFile) ? null : originalXMLFile;
          changedXMLFile = "".equals(changedXMLFile) ? null : changedXMLFile;
          debugOutputFile = "".equals(debugOutputFile) ? null : debugOutputFile;
      
          if (outputFile == null) {
              throw new IllegalArgumentException("You must specify an output filename.");
          }

          DocumentBuilderFactory factory = null;
          DocumentBuilder builder = null;

          Document doc1 = null;
          Document doc2 = null;

          factory = DocumentBuilderFactory.newInstance();
          factory.setNamespaceAware(true);
          factory.setValidating(validating);
          builder = factory.newDocumentBuilder();

          if (verbose > 0) {
              System.out.println("Loading "+originalXMLFile+"...");
          }
          doc1 = builder.parse(originalXMLFile);

          if (verbose > 0) {
              System.out.println("Loading "+changedXMLFile+"...");
          }
          doc2 = builder.parse(changedXMLFile);

          // Setup the output file
          FileOutputStream xmlStream = new FileOutputStream(outputFile);
          OutputStreamWriter xmlWriter = new OutputStreamWriter(xmlStream, "utf-8");
          xmlOut = new PrintWriter(xmlWriter);

          // Setup the debug output file
          String xmlDebugFilename = options.getStringOption("debugout");
          if (xmlDebugFilename != null && !xmlDebugFilename.equals("")) {
              if (xmlDebugFilename.equals("-")) {
                  debugOut = System.out;
              } else {
                  FileOutputStream debugStream = new FileOutputStream(xmlDebugFilename);
                  debugOut = new PrintStream(debugStream);
              }
          }
          
          Document diffdoc = run(doc1, doc2);
          
          FileInputStream f1Stream = null;
          String preamble = "";
          try {
              f1Stream = new FileInputStream(changedXMLFile);
              int fByte = f1Stream.read();
              for (int count = 0; fByte >= 0 && count < 8192; count++) {
                  preamble += (char) fByte;
                  fByte = f1Stream.read();
              }
          } catch (Exception fe) {
              // nop
          }

          int pos = preamble.indexOf("<!DOCTYPE");
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
          serializer.serialize(diffdoc, preamble);

          xmlOut.close();
      } catch (ParserConfigurationException pce) {
          pce.printStackTrace();
      } catch (SAXException se) {
          se.printStackTrace();
      } catch (IOException ioe) {
          ioe.printStackTrace();
      }
  }
  
  public Document run(Document doc1, Document doc2) {
    XMLDiff diff = new XMLDiff(System.out, debugOut, verbose);
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
    return diff.getNewDocument();
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
	 "net.sf.diffmk.DiffMk [options] file1.xml file2.xml [output.xml]",
	 "",
	 "Where options are:",
	 "--output file          Send diff output to 'file'",
	 "                       (or specify output.xml as the last file)",
	 "--debugout file        Send debugging information to 'file'",
	 "",
	 "--validating           Enable validation",
	 "--ignorewhitespace     Enable whitespace trimming",
	 "--words                Enable word diffing",
	 "--verbose number       Select verbosity (>'number'=more verbose)",
	 "--diff difftype        Select diff type (element|text|both)",
	 "",
	 "Options can be abbreviated to the shortest unique string.",
	 "(In other words, --di is sufficient for --diff.)",
	 ""};

    System.out.println(message);
    System.out.println("");
    for (int count = 0; count < usage.length; count++) {
      System.out.println(usage[count]);
    }
  }

  public void setValidating(boolean validate) {
      validating = validate;
  }
  
  public boolean getValidating() {
      return validating;
  }

  public void setVerbosity(int verbose) {
      if (verbose >= 0) {
          this.verbose = verbose;
      } else {
          throw new IllegalArgumentException("Verbose can't be negative.");
      }
  }
  
  public int getVerbosity() {
      return verbose;
  }

  public void setDiffType(String type) {
      if ("text".equals(type) || "element".equals(type) || "both".equals(type)) {
          diffType = type;
      } else {
          throw new IllegalArgumentException("Invalid diff type: " + type);
      }
  }

  public void setDiffWords(boolean diff) {
      diffWords = diff;
  }
  
  public boolean getDiffWords() {
      return diffWords;
  }

  public void setIgnoreWhitespace(boolean ignore) {
      ignoreWhitespace = ignore;
  }
  
  public boolean getIgnoreWhitespace() {
      return ignoreWhitespace;
  }

  public void setUI(boolean ui) {
      this.ui = ui;
  }
  
  public boolean getUI() {
      return ui;
  }

  public void setOutput(String output) {
      outputFile = output;
  }

  public String getOutput() {
      return outputFile;
  }
  
  public void setDebugOutput(String output) {
      debugOutputFile = output;
  }

  public String getDebugOutput() {
      return outputFile;
  }
  
  public void setOriginalInput(String input) {
      originalXMLFile = input;
  }

  public String getOriginalInput() {
      return originalXMLFile;
  }
  
  public void setChangedInput(String input) {
      changedXMLFile = input;
  }

  public String getChangedInput() {
      return changedXMLFile;
  }
}
