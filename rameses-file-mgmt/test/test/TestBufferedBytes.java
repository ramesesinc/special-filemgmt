/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import junit.framework.TestCase;

/**
 *
 * @author ramesesinc
 */
public class TestBufferedBytes extends TestCase {
    
    public TestBufferedBytes(String testName) {
        super(testName);
    }

    public void testMain() throws Exception { 
        ByteBuffer bb = new ByteBuffer( 20 * 1024 ); 
        int len = 25600;
        for (int i=0; i<len; i++) {
            if ( !bb.add(i)) {
                byte[] bytes = bb.getBytes(); 
                System.out.println("bytes-> " + bytes.length);
                bb.reset(); 
            } 
        }
        
        byte[] bytes = bb.getBytes(); 
        if (bytes != null ) {
            System.out.println("bytes-> " + bytes.length);
        }

        bb.close();
    }
    
    
    private class ByteBuffer {
        
        private final Object LOCKED = new Object();
        
        private int index; 
        private int capacity;
        private byte[] bytes;
        private boolean markAsClosed;
        
        ByteBuffer( int capacity ) {
            this.capacity = capacity; 
            reset();
        }
        
        void reset() { 
            synchronized (LOCKED) {
                if ( markAsClosed ) throw new RuntimeException("already marked as closed"); 

                this.index = 0;
                this.bytes = new byte[capacity]; 
            }
        }
        
        boolean add( int b ) {
            synchronized (LOCKED) {
                if ( markAsClosed ) throw new RuntimeException("already marked as closed"); 
                
                if (index >= 0 && index < bytes.length) {
                    bytes[index] = (byte) b; 
                    index += 1; 
                    return true; 
                } else { 
                    return false; 
                } 
            }
        }
        
        byte[] getBytes() { 
            synchronized (LOCKED) {
                if ( index <= 0 ) return null; 

                byte[] newbytes = new byte[index];
                System.arraycopy(bytes, 0, newbytes, 0, newbytes.length); 
                return newbytes; 
            }
        }
        
        void close() {
            synchronized (LOCKED) {
                markAsClosed = true; 
                bytes = null; 
                index = 0; 
            }            
        }
    }
}
