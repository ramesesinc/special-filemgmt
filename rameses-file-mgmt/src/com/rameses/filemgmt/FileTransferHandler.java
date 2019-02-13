/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rameses.filemgmt;

/**
 *
 * @author wflores 
 */
public interface FileTransferHandler {
    
    void onTransfer( FileItem item, long filesize, long bytesprocessed );
    
    void onCompleted( FileItem item );
    
}
