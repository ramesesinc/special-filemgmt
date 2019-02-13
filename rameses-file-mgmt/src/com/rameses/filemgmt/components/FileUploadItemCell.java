/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rameses.filemgmt.components;

import com.rameses.rcp.support.ImageIconSupport;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.Beans;
import java.text.DecimalFormat;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author wflores
 */
class FileUploadItemCell extends JPanel {
    
    private JLabel lblName;
    private JLabel lblSize;
    private StatusLabel lblStat;
    private JButton cmdRemove;

    private String fileId;
    private String fileName; 
    private long fileSize; 
    private long value; 
    
    private DecimalFormat decFormat;
    private ActionListener removeHandler;
    private Object userObject;
    private Info info;
    
    private Color nameColor1;
    private Color nameColor2;
    
    public FileUploadItemCell() {
        initComponents();
    }
    
    private void initComponents() {
        decFormat = new DecimalFormat("#,##0");
        lblName = new JLabel();
        lblSize = new JLabel();
        lblStat = new StatusLabel();
        cmdRemove = new JButton();
        info = new Info();
        
        nameColor1 = Color.decode("#303030");
        nameColor2 = Color.decode("#6787e3");
        nameColor2 = Color.BLUE;
        
        lblStat.setPreferredSize(new Dimension(100,14)); 
        lblSize.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5)); 
        lblSize.setForeground( Color.decode("#888888"));
        lblName.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0)); 
        lblName.setForeground( nameColor1 ); 
        cmdRemove.setIcon( getIcon("com/rameses/filemgmt/images/x.png"));
        cmdRemove.setContentAreaFilled( false ); 
        cmdRemove.setBorderPainted( false ); 
        cmdRemove.setMargin(new Insets(0,0,0,0)); 
        cmdRemove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { 
                FileUploadItemCell.this.removeActionImpl( e ); 
            } 
        }); 
        
        Font font = lblName.getFont();
        if ( font != null ) { 
            font = font.deriveFont( Font.BOLD ); 
            lblName.setFont( font ); 
        } 
        
        font = lblSize.getFont();
        if ( font != null ) { 
            font = font.deriveFont( Font.BOLD ); 
            lblSize.setFont( font ); 
        }
        
        if ( Beans.isDesignTime() ) {
            long num = 1024 * 100;
            setFileName("Sample Attachment File.png");
            setFileSize( num ); 
            setValue((int) (num/2)); 
        } 
        
        setBorder( BorderFactory.createLineBorder(Color.decode("#a0a0a0"), 1)); 
        setLayout( new LayoutManagerImpl()); 
        add( lblName );
        add( lblSize );
        add( lblStat );
        add( cmdRemove );
    }
    
    public String getFileId() { return fileId; }
    public void setFileId( String fileId ) {
        this.fileId = fileId; 
    }
    
    public String getFileName() { return fileName; }
    public void setFileName( String fileName ) {
        this.fileName = fileName; 
        updateLabelName();
    }
    
    public long getFileSize() { return fileSize; } 
    public void setFileSize( long fileSize ) {
        this.fileSize = fileSize;
        updateProgressValue();
        updateLabelSize();
    }
    
    public long getValue() { return value; } 
    public void setValue( long value ) {
        this.value = value; 
        updateProgressValue(); 
    }
    
    public ActionListener getRemoveHandler() { return removeHandler; } 
    public void setRemoveHandler( ActionListener removeHandler ) {
        this.removeHandler = removeHandler; 
    }
    
    public void update( long fileSize, long progressValue ) {
        this.fileSize = fileSize;
        this.value = progressValue; 
        updateProgressValue(); 
        updateLabelSize();
    }
    
    public boolean isCompleted() {
        long value1 = getValue();
        long value2 = getFileSize(); 
        return ( value1 >= value2 ); 
    }
    
    public Object getUserObject() { return userObject; }
    public void setUserObject( Object userObject ) {
        this.userObject = userObject; 
    }
    
    Info getInfo() { 
        return info; 
    } 
    
    protected void onRemove() {
        //do nothing 
    }
    
    private void removeActionImpl( ActionEvent e ) { 
        onRemove(); 
        
        ActionListener al = getRemoveHandler(); 
        if ( al != null ) { 
            al.actionPerformed( e ); 
        } 
    } 

    private void updateLabelName() {
        if ( lblName != null ) { 
            String text = getFileName(); 
            lblName.setText( text == null ? "" : text ); 
        }
    }
    private void updateLabelSize() {
        if ( lblSize != null ) { 
            long num = getFileSize(); 
            if ( num <= 0) {
                lblSize.setText(""); 
            } else if ( num < 1024 ) {
                lblSize.setText(("(" + num +"B)")); 
            } else {
                double value = ((Number) num).doubleValue() / 1024.0; 
                String text = "("+ decFormat.format( value ) + "K)"; 
                lblSize.setText( text ); 
            } 
        } 
    }    
    private void updateProgressValue() {
        if ( lblStat != null ) {
            lblStat.setMaxValue( getFileSize() ); 
            lblStat.setValue( getValue() ); 
            lblStat.repaint();
            lblStat.setVisible(( getValue() < getFileSize())) ;
        }
        
        if ( lblName != null ) {
            if ( isCompleted() ) {
                lblName.setForeground( nameColor2 );
            } else {
                lblName.setForeground( nameColor1 );
            } 
            lblName.repaint(); 
        }
    }
    private ImageIcon getIcon( String pathname ) {
        try {
            return ImageIconSupport.getInstance().getIcon( pathname ); 
        } catch(Throwable t) {
            return null ;
        }
    }

    public class Info {

        FileUploadItemCell root = FileUploadItemCell.this; 
        
        public boolean isCompleted() { 
            return root.isCompleted(); 
        } 
        public Object getData() { 
            return root.getUserObject(); 
        } 
    }
    
    private class LayoutManagerImpl implements LayoutManager {

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
            Dimension dim = cmdRemove.getPreferredSize();
            h = Math.max( dim.height, h ); 
            w += dim.width;
            
            if ( lblStat.isVisible()) {
                dim = lblStat.getPreferredSize(); 
                h = Math.max( dim.height, h ); 
                w += dim.width;
            }

            dim = lblSize.getPreferredSize(); 
            h = Math.max( dim.height, h ); 
            w += (dim.width + 50);
            w += (margin.left + margin.right); 
            h += (margin.top + margin.bottom);
            return new Dimension( w, h );  
        }

        public void layoutContainer(Container parent) {
            synchronized (parent.getTreeLock()) {
                Insets margin = parent.getInsets(); 
                int pw = parent.getWidth(); 
                int ph = parent.getHeight(); 
                int cw = pw - margin.right;
                int ch = ph - (margin.top + margin.bottom);  
                int cy = margin.top;
                int cx = cw; 
                
                Dimension dim = cmdRemove.getPreferredSize();
                cx -= dim.width; 
                cmdRemove.setBounds(cx, cy, dim.width, dim.height); 
                cw -= dim.width; 
                
                if ( lblStat.isVisible()) { 
                    dim = lblStat.getPreferredSize();
                    int yy = Math.max((ch/2)-(dim.height/2), margin.top);
                    cx -= dim.width; 
                    lblStat.setBounds(cx, yy+1, dim.width, dim.height); 
                    cw -= dim.width; 
                }
                
                int ww = getWidth(new Component[]{ lblName, lblSize }); 
                if ( ww > cw ) {
                    dim = lblSize.getPreferredSize(); 
                    cx -= dim.width; 
                    lblSize.setBounds(cx, cy, dim.width, ch); 
                    cw -= dim.width;
                    
                    lblName.setBounds(margin.left, cy, cw, ch);
                    
                } else {
                    cx = margin.left; 
                    dim = lblName.getPreferredSize(); 
                    lblName.setBounds(cx, cy, dim.width, ch); 
                    cx += dim.width; 
                    
                    dim = lblSize.getPreferredSize(); 
                    lblSize.setBounds(cx, cy, dim.width, ch);  
                } 
            }
        }
        
        private int getWidth( Component[] comps ) {
            int w = 0; 
            for (int i=0; i<comps.length; i++) {
                Dimension dim = comps[i].getPreferredSize(); 
                w += dim.width; 
            }
            return w; 
        }
    }
    
    private class StatusLabel extends JLabel { 
        
        private Color borderColor;
        private Color rateColor;
        private long maxValue;
        private long value;
        private double rate; 

        StatusLabel() {
            super(); 
            initComponents();
        }

        private void initComponents() {
            borderColor = Color.decode("#999999"); 
            rateColor = Color.decode("#6787e3");
            setMaxValue( 100 ); 
            setValue( 50 ); 
        } 

        public long getMaxValue() { return maxValue; } 
        public void setMaxValue( long maxValue ) {
            this.maxValue = maxValue; 
            computeRate();
        }

        public long getValue() { return value; } 
        public void setValue( long value ) {
            this.value = value; 
            computeRate();
        }

        private void computeRate() {
            long mv = getMaxValue(); 
            if ( mv <= 0 ) {
                rate = 0.0; 
                return; 
            }

            long v = getValue();
            if ( v >= mv ) {
                rate = 1.0; 
            } else {
                rate = ((Number) v).doubleValue() / ((Number) mv).doubleValue(); 
            }
        }

        public void paint(Graphics g) {
            int pw = Math.max(getWidth(), 0);
            int ph = Math.max(getHeight(), 0);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor( Color.WHITE ); 
            g2.fillRect(0, 0, pw, ph);
            g2.setColor( borderColor );
            g2.drawRect(0, 0, Math.max(pw-1, 0), Math.max(ph-1, 0));

            int cw = Math.max(pw - 4, 0);
            if ( rate > 0.0 ) { 
                Number res = ((Number) cw).doubleValue() * rate; 
                cw = res.intValue(); 
                if ( cw > 0 ) {
                    g2.setColor( rateColor );
                    g2.fillRect(2, 2, cw-1, ph-4);
                }
            }
            g2.dispose();
        } 
    }    
}
