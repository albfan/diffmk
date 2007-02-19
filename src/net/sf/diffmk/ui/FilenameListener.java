// BasicWindowMonitor.java
//
// This code is copied or derived from the examples in _Java Swing_
// by Eckstein, Loy, and Wood. Published by O'Reilly & Associates, Inc.
// ISBN 1-56592-455-X
//
package net.sf.diffmk.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

// Shows how displayedMnemonic and labelFor properties work together
public class FilenameListener implements ActionListener {
  JFrame frame = null;
  JTextField field = null;

  public FilenameListener(JFrame frame, JTextField textField) {
    field = textField;
    this.frame = frame;
  }

  public void actionPerformed(ActionEvent ae) {
    String[] xml = new String[] {"xml"} ;
    String[] html = new String[] {"html", "htm", "xhtml", "xhtm"};
    String[] xsd = new String[] {"xsd"};
    String[] xsl = new String[] {"xsl"};
    String[] svg = new String[] {"svg"};
    JFileChooser chooser = new JFileChooser();
    chooser.setMultiSelectionEnabled(false);
    chooser.addChoosableFileFilter(new SimpleFileFilter(xml, "XML (*.xml)"));
    chooser.addChoosableFileFilter(new SimpleFileFilter(html, "HTML (*.x?html?)"));
    chooser.addChoosableFileFilter(new SimpleFileFilter(xsl, "XSD (*.xsd)"));
    chooser.addChoosableFileFilter(new SimpleFileFilter(xsd, "XSL (*.xsl)"));
    chooser.addChoosableFileFilter(new SimpleFileFilter(svg, "SVG (*.SVG)"));
    int option = chooser.showOpenDialog(frame);
    if (option == JFileChooser.APPROVE_OPTION) {
      if (chooser.getSelectedFile()!=null) 
	field.setText(chooser.getCurrentDirectory() + "/" + chooser.getSelectedFile().getName());
    }
  }
}
