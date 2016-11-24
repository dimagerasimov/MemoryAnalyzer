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
import static bintypes.T_Long.readLong;
import static bintypes.T_Size_t.readSize_t;
import static bintypes.T_Ptr.readPtr;

/**
 *
 * @author master
 */
public class StreamReader {    
    public static int ReadMFreeItem(byte[] content, int global_offset, int size,
            BinfElement binfElement) throws IOException
    {
        int internal_offset = global_offset;
        //READ FUNCTION CODE
        if(size - internal_offset < Byte.BYTES)
            { return 0; }
        else
        {
            binfElement.code_function = content[internal_offset]; 
            internal_offset += Byte.BYTES;
        }
        //READ COUNT
        if(size - internal_offset < Byte.BYTES)
            { return 0; }
        else
        {
            binfElement.count = content[internal_offset];
            internal_offset += Byte.BYTES;
        }
        //READ TYPES
        if(size - internal_offset < binfElement.count * Byte.BYTES)
            { return 0; }
        else
        {
            binfElement.types = new byte[binfElement.count];
            System.arraycopy(content, internal_offset,
                    binfElement.types, 0, binfElement.count * Byte.BYTES);
            internal_offset += binfElement.count * Byte.BYTES;
        }
        //READ SIZE OF DATA
        if(size - internal_offset < Byte.BYTES)
            { return 0; }
        else
        {
            binfElement.size_of_data = content[internal_offset];
            internal_offset += Byte.BYTES;
        }
        
        //READ DATA
        if(size - internal_offset < binfElement.size_of_data * Byte.BYTES)
            { return 0; }
        
        byte[] bytes_buffer;
        binfElement.data = new byte[binfElement.size_of_data];
        for(int i = 0, offset = 0; i < binfElement.count; i++)
        {
            switch(binfElement.types[i]) {
                case BinfElement.TCODE_PTR:
                    T_Ptr pointer = readPtr(content, offset + internal_offset);
                    bytes_buffer = ptrToBytes(pointer);
                    System.arraycopy(bytes_buffer, 0,
                            binfElement.data, offset, bytes_buffer.length);
                    offset += bytes_buffer.length;
                    break;
                case BinfElement.TCODE_SIZE_T:
                    T_Size_t size_t = readSize_t(content, offset + internal_offset);
                    bytes_buffer = size_tToBytes(size_t);
                    System.arraycopy(bytes_buffer, 0,
                            binfElement.data, offset, bytes_buffer.length);
                    offset += bytes_buffer.length;
                    break;
                case BinfElement.TCODE_LONG:
                    T_Long long_value = readLong(content, offset + internal_offset);
                    bytes_buffer = longToBytes(long_value);
                    System.arraycopy(bytes_buffer, 0,
                            binfElement.data, offset, bytes_buffer.length);
                    offset += bytes_buffer.length;
                    break;
                default:
                    return -1;
            }              
        }
        return binfElement.GetSize();
    }
    public static void ReadInputStream(DataInputStream dis,
            ArrayList<BinfElement> binfArray) throws IOException, InterruptedException {
        byte rByte;
        if(dis.available() >= Byte.BYTES) {
            rByte = dis.readByte();
            if(rByte != BEGIN_SESSION) {
                throw new IOException("Error in begin connection!");
            }
        }

        int offset, size, binf_size;
        boolean switcher_on;
        byte[] tmp_binary_data;
        final int size_buffer = 1<<15;
        byte[] binary_data1 = new byte[size_buffer];
        byte[] binary_data2 = new byte[size_buffer];
        
        switcher_on = true;
        tmp_binary_data = binary_data1;
        while(true) {
            offset = 0;
            size = dis.available();
            if(size > size_buffer) {
                size = size_buffer;
            }
            dis.read(tmp_binary_data, 0, size);
            while(true) {
                BinfElement binfElement = new BinfElement();
                binf_size = StreamReader.ReadMFreeItem(tmp_binary_data, offset, size, binfElement);
                if(binf_size > 0) {
                    binfArray.add(binfElement);
                } else if(binf_size == 0) {
                    if(tmp_binary_data[offset] == END_SESSION) {
                        return;
                    }
                    break;
                } else if(binf_size == -1) {
                    throw new IOException();
                }
                offset += binf_size;
            }
            
            if(switcher_on) {
                System.arraycopy(binary_data1, offset, binary_data2, 0, size - offset);
                tmp_binary_data = binary_data2;
            } else {
                System.arraycopy(binary_data2, offset, binary_data1, 0, size - offset);
                tmp_binary_data = binary_data1;
            }
            switcher_on = !switcher_on;
        }
    }
    
    // Private variables
    // Codes to begin / end session
    private static final byte BEGIN_SESSION = (byte) 254;
    private static final byte END_SESSION = (byte) 255;
}
