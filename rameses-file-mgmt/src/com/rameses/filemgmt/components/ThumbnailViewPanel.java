/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rameses.filemgmt.components;

import com.rameses.filemgmt.FileManager;
import com.rameses.osiris2.client.Inv;
import com.rameses.rcp.common.Opener;
import com.rameses.rcp.common.ThumbnailViewModel;
import com.rameses.rcp.control.XPanel;
import com.rameses.rcp.framework.Binding;
import com.rameses.rcp.util.UIControlUtil;
import com.rameses.util.Base64Cipher;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.Beans;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author wflores
 */
public class ThumbnailViewPanel extends XPanel {
    
    private Dimension cellSize; 
    private int cellSpacing;
    
    private Object selectedItem;
    private String handler;
    
    private ListModelImpl listModel;
    private ThumbnailViewModel model;
    private JScrollPane jscroll;
    private JList jlist; 
    
    public ThumbnailViewPanel() {
        super();
        initComponents(); 
    }
    
    public String getHandler() { return handler; } 
    public void setHandler( String handler ) {
        this.handler = handler; 
    }
    
    public Dimension getCellSize() { return cellSize; }
    public void setCellSize( Dimension cellSize ) { 
        if ( cellSize == null ) { 
            cellSize = new Dimension(75, 75); 
        } 
        this.cellSize = cellSize; 
        updateFixedCellSize();
    }
    
    public int getCellSpacing() { return cellSpacing; } 
    public void setCellSpacing( int cellSpacing ) {
        this.cellSpacing = Math.max( cellSpacing, 0);
        updateFixedCellSize(); 
    }
    
    private void updateFixedCellSize() {
        if ( jlist != null ) {
            Dimension size = getCellSize(); 
            int spacing = getCellSpacing();
            jlist.setFixedCellWidth( size.width + spacing ); 
            jlist.setFixedCellHeight( size.height + spacing ); 
        }        
    }
    
    public Object getSelectedItem() { return selectedItem; }
    public void setSelectedItem( Object selectedItem ) {
        this.selectedItem = selectedItem; 
    }
    
    public ThumbnailViewModel getModel() { return model; } 
    public void setModel( ThumbnailViewModel newModel ) {
        if ( newModel == null ) {
            newModel = new ThumbnailViewModel(); 
        }
        newModel.setProvider( new ProviderImpl()); 
        this.model = newModel; 

        listModel = new ListModelImpl();
        listModel.init();
        jlist.setModel( listModel ); 
    }
    
    
    public void clearItems() {
        setSelectedItem( null ); 
        removeAll(); 
        revalidate();
        repaint();
    }
    
    public void load() {
        super.load();
        
        Object handlerObj = null; 
        String shandler = getHandler(); 
        if ( shandler != null && shandler.trim().length() > 0 ) {
            handlerObj = UIControlUtil.getBeanValue(getBinding(), shandler); 
        }
        if ( handlerObj instanceof ThumbnailViewModel ) {
            setModel((ThumbnailViewModel) handlerObj); 
        } else {
            setModel( null ); 
        }
    }
    
    public void refresh() {
        super.refresh();
        
        if ( listModel != null ) {
            listModel.init(); 
            listModel.fireDataChanged(); 
        }
    }

    private  void selectedItemChanged( final Object value ) {
        Runnable proc = new Runnable() {
            public void run() {
                String sname = getName(); 
                if ( sname == null || sname.trim().length() == 0 ) {
                    return; 
                }
                
                Binding binding = getBinding(); 
                if ( binding == null ) return; 
                
                UIControlUtil.setBeanValue(binding, sname, value ); 
                binding.getValueChangeSupport().notify(sname, value);
                binding.notifyDepends( sname ); 
            } 
        };
        EventQueue.invokeLater(proc); 
    }
    
    // <editor-fold defaultstate="collapsed" desc="initComponents">
    private void initComponents() { 
        setLayout( new MainLayoutManager()); 
        setPreferredSize(new Dimension(200, 100)); 
        listModel = new ListModelImpl();
        model = new ThumbnailViewModel(); 
        
        jlist = new JList();
        jlist.setVisibleRowCount(-1);
        jlist.setLayoutOrientation( JList.HORIZONTAL_WRAP ); 
        jlist.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); 
        jlist.setCellRenderer( new ListRendererImpl() ); 
        jlist.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                Object value = jlist.getSelectedValue();
                if ( value instanceof ItemInfo ) {
                    value = ((ItemInfo) value).userObject; 
                }
                setSelectedItem( value ); 
                selectedItemChanged( value ); 
            }
        }); 

        ListMouseHandler mouseHandler = new ListMouseHandler();
        jlist.addMouseListener( mouseHandler );
        jlist.addMouseMotionListener( mouseHandler );
        
        jscroll = new JScrollPane();         
        jscroll.setViewportView( jlist ); 
        
        setCellSize(new Dimension(100, 100) ); 
        setCellSpacing( 5 ); 
        add( jscroll ); 

        if ( Beans.isDesignTime()) { 
            setModel( new TestViewModel()); 
        }
    } 
    
    private class ListMouseHandler extends MouseAdapter { 
        
        ThumbnailViewPanel root = ThumbnailViewPanel.this; 

        public void mouseClicked(MouseEvent e) {
            if ( e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) ) {
                Point p = e.getPoint();
                int index = root.jlist.locationToIndex( p ); 
                if ( index < 0 ) return; 

                Rectangle rect = root.jlist.getCellBounds(index, index); 
                if ( !rect.contains( p )) return; 

                openItem( index ); 
            }
        }

        public void mouseMoved(MouseEvent e) {
            Point p = e.getPoint();
            int index = root.jlist.locationToIndex( p ); 
            if ( index < 0 ) return; 
            
            Rectangle rect = root.jlist.getCellBounds(index, index); 
            if ( !rect.contains( p )) return;
        }
        
        void openItem( int index ) { 
            Object data = root.listModel.items.get( index ).userObject; 
            Object outcome = root.getModel().openItem( data ); 
            if ( outcome instanceof Opener ) { 
                Opener op = (Opener) outcome; 
                String target = op.getTarget(); 
                if ( target == null || target.trim().length() == 0 ) {
                    op.setTarget("popup"); 
                } 
                
                try { 
                    Inv.invoke( op ); 
                } catch(Throwable t) {  
                    t.printStackTrace(); 
                } 
            } 
        }
    }
    // </editor-fold>
        
    // <editor-fold defaultstate="collapsed" desc="List Renderer and Models">
    private class ListModelImpl extends AbstractListModel {

        ThumbnailViewPanel root = ThumbnailViewPanel.this; 
        ArrayList<ItemInfo> items = new ArrayList();
        ThumbnailViewModel tvm;
        Base64Cipher base64; 
        List sourcelist; 
        
        void clear() {
            items.clear(); 
        }
        
        public int getSize() { 
            return items.size(); 
        }

        public Object getElementAt(int index) { 
            if ( index >= 0 && index < getSize()) {
                return items.get( index ); 
            } else {
                return null; 
            }
        }
        
        void init() {
            clear();             
            
            tvm = root.getModel(); 
            sourcelist = tvm.fetchList( new HashMap()); 
            if ( sourcelist == null ) return; 
            
            base64 = new Base64Cipher();
            for (int i=0; i<sourcelist.size(); i++) {
                Object item = sourcelist.get(i); 
                if ( item == null ) continue; 

                addItem( item ); 
            }
        }
        
        void fireDataChanged() { 
            fireContentsChanged( root.jlist, 0, getSize()); 
        }
        void fireItemAdded( int index ) { 
            if ( index >= 0 && index < getSize()) {
                fireIntervalAdded( root.jlist, index, index); 
            }
        } 
        
        int addItem( Object item ) {
            ItemInfo info = new ItemInfo();
            info.userObject = item; 
            info.title = tvm.getTitle(item); 
            info.filetype = tvm.getFileType( item ); 

            Object thumbnailObj = tvm.getThumbnail( item ); 
            if ( thumbnailObj instanceof String ) {
                if ( base64.isEncoded( thumbnailObj.toString() )) {
                    thumbnailObj = base64.decode(thumbnailObj.toString() ); 
                } else {
                    thumbnailObj = null; 
                }
            } else if ( thumbnailObj instanceof byte[] ) {
                //do nothing 
            } else {
                thumbnailObj = null; 
            }

            if ( thumbnailObj instanceof byte[] ) {
                info.image = new ImageIcon((byte[]) thumbnailObj); 
            } else {
                info.image = FileManager.getInstance().getFileTypeIcon( info.filetype ); 
            }

            items.add( info ); 
            return items.size()-1; 
        }
    }
    
    private class ListRendererImpl implements ListCellRenderer {
        
        private ThumbnailItem comp;
        
        ListRendererImpl() {
            comp = new ThumbnailItem();
        }
        
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) { 
            comp.info = (ItemInfo) value; 
            comp.selected = isSelected; 
            comp.hasfocus = cellHasFocus; 
            return comp;
        }
        
    }    
    
    private class ProviderImpl implements ThumbnailViewModel.Provider {
        
        ThumbnailViewPanel root = ThumbnailViewPanel.this; 

        public Binding getBinding() {
            return root.getBinding(); 
        }

        public void addItem(Object item) { 
            int index = root.listModel.addItem( item ); 
            if ( index >= 0 ) {
                root.listModel.fireItemAdded( index ); 
            }
        } 
    }
    
    private class TestViewModel extends ThumbnailViewModel {

        private List<Map> items; 
        
        TestViewModel() {
            items = new ArrayList();
            items.add(createData("item1", "Document 1"));
            items.add(createData("item2", "Document 2"));
            items.add(createData("item3", "Document 3"));
        }
        
        private Map createData( String objid, String caption ) {
            Map m = new HashMap(); 
            m.put("objid", objid); 
            m.put("caption", caption); 
            return m; 
        }
        
        public List fetchList(Map params) {
            return items; 
        }

        public String getFileType(Object item) { 
            return "docx";
        }        
    }
    // </editor-fold>    
    
    // <editor-fold defaultstate="collapsed" desc="ThumbnailItem">
    public class ItemInfo {
        public String title; 
        public String filetype;
        public Object thumbnail;
        public Object userObject; 
        
        private ImageIcon image; 
        
        boolean hasImage() {
            return (image != null); 
        }
    }
    
    private class ThumbnailItem extends JLabel {
        
        ThumbnailViewPanel root = ThumbnailViewPanel.this; 
        
        private Color focusInBorderColor; 
        private Color focusOutBorderColor; 
        private Color hoverBorderColor; 
        
        private boolean hasfocus;
        private boolean hasMouseFocus;
        private ItemInfo info;
        
        private boolean selected; 
        private int labelHeight;
        private JLabel label; 
        
        ThumbnailItem() {
            setHorizontalAlignment( SwingConstants.CENTER); 
            
            labelHeight = 20; 
            focusInBorderColor = Color.BLUE; 
            focusOutBorderColor = Color.decode("#afafaf"); 
            hoverBorderColor = Color.decode("#D3D8FF"); 
            
            label = new JLabel();
            label.setHorizontalAlignment( SwingConstants.CENTER);
            label.setVerticalAlignment( SwingConstants.TOP );
            Font font = getFont();
            if ( font != null ) { 
                label.setFont( font.deriveFont(10.0f)); 
            }
         }
        
        public ItemInfo getInfo() {
            return info; 
        }

        public void paint(Graphics g) {
            //super.paint(g);
            
            int spacing = getCellSpacing(); 
            int w = getWidth() - spacing; 
            int h = getHeight() - spacing; 
            
            Graphics2D g2 = null;
            ItemInfo info = getInfo(); 
            if ( info.hasImage() ) {
                Rectangle rect = scaleToFitRect(); 
                g2 = (Graphics2D) g.create(); 
                try { 
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.drawImage( info.image.getImage(), rect.x, rect.y, rect.width, rect.height, null ); 
                } finally { 
                    g2.dispose(); 
                } 
            }

            g2 = (Graphics2D) g.create(); 
            try { 
                if ( selected ) {
                    g2.setColor( focusInBorderColor ); 
                } else {
                    g2.setColor( focusOutBorderColor ); 
                }
                g2.drawRect(0, 0, w-1, h-1);

                if ( selected ) {
                    g2.setColor( hoverBorderColor ); 
                    g2.drawRect(1, 1, w-3, h-3); 
                }             
                
//                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//                g2.setColor( new Color(0, 0, 0)); 
//                g2.drawString("Hello", 2, getHeight());
            } finally { 
                g2.dispose();
            } 
            
        } 
        
        private Rectangle scaleToFitRect() {
            ImageIcon iicon = getInfo().image; 
            if ( iicon == null ) return null;

            Dimension cellsize = root.getCellSize();
            int iw = iicon.getIconWidth();
            int ih = iicon.getIconHeight(); 
            int cw = cellsize.width;
            int ch = cellsize.height;
            double scaleX = (double)cw  / (double)iw;
            double scaleY = (double)ch / (double)ih;
            double scale  = (scaleY > scaleX)? scaleX: scaleY;
            int nw = (int) (iw * scale);
            int nh = (int) (ih * scale);
            int nx = (cw/2)-(nw/2);
            int ny = (ch/2)-(nh/2); 
            return new Rectangle(nx, ny, nw, nh);         
        }        
    } 
    
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="MainLayoutManager">
    private class MainLayoutManager implements LayoutManager {

        ThumbnailViewPanel root = ThumbnailViewPanel.this; 
        
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
            Dimension dim = root.getCellSize();
            int w = dim.width; 
            int h = dim.height; 
            Insets margin = parent.getInsets(); 
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
                int h = ph - (margin.top + margin.bottom); 
                
                jscroll.setBounds(x, y, w, h);
            }
        }  
    }
    // </editor-fold> 

}
