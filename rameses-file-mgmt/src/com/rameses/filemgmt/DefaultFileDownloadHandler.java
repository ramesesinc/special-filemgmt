/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rameses.filemgmt;

/**
 *
 * @author wflores 
 */
public class DefaultFileDownloadHandler implements FileDownloadHandler { 

    public void onTransfer(String fileid, long filesize, long bytesprocessed) {
    }

    public void onCompleted(String fileid) { 
        System.out.println("download completed. ("+ fileid +")");
    }
}
