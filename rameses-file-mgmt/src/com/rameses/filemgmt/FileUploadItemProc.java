/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rameses.filemgmt;

/**
 *
 * @author wflores
 */
public abstract class FileUploadItemProc implements RunProc {

    private boolean cancelled;
    private Handler handler;
    
    public void cancel() { 
        this.cancelled = true; 
    }
    
    public boolean isCancelled() {
        return cancelled; 
    } 
    
    public Handler getHandler() { return handler; } 
    public void setHandler( Handler handler ) {
        this.handler = handler; 
    }

    public abstract void run(); 
    
    
    public void fireOnCompleted( boolean success, String message ) {
        Handler handler = getHandler(); 
        if ( handler != null ) {
            handler.onCompleted( success, message ); 
        }
    }
    
    public static interface Handler { 
        void onCompleted( boolean success, String message ); 
    } 
}
