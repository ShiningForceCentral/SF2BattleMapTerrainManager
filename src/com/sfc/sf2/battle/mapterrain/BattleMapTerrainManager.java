/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sfc.sf2.battle.mapterrain;

import com.sfc.sf2.battle.mapcoords.BattleMapCoords;
import com.sfc.sf2.battle.mapcoords.BattleMapCoordsManager;
import com.sfc.sf2.battle.mapterrain.io.DisassemblyManager;
import com.sfc.sf2.map.layout.MapLayoutManager;

/**
 *
 * @author wiz
 */
public class BattleMapTerrainManager {

    private final DisassemblyManager disassemblyManager = new DisassemblyManager();
    private BattleMapCoords coords = null;
    private BattleMapTerrain terrain;
    private MapLayoutManager mapLayoutManager = null;
    private BattleMapCoordsManager mapCoordsManager = null;
    private String[][] mapEntries = null;
    
    public void importDisassembly(String basePath, String mapEntriesPath, String terrainEntriesPath, String battleMapCoordsPath, int battleIndex){
        System.out.println("com.sfc.sf2.battlemapterrain.BattleMapTerrainManager.importDisassembly() - Importing disassembly ...");
        mapEntries = disassemblyManager.importMapEntryFile(basePath, mapEntriesPath);
        mapCoordsManager = new BattleMapCoordsManager();
        mapCoordsManager.importDisassembly(battleMapCoordsPath);
        coords = mapCoordsManager.getCoords()[battleIndex];
        String[] terrainEntries = disassemblyManager.importTerrainEntriesFile(terrainEntriesPath);
        terrain = null;
        if (battleIndex < terrainEntries.length) {
            String path = basePath + terrainEntries[battleIndex];
            terrain = disassemblyManager.importDisassembly(path);
        }
        System.out.println("com.sfc.sf2.battlemapterrain.BattleMapTerrainManager.importDisassembly() - Disassembly imported.");
    }
    
    public void importDisassembly(String terrainEntriesPath){
        System.out.println("com.sfc.sf2.battlemapterrain.BattleMapTerrainManager.importDisassembly() - Importing disassembly ...");
        terrain = disassemblyManager.importDisassembly(terrainEntriesPath);
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
