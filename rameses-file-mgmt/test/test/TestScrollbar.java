/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import junit.framework.TestCase;

/**
 *
 * @author ramesesinc
 */
public class TestScrollbar extends TestCase {
    
    public TestScrollbar(String testName) {
        super(testName);
    }

    public void testMain() throws Exception {
        
        ScrollBarImpl sbi = new ScrollBarImpl(); 
        sbi.setOrientation( ScrollBarImpl.VERTICAL ); 
        sbi.setBlockIncrement(0);
        sbi.setMinimum(0);
        sbi.setMaximum(100); 
        sbi.setVisibleAmount(0);
        sbi.setValue(-1);
        
        JPanel p = new JPanel(new BorderLayout()); 
        p.add( BorderLayout.EAST, sbi ); 
        p.setPreferredSize(new Dimension(200, 200)); 
        
        
        JDialog d = new JDialog();
        d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); 
        d.setModal(true); 
        d.setContentPane( p ); 
        d.pack(); 
        d.setVisible(true); 
    }
    
   
    private class ScrollBarImpl extends JScrollBar {
        
        ScrollBarImpl() {
            super();             
        }
    }
     
}
