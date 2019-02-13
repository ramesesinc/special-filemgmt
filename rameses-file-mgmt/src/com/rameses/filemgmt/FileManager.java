/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rameses.filemgmt;

import com.rameses.io.FileLocTypeProvider;
import com.rameses.util.Encoder;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
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
public final class FileManager {
    
    private static FileManager instance = null; 
    
    public static synchronized FileManager getInstance() { 
        if ( instance == null ) { 
            instance = new FileManager(); 
        } 
        return instance; 
    } 
    
    
    private final static Object STATUS_LOCKED = new Object();    
    private final static Object LOCTYPE_LOCKED = new Object();
    private Map<String, FileLocTypeProvider> fileLocTypes;

    private String sessionid;
    private boolean started; 
    private boolean enabled; 
    private boolean enableUpload;
    private boolean enableDownoad; 
    private long fetchLocationInterval;    
    private ExecutorService threadPool;
    private ScheduledExecutorService scheduler;     
    
    private DbProvider dbprovider; 
    private FileLocationProvider fileLocProvider;  
    private FileLocationConfs fileLocConfs;
    private File tempdir; 
    private Helper helper;
    
    private FileManager() { 
        this.sessionid = Encoder.MD5.encode( new java.rmi.server.UID().toString());        
        this.enabled = true; 
        this.enableUpload = true; 
        this.enableDownoad = true; 
        this.fetchLocationInterval = 10000;   
        this.helper = new Helper();
        this.fileLocConfs = new FileLocationConfs();        
        this.threadPool = Executors.newFixedThreadPool(100);  
        this.scheduler = Executors.newScheduledThreadPool(10); 
    } 
    
    public boolean isValidSession() {
        return helper.isValidSession(); 
    }
    
    public boolean isEnabled() { return enabled; } 
    public void setEnabled( boolean enabled ) {
        this.enabled = enabled; 
    } 

    public boolean isEnableUpload() { 
        return (isEnabled() ? enableUpload : false); 
    } 
    public void setEnableUpload( boolean enableUpload ) {
        this.enableUpload = enableUpload; 
    } 
    
    public boolean isEnableDownload() { 
        return (isEnabled() ? enableDownoad : false);         
    } 
    public void setEnableDownload( boolean enableDownoad ) {
        this.enableDownoad = enableDownoad; 
    }     
    
    public long getFetchLocationInterval() {
        return this.fetchLocationInterval; 
    }
    public void setFetchLocationInterval( long fetchLocationInterval ) {
        this.fetchLocationInterval = fetchLocationInterval; 
    } 
    
    public File getTempDir() { 
        return getTempDir( "filemanager" ); 
    } 
    public File getTempDir( String group ) { 
        return helper.getTempDir( group ); 
    } 
    public void setTempDir( File tempdir ) { 
        this.tempdir = tempdir; 
    } 
    
    public Helper getHelper() { return helper; } 
    
    public DbProvider getDbProvider() { return dbprovider; }
    public void setDbProvider( DbProvider dbprovider ) {
        this.dbprovider = dbprovider; 
    }
    
    public FileLocationProvider getLocationProvider() { return fileLocProvider; }
    public void setLocationProvider( FileLocationProvider fileLocProvider ) {
        this.fileLocProvider = fileLocProvider; 
    }
    
    public void loadFileLocTypeProviders( ClassLoader loader ) {
        synchronized (LOCTYPE_LOCKED) { 
            if ( fileLocTypes == null ) { 
                Map<String, FileLocTypeProvider> types = new HashMap();
                ClassLoader cl = (loader != null ? loader : getClass().getClassLoader()); 
                Iterator<FileLocTypeProvider> itr = com.rameses.util.Service.providers( com.rameses.io.FileLocTypeProvider.class, cl ); 
                while (itr.hasNext()) {
                    FileLocTypeProvider prov = itr.next(); 
                    types.put( prov.getName(), prov ); 
                }
                fileLocTypes = types; 
            } 
        } 
    }
    public FileLocTypeProvider getLocType( String name ) {
        synchronized (LOCTYPE_LOCKED) { 
            if ( fileLocTypes == null ) {
                return null; 
            } else {
                return fileLocTypes.get( name ); 
            }
        } 
    } 
    
    public FileLocationConfs getLocationConfs()  {
        return fileLocConfs; 
    } 
    
    
    public ImageIcon getFileTypeIcon( String name ) {
        if ( name == null || name.trim().length() == 0) return null; 
        
        try { 
            URL url = FileManager.class.getResource("images/filetype-"+ name.toLowerCase() +".png"); 
            return (url == null ? null : new ImageIcon(url)); 
        } catch(Throwable t) {
            return null; 
        }
    }
    
    public boolean isStarted() {
        return started; 
    }
    public void start() { 
        synchronized( STATUS_LOCKED ) { 
            if ( !isEnabled() ) { 
                System.out.println("FileManager has been disabled");
                return; 
            }
            if ( started ) { 
                System.out.println("FileManager has already been started");
                return; 
            }
            
            System.out.println("Starting FileManager...");
            helper.createSessionFile(); 
            scheduler.schedule(new SessionCheckProc(), 400, TimeUnit.MILLISECONDS);
            scheduler.schedule(new FileLocationConfFetcher(), 500, TimeUnit.MILLISECONDS); 
            FileUploadManager.getInstance().start(); 
            FileDownloadManager.getInstance().start(); 
            started = true; 
        } 
    } 

    public void stop() { 
        synchronized( STATUS_LOCKED ) { 
            if ( !isEnabled() ) { 
                System.out.println("FileManager has been disabled");
                return; 
            }

            System.out.println("Stopping FileManager...");
            if ( !started ) return; 

            helper.deleteSessionFile(); 
            fileLocConfs.cancel(); 
            shutdown( scheduler.shutdownNow() ); 
            shutdown( threadPool.shutdownNow() ); 
            fileLocConfs.clear();  
            
            FileUploadManager.getInstance().stop(); 
            FileDownloadManager.getInstance().stop(); 
            started = false; 
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
    
    
    public static interface DbProvider {
        Map save( Map data ); 
        Map read( Map params ); 
    } 
    
    private class SessionCheckProc implements RunProc { 

        FileManager root = FileManager.this; 
        private boolean cancelled;
        
        public void cancel() { 
            cancelled = true; 
        }

        public void run() { 
            if ( cancelled ) return; 
            
            boolean valid = true; 
            try {
                if (root.isValidSession()) {
                    // session is still valid 
                } else { 
                    valid = false; 
                    root.stop(); 
                }
            } catch(Throwable t) {
                //do nothing 
            } finally {
                if ( valid ) schedule(); 
            }
        }
        
        void schedule() { 
            if ( cancelled ) return; 
            root.scheduler.schedule( new SessionCheckProc(), 1000, TimeUnit.MILLISECONDS); 
        } 
    }
    
    private class FileLocationConfFetcher implements RunProc { 
        
        FileManager root = FileManager.this; 
        boolean cancelled; 
        int index; 
        
        public void cancel() { 
            this.cancelled = true; 
        }
        
        public void run() { 
            if ( !root.isEnabled() ) {
                return; 
            }
                
            long interval = 0;  
            FileLocationProvider provider = null; 
            
            try {
                interval = root.getFetchLocationInterval(); 
                provider = root.getLocationProvider();
                runImpl(); 
            } catch(Throwable t) {
                System.out.println("[FileLocationConfFetcher] "+ t.getMessage());
            } finally { 
                if ( cancelled ) {
                    return; 
                    
                } else if ( provider != null && interval > 0 ) {
                    FileLocationConfFetcher ff = new FileLocationConfFetcher(); 
                    ff.index = ( this.index == 0 ? 1 : this.index ); 
                    root.scheduler.schedule(ff, interval, TimeUnit.MILLISECONDS); 
                }
            }
        } 
        
        private void runImpl() throws Exception { 
            if ( cancelled ) return; 
            
            FileLocationProvider provider = root.getLocationProvider();
            List<Map> list = (provider == null ? null : provider.getLocations()); 
            if ( list != null ) {
                for ( Map item : list ) { 
                    if ( cancelled ) break; 

                    FileLocationConf flc = new FileLocationConf( item ); 
                    root.fileLocConfs.add( flc ); 

                    FileLocTypeProvider loctype = root.getLocType( flc.getType() );
                    if ( loctype instanceof FileLocationRegistry ) {
                        ((FileLocationRegistry) loctype).register( flc ); 
                    }
                } 
            } 
        }
    }
    
    private final static Object FILE_LOC_CONF_LOCKED = new Object();
    public class FileLocationConfs {
        
        private String defaultName; 
        private boolean cancelled;
        private Map <String, FileLocationConf> confs; 
        
        FileLocationConfs() {
            this.confs = new HashMap(); 
        }
        
        void add( FileLocationConf conf ) { 
            synchronized( FILE_LOC_CONF_LOCKED ) { 
                if ( cancelled ) return; 
                
                String sname = (conf == null ? null : conf.getName()); 
                if ( sname == null ) return; 

                this.confs.put(sname, conf);
                if ( conf.isDefaulted() ) { 
                    this.defaultName = sname; 
                }
            }
        }
        
        void clear() { 
            confs.clear(); 
        } 
        
        void cancel() { 
            synchronized( FILE_LOC_CONF_LOCKED ) { 
                this.cancelled = true; 
            } 
        }
        
        public FileLocationConf getDefaultConf() { 
            return (defaultName == null ? null: confs.get( defaultName)); 
        }
        
        public FileLocationConf get( String name ) {
            synchronized( FILE_LOC_CONF_LOCKED ) { 
                return confs.get( name ); 
            }
        }
        public boolean isEmpty() {
            synchronized( FILE_LOC_CONF_LOCKED ) { 
                return confs.isEmpty(); 
            }
        }
    } 
    
    
    public class Helper { 
        private final String GROUP_NAME = "filemanager"; 
        
        public File getTempDir( String group ) { 
            File tmpdir = FileManager.this.tempdir; 
            if ( tmpdir == null ) { 
                tmpdir = new File(System.getProperty("java.io.tmpdir"));
            } 

            StringBuilder cname = new StringBuilder(); 
            cname.append("rameses"); 
            if ( group != null && group.trim().length() > 0 ) {
                cname.append("/").append( group ); 
            }
            
            File basedir = new File( tmpdir, cname.toString() );
            try {
                if ( !basedir.exists()) { 
                    basedir.mkdir(); 
                } 
                return basedir; 

            } catch (RuntimeException re) {
                throw re; 
            } catch (Exception e) { 
                throw new RuntimeException( e.getMessage(), e ); 
            } 
        }  
        
        private void createSessionFile() {
            File folder = getTempDir( GROUP_NAME ); 
            File[] files = folder.listFiles(); 
            for (int i=0; i<files.length; i++) {
                if ( files[i].getName().startsWith("sessionid_")) {
                    try {
                        files[i].delete(); 
                    } catch(Throwable t) {
                        //do nothing 
                    }
                }
            }
            
            String sessionid = FileManager.this.sessionid;
            createTempFile( "sessionid_"+ sessionid, sessionid ); 
        }
        private void deleteSessionFile() {
            File folder = getTempDir( GROUP_NAME ); 
            File file = new File( folder, "sessionid_"+ FileManager.this.sessionid ); 
            try {
                file.delete(); 
            } catch(Throwable t) {
                //do nothing 
            }
        }
        private boolean isValidSession() {
            File folder = getTempDir( GROUP_NAME ); 
            File file = new File( folder, "sessionid_"+ FileManager.this.sessionid ); 
            return ( file.exists() && !file.isDirectory()); 
        }
        
    
        public void createTempFile( String name, String content ) {
            createTempFile( getTempDir(GROUP_NAME), name, content); 
        }  
        public void createTempFile( File folder, String name, String content ) {
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
            return removeTempFile( getTempDir(GROUP_NAME), name); 
        }
        public boolean removeTempFile( File folder, String name ) {
            try {
                File tmp = new File( folder, name); 
                return (tmp.exists() ? tmp.delete() : true); 
            } catch(Throwable t) {
                return false; 
            } 
        }
    }
}
