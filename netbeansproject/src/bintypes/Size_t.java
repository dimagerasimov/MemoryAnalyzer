/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bintypes;

import static crossplatform.Convert.BytesToInt32;
import static crossplatform.Convert.BytesToInt64;
import static crossplatform.Convert.Int32ToBytes;
import static crossplatform.Convert.Int64ToBytes;
import static crossplatform.Help.GetArchitecture;
import java.io.DataInputStream;
import java.io.IOException;

/**
 *
 * @author master
 */
public class Size_t {
    private final int value_int;
    private final long value_long;
    
    public Size_t(byte[] bytes) {
        String myArch = GetArchitecture();
        if(myArch.contains("64")) {
            value_int = -1; //Size can't be < 0
            value_long = BytesToInt64(bytes);
        }
        else {
            value_int = BytesToInt32(bytes);
            value_long = -1; //Size can't be < 0
        }
    }
    public Size_t(long value) {
        String myArch = GetArchitecture();
        if(myArch.contains("64")) {
            value_int = -1; //Size can't be < 0
            value_long =  value;
        }
        else {
            value_int = (int)value;
            value_long = -1; //Size can't be < 0
        }
    }
    public long getValue() {
        String myArch = GetArchitecture();
        if(myArch.contains("64")) {
            return value_long;
        }
        else {
            return value_int;
        }
    }
    public Size_t reverseBytes() {
        Size_t size;
        String myArch = GetArchitecture();
        if(myArch.contains("64")) {
            size= new Size_t(Long.reverseBytes(value_long));
        }
        else {
            size = new Size_t(Integer.reverseBytes(value_int));
        }
        return size;
    }
    public static int getSize() {
        String myArch = GetArchitecture();
        if(myArch.contains("64")) {
            return Long.BYTES;
        }
        else {
            return Integer.BYTES;
        }
    }
    public static Size_t bytesToSize_t(byte[] bytes) {
        return new Size_t(bytes);
    }
    public static byte[] size_tToBytes(Size_t size) {
        String myArch = GetArchitecture();
        if(myArch.contains("64")) {
            return Int64ToBytes(size.getValue());
        }
        else {
            return Int32ToBytes((int)size.getValue());
        }
    }
    public static Size_t readSize_t(DataInputStream dis,
            boolean reverse) throws IOException {
        Size_t size;
        String myArch = GetArchitecture();
        if(myArch.contains("64")) {
            size = new Size_t(dis.readLong());
        }
        else {
            size = new Size_t(dis.readInt());
        } 
        if(reverse) {
            size = size.reverseBytes();
        }
        return size;
    }
}