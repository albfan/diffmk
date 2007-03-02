/*
 * DiffTest.java
 *
 * Created on March 1, 2007, 7:50 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

import junit.framework.TestCase;
import junit.textui.TestRunner;
import net.sf.diffmk.DiffMk;

/**
 *
 * @author ndw
 */
public class DiffTest extends TestCase {
    
    /** Creates a new instance of DiffTest */
    public DiffTest() {
    }
    
    public static void main(String [] args){
        TestRunner.run(DiffTest.class);
    }
    
    public void test() {
        DiffMk diff = new DiffMk();
        diff.setOriginalInput("samples/doc2-old.xml");
        diff.setChangedInput("samples/doc2-new.xml");
        diff.setOutput("samples/doc2-diff.xml");
        diff.setDiffType("both");
        diff.setVerbosity(1);
        diff.setIgnoreWhitespace(true);
        diff.setDiffWords(true);
        diff.runClean();
    }
}    
