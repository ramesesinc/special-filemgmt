/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rameses.filemgmt;

import com.rameses.io.FileLocTypeProvider;
import com.rameses.io.FileTransferSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author wflores 
 */
public final class FileUploadItem implements FileItem { 
        
    public static synchronized FileUploadItem create( File folder, Map conf ) throws Exception { 
        FileUploadItem fui = new FileUploadItem( folder ); 
        fui.createImpl( conf ); 
        return fui; 
    }
    
    public static synchronized FileUploadItem open( File folder ) throws Exception { 
        FileUploadItem fui = new FileUploadItem( folder ); 
        fui.openImpl(); 
        return fui; 
    }
    
    
    public final static String MODE_TEMP_COPY = "TEMP";
    public final static String MODE_UPLOAD    = "UPLOAD";
    public final static String MODE_COMPLETED = "COMPLETED";
    
    public final static String CONF_FILE_LOC_ID   = "filelocid";
    public final static String CONF_FILE_SOURCE   = "source";
    public final static String CONF_FILE_SIZE     = "filesize";
    public final static String CONF_FILE_TYPE     = "filetype";
    public final static String CONF_FILE_ID       = "fileid";
    public final static String CONF_FILE_GROUP_ID = "filegroupid";
    
    private File folder; 
    private ConfigFile confFile;  
    private ContentFile contentFile;
    
    private FileUploadItem( File file ) {
        this.folder = file; 
    }
    
    public String getName() { 
        return folder.getName(); 
    } 

    public ConfigFile getConfigFile() { 
        return confFile;  
    } 
    public ContentFile getContentFile() {
        return contentFile; 
    }
    
    private void createImpl( Map conf ) throws Exception { 
        verifyFolder();
        confFile = new ConfigFile( folder, ".conf" ); 
        confFile.create( conf ); 
        Number filesize = confFile.getPropertyAsNumber( CONF_FILE_SIZE );
        if ( filesize == null ) filesize = 0; 
        
        String str = MODE_TEMP_COPY +","+ filesize +",0";
        contentFile = new ContentFile( folder, "content" ); 
        contentFile.getStatusFile().create( str );  
        createTempFile(".tempcopy");
        createTempFile(".ready");
        
        FileUploadManager.getInstance().getFileHandlers().register( this ); 
    }
    
    private void openImpl() {
        verifyFolder();
        confFile = new ConfigFile( folder, ".conf" ); 
        confFile.read();
        contentFile = new ContentFile( folder, "content" ); 
        contentFile.getStatusFile().read(); 
    } 
    
    public String getMode() {
        if ( isModeTempCopy()) {
            return MODE_TEMP_COPY; 
        } else if ( isModeUpload()) {
            return MODE_UPLOAD; 
        } else if ( isModeCompleted() ) {
            return MODE_COMPLETED; 
        } else {
            return "";
        }
    }
    
    public boolean isModeReady() {
        File file = new File( folder, ".ready");
        return ( file.exists() && !file.isDirectory());
    }
    public boolean isModeTempCopy() {
        File file = new File( folder, ".tempcopy");
        return ( file.exists() && !file.isDirectory());
    }
    public boolean isModeCompleted() { 
        File file = new File( folder, ".completed");
        return ( file.exists() && !file.isDirectory());
    } 
    public boolean isModeUpload() { 
        File file = new File( folder, ".upload");
        return ( file.exists() && !file.isDirectory());
    } 
    
    public FileUploadItemProc createProcessHandler() { 
        if ( isMarkedForRemoval()) {
            return new ModeRemovalProcess(); 
        } else if ( isModeReady()) { 
            return new ModeReadyProcess();  
        } else if ( isModeTempCopy()) { 
            return new ModeTempCopyProcess();  
        } else if ( isModeUpload()) { 
            return new ModeUploadProcess(); 
        } 
        return null; 
    } 
    
    public void markForRemoval() {
        createTempFile(".forremoval"); 
    }
    public boolean isMarkedForRemoval() {
        File file = new File( folder, ".forremoval");
        return ( file.exists() && !file.isDirectory());
    }
    
    public void remove() { 
        remove( this.folder ); 
    }   
    private void remove( File file ) {
        if ( file==null || !file.exists()) return; 
        
        if ( file.isDirectory()) {
            File[] files = file.listFiles(); 
            for ( File child : files ) {
                remove( child ); 
            }
        } 

        try { 
            file.delete(); 
        } catch(Throwable t) { 
            t.printStackTrace(); 
        }
    }
    
    void verifyFolder() {
        if ( !folder.exists()) { 
            folder.mkdir(); 
            
        } else if ( !folder.isDirectory()) {
            folder.delete();  
            folder.mkdir(); 
        } 
    }
    public void createTempFile( String name ) {
        createTempFile(name, null); 
    }
    public void createTempFile( String name, String content ) {
        FileOutputStream fos = null; 
        try {
            fos = new FileOutputStream( new File( folder, name)); 
            fos.write((content == null ? "" : content).getBytes()); 
            fos.flush(); 
        } catch(RuntimeException re) {
            throw re; 
        } catch(Exception e) {
            throw new RuntimeException( e.getMessage(), e ); 
        } finally {
            try { fos.close(); }catch(Throwable t){;} 
        }
    }
    public boolean removeTempFile( String name ) {
        try {
            File tmp = new File( folder, name); 
            return (tmp.exists() ? tmp.delete() : true); 
        } catch(Throwable t) {
            return false; 
        } 
    }
    

    public class ConfigFile { 
        private File folder;
        private File idxfile; 
        private Map conf; 
        
        ConfigFile( File folder, String name ) {
            this.folder = folder; 
            this.idxfile = new File( folder, name ); 
            this.conf = new HashMap(); 
        }

        void create( Map conf ) {
            if ( conf == null || conf.isEmpty() ) return; 
            
            this.conf.clear(); 
            this.conf.putAll( conf ); 
                        
            update(); 
        } 

        Map copyData() {
            Map data = new HashMap();
            data.putAll( this.conf ); 
            data.put( CONF_FILE_SIZE, getPropertyAsNumber( CONF_FILE_SIZE));
            return data; 
        }
        
        public ConfigFile read() { 
            FileInputStream inp = null; 
            try { 
                inp = new FileInputStream( idxfile );
                Properties props = new Properties(); 
                props.load( inp ); 
                this.conf.clear(); 
                this.conf.putAll( props ); 
            } catch(Throwable t) { 
                //do nothing 
            } finally { 
                try { inp.close(); }catch(Throwable t){;} 
            }
            return this; 
        }
        
        public void update() {
            FileOutputStream fos = null; 
            try {
                StringBuilder sb = new StringBuilder();
                Set<Entry<Object,Object>> sets = this.conf.entrySet(); 
                for ( Entry<Object,Object> entry : sets ) {
                    String skey = (entry.getKey()==null? null: entry.getKey().toString()); 
                    if ( skey != null && skey.trim().length() > 0 ) {
                        String sval = (entry.getValue()==null? "": entry.getValue().toString()); 
                        sb.append(skey).append("=").append(sval).append("\n"); 
                    } 
                } 

                fos = new FileOutputStream( idxfile ); 
                fos.write( sb.toString().replace('\\','/').getBytes() ); 
                fos.flush(); 
            } catch(RuntimeException re) { 
                throw re; 
            } catch(Exception e) {
                throw new RuntimeException(e.getMessage(), e); 
            } finally {
                try { fos.close(); }catch(Throwable t){;} 
            }
        }
        public String getProperty( String name ) {
            Object value = this.conf.get( name ); 
            return (value == null? null: value.toString()); 
        }
        public Number getPropertyAsNumber( String name ) { 
            try { 
                String value = getProperty( name ); 
                return new Long( value );  
            } catch(Throwable t) { 
                return null; 
            } 
        }
        public void setProperty( String name, Object value ) {
            this.conf.put( name, value ); 
        } 
    }
    
    public class ContentFile {
        
        private File folder; 
        private File child; 
        private StatusFile statusFile;
        
        ContentFile( File folder, String name ) {
            this.folder = folder; 
            this.child = new File( folder, name ); 
            this.statusFile = new StatusFile( folder, name+".index" ); 
        }
        
        public File getFile() { return child; } 
        
        StatusFile getStatusFile() { return statusFile; } 
        
        public boolean isModeTempCopy() { 
            String sval = getStatusFile().getMode(); 
            if ( sval==null || sval.trim().length()==0 ) { 
                return true; 
            } else { 
                return MODE_TEMP_COPY.equalsIgnoreCase(sval); 
            } 
        } 
        public boolean isModeUpload() { 
            String sval = getStatusFile().getMode(); 
            return MODE_UPLOAD.equalsIgnoreCase(sval+""); 
        } 
        public boolean isModeCompleted() { 
            String sval = getStatusFile().getMode(); 
            return MODE_COMPLETED.equalsIgnoreCase(sval+""); 
        }         
    }
    
    private class StatusFile {
        
        private File folder;
        private File file; 
        
        private String mode; 
        private Number numSize;
        private Number numPos;
        
        StatusFile( File folder, String name ) { 
            this.folder = folder; 
            this.file = new File( folder, name );  
        } 
        
        String getMode() {
            return ( mode==null || mode.trim().length()==0 ? MODE_TEMP_COPY : mode ); 
        }
        long getSize() {
            return (numSize == null ? 0 : numSize.longValue()); 
        }
        long getPos() {
            return (numPos == null ? 0 : numPos.longValue()); 
        }
        void setPos( long pos ) {
            this.numPos = pos; 
        }
        
        void create( String data ) {
            OutputStream out = null; 
            try {
                out = new FileOutputStream( file );   
                out.write( data.getBytes()  );  
                out.flush(); 
            } catch(RuntimeException re) {
                throw re; 
            } catch(Exception e) {
                throw new RuntimeException(e.getMessage(), e); 
            } finally { 
                try { out.close(); }catch(Throwable t){;} 
            } 
            
            read(); 
        }
        StatusFile read() {
            FileInputStream inp = null; 
            try {
                inp = new FileInputStream( file );      

                StringBuilder sb = new StringBuilder(); 
                byte[] bytes = new byte[1024]; 
                int read = -1;
                while ((read=inp.read(bytes)) != -1) {
                    sb.append(new String(bytes, 0, read)); 
                }

                mode = null; 
                numSize = null; 
                numPos = null; 
                // mode,size,pos
                String[] arrs = sb.toString().replaceAll("[\\s]{1,}","").split(","); 
                if ( arrs.length >= 1 ) mode = arrs[0]; 
                if ( arrs.length >= 2 ) numSize = convertNumber( arrs[1]); 
                if ( arrs.length >= 3 ) numPos = convertNumber( arrs[2]); 
            } catch(Throwable t) {
                //do nothing 
            } finally { 
                try { inp.close(); }catch(Throwable t){;} 
            } 
            return this; 
        }
        void update() {
            OutputStream out = null; 
            try {
                String str = getMode() +","+ getSize() +","+ getPos();
                out = new FileOutputStream( file );   
                out.write( str.getBytes()  );  
                out.flush(); 
            } catch(RuntimeException re) {
                throw re; 
            } catch(Exception e) {
                throw new RuntimeException(e.getMessage(), e); 
            } finally { 
                try { out.close(); }catch(Throwable t){;} 
            } 
        }        
        
        void changeMode( String mode ) { 
            if ( mode == null ) mode = ""; 
            
            if ( MODE_UPLOAD.equalsIgnoreCase(mode)) {
                this.mode = MODE_UPLOAD; 
            } else if ( MODE_COMPLETED.equalsIgnoreCase(mode)) {
                this.mode = MODE_COMPLETED; 
            } else { 
                this.mode = MODE_TEMP_COPY; 
            } 
            update(); 
        } 
        
        Number convertNumber( Object value ) {
            try {
                if( value instanceof Number ) {
                    return (Number) value; 
                }
                return new Long( value.toString());
            } catch(Throwable t) {
                return null; 
            }
        } 
    }    

    private class ModeReadyProcess extends FileUploadItemProc { 
        
        FileUploadItem root = FileUploadItem.this; 
        
        public void run() {
            boolean completed = false; 
            boolean success = false; 
            String message = null; 
            
            try {
                if ( isCancelled() ) { 
                    throw new InterruptedException();
                } 

                root.createTempFile(".started"); 
                root.removeTempFile(".ready"); 
                completed = success = true; 
                message = "success";
            } catch(InterruptedException ie) { 
                //do nothing 
            } catch(Throwable t) {
                t.printStackTrace(); 
            } 
            
            if ( completed ) { 
                fireOnCompleted( success, message ); 
            } 
        }
    } 

    private class ModeTempCopyProcess extends FileUploadItemProc { 
        
        FileUploadItem root = FileUploadItem.this; 
        
        public void run() {
            boolean completed = false; 
            boolean success = false; 
            String message = null; 
            
            while (true) {
                try {
                    if ( isCancelled() ) { 
                        throw new InterruptedException();
                    } 
                    
                    String status = runImpl(); 
                    if ( "no_file_source".equals( status)) { 
                        message = "no source file specified in the conf";
                        root.createTempFile(".error", message);
                        root.removeTempFile(".tempcopy"); 
                        completed = true; 
                        break;
                    } else if ( "source_file_not_exist".equals( status)) {
                        message = "source file does not exist";
                        root.createTempFile(".error", message);
                        root.removeTempFile(".tempcopy"); 
                        completed = true; 
                        break; 
                    } else if ( "success".equals( status)) {
                        root.createTempFile(".upload"); 
                        root.removeTempFile(".tempcopy"); 
                        root.removeTempFile(".error"); 
                        message = "success";
                        completed = true; 
                        success = true; 
                        break; 
                    }
                } catch(InterruptedException ie) { 
                    break; 
                } catch(Throwable t) {
                    t.printStackTrace(); 
                } 
            }
            
            if ( completed ) { 
                fireOnCompleted( success, message ); 
            } 
        }
        
        private String runImpl() throws Exception { 
            ConfigFile conf = root.getConfigFile(); 
            String strsource = conf.getProperty( CONF_FILE_SOURCE ); 
            if ( strsource == null || strsource.trim().length()==0 ) { 
                return "no_file_source";
            }
            strsource = strsource.replace('\\', '/'); 
            File sourcefile = new java.io.File( strsource ); 
            if ( !sourcefile.exists() ) {
                return "source_file_not_exist";
            }
            
            File targetfile = root.getContentFile().getFile(); 
            FileInputStream inp = null; 
            FileOutputStream out = null; 
            try {
                out = new FileOutputStream( targetfile );
                inp = new FileInputStream( sourcefile ); 
                byte[] bytes = new byte[1024 * 100]; 
                int read = -1; 
                while ((read=inp.read(bytes)) != -1) { 
                    if ( isCancelled() ) throw new InterruptedException(); 
                    
                    out.write(bytes, 0, read); 
                } 
                out.flush(); 
                
                StatusFile sf = root.getContentFile().getStatusFile(); 
                sf.setPos( 0 ); 
                sf.changeMode( MODE_UPLOAD );  
                return "success"; 
            } finally { 
                try { inp.close(); }catch(Throwable t){;} 
                try { out.close(); }catch(Throwable t){;} 
            } 
        } 
    } 
    
    private class ModeUploadProcess extends FileUploadItemProc {

        private final int STAT_SUCCESS    = 1;
        private final int STAT_PROCESSING = 2;
        private final int STAT_FILE_LOC_CONF_NOT_FOUND = 3; 
        private final int STAT_FILE_LOC_TYPE_NOT_FOUND = 4; 
        
        FileUploadItem root = FileUploadItem.this; 

        FileTransferSession sess; 
        LinkedBlockingQueue queue;
        
        public void cancel() { 
            super.cancel(); 
            
            try {
                sess.cancel(); 
            } catch(Throwable t){;} 
            
            sess = null; 
        } 

        public void run() { 
            boolean completed = false;
            boolean success = false;
            String message = null; 
            
            while (true) {
                try {
                    if ( isCancelled() ) { 
                        throw new InterruptedException();
                    } 
                    
                    int stat = runImpl(); 
                    if ( stat == STAT_SUCCESS ) {
                        message = "success";
                        root.createTempFile(".completed");
                        root.removeTempFile(".upload");
                        root.removeTempFile(".error");                         
                        success = true;
                        completed = true; 
                        break; 
                    } else if ( stat == STAT_FILE_LOC_CONF_NOT_FOUND ) {
                        message = "file location config not found";
                        root.createTempFile(".error", message);
                        root.removeTempFile(".upload");                         
                        completed = true; 
                        break;
                    } else if ( stat == STAT_FILE_LOC_TYPE_NOT_FOUND ) {
                        message = "file location type not found";
                        root.createTempFile(".error", message);
                        root.removeTempFile(".upload");                         
                        completed = true; 
                        break;
                    }
                    
                } catch(InterruptedException ie) {
                    break; 
                } catch(Throwable t) { 
                    Throwable c = getNonRuntimeException(t); 
                    System.out.println("[ModeUploadProcess] error -> "+ c.getMessage());
                    
                    if ( !root.folder.exists()) { 
                        System.out.println("[ModeUploadProcess] file item "+ root.getName() +" has been deleted manually");
                        FileUploadManager.getInstance().remove( root ); 
                        break; 
                    } 
                } 
                
                pause( 1000 );  
            }
            
            if ( completed ) {
                fireOnCompleted(success, message);
            }
        }
        
        private int runImpl() throws Exception { 
            ConfigFile conf = root.getConfigFile().read(); 
            StatusFile sf = root.getContentFile().getStatusFile().read();
            
            long filepos = sf.getPos(); 
            long filesize = sf.getSize(); 
            if ( filepos >= filesize ) { 
                sf.changeMode( MODE_COMPLETED ); 
                return STAT_SUCCESS; 
            } 

            FileUploadManager fum = FileUploadManager.getInstance();
            String filelocid = conf.getProperty( CONF_FILE_LOC_ID ); 
            FileLocationConf fileloc = FileManager.getInstance().getLocationConfs().get( filelocid );
            if ( fileloc == null ) {
                System.out.println("[ModeUploadProcess] '"+ filelocid +"' file location config not found for "+ root.getName()); 
                return STAT_FILE_LOC_CONF_NOT_FOUND; 
            }
            
            FileLocTypeProvider loctype = fum.getLocType( fileloc.getType() ); 
            if ( loctype == null ) {
                System.out.println("[ModeUploadProcess] '"+ fileloc.getType() +"' file location type not found for "+ root.getName()); 
                return STAT_FILE_LOC_TYPE_NOT_FOUND; 
            }
            
            String filetype = conf.getProperty( CONF_FILE_TYPE ); 
            StringBuilder sb = new StringBuilder(); 
            sb.append( root.getName() ); 
            if ( filetype != null && filetype.trim().length() > 0 ) {
                sb.append(".").append( filetype.trim()); 
            }
            
            sess = loctype.createUploadSession(); 
            sess.setFile( root.getContentFile().getFile() ); 
            sess.setLocationConfigId( filelocid );
            sess.setTargetName( sb.toString() );
            sess.setOffset( filepos ); 
            
            TransferHandler th = new TransferHandler(); 
            th.proc = this; 
            sess.setHandler( th ); 
            sess.run(); 
            return STAT_SUCCESS; 
        } 
        
        private Throwable getNonRuntimeException( Throwable t ) {
            if ( t instanceof RuntimeException ) {
                Throwable c = t.getCause(); 
                if ( c == null ) return t; 
                
                return getNonRuntimeException(c); 
            } else {
                return t; 
            } 
        }    
        
        private void pause( long millis ) {
            try {
                if ( queue == null ) {
                    queue = new LinkedBlockingQueue(); 
                }
                
                queue.poll(millis, TimeUnit.MILLISECONDS ); 
            } catch(Throwable t) {
                //do nothing 
            }
        }
    }
    
    private class ModeRemovalProcess extends FileUploadItemProc {

        FileUploadItem root = FileUploadItem.this; 

        public void run() { 
            try {
                if ( !isCancelled() ) {
                    runImpl(); 
                } 
            } catch(Throwable t) { 
                Throwable c = getNonRuntimeException(t); 
                System.out.println("[ModeRemovalProcess] error -> "+ c.getMessage());
            } 
        }
        
        private void runImpl() throws Exception { 
            ConfigFile conf = root.getConfigFile().read(); 
            String filelocid = conf.getProperty( CONF_FILE_LOC_ID ); 
            FileUploadManager fum = FileUploadManager.getInstance();
            FileLocationConf fileloc = FileManager.getInstance().getLocationConfs().get( filelocid );
            if ( fileloc == null ) {
                System.out.println("[ModeRemovalProcess] '"+ filelocid +"' file location config not found for "+ root.getName()); 
                return; 
            }
            
            FileLocTypeProvider loctype = fum.getLocType( fileloc.getType() ); 
            if ( loctype == null ) {
                System.out.println("[ModeRemovalProcess] '"+ fileloc.getType() +"' file location type not found for "+ root.getName()); 
                return; 
            }
            
            String filetype = conf.getProperty( CONF_FILE_TYPE ); 
            StringBuilder sb = new StringBuilder(); 
            sb.append( root.getName() ); 
            if ( filetype != null && filetype.trim().length() > 0 ) {
                sb.append(".").append( filetype.trim()); 
            }
            
            fum.remove( root ); 
            try { 
                loctype.deleteFile( sb.toString(), filelocid ); 
            } catch(Throwable t) {
                //do nothing 
            }
        } 
        
        private Throwable getNonRuntimeException( Throwable t ) {
            if ( t instanceof RuntimeException ) {
                Throwable c = t.getCause(); 
                if ( c == null ) return t; 
                
                return getNonRuntimeException(c); 
            } else {
                return t; 
            } 
        }    
    }    
    
    private class TransferHandler implements FileTransferSession.Handler {

        FileUploadItem root = FileUploadItem.this; 
        FileUploadItemProc proc; 
        
        public void ontransfer(long filesize, long bytesprocessed) { 
            if ( root.isMarkedForRemoval() && proc != null ) {
                proc.cancel();
                return; 
            }
            
            StatusFile sf = root.getContentFile().getStatusFile().read(); 
            sf.setPos( bytesprocessed );
            sf.update(); 

            FileUploadManager.getInstance().getFileHandlers().notifyOnTransfer( root, filesize, bytesprocessed );
        }

        public void oncomplete() { 
            root.getContentFile().getStatusFile().changeMode( MODE_COMPLETED ); 
            FileUploadManager.getInstance().getFileHandlers().unregister( root ); 
        } 

        public void ontransfer(long bytesprocessed) {
        }
    } 
}
