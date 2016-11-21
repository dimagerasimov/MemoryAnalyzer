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
public class T_Long {
    public T_Long(byte[] bytes) {
        value_long = BytesToInt64(bytes);
    }
    public T_Long(long value) {
        value_long =  value;
    }
    public long getValue() {
        return value_long;
    }
    public T_Long reverseBytes() {
        return new T_Long(Long.reverseBytes(value_long));
    }
    public static int getSize() {
        return Long.BYTES;
    }
    public static T_Long bytesToLong(byte[] bytes) {
        return new T_Long(bytes);
    }
    public static byte[] longToBytes(T_Long value) {
        return Int64ToBytes(value.getValue());
    }
    public static T_Long readLong(byte[] content, int offset) throws IOException {
        byte[] buffer = new byte[Long.BYTES];
        System.arraycopy(content, offset, buffer, 0, Long.BYTES);
        return bytesToLong(buffer);
    }
    public static T_Long readLong(DataInputStream dis) throws IOException {
        return new T_Long(dis.readLong());
    }
    
    // Private variables
    private final long value_long;
}
