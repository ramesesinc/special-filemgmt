/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rameses.filemgmt;

import java.util.Map;

/**
 *
 * @author wflores 
 */
final class FileLocationConf { 
    
    private String name; 
    private String type;
    private String url;
    private String rootdir;
    private String username;
    private String password;
    private boolean defaulted;
    private Map properties; 
    
    FileLocationConf( Map data ) {
        if ( data == null || data.isEmpty()) return; 

        this.properties = data;         
        this.name = getProperty(data, "name"); 
        this.type = getProperty(data, "type"); 
        this.url = getProperty(data, "url"); 
        this.rootdir = getProperty(data, "rootdir"); 
        this.username = getProperty(data, "username"); 
        this.password = getProperty(data, "password"); 
        this.defaulted = (getProperty(data, "defaulted")+"").matches("true|1"); 
    }

    public boolean isDefaulted() { return defaulted; }
    
    public String getName() { return name; } 

    public String getType() { return type; } 
    
    public String getUrl() { return url; } 
    
    public String getRootDir() { return rootdir; } 

    public String getUser() { return username; }
    
    public String getPassword() { return password; } 
    
    public Object getProperty( String name ) {
        return properties.get( name ); 
    }
    public void setProperty( String name, Object value ) {
        properties.put( name, value ); 
    }

    private String getProperty( Map data, String name ) {
        Object value = ( data == null ? null : data.get(name)); 
        return (value == null ? null : value.toString()); 
    } 
}
