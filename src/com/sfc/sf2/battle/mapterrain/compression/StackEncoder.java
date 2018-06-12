/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sfc.sf2.battle.mapterrain.compression;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author wiz
 */
public class StackEncoder {
    
    private static final List<String> historyBitStrings = new ArrayList<String>(Arrays.asList(new String[] {"00","01","100","101","110","11100","11101","11110","1111100",
                                                                                                "1111101","1111110","111111100","111111101","111111110","1111111110","1111111111"}));    
    private static final int MAX_COPY_OFFSET = 2047;
    
    private static byte[] newDataFileBytes;  
    
    private static final Logger LOG = Logger.getLogger(StackEncoder.class.getName());  
    
    public static void produceData(byte[] inputData){
        LOG.entering(LOG.getName(),"produceData");
        List<Integer> historyStack = new ArrayList<Integer>(Arrays.asList(new Integer[] {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15}));
        StringBuilder outputSb = new StringBuilder();
        StringBuilder commandSb = new StringBuilder(16);
        StringBuilder dataSb = new StringBuilder();
        LOG.fine("input = " + bytesToHex(inputData));
        int inputCursor = 0;
        byte[] output;
        int potentialCopyLength;
        int candidateSourceCursor;
        int copyCursor;
        StringBuilder offsetSb = new StringBuilder(11);
        StringBuilder lengthSb = new StringBuilder();
        while(inputCursor<inputData.length){
       
            short inputWord = getInputWord(inputData, inputCursor);
            
            //LOG.fine("inputWord = " + Integer.toHexString(inputWord & 0xFFFF));
        
            /* Get number of potential word sequence to copy */
            potentialCopyLength = 0;
            candidateSourceCursor = 0;
            copyCursor = inputCursor-2;
            while(copyCursor>=0&&(((inputCursor - copyCursor)/2)<MAX_COPY_OFFSET)){
                int testLength = 0;
                short destWord = inputWord;        
                short sourceWord = getInputWord(inputData, copyCursor);
                while(sourceWord==destWord){
                    testLength++;
                    if((inputCursor+testLength*2)<inputData.length){          
                        sourceWord = getInputWord(inputData, copyCursor+testLength*2);                                 
                        destWord = getInputWord(inputData, inputCursor+testLength*2); 
                    }else{
                        break;
                    }
                } 
                if(testLength>potentialCopyLength){
                    candidateSourceCursor = copyCursor;
                    potentialCopyLength = testLength;
                }
                copyCursor-=2;      
            }
            //LOG.fine("Potential copy length from " + candidateSourceCursor + " = " + potentialCopyLength); 
            
            if(potentialCopyLength>1){
                // Apply word sequence copy
                int startOffset = (inputCursor - candidateSourceCursor) / 2;
                int copyLength = potentialCopyLength;
                commandSb.append("1");
                offsetSb.setLength(0);
                offsetSb.append(Integer.toBinaryString(startOffset));
                while(offsetSb.length()<11){
                    offsetSb.insert(0, "0");
                }
                dataSb.append(offsetSb);
                lengthSb.setLength(0);
                copyLength-=2;
                while(copyLength>=0){
                    switch(copyLength){
                        case 0:
                            lengthSb.append("1");
                            copyLength=-1;
                            break;
                        case 1:
                            lengthSb.append("01");
                            copyLength=-1;
                            break;
                        default:
                            lengthSb.append("00");
                            copyLength-=2;
                            break;  
                    }
                }
                dataSb.append(lengthSb);
                inputCursor+=potentialCopyLength*2;
                LOG.fine("input word "+Integer.toHexString(inputWord & 0xFFFF)+" copy : offset=" + startOffset + "/" + offsetSb.toString() + ", length="+potentialCopyLength + "/" + lengthSb);
            }else{
                // No copy : word value
                commandSb.append("0");
                String valueBitString = getValueBitString(historyStack, inputWord);
                dataSb.append(valueBitString);
                inputCursor+=2;
                LOG.fine("input word "+Integer.toHexString(inputWord & 0xFFFF)+" value : " + valueBitString+", history="+historyStack.toString());
            }
          
            if(commandSb.length()==16){
                String commandBitString = getCommandBitString(commandSb);
                LOG.fine("commandSb=" + commandSb.toString()+", commandBitString="+commandBitString);
                outputSb.append(commandBitString);
                outputSb.append(dataSb);
                commandSb.setLength(0);
                dataSb.setLength(0);
                LOG.fine("output = " + outputSb.toString());
            }            

            
        }
        /* Add ending command with offset 0 */
        commandSb.append("1");
        dataSb.append("000000000001");
        while(commandSb.length()!=16){
            commandSb.append("1");
        }
        String commandBitString = getCommandBitString(commandSb);
        outputSb.append(commandBitString);
        outputSb.append(dataSb);
        LOG.fine("output = " + outputSb.toString());
        
        /* Word-wise padding */
        while(outputSb.length()%16 != 0){
            outputSb.append("1");
        }
        
        /* Byte array conversion */
        output = new byte[outputSb.length()/8];
        for(int i=0;i<output.length;i++){
            Byte b = (byte)(Integer.valueOf(outputSb.substring(i*8, i*8+8),2)&0xFF);
            output[i] = b;
        }
        LOG.fine("output bytes length = " + output.length);
        LOG.fine("output = " + bytesToHex(output));
        LOG.exiting(LOG.getName(),"produceData");
        newDataFileBytes = output;
    }
    
    public static byte[] getNewDataFileBytes(){
        return newDataFileBytes;
    }
    
    private static String getValueBitString(List<Integer> historyStack, short value){
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<4;i++){
            int quartet = (value>>(16-4*(i+1)))&0xF;
            for(int j=0;j<16;j++){
                if(quartet == historyStack.get(j)){
                    sb.append(historyBitStrings.get(j));
                    historyStack.remove(j);
                    historyStack.add(0, quartet);
                    break;
                }
            }        
        }
        return sb.toString();
    }
    
    private static String getCommandBitString(StringBuilder commandSb){
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<4;i++){
            String quartet = commandSb.substring(i*4, i*4+4);
            switch(quartet){
                /* input bitstring	==> 4 command bits : 0 = word value, 1 = section copy */
                case "0000":
                    /* 0 		==> 0000 (4 word values, most frequent pattern, naturally) */
                    sb.append("0");
                    break;
                case "0001":
                    /* 100 		==> 0001 (3 word values, then 1 section copy) */
                    sb.append("100");
                    break;
                case "0010":
                    /* 101 		==> 0010 (2 word values etc ...) */
                    sb.append("101");
                    break;
                case "0100":
                    /* 110 		==> 0100 */
                    sb.append("110");
                    break;
                case "1000":
                    /* 1110 		==> 1000 */
                    sb.append("1110");
                    break;
                default:
                    /* 1111 xxxx 	==> xxxx (custom command pattern) */
                    sb.append("1111").append(quartet);
                    break;
            }
                    
        }
        return sb.toString();
    }
    
    private static short getInputWord(byte[] inputData, int cursor){
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.put(inputData[cursor+1]);
        bb.put(inputData[cursor]);
        short s = bb.getShort(0);
        return s;
    }
    
    final protected static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }    
    public static String byteListToHex(List<Byte> bytes) {
        char[] hexChars = new char[bytes.size() * 2];
        for ( int j = 0; j < bytes.size(); j++ ) {
            int v = bytes.get(j) & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }   
    public static String shortListToHex(List<Short> shorts) {
        char[] hexChars = new char[shorts.size() * 4];
        for ( int j = 0; j < shorts.size(); j++ ) {
            short v = (short)(shorts.get(j) & 0xFFFF);
            hexChars[j * 4] = HEX_ARRAY[(v & 0xF000) >>> 12];
            hexChars[(j * 4) + 1] = HEX_ARRAY[(v & 0x0F00) >>> 8];
            hexChars[(j * 4) + 2] = HEX_ARRAY[(v & 0x00F0) >>> 4];
            hexChars[(j * 4) + 3] = HEX_ARRAY[(v & 0x000F)];            
        }
        return new String(hexChars);
    }      

}
