/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sfc.sf2.battle.mapterrain.layout;

import com.sfc.sf2.battle.mapcoords.layout.BattleMapCoordsLayout;
import com.sfc.sf2.battle.mapterrain.BattleMapTerrain;
import com.sfc.sf2.map.layout.MapLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

/**
 *
 * @author wiz
 */
public class BattleMapTerrainLayout extends BattleMapCoordsLayout implements MouseListener, MouseMotionListener {
    
    protected BattleMapTerrain terrain;    
    protected boolean drawTerrain = true;
    
    private BufferedImage terrainImage;
    
    public BattleMapTerrainLayout() {
        super();
        addMouseListener(this);
        addMouseMotionListener(this);
    }
    
    @Override
    public BufferedImage buildImage(MapLayout layout, int tilesPerRow) {
        BufferedImage image = super.buildImage(layout, tilesPerRow);
        Graphics graphics = image.getGraphics();
        if (drawTerrain) {
            graphics.drawImage(drawTerrain(), 0, 0, null);
        }
        graphics.dispose();
        return image;
    }
    
    private BufferedImage drawTerrain(){
        if (terrainImage == null) {
            terrainImage = new BufferedImage(3*8*64, 3*8*64, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = (Graphics2D) terrainImage.getGraphics();
            byte[] data = terrain.getData();
            int width = coords.getWidth();
            int height = coords.getHeight();
            int x = coords.getX();
            int y = coords.getY();
            for (int i=0; i<height; i++){
                for (int j=0; j<width; j++){
                    int value = data[i*48+j];
                    g2.drawString(String.valueOf(value), (x+j)*3*8+8, (y+i)*3*8+16);
                }
            }
        }
        return terrainImage;
    }

    public void setDrawTerrain(boolean drawTerrain) {
        this.drawTerrain = drawTerrain;
        this.redraw = true;
    }

    public BattleMapTerrain getTerrain() {
        return terrain;
    }

    public void setTerrain(BattleMapTerrain terrain) {
        this.terrain = terrain;
    }
    
    public void updateTerrainDisplay(){
        terrainImage = null;
        this.redraw = true;
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
    }
    @Override
    public void mouseEntered(MouseEvent e) {

    }
    @Override
    public void mouseExited(MouseEvent e) {

    }
    @Override
    public void mousePressed(MouseEvent e) {
        int x = e.getX() / (displaySize * 3*8);
        int y = e.getY() / (displaySize * 3*8);
        int startX = coords.getX();
        int startY = coords.getY();
        int width = coords.getWidth();
        int height = coords.getHeight();
        if(x>=startX && x<=startX+width
                && y>=startY && y<=startY+height){
            switch (e.getButton()) {
                case MouseEvent.BUTTON1:
                    terrain.getData()[(startY+y)*48+startX+x]++;
                    break;
                case MouseEvent.BUTTON3:
                    terrain.getData()[(startY+y)*48+startX+x]--;
                    break;
                default:
                    break;
            } 
            terrainImage = null;
            redraw = true;
            this.revalidate();
            this.repaint();
        }
        //System.out.println("Map press "+e.getButton()+" "+x+" - "+y);
    }
    @Override
    public void mouseReleased(MouseEvent e) {        
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        
    }
}
