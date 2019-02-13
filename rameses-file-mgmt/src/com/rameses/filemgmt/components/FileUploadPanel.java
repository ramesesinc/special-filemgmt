/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rameses.filemgmt.components;

import com.rameses.common.PropertyResolver;
import com.rameses.filemgmt.FileUploadItem;
import com.rameses.filemgmt.FileUploadManager;
import com.rameses.filemgmt.components.FileUploadItemCell.Info;
import com.rameses.rcp.common.FileUploadModel;
import com.rameses.rcp.common.MsgBox;
import com.rameses.rcp.control.XComponentPanel;
import com.rameses.rcp.framework.Binding;
import com.rameses.rcp.ui.annotations.ComponentBean;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.Beans;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 *
 * @author wflores
 */
@ComponentBean("com.rameses.filemgmt.components.FileUploadPanelModel")
public class FileUploadPanel extends XComponentPanel {
    
    private JPanel pnlCanvas;
    private JScrollPane scrollPane;

    private String handler;
    private FileUploadModelProxy proxyHandler;
    
    public FileUploadPanel() {
        initComponents();
    }
    
    public String getHandler() { return handler; } 
    public void setHandler( String handler ) {
        this.handler = handler; 
    }
    
    public void afterLoad() {
        super.afterLoad();

        Object caller = getBean();
        Object bean = getComponentBean(); 

        proxyHandler = new FileUploadModelProxy();         
        PropertyResolver pr = PropertyResolver.getInstance();
        
        String shandler = getHandler(); 
        if ( shandler != null && shandler.trim().length()>0 ) {
            Object oval = pr.getProperty(caller, shandler); 
            if ( oval instanceof FileUploadModel ) {
                proxyHandler.source = (FileUploadModel) oval; 
            }
        }
        
        proxyHandler.init(); 
        pr.setProperty(bean, "handler", proxyHandler ); 
    } 
    
    public void afterRefresh() {
        super.afterRefresh();
        
        Object caller = getBean();
        Object bean = getComponentBean(); 
        PropertyResolver pr = PropertyResolver.getInstance();
    } 
    
    
    private void initComponents() { 
        pnlCanvas = new JPanel();
        pnlCanvas.setLayout( new CanvasLayoutManager());
        pnlCanvas.setBorder( BorderFactory.createEmptyBorder(5, 5, 5, 5)); 
        pnlCanvas.setBackground( Color.WHITE ); 
        
        scrollPane = new JScrollPane( pnlCanvas );
        scrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
        scrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
        scrollPane.setPreferredSize(new Dimension(200, 100)); 
        scrollPane.setFocusable( false ); 
        scrollPane.setBackground( Color.WHITE ); 
        
        setLayout( new MainLayoutManager());
        setOpaque( false ); 
        add( scrollPane ); 
        
        if ( Beans.isDesignTime()) { 
            pnlCanvas.add( new FileUploadItemCell() );
            pnlCanvas.add( new FileUploadItemCell() );
        }        
    }
    
    
    private class FileUploadModelProxy extends FileUploadModel {
     
        FileUploadPanel root = FileUploadPanel.this; 
        FileUploadModelProvider provider;
        FileUploadModel source; 

        void init() {
            provider = new FileUploadModelProvider(); 
            if ( source != null ) { 
                source.setProvider( provider ); 
            } 
        }
        
        public void afterRemoveItem() { 
            if ( source != null ) {
                source.afterRemoveItem(); 
            }
        }        
    }
    
    private class FileUploadModelProvider implements FileUploadModel.Provider { 
        
        FileUploadPanel root = FileUploadPanel.this; 

        public Binding getBinding() {
            return root.getBinding(); 
        }

        public Binding getInnerBinding() { 
            return root.getInnerBinding(); 
        }

        public void add(FileUploadItem item, String filename, long filesize, Map props ) {
            if ( item == null ) return; 
            if ( props != null ) props.put("completed", false);
            
            FileTransferHandler fh = new FileTransferHandler(); 
            FileUploadManager.getInstance().getFileHandlers().add(item, fh); 
            
            FileUploadItemCell cell = new FileUploadItemCell(); 
            cell.setFileId( item.getName() ); 
            cell.setFileName( filename ); 
            cell.setFileSize( filesize ); 
            cell.setUserObject( props ); 
            fh.props = props;
            fh.cell = cell;
            
            FileItemRemoveHandler rh = new FileItemRemoveHandler();
            cell.setRemoveHandler( rh );
            rh.cell = cell; 
            rh.item = item; 
            
            root.pnlCanvas.add( cell );
            root.pnlCanvas.revalidate();
            root.pnlCanvas.repaint();
        }

        public void remove(FileUploadItem item) {
        }

        public List<Info> getItems() { 
            List<Info> items = new ArrayList();
            Component[] comps = root.pnlCanvas.getComponents(); 
            for (int i=0; i<comps.length; i++) {
                if ( comps[i] instanceof FileUploadItemCell ) {
                    FileUploadItemCell cell = (FileUploadItemCell) comps[i]; 
                    items.add( cell.getInfo() ); 
                }
            }
            return items; 
        }
    }
    
    private class FileItemRemoveHandler implements ActionListener {

        FileUploadPanel root = FileUploadPanel.this; 
        FileUploadItemCell cell;
        FileUploadItem item;
        
        public void actionPerformed(ActionEvent e) { 
            if (MsgBox.confirm("You are about to remove "+ cell.getFileName() +". Continue?")) {
                item.markForRemoval();
                root.pnlCanvas.remove( cell ); 
                root.pnlCanvas.revalidate(); 
                root.pnlCanvas.repaint(); 
                
                FileUploadManager.getInstance().schedule( item ); 
                if ( root.proxyHandler != null ) { 
                    root.proxyHandler.afterRemoveItem(); 
                } 
            }
        }
    }
    
    private class FileTransferHandler implements FileUploadManager.FileHandler {
        
        FileUploadPanel root = FileUploadPanel.this; 
        FileUploadItemCell cell;
        Map props;

        public void onTransfer(FileUploadItem item, long filesize, long bytesprocessed) {
            if ( item == null ) return; 

            boolean completed = ( bytesprocessed >= filesize ); 
            if ( props != null ) props.put("completed", completed);
            
            cell.update( filesize, bytesprocessed ); 
            cell.revalidate();
            cell.repaint(); 
            
            Runnable proc = new Runnable() {
                public void run() { 
                    cell.revalidate();
                    cell.repaint(); 
                }
            };
            SwingUtilities.invokeLater( proc ); 
        }

        public void onCompleted(FileUploadItem item) {
        }
    }
    
    private class MainLayoutManager implements LayoutManager {

        public void addLayoutComponent(String name, Component comp) {
        }
        public void removeLayoutComponent(Component comp) {
        }

        public Dimension preferredLayoutSize(Container parent) {
            synchronized (parent.getTreeLock()) {
                return getLayoutSize( parent );
            }
        }

        public Dimension minimumLayoutSize(Container parent) {
            synchronized (parent.getTreeLock()) {
                return getLayoutSize( parent );
            }
        }
        
        private Dimension getLayoutSize(Container parent) {
            Insets margin = parent.getInsets(); 
            Dimension dim = scrollPane.getPreferredSize(); 
            int w = dim.width + (margin.left + margin.right); 
            int h = dim.height + (margin.top + margin.bottom);
            return new Dimension( w, h );  
        }

        public void layoutContainer(Container parent) {
            synchronized (parent.getTreeLock()) {
                Insets margin = parent.getInsets(); 
                int pw = parent.getWidth(); 
                int ph = parent.getHeight(); 
                int x = margin.left; 
                int y = margin.top;
                int w = pw - (margin.left + margin.right); 
                int h = ph - (margin.top + margin.bottom); 
                scrollPane.setBounds(x, y, w, h);  
            } 
        } 
        
        private Component[] getVisibleComponents(Container parent) {
            ArrayList<Component> list = new ArrayList(); 
            Component[] comps = parent.getComponents(); 
            for (int i=0; i<comps.length; i++) {
                if ( comps[i].isVisible()) {
                    list.add( comps[i]); 
                }
            }
            try { 
                return list.toArray(new Component[]{}); 
            } finally {
                list.clear(); 
            }
        }
    }
    
    private class CanvasLayoutManager implements LayoutManager {

        private int cell_spacing = 4; 
        
        public void addLayoutComponent(String name, Component comp) {
        }
        public void removeLayoutComponent(Component comp) {
        }

        public Dimension preferredLayoutSize(Container parent) {
            synchronized (parent.getTreeLock()) {
                return getLayoutSize( parent );
            }
        }

        public Dimension minimumLayoutSize(Container parent) {
            synchronized (parent.getTreeLock()) {
                return getLayoutSize( parent );
            }
        }
        
        private Dimension getLayoutSize(Container parent) {
            int w=0, h=0; 
            Insets margin = parent.getInsets(); 
            Component[] comps = getVisibleComponents( parent ); 
            for (int i=0; i<comps.length; i++) {
                Dimension dim = comps[i].getPreferredSize(); 
                w = Math.max( w, dim.width ); 
                h += (dim.height + (i == 0 ? 0 : cell_spacing));  
            }
            w += (margin.left + margin.right); 
            h += (margin.top + margin.bottom);
            return new Dimension( w, h );  
        }

        public void layoutContainer(Container parent) {
            synchronized (parent.getTreeLock()) {
                Insets margin = parent.getInsets(); 
                int pw = parent.getWidth(); 
                int ph = parent.getHeight(); 
                int x = margin.left; 
                int y = margin.top;
                int w = pw - (margin.left + margin.right); 

                Component[] comps = getVisibleComponents( parent ); 
                for (int i=0; i<comps.length; i++) {
                    Dimension dim = comps[i].getPreferredSize(); 
                    comps[i].setBounds(x, y, w, dim.height); 
                    y += (dim.height + cell_spacing);
                }
            }
        }
        
        private Component[] getVisibleComponents(Container parent) {
            ArrayList<Component> list = new ArrayList(); 
            Component[] comps = parent.getComponents(); 
            for (int i=0; i<comps.length; i++) {
                if ( comps[i].isVisible()) {
                    list.add( comps[i]); 
                }
            }
            try { 
                return list.toArray(new Component[]{}); 
            } finally {
                list.clear(); 
            }
        }
    }
    
}
