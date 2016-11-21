/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bintypes;

import java.io.DataInputStream;
import java.io.IOException;
import static crossplatform.Convert.BytesToInt64;
import static crossplatform.Convert.Int64ToBytes;

/**
 *
 * @author master
 */
public class T_Ptr {  
    public T_Ptr(byte[] bytes) {
        value_long = BytesToInt64(bytes);
    }
    public T_Ptr(long value) {
        value_long =  value;
    }
    public long getValue() {
        return value_long;
    }
    public T_Ptr reverseBytes() {
        return new T_Ptr(Long.reverseBytes(value_long));
    }
    public static int getSize() {
        return Long.BYTES;
    }
    public static T_Ptr bytesToPtr(byte[] bytes) {
        return new T_Ptr(bytes);
    }
    public static byte[] ptrToBytes(T_Ptr pointer) {
        return Int64ToBytes(pointer.getValue());
    }
    public static T_Ptr readPtr(byte[] content, int offset) throws IOException {
        byte[] buffer = new byte[Long.BYTES];
        System.arraycopy(content, offset, buffer, 0, Long.BYTES);
        return bytesToPtr(buffer);
    }
    public static T_Ptr readPtr(DataInputStream dis) throws IOException {
        return new T_Ptr(dis.readLong());
    }
    
    // Private variables
    private final long value_long;
}
