/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rameses.rcp.common;

import com.rameses.common.PropertyResolver;
import com.rameses.rcp.framework.Binding;
import java.util.List;
import java.util.Map;

/**
 *
 * @author wflores 
 */
public class ThumbnailViewModel {
    
    public List fetchList( Map params ) {
        return null; 
    }
    
    public String getFileType( Object item ) { 
        try {
            Object value = PropertyResolver.getInstance().getProperty( item, "filetype" ); 
            return ( value == null ? null : value.toString()); 
        } catch( Throwable t ) {
            return null; 
        }
    }
    
    public Object getThumbnail( Object item ) { 
        try {
            return PropertyResolver.getInstance().getProperty( item, "thumbnail" ); 
        } catch( Throwable t ) {
            return null; 
        }
    } 
    
    public String getTitle( Object item ) {
        try {
            Object value = PropertyResolver.getInstance().getProperty( item, "caption" ); 
            return ( value == null ? null : value.toString()); 
        } catch( Throwable t ) {
            return null; 
        }
    }
    
    public Object openItem( Object item ) {
        return null; 
    }
    
    
    
    public Binding getBinding() {
        return (provider == null ? null : provider.getBinding()); 
    }
    public void addItem( Object item ) {
        if ( provider != null ) { 
            provider.addItem( item ); 
        } 
    }
    
    private Provider provider; 
    public void setProvider( Provider provider ) { 
        this.provider = provider; 
    } 
    public static interface Provider { 
        Binding getBinding(); 
        void addItem( Object item ); 
    }    
}
