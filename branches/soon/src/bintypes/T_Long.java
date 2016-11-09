/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bintypes;

import static crossplatform.Convert.BytesToInt64;
import static crossplatform.Convert.Int64ToBytes;
import java.io.DataInputStream;
import java.io.IOException;

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
        T_Long value = new T_Long(Long.reverseBytes(value_long));
        return value;
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
    public static T_Long readLong(byte[] content, int offset,
            boolean reverse) throws IOException {
        T_Long value;
        byte[] buffer;
        buffer = new byte[Long.BYTES];
        System.arraycopy(content, offset, buffer, 0, Long.BYTES);
        value = bytesToLong(buffer);
        if(reverse) {
            value = value.reverseBytes();
        }
        return value;
    }
    public static T_Long readLong(DataInputStream dis,
            boolean reverse) throws IOException {
        T_Long value = new T_Long(dis.readLong());
        if(reverse) {
            value = value.reverseBytes();
        }
        return value;
    }
    
    // Private variables
    private final long value_long;
}
