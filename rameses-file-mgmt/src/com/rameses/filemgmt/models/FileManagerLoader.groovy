package com.rameses.filemgmt.models;

import com.rameses.rcp.annotations.*;
import com.rameses.rcp.common.*;
import com.rameses.osiris2.client.*;
import com.rameses.osiris2.common.*;

public class FileManagerLoader {  
    
    @Script('FileDbProviderImpl') 
    def dbProvider; 

    @Script('FileLocationProviderImpl') 
    def fileLocationProvider; 

    void doStart() {
        def ctx = com.rameses.rcp.framework.ClientContext.currentContext;
        def value = (ctx.appEnv ? ctx.appEnv.get('filemgmt.enabled') : null).toString();
        def enabled = (value == 'false' ? false : true); 

        def fm = com.rameses.filemgmt.FileManager.instance; 
        fm.loadFileLocTypeProviders( ctx.classLoader ); 
        fm.locationProvider = fileLocationProvider; 
        fm.dbProvider = dbProvider; 
        fm.enabled = enabled; 
        fm.start(); 
    } 
} 