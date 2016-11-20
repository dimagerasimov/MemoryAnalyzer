/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bintypes;

import java.io.DataInputStream;
import java.io.IOException;
import static crossplatform.Convert.BytesToInt32;
import static crossplatform.Convert.BytesToInt64;
import static crossplatform.Convert.Int32ToBytes;
import static crossplatform.Convert.Int64ToBytes;
import static crossplatform.Help.GetArchitecture;

/**
 *
 * @author master
 */
public class T_Size_t {    
    public T_Size_t(byte[] bytes) {
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
    public T_Size_t(long value) {
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
    public T_Size_t reverseBytes() {
        T_Size_t size;
        String myArch = GetArchitecture();
        if(myArch.contains("64")) {
            size= new T_Size_t(Long.reverseBytes(value_long));
        }
        else {
            size = new T_Size_t(Integer.reverseBytes(value_int));
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
    public static T_Size_t bytesToSize_t(byte[] bytes) {
        return new T_Size_t(bytes);
    }
    public static byte[] size_tToBytes(T_Size_t size) {
        String myArch = GetArchitecture();
        if(myArch.contains("64")) {
            return Int64ToBytes(size.getValue());
        }
        else {
            return Int32ToBytes((int)size.getValue());
        }
    }
    public static T_Size_t readSize_t(byte[] content, int offset,
            boolean reverse) throws IOException {
        T_Size_t size;
        byte[] buffer;
        String myArch = GetArchitecture();
        if(myArch.contains("64")) {
            buffer = new byte[Long.BYTES];
            System.arraycopy(content, offset, buffer, 0, Long.BYTES);
            size = bytesToSize_t(buffer);
        }
        else {
            buffer = new byte[Integer.BYTES];
            System.arraycopy(content, offset, buffer, 0, Integer.BYTES);
            size = bytesToSize_t(buffer);
        } 
        if(reverse) {
            size = size.reverseBytes();
        }
        return size;
    }
    public static T_Size_t readSize_t(DataInputStream dis,
            boolean reverse) throws IOException {
        T_Size_t size;
        String myArch = GetArchitecture();
        if(myArch.contains("64")) {
            size = new T_Size_t(dis.readLong());
        }
        else {
            size = new T_Size_t(dis.readInt());
        } 
        if(reverse) {
            size = size.reverseBytes();
        }
        return size;
    }
    
    // Private variables
    private final int value_int;
    private final long value_long;
}
