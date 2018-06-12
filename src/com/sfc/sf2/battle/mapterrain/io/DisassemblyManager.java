/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sfc.sf2.battle.mapterrain.io;

import com.sfc.sf2.battle.mapterrain.BattleMapTerrain;
import com.sfc.sf2.battle.mapterrain.compression.StackDecoder;
import com.sfc.sf2.battle.mapterrain.compression.StackEncoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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

    private static short getWord(byte[] data, int cursor){
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.put(data[cursor+1]);
        bb.put(data[cursor]);
        short s = bb.getShort(0);
        return s;
    }
    
    private static byte getByte(byte[] data, int cursor){
        ByteBuffer bb = ByteBuffer.allocate(1);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.put(data[cursor]);
        byte b = bb.get(0);
        return b;
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
