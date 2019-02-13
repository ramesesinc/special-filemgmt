/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rameses.filemgmt;

import com.rameses.io.FileLocType;
import com.rameses.io.FileLocTypeProvider;
import com.rameses.io.FileTransferSession;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.ImageIcon;

/**
 *
 * @author wflores 
 */
public final class FileUploadManager { 
    
    private final static Object INIT_LOCKED = new Object();
    private static FileUploadManager instance = null; 
    
    public static FileUploadManager getInstance() { 
        synchronized ( INIT_LOCKED ) {
            if ( instance == null ) { 
                instance = new FileUploadManager(); 
            }
            return instance; 
        }
    }
    private static void reset() {
        synchronized ( INIT_LOCKED ) { 
            FileUploadManager fum = new FileUploadManager(); 
            if ( instance != null ) { 
                instance.stop(); 
            } 
            instance = fum; 
        }
    }

    
    private File tempdir;    
    private Helper helper;    
    private FileUploadProvider fileUploadProvider; 
    private FileHandlers fileHandlers; 
    
    private ExecutorService threadPool;
    private ScheduledExecutorService scheduler; 
    private Map<String, String> cache;
    
    private final static Object STATUS_LOCKED = new Object();
    private final static Object CACHE_LOCKED = new Object();
    
    private boolean started; 

    private FileUploadManager() { 
        this.helper = new Helper();
        this.cache = new HashMap();
        this.fileHandlers = new FileHandlers(); 
        this.threadPool = Executors.newFixedThreadPool(100);
        this.scheduler = Executors.newScheduledThreadPool(10); 
        this.fileUploadProvider = new FileUploadProviderImpl();
    } 
    
    @Deprecated
    public boolean isEnabled() { 
        return FileManager.getInstance().isEnableUpload(); 
    } 
    @Deprecated
    public void setEnabled( boolean enabled ) {
        FileManager.getInstance().setEnableUpload( enabled ); 
    } 
        
    public void setTempDir( File tempdir ) { 
        this.tempdir = tempdir; 
    } 
    public File getTempDir() { 
        return getTempDir( "fileupload" ); 
    } 

    File getTempDir( String group ) { 
        return FileManager.getInstance().getHelper().getTempDir( group ); 
    } 
    
    @Deprecated
    public FileLocationProvider getLocationProvider() { 
        return FileManager.getInstance().getLocationProvider(); 
    }
    @Deprecated    
    public void setLocationProvider( FileLocationProvider fileLocProvider ) {
        FileManager.getInstance().setLocationProvider( fileLocProvider ); 
    } 

    public FileUploadProvider getUploadProvider() { return fileUploadProvider; }
    public void setUploadProvider( FileUploadProvider fileUploadProvider ) {
        this.fileUploadProvider = fileUploadProvider; 
    }


    public Helper getHelper() { 
        return helper; 
    }
    public FileHandlers getFileHandlers() {
        return fileHandlers; 
    }
    
    public FileLocTypeProvider getLocType( String name ) {
        return FileManager.getInstance().getLocType( name ); 
    }
    
    void start() { 
        synchronized( STATUS_LOCKED ) { 
            if ( !isEnabled() ) { 
                System.out.println("FileUploadManager has been disabled");
                return; 
            }
            if ( started ) { 
                System.out.println("FileUploadManager has already been started");
                return; 
            }
            
            System.out.println("Starting FileUploadManager...");
            new FileScanner().schedule(); 
            started = true; 
        }        
    } 

    void stop() { 
        synchronized( STATUS_LOCKED ) { 
            if ( !isEnabled() ) { 
                System.out.println("FileUploadManager has been disabled");
                return; 
            }

            System.out.println("Stopping FileUploadManager...");
            if ( !started ) return; 

            shutdown( scheduler.shutdownNow() );
            shutdown( threadPool.shutdownNow() );

            cache.clear(); 
            started = false; 
        }
    } 
    
    public void schedule( FileUploadItem fui ) { 
        synchronized ( CACHE_LOCKED ) {
            if ( fui == null ) return; 
            if ( !isEnabled() ) return; 

            FileUploadItemProc proc = null;
            String keyname = fui.getName(); 
            if ( fui.isMarkedForRemoval()) { 
                cache.remove( keyname ); 
                getFileHandlers().unregister( fui ); 
                proc = fui.createProcessHandler(); 
                if ( proc != null ) {
                    threadPool.submit( proc ); 
                }
                return; 
            } 
            
            if ( fui.isModeCompleted()) {
                cache.remove( keyname ); 
                getFileHandlers().unregister( fui ); 
            }

            proc = fui.createProcessHandler(); 
            if ( proc == null ) { 
                cache.remove( keyname ); 
                getFileHandlers().unregister( fui ); 
                return; 
            } 
            
            if ( cache.containsKey( keyname)) return; 
            
            if ( fui.isModeUpload()) {
                proc.setHandler( new FileItemProcHandler( fui)); 
            } else {
                proc.setHandler( new FileItemProcHandler( fui)); 
            }
            
            cache.put( keyname, keyname ); 
            threadPool.submit( proc ); 
        }
    }
    
    public void remove( FileUploadItem item ) {
        synchronized ( CACHE_LOCKED ) {
            if ( item == null ) return; 

            String keyname = item.getName(); 
            item.remove(); 
            cache.remove( keyname ); 
            getFileHandlers().unregister( item ); 
        }
    }
    
    private void removeCache( String name ) {
        synchronized ( CACHE_LOCKED ) {
            if ( name != null ) { 
                cache.remove( name );
            } 
        }
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
    
    private class FileItemProcHandler implements FileUploadItemProc.Handler { 
        
        private FileUploadItem item; 
        
        FileItemProcHandler( FileUploadItem item ) {
            this.item = item; 
        }
                
        public void onCompleted( boolean success, String message ) { 
            if ( item == null ) return; 
            
            FileUploadManager.this.removeCache( item.getName()); 
            if ( success ) { 
                FileUploadManager.this.schedule( item ); 
            } 
        } 
    } 
        
    public class FileHandlers {
        
        private final Object LOCKED = new Object();
        private final Map<String, FileHandlerProxy> handlers = new HashMap();
        
        void register( FileUploadItem item ) { 
            synchronized (LOCKED) {
                if ( item == null ) return; 
                
                handlers.put( item.getName(), new FileHandlerProxy()); 
            }
        }
        void unregister( FileUploadItem item ) { 
            synchronized (LOCKED) {
                if ( item == null ) return; 
                
                FileHandlerProxy proxy = handlers.remove( item.getName()); 
                if ( proxy != null ) proxy.clear(); 
            }
        }
        
        public void add( FileUploadItem item, FileHandler handler ) { 
            if ( item == null || handler == null ) return;
            
            FileHandlerProxy proxy = handlers.get( item.getName()); 
            if ( proxy != null ) proxy.add( handler ); 
        }
        public void remove( FileUploadItem item, FileHandler handler ) { 
            if ( item == null || handler == null ) return;
            
            FileHandlerProxy proxy = handlers.get( item.getName()); 
            if ( proxy != null ) proxy.remove( handler ); 
        } 
        
        
        public void notifyOnTransfer( FileUploadItem item, long filesize, long bytesprocessed) { 
            FileHandlerProxy proxy = handlers.get( item.getName()); 
            if ( proxy != null ) {
                proxy.onTransfer( item, filesize, bytesprocessed ); 
            }
        }
        public void notifyOnCompleted( FileUploadItem item ) { 
            FileHandlerProxy proxy = handlers.get( item.getName()); 
            if ( proxy != null ) { 
                proxy.onCompleted( item ); 
            }
        } 
    }
    
    public static interface FileHandler { 
        void onTransfer( FileUploadItem item, long filesize, long bytesprocessed );
        void onCompleted( FileUploadItem item );
    } 
    
    private class FileHandlerProxy implements FileHandler {
        
        ArrayList<FileHandler> handlers = new ArrayList();
        
        void clear() { 
            handlers.clear(); 
        }
        void add( FileHandler handler ) {
            if ( handler != null && !handlers.contains(handler)) {
                handlers.add( handler ); 
            }
        }
        void remove( FileHandler handler ) {
            if ( handler != null ) {
                handlers.remove( handler ); 
            }
        }
        
        public void onTransfer(FileUploadItem item, long filesize, long bytesprocessed) { 
            FileHandler[] arr = handlers.toArray(new FileHandler[]{}); 
            for (int i=0; i<arr.length; i++) { 
                try {
                    arr[i].onTransfer(item, filesize, bytesprocessed); 
                } catch(Throwable t) {
                    //do nothing 
                }
            }
        }

        public void onCompleted(FileUploadItem item) { 
            FileHandler[] arr = handlers.toArray(new FileHandler[]{}); 
            for (int i=0; i<arr.length; i++) { 
                try {
                    arr[i].onCompleted(item); 
                } catch(Throwable t) {
                    //do nothing 
                }
            } 
        }
    }
        
    private class FileScanner implements RunProc {
        
        FileUploadManager root = FileUploadManager.this; 
        
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
                FileUploadItem fui = FileUploadItem.open( file ); 
                root.schedule( fui );
            } 
        }
    }
    
    private class ValidFileFilter implements FileFilter { 
        
        FileUploadManager root = FileUploadManager.this; 
        
        public boolean accept(File file) { 
            if ( !file.isDirectory() ) return false; 
            if ( file.getName().endsWith("~")) return false; 
            if ( root.cache.containsKey(file.getName())) return false; 

            File child = new File( file, ".error");
            if ( child.exists()) return false; 
            
            child = new File( file, ".ready"); 
            if ( child.exists()) return false; 

            child = new File( file, ".forremoval"); 
            if ( child.exists()) return true; 
            
            child = new File( file, ".completed"); 
            if ( child.exists()) return false; 

            child = new File( file, ".conf"); 
            if ( !child.exists()) return false; 
            
            child = new File( file, "content.index");
            if ( !child.exists()) return false; 
            
            child = new File( file, ".started"); 
            return child.exists();
        } 
    } 
        
    public class Helper {

        private FileUploadManager getRoot() { 
            return FileUploadManager.this; 
        } 
        
        public long getFileSize( File file ) throws Exception { 
            RandomAccessFile raf = null; 
            FileChannel fc = null; 
            try { 
                raf = new RandomAccessFile( file, "r" );
                fc = raf.getChannel(); 
                return fc.size(); 
            } finally {
                try { fc.close(); }catch(Throwable t){;}
                try { raf.close(); }catch(Throwable t){;}
            }
        }
        
        public File download( final String filelocid, final String remoteName, String filetype  ) {
            File file = getDownloadFile( remoteName ); 
            String status = getDownloadStatus( remoteName ); 
            if ( "completed".equals(status)) return file;
            
            FileLocationConf fileloc = FileManager.getInstance().getLocationConfs().get( filelocid );
            FileLocTypeProvider provider = FileLocType.getProvider( fileloc.getType()); 
            FileTransferSession sess = provider.createDownloadSession();
            sess.setLocationConfigId( filelocid ); 
            sess.setTargetName( remoteName+"."+filetype ); 
            sess.setFile( file ); 
            sess.setHandler(new FileTransferSession.Handler() {
                public void ontransfer(long bytesprocessed) {
                }
                public void ontransfer(long filesize, long bytesprocessed) {
                }
                public void oncomplete() { 
                    try {
                        removeDownloadStatus( remoteName, "processing" ); 
                        createDownloadStatus( remoteName, "completed" ); 
                    } catch (Throwable t) { 
                        t.printStackTrace();  
                    } 
                }
            });
            sess.run(); 
            return null; 
        }
        
        public String getDownloadStatus( String name ) { 
            File file = new File( getRoot().getTempDir("filedownload"), name); 
            if ( !file.exists()) return null; 
            
            if ( new File(file, ".processing").exists()) {
                return "processing"; 
            } else if ( new File(file, ".completed").exists()) {
                return "completed"; 
            } else { 
                return null; 
            } 
        }
        public File createDownloadStatus( String name, String status ) throws Exception { 
            File file = new File( getRoot().getTempDir("filedownload"), name+"/."+status); 
            if ( !file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile(); 
            } 
            return file; 
        }
        public void removeDownloadStatus( String name, String status ) throws Exception { 
            File file = new File( getRoot().getTempDir("filedownload"), name+"/."+status); 
            if ( file.exists()) file.delete(); 
        }
        public File createDownloadFile( String name ) throws Exception { 
            createDownloadStatus( name, "processing" ); 
            File file = new File( getRoot().getTempDir("filedownload"), name+"/content"); 
            if ( !file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile(); 
            } 
            return file; 
        }
        public File getDownloadFile( String name ) {
            return new File( getRoot().getTempDir("filedownload"), name+"/content");             
        }
        
        void write( File file, String data ) {
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
        } 
        String read( File file ) {
            FileInputStream inp = null; 
            try {
                inp = new FileInputStream( file ); 
                StringBuilder sb = new StringBuilder(); 
                byte[] bytes = new byte[1024]; 
                int read = -1;
                while ((read=inp.read(bytes)) != -1) {
                    sb.append(new String(bytes, 0, read)); 
                }
                return sb.toString(); 
            } catch(Throwable t) {
                return null; 
            } finally { 
                try { inp.close(); }catch(Throwable t){;} 
            } 
        } 
        
        public ImageIcon getDownloadImage( String name ) throws Exception { 
            File file = getDownloadFile( name ); 
            return new javax.swing.ImageIcon( file.toURI().toURL() );
        }
    }
}
