/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sfc.sf2.battle.mapterrain.io;

import com.sfc.sf2.battle.mapterrain.BattleMapTerrain;
import com.sfc.sf2.battle.mapterrain.compression.StackDecoder;
import com.sfc.sf2.battle.mapterrain.compression.StackEncoder;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author wiz
 */
public class DisassemblyManager {
    
    public BattleMapTerrain importDisassembly(String mapTerrainPath){
        System.out.println("com.sfc.sf2.battlemapterrain.io.DisassemblyManager.importDisassembly() - Importing disassembly ...");
        BattleMapTerrain terrain = new BattleMapTerrain();
        try{
            Path path = Paths.get(mapTerrainPath);
            if(path.toFile().exists()){
                byte[] data = Files.readAllBytes(path);
                StackDecoder decoder = new StackDecoder();
                byte[] decodedData = decoder.decodeStackData(data);
                terrain.setData(decodedData);
                for(int i=0;i<48;i++){
                    StringBuilder sb = new StringBuilder();
                    for(int j=0;j<48;j++){
                        sb.append(decodedData[i*48+j]).append("\t");
                    }
                    System.out.println(sb);
                }
            }            
        }catch(Exception e){
             System.err.println("com.sfc.sf2.battle.mapterrain.io.DisassemblyManager.importDisassembly() - Error while parsing graphics data : "+e);
             e.printStackTrace();
        }    
        System.out.println("com.sfc.sf2.battle.mapterrain.io.DisassemblyManager.importDisassembly() - Disassembly imported.");
        return terrain;
    }
    
    public String[][] importMapEntryFile(String basePath, String mapEntriesFilePath){
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
             System.err.println("com.sfc.sf2.battlemapterrain.io.DisassemblyManager.importMapEntryFile() - Error while parsing map entries data : "+e);
        }         
        return entries;
    }
    
    public String[] importTerrainEntriesFile(String terrainEntriesFilePath) {
        String[] entries = null;
        try {
            List<Integer> entriesIndices = new ArrayList<>();
            String[] paths = null;
            File entryFile = new File(terrainEntriesFilePath);
            Scanner scan = new Scanner(entryFile);
            while (scan.hasNext()) {
                String line = scan.nextLine();
                if (line.startsWith("pt_BattleTerrainData")) {
                    line = scan.nextLine().trim();
                    while (line.startsWith("dc.l")) {
                        entriesIndices.add(Integer.valueOf(line.replaceAll("[^0-9]", "")));
                        line = scan.nextLine().trim();
                    }
                    paths = new String[entriesIndices.size()];
                    while(line.startsWith("BattleTerrain")) {
                        int index = Integer.parseInt(line.substring(0, line.indexOf(":")).replaceAll("[^0-9]", ""));
                        String path = line.substring(line.indexOf("incbin")+7).replaceAll("\"", "");
                        paths[index] = path;
                        line = scan.hasNext() ? scan.nextLine() : "";
                    }
                }
            }
            
            entries = new String[entriesIndices.size()];
            for (int i = 0; i < entries.length; i++) {
                entries[i] = paths[entriesIndices.get(i)];
            }
        }catch(Exception e){
             System.err.println("com.sfc.sf2.battlemapterrain.io.DisassemblyManager.importTerrainEntriesFile() - Error while parsing map entries data : "+e);
        }         
        return entries;
    }
    
    public void exportDisassembly(BattleMapTerrain terrain, String terrainFilePath){
        System.out.println("com.sfc.sf2.battlemapterrain.io.DisassemblyManager.exportDisassembly() - Exporting disassembly ...");
        try { 
            byte[] terrainBytes = produceTerrainBytes(terrain);
            Path terrainPath = Paths.get(terrainFilePath);
            Files.write(terrainPath,terrainBytes);
            System.out.println(terrainBytes.length + " bytes into " + terrainFilePath);
        } catch (Exception ex) {
            Logger.getLogger(DisassemblyManager.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
            System.out.println(ex);
        }            
        System.out.println("com.sfc.sf2.battlemapterrain.io.DisassemblyManager.exportDisassembly() - Disassembly exported.");        
    }     
   
    private static byte[] produceTerrainBytes(BattleMapTerrain terrain){
        StackEncoder encoder = new StackEncoder();
        encoder.produceData(terrain.getData());
        byte[] terrainBytes = encoder.getNewDataFileBytes();
        return terrainBytes;
    }    
 
    
    
    
}
