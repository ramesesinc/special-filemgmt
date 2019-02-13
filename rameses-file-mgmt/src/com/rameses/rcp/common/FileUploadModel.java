/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rameses.rcp.common;

import com.rameses.filemgmt.FileUploadItem;
import com.rameses.rcp.framework.Binding;
import java.util.List;
import java.util.Map;

/**
 *
 * @author wflores
 */
public class FileUploadModel {
    
    public Binding getBinding() {
        return (provider == null ? null : provider.getBinding()); 
    }
    public Binding getInnerBinding() {
        return (provider == null ? null : provider.getInnerBinding()); 
    }
    public void add( FileUploadItem item, String filename, long filesize, Map props ) {
        if ( provider != null ) {
            provider.add(item, filename, filesize, props);
        }
    }
    public void remove( FileUploadItem item ) {
        if ( provider != null ) {
            provider.remove(item);
        }
    }
    
    public final List getItems() { 
        return (provider == null ? null: provider.getItems()); 
    }
    
    public final int getItemCount() {
        List items = getItems(); 
        return (items == null ? 0 : items.size()); 
    }

    public void afterRemoveItem() {
    }
    

    private Provider provider; 
    public void setProvider( Provider provider ) { 
        this.provider = provider; 
    } 
    public static interface Provider { 
        Binding getBinding(); 
        Binding getInnerBinding(); 
        void add( FileUploadItem item, String filename, long filesize, Map props ); 
        void remove( FileUploadItem item ); 
        List getItems();
    }
}
