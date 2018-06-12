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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author wiz
 */
public class StackDecoder {

    private static final Logger LOG = Logger.getLogger(StackDecoder.class.getName());
    
    private byte[] inputData;
    private short inputWord = 0;
    private int inputCursor = -2;
    private int inputBitCursor = 16;
    private List<Byte> output = new ArrayList();
    
    private List<Integer> historyStack = new ArrayList<Integer>(Arrays.asList(new Integer[] {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15}));
    
    public byte[] decodeStackData(byte[] input){
        LOG.entering(LOG.getName(),"decodeStackData");
        LOG.fine("Data length = " + input.length + " bytes.");
        this.inputData = input;
        boolean decodingDone = false;
        short commandBitmap = 0;
        int commandPattern = 0;
        int command = 0;
        short value = 0;
        int copyOffset = 0;
        int copyLength = 0;
        inputCursor = -2;
        inputBitCursor = 16;
        byte[] bytes = null;
        try{
            while(!decodingDone){
                /* Step 1 - Parse command bitmap word */
                commandBitmap = 0;
                for(int i=0;i<4;i++){
                    commandPattern = getNextCommandPattern();
                    commandBitmap = (short) (commandBitmap << 4);
                    commandBitmap += commandPattern;
                }
                LOG.log(Level.FINE, "command bitmap = {0}", Integer.toHexString(commandBitmap&0xFFFF));
                
                /* Step 2 - Apply commands on following data */
                for(int i=0;i<16;i++){
                    command = (commandBitmap>>15-i) & 1;
                    if(command==0){
                        /* command 0 : word value built from four 4-bit values taken from history stack */
                        value = getWordValue();
                        LOG.log(Level.FINE, "0 - word value = {0}", Integer.toHexString(value&0xFFFF));
                        writeWord(value);
                    }else{
                        /* command 1 : section copy */
                        copyOffset = getCopyOffset();
                        if(copyOffset==0){
                            decodingDone = true;
                            break; 
                        }
                        copyLength = getCopyLength();
                        LOG.log(Level.FINE, "1 - section copy offset="+Integer.toHexString(copyOffset&0xFFFF)+", length="+ Integer.toHexString(copyLength&0xFFFF));
                        for(int j=0;j<copyLength;j++){
                            output.add(output.get(output.size()-2*copyOffset));
                            output.add(output.get(output.size()-2*copyOffset));
                        }
                    }

                }
                
            }
        }catch(Exception e){
            LOG.throwing(LOG.getName(),"decodeStackData",e);
        }finally{
            bytes = new byte[output.size()];
            for(int i=0;i<bytes.length;i++){
                bytes[i] = output.get(i);
            }
        }
        LOG.exiting(LOG.getName(),"decodeStackData");
        return bytes;
    }  
    
    private static short getNextWord(byte[] data, int cursor){
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.put(data[cursor+1]);
        bb.put(data[cursor]);
        short s = bb.getShort(0);
        return s;
    }    
    
    private int getNextBit(){
        int bit = 0;
        if(inputBitCursor>=16){
            inputBitCursor = 0;
            inputCursor+=2;
            inputWord = getNextWord(inputData, inputCursor);
        } 
        bit = (inputWord>>(15-inputBitCursor)) & 1;
        inputBitCursor++;
        return bit;
    }
    
    private int getNextCommandPattern(){
        int commandPattern = 0;
        /* input bitstring	==> 4 command bits : 0 = word value, 1 = section copy */
        if(getNextBit()==0){
            /* 0 		==> 0000 (4 word values, most frequent pattern, naturally) */
            commandPattern = 0;
        }else{
            if(getNextBit()==0){
                if(getNextBit()==0){
                    /* 100 		==> 0001 (3 word values, then 1 section copy) */
                    commandPattern = 1;
                }else{
                    /* 101 		==> 0010 (2 word values etc ...) */
                    commandPattern = 2;
                }
            }else{
                if(getNextBit()==0){
                    /* 110 		==> 0100 */
                    commandPattern = 4;
                }else{
                    if(getNextBit()==0){
                        /* 1110 		==> 1000 */
                        commandPattern = 8;
                    }else{
                        /* 1111 xxxx 	==> xxxx (custom command pattern) */
                        commandPattern = getCustomCommandPattern();
                    }
                }
            }
        }
        
        return commandPattern;
    }
    
    private int getCustomCommandPattern(){
        int customPattern = 8*getNextBit() + 4*getNextBit() + 2*getNextBit() + getNextBit();
        return customPattern;
    }
    
    private short getWordValue(){
        short wordValue = 0;
        for(int i=0;i<4;i++){
            wordValue = (short) (wordValue<<4);
            wordValue+=getNextValue();
        }
        return wordValue;
    }
    
    private int getNextValue(){
        int value = 0;
        int valueIndex = 0;
        /* Initial history stack order : 0 1 2 3 4 5 6 7 8 9 A B C D E F
            input bitstring	==> ouput 4-bit value */
        if(getNextBit()==0){
            if(getNextBit()==0){
                /* 00 		==> 0 : history index, most recent value */
                valueIndex = 0;
            }else{
                /* 01 		==> 1 : second most recent value */
                valueIndex = 1;
            }
        }else{
            if(getNextBit()==0){
                if(getNextBit()==0){
                    /* 100 		==> 2 : etc... */
                    valueIndex = 2;
                }else{
                    /* 101 		==> 3 */
                    valueIndex = 3;
                }
            }else{
                if(getNextBit()==0){
                    /* 110 		==> 4 */
                    valueIndex = 4;
                } else{
                    /* 111... */
                    if(getNextBit()==0){
                        if(getNextBit()==0){
                            /* 11100 		==> 5 */
                            valueIndex = 5;
                        }else{
                            /* 11101 		==> 6 */
                            valueIndex = 6;
                        }
                    } else{
                        if(getNextBit()==0){
                            /* 11110 		==> 7 */
                            valueIndex = 7;
                        }else{
                            /* 11111... */
                            if(getNextBit()==0){
                                if(getNextBit()==0){
                                    /* 1111100 	==> 8 */
                                    valueIndex = 8;
                                } else{
                                    /* 1111101 	==> 9 */
                                    valueIndex = 9;
                                }
                            } else{
                                if(getNextBit()==0){
                                    /* 1111110 	==> A */
                                    valueIndex = 10;
                                } else{
                                    if(getNextBit()==0){
                                        if(getNextBit()==0){
                                            /* 111111100 	==> B */
                                            valueIndex = 11;
                                        } else{
                                            /* 111111101 	==> C */
                                            valueIndex = 12;
                                        }
                                    } else{
                                        if(getNextBit()==0){
                                            /* 111111110 	==> D */
                                            valueIndex = 13;
                                        } else{
                                            if(getNextBit()==0){
                                                /* 1111111110 	==> E */
                                                valueIndex = 14;
                                            } else{
                                                /* 1111111111 	==> F */
                                                valueIndex = 15;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        value = historyStack.get(valueIndex);
        if(valueIndex != 0 ){
            historyStack.remove(valueIndex);
            historyStack.add(0, value);
        }
        return value;
    }
    
    private int getCopyOffset(){
        int offset = 0;
        for(int i=0;i<11;i++){
            offset = offset << 1;
            offset+=getNextBit();
        }
        return offset;
    }
    
    private int getCopyLength(){
        int length = 2;
        while(getNextBit()==0){
            if(getNextBit()==0){
                length+=2;
            } else{
                length+=1;
                break;
            }
        }
        return length;
    }
    
    private void writeWord(short word){
        byte firstByte = (byte)((word>>8)&0xFF);
        byte secondByte = (byte)(word&0xFF);
        output.add(firstByte);
        output.add(secondByte);
    }
    
    final protected static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(List<Byte> bytes) {
        char[] hexChars = new char[bytes.size() * 2];
        for ( int j = 0; j < bytes.size(); j++ ) {
            int v = bytes.get(j) & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }    
    
}
