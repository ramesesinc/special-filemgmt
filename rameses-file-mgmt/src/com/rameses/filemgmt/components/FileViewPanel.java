/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rameses.filemgmt.components;

import com.rameses.common.MethodResolver;
import com.rameses.common.PropertyResolver;
import com.rameses.rcp.common.FileViewModel;
import com.rameses.rcp.control.XButton;
import com.rameses.rcp.control.XComponentPanel;
import com.rameses.rcp.control.XLabel;
import com.rameses.rcp.control.XList;
import com.rameses.rcp.control.XPanel;
import com.rameses.rcp.framework.Binding;
import com.rameses.rcp.ui.annotations.ComponentBean;
import com.rameses.rcp.util.UIControlUtil;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

/**
 *
 * @author wflores
 */
@ComponentBean("com.rameses.filemgmt.components.FileViewPanelModel")
public class FileViewPanel extends XComponentPanel {

    private String items; 
    private String handler; 
    private String allowAddWhen;
    private String allowRemoveWhen;
    private String editableWhen;
    
    private HeaderPanel headerPanel; 
    private SplitterPanel splitPanel;
    private FileViewModel modelHandler;
    private ThumbnailViewPanel viewPanel; 
    
    private Dimension cellSize;
    private int cellSpacing;
    
    public FileViewPanel() { 
        initComponents(); 
    } 
        
    // <editor-fold defaultstate="collapsed" desc="initComponents">
    private void initComponents() { 
        setLayout(new MainLayoutManager());  
        setPreferredSize(new Dimension(200, 100)); 
        setName("fileviewpanel");
        
        modelHandler = new FileViewModelImpl(null); 
        headerPanel = new HeaderPanel();
        splitPanel = new SplitterPanel();
        add( "header", headerPanel); 
        add( "content", splitPanel );

        XList xlist = new XList();
        xlist.setName("selectedItem"); 
        xlist.setHandler("listHandler"); 
        xlist.setExpression("#{item.title}"); 
        xlist.setFixedCellHeight( 20 ); 
        
        JScrollPane jsp = new JScrollPane( xlist ); 
        jsp.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED); 
        jsp.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED ); 
        splitPanel.setSideView( jsp ); 
                
        XPanel cardpanel = new XPanel();
        cardpanel.setLayout(new CardLayout()); 
        cardpanel.setDepends(new String[]{"selectedItem"}); 
        cardpanel.setName("cardName"); 

        XLabel loadComp = new XLabel();
        loadComp.setHorizontalAlignment(SwingConstants.CENTER);
        loadComp.setDepends(new String[] {"selectedItem"});
        loadComp.setIconResource("com/rameses/rcp/icons/loading32.gif");
        loadComp.setVisibleWhen("#{cardName == 'loading'}"); 
        cardpanel.add( loadComp, "loading" );
        
        XLabel blankComp = new XLabel(); 
        blankComp.setHorizontalAlignment( SwingConstants.CENTER ); 
        blankComp.setVerticalAlignment( SwingConstants.TOP ); 
        blankComp.setText("<html><br/><h3>No available item(s)</h3></html>");
        blankComp.setForeground( Color.decode("#a0a0a0")); 
        blankComp.setVisibleWhen("#{cardName == 'blank'}"); 
        blankComp.setDepends(new String[] {"selectedItem"});
        blankComp.setOpaque(true);
        cardpanel.add( blankComp, "blank" ); 

        Color borderColor = new Color(150, 150, 150); 
        loadComp.setBorder( BorderFactory.createLineBorder( borderColor, 1)); 
        blankComp.setBorder( BorderFactory.createLineBorder( borderColor, 1)); 
        
        viewPanel = new ThumbnailViewPanel();
        viewPanel.setName("selectedThumbnail"); 
        viewPanel.setHandler("thumbnailListHandler"); 
        viewPanel.setDepends(new String[]{"selectedItem"});
        viewPanel.setVisibleWhen("#{cardName == 'view'}"); 
        viewPanel.setCellSize(new Dimension(100, 80)); 
        cardpanel.add( viewPanel, "view" ); 

        XPanel headerpanel = new XPanel();
        headerpanel.setLayout(new BorderLayout());
        headerpanel.setDepends(new String[]{"selectedItem"}); 
        
        XPanel contentview = new XPanel();
        splitPanel.setContentView( contentview ); 
        
        contentview.setLayout(new BorderLayout());
        contentview.add( BorderLayout.NORTH, headerpanel ); 
        contentview.add( cardpanel ); 
        
        Border bout = BorderFactory.createLineBorder( borderColor, 1); 
        Border bin = BorderFactory.createEmptyBorder(3, 5, 3, 5); 
        
        XLabel headerinfo = new XLabel();
        headerinfo.setBorder( BorderFactory.createCompoundBorder(bout, bin));  
        headerinfo.setVerticalAlignment( SwingConstants.TOP ); 
        headerinfo.setMinimumSize(new Dimension(100, 30)); 
        headerinfo.setBackground( Color.WHITE );
        headerinfo.setOpaque(true);
        headerinfo.setText("Header Message");
        headerinfo.setExpression("#{headerMessage}");
        headerinfo.setDepends(new String[]{"selectedItem"}); 
        headerpanel.add( BorderLayout.NORTH, headerinfo ); 
        headerpanel.add( BorderLayout.SOUTH, Box.createVerticalStrut(5)); 
    }
    // </editor-fold> 
        
    public String getHandler() { return handler; } 
    public void setHandler( String handler ) { 
        this.handler = handler; 
    } 
    
    public String getItems() { return items; } 
    public void setItems( String items ) { 
        this.items = items; 
    }
    
    public int getDividerSize() { 
        return (splitPanel == null ? 0 : splitPanel.getDividerSize());
    } 
    public void setDividerSize( int dividerSize ) {
        if ( splitPanel != null ) {
            splitPanel.setDividerSize( dividerSize );
        }
    }
    
    public int getDividerLocation() {  
        return (splitPanel == null ? 0 : splitPanel.getDividerLocation());
    } 
    public void setDividerLocation( int dividerLocation ) {
        if ( splitPanel != null ) {
            splitPanel.setDividerLocation( dividerLocation );
        }
    } 
    
    public Dimension getCellSize() { return cellSize; }
    public void setCellSize( Dimension cellSize ) {
        this.cellSize = cellSize; 
        if ( viewPanel != null ) {
            viewPanel.setCellSize( this.cellSize ); 
        }
    }
    
    public int getCellSpacing() { return cellSpacing; } 
    public void setCellSpacing( int cellSpacing ) {
        this.cellSpacing = cellSpacing; 
        if ( viewPanel != null ) {
            viewPanel.setCellSpacing( this.cellSpacing ); 
        }
    }
    
    public String getAllowAddWhen() { return allowAddWhen; } 
    public void setAllowAddWhen( String allowAddWhen ) { 
        this.allowAddWhen = allowAddWhen; 
    }

    public String getAllowRemoveWhen() { return allowRemoveWhen; }
    public void setAllowRemoveWhen( String allowRemoveWhen ) { 
        this.allowRemoveWhen = allowRemoveWhen; 
    }
    
    public String getEditableWhen() { return editableWhen; }
    public void setEditableWhen( String editableWhen ) {
        this.editableWhen = editableWhen;
    }

    protected void initComponentBean(com.rameses.rcp.common.ComponentBean bean) {
        PropertyResolver pr = PropertyResolver.getInstance();

        FileViewModel newhandler = null; 
        String shandler = getHandler(); 
        if ( shandler != null && shandler.trim().length()>0 ) {
            Object caller = getBean();
            Object oval = pr.getProperty(caller, shandler); 
            if ( oval instanceof FileViewModel ) {
                newhandler = (FileViewModel) oval; 
            } 
        } 
        
        String items = getItems();
        if ( newhandler == null && items != null && items.trim().length() > 0) {
            Object caller = getBean();
            Object oval = pr.getProperty(caller, items); 
            if ( oval instanceof List ) {
                newhandler = new FileViewModelImpl((List) oval); 
            } 
        }
        
        if ( newhandler == null ) {
            newhandler = new FileViewModelImpl( null ); 
        } 
        newhandler.setProvider(new FileViewModelProvider()); 
        modelHandler = newhandler; 
        pr.setProperty(bean, "handlerProxy", modelHandler);        
    } 

    public void afterLoad() {
        super.afterLoad();
    } 
    
    public void refresh() { 
        boolean bool = getExprValue(getBean(), getEditableWhen(), true); 
        modelHandler.setEditable( bool ); 

        bool = getExprValue(getBean(), getAllowAddWhen(), true); 
        modelHandler.setAllowAdd( bool ); 
        
        bool = getExprValue(getBean(), getAllowRemoveWhen(), true); 
        modelHandler.setAllowRemove( bool ); 

        if (headerPanel != null) { 
            headerPanel.setEditable( modelHandler.isEditable() ); 
        } 
        
        super.refresh();        
    } 
    
    private boolean getExprValue( Object bean, String expr, boolean defaultValue ) {
        if (expr != null && expr.trim().length() > 0) {
            try { 
                return UIControlUtil.evaluateExprBoolean(getBean(), expr);
            } catch(Throwable t) {;} 
        } 
        return defaultValue; 
    }
    

    // <editor-fold defaultstate="collapsed" desc="FileViewModel">  
    private class FileViewModelImpl extends FileViewModel {
        
        private List items; 
        
        FileViewModelImpl( List items ) {
            this.items = items; 
        }

        public List fetchList(Map params) {
            return items; 
        }

        public boolean removeItem(Object item) {
            if ( item == null ) return false; 
            else if ( items == null || items.isEmpty() ) return false; 
            else return items.remove( item );
        }

        public void afterAddItem(Object item) {
            if ( items != null ) {
                items.add( item ); 
            }
        }
    }

    private class FileViewModelProvider implements FileViewModel.Provider {

        FileViewPanel root = FileViewPanel.this; 
        
        public Binding getBinding() {
            return root.getBinding();
        }

        public Binding getInnerBinding() {
            return root.getInnerBinding(); 
        }

        public void addItem(Object item) throws Exception {
            Object bean = root.getInnerBinding().getBean(); 
            MethodResolver.getInstance().invoke(bean, "addItem", new Object[]{ item }); 
            root.modelHandler.afterAddItem( item ); 
        } 
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="MainLayoutManager">
    private class MainLayoutManager implements LayoutManager {

        private Component header;
        private Component content;
        private Component footer; 
        
        public void addLayoutComponent(String name, Component comp) { 
            if ( name == null || comp == null ) {
                // do nothing 
            } else if ( "header".equals(name)) {
                header = comp;
            } else if ( "content".equals(name)) {
                content = comp;
            } else if ( "footer".equals(name)) {
                footer = comp;
            }            
        }
        public void removeLayoutComponent(Component comp) { 
            if ( comp == null ) {
                // do nothing 
            } else if ( header != null && header.equals(comp)) {
                header = null; 
            } else if ( content != null && content.equals(comp)) {
                content = null;
            } else if ( footer != null && footer.equals(comp)) {
                footer = null;
            }            
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
            int w = 0, h = 0;
            Component[] comps = new Component[]{ header, content, footer };
            for (int i=0; i<comps.length; i++) {
                if ( comps[i] != null && comps[i].isVisible()) {
                    Dimension dim = comps[i].getPreferredSize(); 
                    w = Math.max( w, dim.width ); 
                    h += dim.height; 
                }
            }
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
                
                int topY = margin.top; 
                if ( header != null && header.isVisible()) {
                    Dimension dim = header.getPreferredSize(); 
                    header.setBounds(x, topY, w, dim.height); 
                    topY += dim.height; 
                }
                
                int botY = ph - margin.bottom; 
                if ( footer != null && footer.isVisible()) {
                    Dimension dim = footer.getPreferredSize(); 
                    botY -= dim.height; 
                    footer.setBounds(x, botY, w, dim.height); 
                }
                
                if ( content != null && content.isVisible()) {
                    int ch = Math.max((botY - topY), 0);
                    content.setBounds(x, topY, w, ch); 
                }
            } 
        }         
    }
    // </editor-fold> 
    
    // <editor-fold defaultstate="collapsed" desc="HeaderPanel">
    private class HeaderPanel extends JPanel { 
        
        FileViewPanel root = FileViewPanel.this; 
        
        private XLabel label;
        private XButton btn1;
        private XButton btn2; 
                
        HeaderPanel() {
            setLayout( new HeaderPanelLayout());
            setBorder( BorderFactory.createEmptyBorder(0, 0, 0, 0)); 
            
            label = new XLabel();
            label.setExpression("<b>Attachments</b>");
            label.setFontStyle("font-weight:bold; font-size:12;"); 
            label.setForeground(Color.decode("#505050")); 
            label.setUseHtml(true);
            label.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 20));
            add("label", label); 
            
            btn1 = new XButton();
            btn1.setMargin(new Insets(2,2,2,2));
            btn1.setText("");
            btn1.setName("addFile"); 
            btn1.setToolTipText("Attach File(s)"); 
            btn1.setContentAreaFilled( false );
            btn1.setDisableWhen("#{allowAddPermitted != true}");  
            btn1.setVisibleWhen("#{allowAddPermitted == true}"); 
            btn1.setIconResource("com/rameses/filemgmt/images/attachment-16.png"); 
            add("btn1", btn1); 

            btn2 = new XButton();
            btn2.setMargin(new Insets(2,2,2,2));
            btn2.setText("");
            btn2.setName("removeFile"); 
            btn2.setToolTipText("Remove Selected Attachment"); 
            btn2.setContentAreaFilled( false );
            btn2.setDepends(new String[]{"selectedItem"}); 
            btn2.setDisableWhen("#{allowRemovePermitted != true}");  
            btn2.setVisibleWhen("#{allowRemovePermitted == true}"); 
            btn2.setIconResource("com/rameses/filemgmt/images/recyclebin-16.png"); 
            add("btn2", btn2); 
        } 
        
        void setEditable( boolean editable ) {
            btn1.setVisible( editable ); 
            btn2.setVisible( editable ); 
        }
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="HeaderPanelLayout">
    private class HeaderPanelLayout implements LayoutManager {

        private Component label; 
        private Component btn1; 
        private Component btn2; 
        
        public void addLayoutComponent(String name, Component comp) { 
            if ( name == null || comp == null ) {
                // do nothing 
            } else if ( "label".equals(name)) {
                label = comp;
            } else if ( "btn1".equals(name)) {
                btn1 = comp;
            } else if ( "btn2".equals(name)) {
                btn2 = comp;
            }
        }
        public void removeLayoutComponent(Component comp) { 
            if ( comp == null ) {
                // do nothing 
            } else if ( label != null && label.equals(comp)) {
                label = null;
            } else if ( btn1 != null && btn1.equals(comp)) {
                btn1 = null;
            } else if ( btn2 != null && btn2.equals(comp)) {
                btn2 = null;
            }            
        }

        public Dimension preferredLayoutSize(Container parent) {
            synchronized( parent.getTreeLock() ) {
                return getLayoutSize( parent ); 
            }
        }
        public Dimension minimumLayoutSize(Container parent) {
            synchronized( parent.getTreeLock() ) {
                return getLayoutSize( parent ); 
            }
        }
        private Dimension getLayoutSize(Container parent) { 
            int w = 0, h = 0, flag = 0; 
            Component[] comps = new Component[]{ label, btn1, btn2 };
            for (int i=0; i<comps.length; i++ ) {
                if ( comps[i] == null || !comps[i].isVisible() ) continue; 
                
                Dimension dim = comps[i].getPreferredSize(); 
                if ( flag > 0 ) w += 1;
                w += dim.width;
                h = Math.max(h, dim.height); 
                flag = 1; 
            }
            Insets margin = parent.getInsets(); 
            w += (margin.left + margin.right);
            h += (margin.top + margin.bottom);
            return new Dimension( w, h ); 
        }
        public void layoutContainer(Container parent) {
            synchronized( parent.getTreeLock() ) {
                Insets margin = parent.getInsets(); 
                int x = margin.left, y = margin.top;
                int w = parent.getWidth() - (margin.left + margin.right); 
                int h = parent.getHeight() - (margin.top + margin.bottom);
                int rPos = parent.getWidth() - margin.right; 
                
                Component[] comps = new Component[]{ label, btn1, btn2 };
                for (int i=0; i<comps.length; i++ ) {
                    if ( comps[i] == null || !comps[i].isVisible() ) continue; 

                    Dimension dim = comps[i].getPreferredSize(); 
                    comps[i].setBounds(x, y, dim.width, h); 
                    x += dim.width; 
                }
            }
        }
    }
    // </editor-fold>
}
