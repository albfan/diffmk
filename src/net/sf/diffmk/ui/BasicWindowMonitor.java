// BasicWindowMonitor.java
//
// This code is copied or derived from the examples in _Java Swing_
// by Eckstein, Loy, and Wood. Published by O'Reilly & Associates, Inc.
// ISBN 1-56592-455-X
//
package net.sf.diffmk.ui;

import java.awt.event.*;
import java.awt.Window;

public class BasicWindowMonitor extends WindowAdapter {

  public void windowClosing(WindowEvent e) {
    Window w = e.getWindow();
    w.setVisible(false);
    w.dispose();
    System.exit(0);
  }
}
