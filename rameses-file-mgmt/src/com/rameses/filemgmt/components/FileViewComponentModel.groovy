package com.rameses.filemgmt.components;

import com.rameses.rcp.annotations.*;
import com.rameses.rcp.common.*;
import com.rameses.osiris2.client.*;
import com.rameses.osiris2.common.*;

class FileViewComponentModel extends ComponentBean implements IFileViewModel {

    @Binding
    def binding;
    
    @Service('PersistenceService') 
    def service; 
        
    final def base64 = new com.rameses.util.Base64Cipher();    
    final def uploadHelper = com.rameses.filemgmt.FileUploadManager.Helper; 
    
    def data;
    def handler;
    def fileid;
    def thumbnails = [];
    
    def selectedItem;
    def itemHandler = [
        fetchList: { o-> 
            return thumbnails; 
        }, 
        onselect: { o-> 
            if ( !o ) return; 
            
            def stat = uploadHelper.getDownloadStatus( o.objid ); 
            if ( stat == null ) {
                o.message = 'downloading in progress...'; 
                uploadHelper.download( o.filelocid, o.objid, data.filetype ); 
            } else if ( stat == 'processing' ) {
                o.message = 'downloading in progress...'; 
            } else if ( stat == 'completed' ) {
                o.message = null; 
                o.actualimage = uploadHelper.getDownloadImage( o.objid ); 
                binding.refresh('selectedItem.actualimage'); 
            }
        }
    ] as ImageGalleryModel;
    
    def getCardname() {
        if ( selectedItem?.actualimage ) {
            return 'image'; 
        } else {
            return 'noimage'; 
        }
    }
    
    void loadFile( ) { 
        if ( fileid ) { 
            data = service.read([_schemaname:'sys_file', findBy:[ objid: fileid ]]); 
        } 
        
        thumbnails.clear();
        data?.items.each{
            thumbnails << [ 
                objid     : it.objid,
                caption   : it.caption, 
                filelocid : it.filelocid, 
                image     : decodeImage( it.thumbnail) 
            ]; 
        }
        itemHandler.reload();
    }
    
    def decodeImage( o ) {
        if ( o instanceof String ) {
            if ( base64.isEncoded( o)) {
                return base64.decode( o ); 
            }
        }
        return o; 
    }
}
