/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rameses.filemgmt;

import com.rameses.ftp.FtpLocationConf;
import com.rameses.ftp.FtpManager;
import com.rameses.ftp.FtpSession;
import com.rameses.io.FileLocTypeProvider;
import com.rameses.io.FileTransferSession;

/**
 *
 * @author wflores 
 */
public class FTPLocTypeProvider implements FileLocTypeProvider, FileLocationRegistry { 

    private final static String PROVIDER_NAME = "ftp";  
    
    public String getName() { 
        return PROVIDER_NAME; 
    }
    
    public FileTransferSession createUploadSession() { 
        return new UploadSession(); 
    }
    public FileTransferSession createDownloadSession() { 
        return new DownloadSession(); 
    }    
    
    public void deleteFile( String name, String locationConfigId ) {
        FtpSession sess = null; 
        try { 
            sess = FtpManager.createSession( locationConfigId ); 
            sess.deleteFile( name ); 
        } catch(Throwable t) { 
            System.out.println("[FTPLocTypeProvider] delete file error: "+ t.getMessage()); 
        } finally { 
            try { sess.disconnect(); } catch(Throwable t) {;} 
        } 
    }

    public void register(FileLocationConf conf) { 
        if ( conf == null ) return; 
        if ( PROVIDER_NAME.equals(conf.getType())) {
            FtpLocationConf ftpc = FtpLocationConf.add( conf.getName()); 
            ftpc.setHost( conf.getUrl()); 
            ftpc.setUser( conf.getUser()); 
            ftpc.setPassword( conf.getPassword()); 
            ftpc.setRootDir( conf.getRootDir()); 
        }
    }

    private class UploadSession extends FileTransferSession implements FtpSession.Handler {
        
        private FtpSession sess; 

        public void cancel() {
            super.cancel(); 
            if ( isCancelled() ) {
                disconnect(); 
            } 
        } 
        
        public void run() { 
            if ( isCancelled()) {
                disconnect(); 
                return; 
            }
            
            sess = FtpManager.createSession( getLocationConfigId() ); 
            sess.setBufferSize( 100 * 1024 ); 
            sess.setHandler( this ); 
            sess.upload( getTargetName(), getFile(), getOffset()); 
            disconnect(); 
        } 
        
        private void disconnect() {
            try {
                sess.disconnect(); 
            } catch(Throwable t){
                // do nothing 
            } finally {
                sess = null; 
            }
        } 
                
        public void onTransfer(long filesize, long bytesprocessed) {
            Handler handler = getHandler(); 
            if ( handler == null ) return; 
            
            handler.ontransfer( filesize, bytesprocessed ); 
        }

        public void onTransfer(long bytesprocessed) {
        }

        public void onComplete() {
            Handler handler = getHandler(); 
            if ( handler == null ) return; 

            handler.oncomplete(); 
        }        
    }
    
    private class DownloadSession extends FileTransferSession implements FtpSession.Handler {
        private FtpSession sess; 

        public void cancel() {
            super.cancel(); 
            if ( isCancelled() ) {
                disconnect(); 
            } 
        } 
        
        public void run() { 
            if ( isCancelled()) {
                disconnect(); 
                return; 
            }
            
            sess = FtpManager.createSession( getLocationConfigId() ); 
            sess.setBufferSize( 100 * 1024 ); 
            sess.setHandler(this);
            sess.download( getTargetName(), getFile() ); 
            disconnect(); 
        } 
        
        private void disconnect() {
            try {
                sess.disconnect(); 
            } catch(Throwable t){
                // do nothing 
            } finally {
                sess = null; 
            }
        } 

        public void onTransfer(long filesize, long bytesprocessed) {
        }

        public void onTransfer(long bytesprocessed) {
            FileTransferSession.Handler handler = getHandler(); 
            if ( handler == null ) return; 
            
            handler.ontransfer( bytesprocessed ); 
        }

        public void onComplete() {
            FileTransferSession.Handler handler = getHandler(); 
            if ( handler == null ) return; 

            handler.oncomplete(); 
        }
    }    
}
