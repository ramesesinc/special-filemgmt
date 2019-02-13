/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import junit.framework.TestCase;

/**
 *
 * @author ramesesinc
 */
public class TestTextArea extends TestCase {
    
    public TestTextArea(String testName) {
        super(testName);
    }

    public void testMain() throws Exception {
        TextAreaImpl txtarea = new TextAreaImpl(); 
        txtarea.setFont( txtarea.getFont().deriveFont(24.0f) ); 
        txtarea.setLineWrap(true);
        txtarea.setWrapStyleWord(true);
        
        ScrollPanelImpl jsp = new ScrollPanelImpl( txtarea );
        jsp.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS ); 
        jsp.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS ); 
        jsp.setPreferredSize(new Dimension(200, 100)); 
        
        JDialog d = new JDialog();
        d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); 
        d.setModal(true); 
        d.setContentPane( jsp ); 
        d.pack(); 
        d.setVisible(true); 
    }
    
    private class TextAreaImpl extends JTextArea {
        public Dimension getPreferredScrollableViewportSize() {
            Dimension dim = super.getPreferredScrollableViewportSize(); 
            System.out.println("getPreferredScrollableViewportSize: "+ dim);
            return dim;
        }

        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            int value = super.getScrollableUnitIncrement(visibleRect, orientation, direction); 
            System.out.println("getScrollableUnitIncrement: "+ value);
            System.out.println("  rect  : "+ visibleRect );
            return value; 
        }

        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            int value = super.getScrollableBlockIncrement(visibleRect, orientation, direction); 
            System.out.println("getScrollableBlockIncrement: "+ value);
            System.out.println("  rect  : "+ visibleRect );
            return value; 
        }

        public boolean getScrollableTracksViewportWidth() { 
            boolean value = super.getScrollableTracksViewportWidth(); 
            System.out.println("getScrollableTracksViewportWidth: "+ value);
            return value; 
        }

        public boolean getScrollableTracksViewportHeight() { 
            boolean value = super.getScrollableTracksViewportHeight(); 
            System.out.println("getScrollableTracksViewportHeight: " + value);
            return value; 
        }        
    }
    
    private class ScrollPanelImpl extends JScrollPane {
        
        ScrollPanelImpl( Component view ) {
            super( view );             
        }
    }
     
}
