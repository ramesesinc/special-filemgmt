/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import com.rameses.ftp.FtpLocationConf;
import com.rameses.ftp.FtpSession;
import junit.framework.TestCase;

/**
 *
 * @author ramesesinc
 */
public class TestFtp extends TestCase {
    
    public TestFtp(String testName) {
        super(testName);
    }

    public void testDeleteFile() throws Exception {
        FtpLocationConf conf = FtpLocationConf.add("default");
        conf.setHost("127.0.0.1:2121");
        conf.setUser("admin");
        conf.setPassword("admin");
        
        FtpSession sess = null; 
        try {
            sess = new FtpSession( conf ); 
            sess.connect(); 
            sess.deleteFile("4b51742c288cc1cce94901233fe91e1f01.jpg"); 
        } finally {
            try {
                sess.disconnect(); 
            } catch(Throwable t) {;} 
        }
    }
}
