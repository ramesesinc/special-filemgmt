/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rameses.filemgmt;

/**
 *
 * @author ramesesinc
 */
public class RunProcProxy implements RunProc {

    private Runnable source; 
    
    public RunProcProxy( Runnable source ) {
        this.source = source; 
    }
    
    public void cancel() {
    }

    public void run() { 
        if ( source != null ) {
            source.run(); 
        }
    } 
}
