/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rameses.filemgmt;

/**
 *
 * @author wflores 
 */
public interface FileDownloadHandler {
    
    void onTransfer( String fileid, long filesize, long bytesprocessed );
    void onCompleted( String fileid );
    
}
