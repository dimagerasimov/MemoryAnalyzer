/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package analyzer;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import bintypes.BinfElement;
import bintypes.T_Long;
import bintypes.T_Ptr;
import bintypes.T_Size_t;
import static bintypes.T_Long.readLong;
import static bintypes.T_Size_t.readSize_t;
import static bintypes.T_Ptr.readPtr;
import static bintypes.T_Long.longToBytes;
import static bintypes.T_Ptr.ptrToBytes;
import static bintypes.T_Size_t.size_tToBytes;

/**
 *
 * @author master
 */
public class StreamReader {
    // Codes to begin / end session
    private static final byte BEGIN_SESSION = (byte) 254;
    private static final byte END_SESSION = (byte) 255;
    
    public static BinfElement ReadMFreeItem(DataInputStream dis) throws IOException
    {
        BinfElement element = new BinfElement();
        //READ FUNCTION CODE
        if(dis.available() < Byte.BYTES)
            { return null; }
        else
            { element.code_function = dis.readByte(); }
        //READ COUNT
        if(dis.available() < Byte.BYTES)
            { return null; }
        else
            { element.count = dis.readByte(); }
        //READ TYPES
        if(dis.available() < element.count * Byte.BYTES)
            { return null; }
        else
        {
            element.types = new byte[element.count];
            dis.read(element.types, 0, element.count);
        }
        //READ SIZE OF DATA
        if(dis.available() < Byte.BYTES)
            { return null; }
        else
            { element.size_of_data = dis.readByte(); }
        //READ DATA
        byte[] bytes_buffer;
        element.data = new byte[element.size_of_data];
        for(int i = 0, offset = 0; i < element.count; i++)
        {
            switch(element.types[i]) {
                case BinfElement.TCODE_PTR:
                    T_Ptr pointer = readPtr(dis);
                    bytes_buffer = ptrToBytes(pointer);
                    System.arraycopy(bytes_buffer, 0,
                            element.data, offset, bytes_buffer.length);
                    offset += bytes_buffer.length;
                    break;
                case BinfElement.TCODE_SIZE_T:
                    T_Size_t size = readSize_t(dis);
                    bytes_buffer = size_tToBytes(size);
                    System.arraycopy(bytes_buffer, 0,
                            element.data, offset, bytes_buffer.length);
                    offset += bytes_buffer.length;
                    break;
                case BinfElement.TCODE_LONG:
                    T_Long value = readLong(dis);
                    bytes_buffer = longToBytes(value);
                    System.arraycopy(bytes_buffer, 0,
                            element.data, offset, bytes_buffer.length);
                    offset += bytes_buffer.length;
                    break;
                default:
                    return null;
            }              
        }
        return element;
    }
    public static void ReadInputStream(DataInputStream dis,
            ArrayList<BinfElement> binfArray) throws IOException {
        byte rByte;
        if(dis.available() >= Byte.BYTES) {
            rByte = dis.readByte();
            if(rByte != BEGIN_SESSION) {
                throw new IOException("Error in begin connection!");
            }
        }

        while(true) {
            if(dis.available() == Byte.BYTES) {
                rByte = dis.readByte();
                if(rByte == END_SESSION) {
                    break;
                }
            } else if(dis.available() != 0) {
                BinfElement binfElement = StreamReader.ReadMFreeItem(dis);
                if(binfElement == null) {
                    throw new IOException("Indalid data!");
                }
                binfArray.add(binfElement);
            }
        }
    }
}