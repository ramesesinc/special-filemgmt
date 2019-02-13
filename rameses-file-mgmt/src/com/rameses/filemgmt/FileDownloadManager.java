/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rameses.filemgmt;

import com.rameses.io.FileLocTypeProvider;
import com.rameses.io.FileTransferSession;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author wflores
 */
public final class FileDownloadManager {
    
    private final static Object INIT_LOCKED = new Object();
    private static FileDownloadManager instance = null; 
    
    public static FileDownloadManager getInstance() { 
        synchronized ( INIT_LOCKED ) {
            if ( instance == null ) { 
                instance = new FileDownloadManager(); 
            }
            return instance; 
        }
    }
    private static void reset() {
        synchronized ( INIT_LOCKED ) { 
            FileDownloadManager o = new FileDownloadManager(); 
            if ( instance != null ) { 
                instance.stop(); 
            } 
            instance = o; 
        }
    } 
    
    
    private final static Object STATUS_LOCKED = new Object();
    private final static Object CACHE_LOCKED = new Object();
    
    private boolean started; 
    private Map<String, String> cache;
    private ExecutorService threadPool;
    private ScheduledExecutorService scheduler; 
    private FileHandlers fileHandlers; 

    private FileDownloadManager() { 
        this.cache = new HashMap();
        this.fileHandlers = new FileHandlers();
        this.threadPool = Executors.newFixedThreadPool(100); 
        this.scheduler = Executors.newScheduledThreadPool(10); 
    }     
    
    @Deprecated
    public boolean isEnabled() { 
        return FileManager.getInstance().isEnableDownload(); 
    } 
    @Deprecated
    public void setEnabled( boolean enabled ) {
        FileManager.getInstance().setEnableDownload( enabled ); 
    } 
    
    public File getTempDir() { 
        return FileManager.getInstance().getTempDir("filedownload");  
    } 
    public FileHandlers getFileHandlers() {
        return fileHandlers; 
    }
    
    public String getStatus( String fileid ) {
        DownloadItem di = new DownloadItem( fileid );
        di.verifyFolder();
        
        if ( di.isModeCompleted()) return "completed"; 
        else if (di.isModeDownload()) return "download";
        else if (di.isModeStarted()) return "started"; 
        else return null; 
    }
    
    public File getContentFile( String fileid ) {
        DownloadItem di = new DownloadItem( fileid );
        di.verifyFolder();
        
        File file = di.getContentFile();
        try {
            return file.getCanonicalFile(); 
        } catch(Throwable t) {
            return file; 
        }
    }
    
    public boolean isStarted() {
        return started; 
    } 
    void start() { 
        synchronized( STATUS_LOCKED ) { 
            if ( !isEnabled() ) { 
                System.out.println("FileDownloadManager has been disabled");
                return; 
            }
            if ( isStarted() ) { 
                System.out.println("FileDownloadManager has already been started");
                return; 
            }
            
            System.out.println("Starting FileDownloadManager...");
            new FileScanner().schedule(); 
            started = true; 
        }        
    } 

    void stop() { 
        synchronized( STATUS_LOCKED ) { 
            if ( !isEnabled() ) { 
                System.out.println("FileDownloadManager has been disabled");
                return; 
            }

            System.out.println("Stopping FileDownloadManager...");
            if ( !isStarted() ) return; 

            shutdown( scheduler.shutdownNow() );
            shutdown( threadPool.shutdownNow() );
            started = false; 
        }
    }   
    
    public synchronized void download( String fileid, String filetype, String filelocid, long filesize, FileDownloadHandler handler ) { 
        if ( fileid == null || fileid.trim().length() == 0 ) 
            throw new RuntimeException("fileid parameter is required"); 
        if ( filelocid == null || filelocid.trim().length() == 0 ) 
            throw new RuntimeException("filelocid parameter is required"); 
        if ( filesize <= 0 ) 
            throw new RuntimeException("filesize must be greater than zero"); 
        
        fileHandlers.register( fileid ); 
        fileHandlers.add( fileid, handler ); 
        
        DownloadItem di = new DownloadItem(fileid, filetype, filelocid, filesize).init().start(); 
        schedule( di ); 
    } 
    
    void schedule( DownloadItem item ) { 
        synchronized ( CACHE_LOCKED ) {
            if ( item == null ) return; 
            if ( !isEnabled() ) return; 
            
            String keyname = item.getName(); 
            if ( item.isModeCompleted()) {
                cache.remove( keyname ); 
                fileHandlers.unregister( keyname ); 
                return; 
            } 
                        
            RunProc proc = item.createProcessHandler(); 
            if ( proc == null ) { 
                cache.remove( keyname ); 
                fileHandlers.unregister( keyname ); 

            } else if ( !cache.containsKey( keyname )) {
                cache.put( keyname, keyname ); 
                threadPool.submit( proc ); 
            }
        }
    }
    
    void detach( DownloadItem item ) {
        String keyname = (item == null ? null : item.getName()); 
        if ( keyname == null ) return; 
        
        fileHandlers.unregister(keyname);
        cache.remove( keyname ); 
    }
    
    
    
    private void shutdown( List<Runnable> procs ) {
        if ( procs == null ) return; 
        
        while (!procs.isEmpty()) { 
            Runnable r = procs.remove(0); 
            if ( r instanceof RunProc ) {
                try { 
                    ((RunProc) r).cancel(); 
                } catch(Throwable t) { 
                    //do nothing 
                } 
            } 
        } 
    } 
    

    private class FileDownloadHandlerProxy implements FileDownloadHandler {
        
        ArrayList<FileDownloadHandler> handlers = new ArrayList();
        
        void clear() { 
            handlers.clear(); 
        }
        void add( FileDownloadHandler handler ) {
            if ( handler != null && !handlers.contains(handler)) {
                handlers.add( handler ); 
            }
        }
        void remove( FileDownloadHandler handler ) {
            if ( handler != null ) {
                handlers.remove( handler ); 
            }
        }
        
        public void onTransfer( String fileid, long filesize, long bytesprocessed) { 
            FileDownloadHandler[] arr = handlers.toArray(new FileDownloadHandler[]{}); 
            for (int i=0; i<arr.length; i++) { 
                try {
                    arr[i].onTransfer( fileid, filesize, bytesprocessed); 
                } catch(Throwable t) {
                    //do nothing 
                }
            }
        }

        public void onCompleted( String fileid ) { 
            FileDownloadHandler[] arr = handlers.toArray(new FileDownloadHandler[]{}); 
            for (int i=0; i<arr.length; i++) { 
                try {
                    arr[i].onCompleted( fileid ); 
                } catch(Throwable t) {
                    //do nothing 
                }
            } 
        }
    }    
    
    public class FileHandlers {
        
        private final Object LOCKED = new Object();
        private final Map<String, FileDownloadHandlerProxy> handlers = new HashMap();
        
        void register( String name ) { 
            synchronized (LOCKED) {
                if ( name == null || name.trim().length() == 0) return; 
                if ( !handlers.containsKey( name )) {
                    handlers.put( name, new FileDownloadHandlerProxy()); 
                }
            }
        }
        void unregister( String name ) { 
            synchronized (LOCKED) {
                if ( name == null || name.trim().length() == 0) return; 
                
                FileDownloadHandlerProxy proxy = handlers.remove( name ); 
                if ( proxy != null ) proxy.clear(); 
            }
        }
        
        public void add( String name, FileDownloadHandler handler ) { 
            if ( name == null || name.trim().length() == 0 || handler == null ) return; 
            
            FileDownloadHandlerProxy proxy = handlers.get( name ); 
            if ( proxy != null ) proxy.add( handler ); 
        }
        public void remove( String name, FileDownloadHandler handler ) { 
            if ( name == null || name.trim().length() == 0 || handler == null ) return; 
            
            FileDownloadHandlerProxy proxy = handlers.get( name ); 
            if ( proxy != null ) proxy.remove( handler ); 
        } 
        
        public void notifyOnTransfer( String fileid, long filesize, long bytesprocessed) { 
            FileDownloadHandlerProxy proxy = handlers.get( fileid ); 
            if ( proxy != null ) {
                proxy.onTransfer( fileid, filesize, bytesprocessed ); 
            }
        }
        public void notifyOnCompleted( String fileid ) { 
            FileDownloadHandlerProxy proxy = handlers.get( fileid); 
            if ( proxy != null ) { 
                proxy.onCompleted( fileid ); 
            }
        } 
    }
        
    private class DownloadItem {
        
        FileDownloadManager root = FileDownloadManager.this; 
        
        private String fileid; 
        private String filetype; 
        private String filelocid;
        private long filesize; 
        private File basedir;
        
        DownloadItem( String fileid ) { 
            this( fileid, null, null, 0 ); 
        }
        
        DownloadItem( String fileid, String filetype, String filelocid, long filesize ) {
            this.fileid = fileid; 
            this.filetype = filetype; 
            this.filelocid = filelocid; 
            this.filesize = filesize; 
        }
        
        String getName() { return fileid; } 
        String getFileType() { return filetype; }
        String getFileLocId() { return filelocid; } 
        long getFileSize() { return filesize; } 
        
        File getBaseFolder() { 
            if ( basedir == null ) {
                File tempdir = root.getTempDir(); 
                try { 
                    tempdir = tempdir.getCanonicalFile(); 
                } catch(Throwable t) {;} 
                
                basedir = new File( tempdir, fileid );                 
            }
            return basedir; 
        } 
        
        File verifyFolder() {
            File dir = getBaseFolder(); 
            if ( !dir.exists()) dir.mkdirs(); 
            
            return dir; 
        }
        
        DownloadItem init() { 
            File dir = verifyFolder(); 
            File file = new File( dir, ".completed");
            if ( file.exists()) return null;
            
            file = new File( dir, ".conf" );
            if ( !file.exists()) { 
                DataFile df = new DataFile( dir, ".conf" ); 
                df.setProperty("filelocid", filelocid);
                df.setProperty("filetype", filetype);
                df.setProperty("filesize", filesize);
                df.setProperty("value", 0 ); 
                df.update(); 
            }
            return this; 
        } 
        
        DownloadItem start() { 
            File dir = getBaseFolder(); 
            File file = new File( dir, ".completed" );
            if ( file.exists()) return null; 
            
            FileManager fm = FileManager.getInstance();
            fm.getHelper().createTempFile( dir, ".started", null ); 
            return this; 
        }
        
        DownloadItem open() {
            File dir = getBaseFolder(); 
            if ( dir.exists()) {
                DataFile df = new DataFile( dir, ".conf" ).read();
                filetype = df.getProperty("filetype"); 
                filelocid = df.getProperty("filelocid"); 
                filesize = df.getPropertyAsNumber("filesize").longValue();
            }
            return this; 
        }
        
        boolean isModeCompleted() {
            return new File( getBaseFolder(), ".completed" ).exists(); 
        }
        boolean isModeDownload() {
            return new File( getBaseFolder(), ".download" ).exists(); 
        }
        boolean isModeStarted() {
            return new File( getBaseFolder(), ".started" ).exists(); 
        }
        
        File getContentFile() { 
            return new File( getBaseFolder(), "content" ); 
        }
        
        
        RunProc createProcessHandler() {
            if ( isModeCompleted()) {
                return null; 
            } else if ( isModeDownload()) {
                return new ModeDownloadProcess( this ); 
            } else if ( isModeStarted()) {
                return new ModeStartProcess( this ); 
            } else {
                return null; 
            }
        }
    }
    
    private class FileScanner implements RunProc {
        
        FileDownloadManager root = FileDownloadManager.this; 
        
        boolean cancelled; 
        
        public void cancel() {
            this.cancelled = true; 
        }
        
        void schedule() { 
            root.scheduler.schedule(this, 1000, TimeUnit.MILLISECONDS); 
        }

        public void run() { 
            try {
                if ( cancelled ) return; 
                
                runImpl();
            } catch(Throwable t) {
                t.printStackTrace(); 
            } finally {
                if ( cancelled ) return; 
                
                new FileScanner().schedule();
            }
        }
        
        private void runImpl() throws Exception { 
            if ( FileManager.getInstance().getLocationConfs().isEmpty() ) { 
                // no available location conf yet... 
                // proceed to next schedule 
                return; 
            }
            
            File tempdir = root.getTempDir(); 
            File[] files = tempdir.listFiles( new ValidFileFilter());  
            for ( File file : files ) { 
                if ( root.cache.containsKey( file.getName())) continue;
                
                DownloadItem di = new DownloadItem( file.getName() ).open(); 
                root.schedule( di );
            } 
        }
    }
    
    private class ValidFileFilter implements FileFilter { 
        
        FileDownloadManager root = FileDownloadManager.this; 
        
        public boolean accept(File file) { 
            if ( !file.isDirectory() ) return false; 
            if ( file.getName().endsWith("~")) return false; 
            if ( root.cache.containsKey(file.getName())) return false; 

            File child = new File( file, ".error");
            if ( child.exists()) return false; 
            
            child = new File( file, ".forremoval"); 
            if ( child.exists()) return true; 
            
            child = new File( file, ".completed"); 
            if ( child.exists()) return false; 

            child = new File( file, ".conf"); 
            if ( !child.exists()) return false; 
                        
            child = new File( file, ".started"); 
            return child.exists();
        } 
    } 
    
    private class DataFile { 
        private File basedir;
        private File file; 
        private Map conf; 
        
        DataFile( File basedir, String name ) {
            this.basedir = basedir; 
            this.file = new File( basedir, name ); 
            this.conf = new HashMap(); 
        }

        void create( Map conf ) {
            if ( conf != null && !conf.isEmpty() ) {
                this.conf.clear(); 
                this.conf.putAll( conf ); 
                
                update(); 
            }
        } 

        Map copyData() {
            Map data = new HashMap();
            data.putAll( this.conf ); 
            return data; 
        }
        
        public DataFile read() { 
            FileInputStream inp = null; 
            try { 
                if ( !file.exists()) return null; 
                
                inp = new FileInputStream( file );
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

                fos = new FileOutputStream( file ); 
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
    
    private class RescheduleException extends Exception {}
    
    private class ModeStartProcess implements RunProc {
        
        private DownloadItem item; 
        private boolean cancelled;
        
        FileDownloadManager root = FileDownloadManager.this; 
        
        ModeStartProcess( DownloadItem item ) {
            this.item = item;
        }

        public void cancel() {
            this.cancelled = true; 
        }

        public void run() { 
            try {
                if ( cancelled ) return; 
                
                FileManager fm = FileManager.getInstance();
                fm.getHelper().createTempFile( item.getBaseFolder(), ".download", "");
                fm.getHelper().removeTempFile( item.getBaseFolder(), ".started"); 
                
            } catch(Throwable t) {
                System.out.println("[ModeStartProcess] ("+ item.getName() +") error caused by "+ t.getMessage());
            } finally {
                if ( cancelled ) return; 
            }
            
            RunProc proc = item.createProcessHandler(); 
            if ( proc == null ) {
                root.detach( item ); 
            } else {
                root.threadPool.submit( proc );
            }
        }
    }
       
    private class ModeDownloadProcess implements RunProc {

        private boolean cancelled;         
        private DownloadItem item; 
        private FileTransferSession sess;
        
        FileDownloadManager root = FileDownloadManager.this; 

        ModeDownloadProcess( DownloadItem item ) {
            this.item = item; 
        }

        public void cancel() {
            this.cancelled = true; 
            
            try {
                sess.cancel(); 
            } catch(Throwable t) {
                //do nothing
            } finally {
                sess = null; 
            }
        }

        public void run() {
            if ( cancelled ) return;
            
            FileManager fm = FileManager.getInstance();
            String fileid = item.getName(); 
            
            try {
                item.open();
                
                if ( fm.getLocationConfs().isEmpty()) {
                    // location confs are not yet loaded, proceed to next schedule 
                    throw new RescheduleException(); 
                }
                
                String filelocid = item.getFileLocId();
                FileLocationConf conf = fm.getLocationConfs().get( filelocid ); 
                if ( conf == null ) 
                    throw new Exception(""+ filelocid +" file location conf does not exist"); 
                
                FileLocTypeProvider loctype = fm.getLocType( conf.getType() ); 
                if ( loctype == null ) 
                    throw new Exception("No available file location type provider for "+ conf.getType()); 
                
                String filetype = item.getFileType(); 
                StringBuilder remoteName = new StringBuilder(); 
                remoteName.append( fileid );
                if ( filetype != null && filetype.trim().length() > 0 ) {
                    remoteName.append(".").append( filetype ); 
                }
                
                File localFile = new File( item.getBaseFolder(), "content"); 
                FileTransferSession sess = loctype.createDownloadSession(); 
                sess.setTargetName( remoteName.toString() ); 
                sess.setLocationConfigId( filelocid ); 
                sess.setFile( localFile ); 
                sess.setHandler(new FileTransferSession.Handler() {
                    public void ontransfer(long bytesprocessed) { 
                        ontransfer( item.getFileSize(), bytesprocessed );
                    }
                    public void ontransfer(long filesize, long bytesprocessed) {
                        fireOnTransfer(filesize, bytesprocessed);
                    }
                    public void oncomplete() { 
                        fireOnComplete(); 
                    }
                });
                sess.run(); 

            } catch(RescheduleException rse) { 
                //do nothing 
            } catch(Throwable t) {
                System.out.println("[ModeDownloadProcess] ("+ fileid +") error caused by " + t.getMessage());
            } finally {
                if ( cancelled ) return; 
            }

            RunProc proc = item.createProcessHandler(); 
            if ( proc == null ) {
                root.detach( item ); 
            } else {
                root.scheduler.schedule(proc, 1000, TimeUnit.MILLISECONDS);
            }
        }
        
        void fireOnTransfer( long filesize, long bytesprocessed ) { 
            root.fileHandlers.notifyOnTransfer( item.getName(), filesize, bytesprocessed );  
        }
        void fireOnComplete() {
            FileManager fm = FileManager.getInstance();
            fm.getHelper().createTempFile( item.getBaseFolder(), ".completed", "");
            fm.getHelper().removeTempFile( item.getBaseFolder(), ".download");
            fm.getHelper().removeTempFile( item.getBaseFolder(), ".started");
            
            root.fileHandlers.notifyOnCompleted( item.getName() ); 
            root.fileHandlers.unregister( item.getName()); 
        } 
    } 
    
}
