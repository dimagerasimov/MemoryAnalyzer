/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bintypes;

import static crossplatform.Help.GetArchitecture;
import static crossplatform.Convert.BytesToInt32;
import static crossplatform.Convert.BytesToInt64;
import static crossplatform.Convert.Int32ToBytes;
import static crossplatform.Convert.Int64ToBytes;
import java.io.DataInputStream;
import java.io.IOException;

/**
 *
 * @author master
 */
public class Ptr {
    private final int value_int;
    private final long value_long;
    
    public Ptr(byte[] bytes) {
        String myArch = GetArchitecture();
        if(myArch.contains("64")) {
            value_int = -1; //Address can't be < 0
            value_long = BytesToInt64(bytes);
        }
        else {
            value_int = BytesToInt32(bytes);
            value_long = -1; //Address can't be < 0
        }
    }
    public Ptr(long value) {
        String myArch = GetArchitecture();
        if(myArch.contains("64")) {
            value_int = -1; //Address can't be < 0
            value_long =  value;
        }
        else {
            value_int = (int)value;
            value_long = -1; //Address can't be < 0
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
    public Ptr reverseBytes() {
        Ptr pointer;
        String myArch = GetArchitecture();
        if(myArch.contains("64")) {
            pointer = new Ptr(Long.reverseBytes(value_long));
        }
        else {
            pointer = new Ptr(Integer.reverseBytes(value_int));
        }
        return pointer;
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
    public static Ptr bytesToPtr(byte[] bytes) {
        return new Ptr(bytes);
    }
    public static byte[] ptrToBytes(Ptr pointer) {
        String myArch = GetArchitecture();
        if(myArch.contains("64")) {
            return Int64ToBytes(pointer.getValue());
        }
        else {
            return Int32ToBytes((int)pointer.getValue());
        }
    }
    public static Ptr readPtr(byte[] content, int offset,
            boolean reverse) throws IOException {
        Ptr pointer;
        byte[] buffer;
        String myArch = GetArchitecture();
        if(myArch.contains("64")) {
            buffer = new byte[Long.BYTES];
            System.arraycopy(content, offset, buffer, 0, Long.BYTES);
            pointer = bytesToPtr(buffer);
        }
        else {
            buffer = new byte[Integer.BYTES];
            System.arraycopy(content, offset, buffer, 0, Integer.BYTES);
            pointer = bytesToPtr(buffer);
        }        
        if(reverse) {
            pointer = pointer.reverseBytes();
        }
        return pointer;
    }
    public static Ptr readPtr(DataInputStream dis,
            boolean reverse) throws IOException {
        Ptr pointer;
        String myArch = GetArchitecture();
        if(myArch.contains("64")) {
            pointer = new Ptr(dis.readLong());
        }
        else {
            pointer = new Ptr(dis.readInt());
        }        
        if(reverse) {
            pointer = pointer.reverseBytes();
        }
        return pointer;
    }
}
