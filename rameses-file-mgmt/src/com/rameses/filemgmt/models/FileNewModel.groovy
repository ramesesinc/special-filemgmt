package com.rameses.filemgmt.models;

import com.rameses.rcp.annotations.*;
import com.rameses.rcp.common.*;
import com.rameses.osiris2.client.*;
import com.rameses.osiris2.common.*;
import com.rameses.seti2.models.*;
import com.rameses.util.Base64Cipher;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class FileNewModel  {
        
    @Script('FileType') 
    def fileType;
    
    def info = [:];
    
    def handler;    
    def base64 = new Base64Cipher();
    def _entity = [ objid:'FILE'+ new java.rmi.server.UID(), info: [:]]; 
    
    def getFileTypes() {
        return fileType.getTypes(); 
    } 
    
    def getEntity() { 
        return _entity; 
    }
    
    def doOk() {
        def fm = com.rameses.filemgmt.FileManager.instance;
        def fileLocConf = fm.locationConfs.defaultConf;
        if ( fileLocConf == null ) throw new Exception('Please provide a default location conf'); 

        def scaler = new ImageScaler();
        def items = uploadHandler.getItems(); 
        if ( items.find{( it.completed == false )} ) 
            throw new Exception('Wait until all items were uploaded'); 
            
        entity.items = []; 
        items.each{ o-> 
            def data = o.data; 
            def m = [:];
            m.objid = data.objid; 
            m.parentid = entity.objid;
            m.state = 'COMPLETED';
            m.caption = data.caption;
            m.filesize = data.filesize;
            m.filetype = data.filetype;
            m.filelocid = data.filelocid;
            m.bytestransferred = data.filesize;
            
            def image = null; 
            def aaa = fileType.getType( m.filetype );
            if ( "true".equals( ""+ aaa?.image )) {
                image = scaler.createThumbnail( data.file );  
            } else {
                def icon = fm.getFileTypeIcon( m.filetype ); 
                if ( icon ) image = icon.image; 
            }
            
            m.thumbnail = base64.encode((Object) scaler.getBytes( image )); 
            entity.items << m; 
        } 
        entity.info = info;        
        
        def dbp = fm.dbProvider;
        if ( dbp ) { 
            def o = dbp.save( entity ); 
            if ( o ) _entity.putAll( o ); 
        }
        
        try { 
            if ( handler ) handler( entity ); 
        } finally {
            return "_close";
        }        
    }

    def doCancel() {
        return "_close";
    }    
    
    def self = this; 
    def uploadHandler = [ 
        afterRemoveItem: {
            self.refreshFileType(); 
        }
    ] as FileUploadModel;
    
    void refreshFileType() {
        if ( uploadHandler?.binding ) {
            uploadHandler.binding.refresh('entity.filetype');
        } 
    }
    
    int fileIndexNo; 
    def fileChooser; 
    def selectedAttachment;
    void attachFile() { 
        def filetype = fileTypes.find{ it.objid==entity.filetype }
        if ( !filetype ) throw new Exception('file type not supported');

        if ( fileChooser == null ) {
            fileChooser = new JFileChooser(); 
            fileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
            fileChooser.setAcceptAllFileFilterUsed( false );
        }
        
        fileChooser.setMultiSelectionEnabled( filetype.multiselect ); 
        if ( entity.filetype == 'jpg' ) { 
            fileChooser.setFileFilter( new FileNameExtensionFilter( filetype.title, "jpg", "jpeg"));
        } else if ( entity.filetype == 'png' ) {
            fileChooser.setFileFilter( new FileNameExtensionFilter( filetype.title, "png"));
        } else { 
            fileChooser.setFileFilter( new FileNameExtensionFilter( filetype.title, filetype.objid ));
        } 
        
        def fum = com.rameses.filemgmt.FileUploadManager.instance; 
        def locconf = com.rameses.filemgmt.FileManager.instance.locationConfs.defaultConf; 
        if ( !locconf ) throw new Exception('No active location config available'); 
        
        def tempdir = fum.getTempDir();         
        int opt = fileChooser.showOpenDialog( uploadHandler.binding?.owner ); 
        if ( opt == JFileChooser.APPROVE_OPTION ) { 
            def files = null; 
            if ( filetype.multiselect ) {
                files = fileChooser.getSelectedFiles(); 
            } else {
                files = [ fileChooser.getSelectedFile() ];  
            }
            
            files.each{ o-> 
                fileIndexNo += 1; 
                def item = [:];
                def encstr = com.rameses.util.Encoder.MD5.encode( entity.objid ); 
                item.objid = encstr + fileIndexNo.toString().padLeft(2, '0'); 
                item.filesize = fum.helper.getFileSize( o ); 
                item.filetype = entity.filetype; 
                item.filelocid = locconf.name; 
                item.fileid = item.objid; 
                item.source = o.canonicalPath; 
                item.caption = o.name; 
                item.file = o;

                def folder = new java.io.File( tempdir, item.fileid ); 
                def fui = com.rameses.filemgmt.FileUploadItem.create( folder, item ); 
                uploadHandler.add( fui, item.caption, item.filesize, item ); 
                item.uploaditem = fui;
                fum.schedule( fui ); 
            } 
            
            refreshFileType(); 
        } 
    }
}