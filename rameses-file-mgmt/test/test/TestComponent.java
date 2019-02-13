/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import com.rameses.filemgmt.components.FileViewPanel;
import com.rameses.filemgmt.components.ThumbnailViewPanel;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import junit.framework.TestCase;

/**
 *
 * @author ramesesinc
 */
public class TestComponent extends TestCase {
    
    public TestComponent(String testName) {
        super(testName);
    }

    public void testMain() throws Exception {

        ThumbnailViewPanel panel = new ThumbnailViewPanel(); 
        panel.setCellSize(new Dimension(120, 100)); 
        panel.setBorder( BorderFactory.createEmptyBorder(10, 10, 5, 5)); 
        panel.setPreferredSize(new Dimension(400, 200)); 
        
        FileViewPanel fvp = new FileViewPanel();
        fvp.setPreferredSize(new Dimension(400, 300)); 
        
        JDialog d = new JDialog();
        d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); 
        d.setModal(true); 
        d.setContentPane( fvp ); 
        d.pack(); 
        d.setVisible(true); 
    }
}
