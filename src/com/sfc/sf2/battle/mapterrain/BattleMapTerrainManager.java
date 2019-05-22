/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sfc.sf2.battle.mapterrain;

import com.sfc.sf2.battle.mapcoords.BattleMapCoords;
import com.sfc.sf2.battle.mapcoords.BattleMapCoordsManager;
import com.sfc.sf2.graphics.GraphicsManager;
import com.sfc.sf2.graphics.Tile;
import com.sfc.sf2.map.block.MapBlock;
import com.sfc.sf2.map.block.MapBlockManager;
import com.sfc.sf2.battle.mapterrain.io.DisassemblyManager;
import com.sfc.sf2.map.layout.MapLayoutManager;
import com.sfc.sf2.palette.PaletteManager;
import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author wiz
 */
public class BattleMapTerrainManager {

    private DisassemblyManager disassemblyManager = null;
    private BattleMapCoords coords = null;
    private BattleMapTerrain terrain;
    private MapLayoutManager mapLayoutManager = null;
    private BattleMapCoordsManager mapCoordsManager = null;
    private String[][] mapEntries = null;
    
    public void importDisassembly(String basePath, String mapEntriesPath, String battleMapCoordsPath, int battleIndex, String battleMapTerrainPath){
        System.out.println("com.sfc.sf2.battlemapterrain.BattleMapTerrainManager.importDisassembly() - Importing disassembly ...");
        disassemblyManager = new DisassemblyManager();
        mapEntries = importMapEntryFile(basePath, mapEntriesPath);
        mapCoordsManager = new BattleMapCoordsManager();
        mapCoordsManager.importDisassembly(battleMapCoordsPath);
        coords = mapCoordsManager.getCoords()[battleIndex];
        terrain = disassemblyManager.importDisassembly(battleMapTerrainPath);
        System.out.println("com.sfc.sf2.battlemapterrain.BattleMapTerrainManager.importDisassembly() - Disassembly imported.");
    }
    
    public void importDisassembly(String battleMapTerrainPath){
        System.out.println("com.sfc.sf2.battlemapterrain.BattleMapTerrainManager.importDisassembly() - Importing disassembly ...");
        disassemblyManager = new DisassemblyManager();
        terrain = disassemblyManager.importDisassembly(battleMapTerrainPath);
        System.out.println("com.sfc.sf2.battlemapterrain.BattleMapTerrainManager.importDisassembly() - Disassembly imported.");
    }    
    
    public void exportDisassembly(String battleMapTerrainPath){
        System.out.println("com.sfc.sf2.battlemapterrain.BattleMapTerrainManager.importDisassembly() - Exporting disassembly ...");
        disassemblyManager.exportDisassembly(terrain,battleMapTerrainPath);
        System.out.println("com.sfc.sf2.battlemapterrain.BattleMapTerrainManager.importDisassembly() - Disassembly exported.");        
    }   

    public BattleMapTerrain getTerrain() {
        return terrain;
    }

    public void setTerrain(BattleMapTerrain terrain) {
        this.terrain = terrain;
    }

    public MapLayoutManager getMapLayoutManager() {
        return mapLayoutManager;
    }

    public void setMapLayoutManager(MapLayoutManager mapLayoutManager) {
        this.mapLayoutManager = mapLayoutManager;
    }
    
    private String[][] importMapEntryFile(String basePath, String mapEntriesFilePath){
        String[][] entries = null;
        List<String> tilesetsPaths = new ArrayList();
        List<String> blocksPaths = new ArrayList();
        List<String> layoutPaths = new ArrayList();
        try{
            File entryFile = new File(mapEntriesFilePath);
            Scanner scan = new Scanner(entryFile);
            while(scan.hasNext()){
                String line = scan.nextLine();
                if(line.contains("pt_MapData:")){
                    System.out.println("pt_MapData found");
                    while(scan.hasNext()&&line.contains("dc.l")){
                        String mapPointer = line.substring(line.indexOf("dc.l")+5).trim();
                        System.out.println(mapPointer+" : ");
                        Scanner mapScan = new Scanner(entryFile);
                        while(mapScan.hasNext()){
                            String mapline = mapScan.nextLine();
                            if(mapline.startsWith(mapPointer)){
                                while(mapScan.hasNext()&&!mapline.contains("include")){
                                    mapline = mapScan.nextLine();
                                }
                                String tilesetsPath = mapline.substring(mapline.indexOf("\"")+1, mapline.lastIndexOf("\""));
                                System.out.println("  tilesetsPath : "+tilesetsPath);
                                tilesetsPaths.add(tilesetsPath);
                                mapline = mapScan.nextLine();
                                while(mapScan.hasNext()&&!mapline.contains("dc.l")){
                                    mapline = mapScan.nextLine();
                                }
                                String blocksPointer = mapline.substring(mapline.indexOf("dc.l")+5).trim();
                                Scanner blocksScan = new Scanner(entryFile);
                                while(blocksScan.hasNext()){
                                    String blocksLine = blocksScan.nextLine();
                                    if(blocksLine.startsWith(blocksPointer)){
                                        while(blocksScan.hasNext()&&!blocksLine.contains("incbin")){
                                            blocksLine = blocksScan.nextLine();
                                        }
                                        String blocksPath = blocksLine.substring(blocksLine.indexOf("\"")+1, blocksLine.lastIndexOf("\""));
                                        System.out.println("  blocksPath : "+blocksPath);                                        
                                        blocksPaths.add(blocksPath);
                                        break;
                                    }
                                }
                                mapline = mapScan.nextLine();
                                while(mapScan.hasNext()&&!mapline.contains("dc.l")){
                                    mapline = mapScan.nextLine();
                                }
                                String layoutPointer = mapline.substring(mapline.indexOf("dc.l")+5).trim();
                                Scanner layoutsScan = new Scanner(entryFile);
                                while(layoutsScan.hasNext()){
                                    String layoutLine = layoutsScan.nextLine();
                                    if(layoutLine.startsWith(layoutPointer)){
                                        while(layoutsScan.hasNext()&&!layoutLine.contains("incbin")){
                                            layoutLine = layoutsScan.nextLine();
                                        }
                                        String layoutPath = layoutLine.substring(layoutLine.indexOf("\"")+1, layoutLine.lastIndexOf("\""));
                                        System.out.println("  layoutPath : "+layoutPath); 
                                        layoutPaths.add(layoutPath);
                                        break;
                                    }
                                }
                                break;
                            }
                        }
                        line = scan.nextLine();
                    }
                    break;
                }
            }         
            entries = new String[tilesetsPaths.size()][];
            for(int i=0;i<entries.length;i++){
                entries[i] = new String[3];
                entries[i][0] = basePath + tilesetsPaths.get(i);
                entries[i][1] = basePath + blocksPaths.get(i);
                entries[i][2] = basePath + layoutPaths.get(i);
                System.out.println(entries[i][0]+" / "+entries[i][1]+" / "+entries[i][2]);
            }
        }catch(Exception e){
             System.err.println("com.sfc.sf2.mapsprite.io.PngManager.importPng() - Error while parsing map entries data : "+e);
        }         
        return entries;
    }

    public String[][] getMapEntries() {
        return mapEntries;
    }

    public void setMapEntries(String[][] mapEntries) {
        this.mapEntries = mapEntries;
    }

    public BattleMapCoords getCoords() {
        return coords;
    }

    public void setCoords(BattleMapCoords coords) {
        this.coords = coords;
    }
    
    
    
}
