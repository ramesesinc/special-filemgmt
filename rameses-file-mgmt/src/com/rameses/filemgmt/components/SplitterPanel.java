/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rameses.filemgmt.components;

import com.rameses.rcp.control.layout.SplitterLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JPanel;

/**
 *
 * @author wflores 
 */
public class SplitterPanel extends JPanel implements SplitterLayout.Canvas { 
    
    private Rectangle viewRect; 
    private Rectangle dividerRect;
    private Point targetPoint;
    
    private SplitterLayout layout; 
    
    public SplitterPanel() {
        initComponents();
    }
    
    public int getDividerSize() { 
        return (layout == null ? 0 : layout.getDividerSize());
    } 
    public void setDividerSize( int dividerSize ) {
        if ( layout != null ) {
            layout.setDividerSize( dividerSize );
            revalidate();
            repaint();
        }
    }
    
    public int getDividerLocation() {  
        return (layout == null ? 0 : layout.getDividerLocation());
    } 
    public void setDividerLocation( int dividerLocation ) {
        if ( layout != null ) {
            layout.setDividerLocation( dividerLocation );
            revalidate();
            repaint();
        }
    } 
    
    public void setSideView( Component comp ) {
        if ( comp == null ) return; 
        
        add( SplitterLayout.VW_SIDE, comp ); 
    }
    
    public void setContentView( Component comp ) {
        if ( comp == null ) return; 
        
        add( SplitterLayout.VW_CONTENT, comp ); 
    }
    
    public void paint(Graphics g) {
        super.paint(g); 
        if (dividerRect != null && targetPoint != null) { 
            Rectangle newRect = new Rectangle();
            newRect.x = dividerRect.x;
            newRect.y = dividerRect.y;
            newRect.width = dividerRect.width;
            newRect.height = dividerRect.height;
            
            newRect.x = dividerRect.x + targetPoint.x;
            
            Color oldColor = g.getColor();
            Color newColor = getBackground();
            if (newColor == null) {
                newColor = Color.DARK_GRAY;
            } else {
                newColor = newColor.darker();
            }
            
            Graphics gg = g.create();             
            gg.setColor(newColor); 
            gg.fillRect(newRect.x, newRect.y, newRect.width, newRect.height); 
            gg.setColor(oldColor); 
            dividerRect = null; 
            targetPoint = null; 
        }
    }

    public void paintDividerHandle(Rectangle viewRect, Rectangle dividerRect, Point targetPoint) {
        this.viewRect = viewRect;
        this.dividerRect = dividerRect;
        this.targetPoint = targetPoint; 
        repaint();
    }     
    
    // <editor-fold defaultstate="collapsed" desc="initComponents">
    private void initComponents() { 
        layout = new SplitterLayout(); 
        setLayout( layout ); 
    }
    // </editor-fold> 

}
