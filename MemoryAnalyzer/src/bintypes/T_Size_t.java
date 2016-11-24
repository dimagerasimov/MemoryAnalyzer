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
public class T_Size_t {    
    public T_Size_t(byte[] bytes) {
        value_long = BytesToInt64(bytes);
    }
    public T_Size_t(long value) {
        value_long =  value;
    }
    public long getValue() {
        return value_long;
    }
    public T_Size_t reverseBytes() {
        return new T_Size_t(Long.reverseBytes(value_long));
    }
    public static int getSize() {
        return Long.BYTES;
    }
    public static T_Size_t bytesToSize_t(byte[] bytes) {
        return new T_Size_t(bytes);
    }
    public static byte[] size_tToBytes(T_Size_t size) {
        return Int64ToBytes(size.getValue());
    }
    public static T_Size_t readSize_t(byte[] content, int offset) throws IOException {
        byte[] buffer = new byte[Long.BYTES];
        System.arraycopy(content, offset, buffer, 0, Long.BYTES);
        return bytesToSize_t(buffer);
    }
    public static T_Size_t readSize_t(DataInputStream dis) throws IOException {
        return new T_Size_t(dis.readLong());
    }
    
    // Private variables
    private final long value_long;
}
