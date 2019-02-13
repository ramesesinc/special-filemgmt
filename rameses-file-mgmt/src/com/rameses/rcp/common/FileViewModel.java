/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rameses.rcp.common;

import com.rameses.filemgmt.FileManager;
import com.rameses.filemgmt.FileManager.DbProvider;
import com.rameses.rcp.framework.Binding;
import java.util.List;
import java.util.Map;

/**
 *
 * @author wflores
 */
public class FileViewModel {

    private boolean editable = true; 
    private boolean allowAdd = true; 
    private boolean allowRemove = true; 
    
    public boolean isEditable() { return editable; } 
    public void setEditable( boolean editable ) {
        this.editable = editable; 
    }
    
    public boolean isAllowAdd() { return allowAdd; } 
    public void setAllowAdd( boolean allowAdd ) {
        this.allowAdd = allowAdd; 
    }
    
    public boolean isAllowRemove() { return allowRemove; }
    public void setAllowRemove( boolean allowRemove ) {
        this.allowRemove = allowRemove; 
    }
    
    public List fetchList( Map params ) { 
        return null; 
    } 
    
    public boolean removeItem( Object item ) {
        return true; 
    }
    
    public void afterAddItem( Object item ) {
    }
    
    
    Object getItem( Map params ) { 
        DbProvider dbp = FileManager.getInstance().getDbProvider(); 
        return ( dbp == null ? null : dbp.read(params)); 
    } 
    
    
    public Binding getBinding() {
        return (provider == null ? null : provider.getBinding()); 
    }
    public Binding getInnerBinding() {
        return (provider == null ? null : provider.getInnerBinding()); 
    }
    public void addItem( Object item ) throws Exception {
        if ( provider == null ) return; 
        provider.addItem( item ); 
    }
    
    private Provider provider; 
    public void setProvider( Provider provider ) { 
        this.provider = provider; 
    } 
    public static interface Provider { 
        Binding getBinding(); 
        Binding getInnerBinding(); 
        void addItem( Object item ) throws Exception;
    }    
}
