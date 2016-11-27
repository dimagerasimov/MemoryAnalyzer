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
import static bintypes.T_Long.longToBytes;
import static bintypes.T_Ptr.ptrToBytes;
import static bintypes.T_Size_t.size_tToBytes;
import static bintypes.T_Long.readLong;
import static bintypes.T_Size_t.readSize_t;
import static bintypes.T_Ptr.readPtr;
import network.Protocol;

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
            if(binfElement.code_function != BinfElement.FCODE_MALLOC
                && binfElement.code_function != BinfElement.FCODE_FREE) {
                // Special case (multi-purpose symbol)
                return -1;
            }
            internal_offset += Byte.BYTES;
        }
        //READ COUNT
        if(size - internal_offset < Byte.BYTES)
            { return 0; }
        else
        {
            binfElement.count = content[internal_offset];
            if(binfElement.count <= 0) {
                specialInfoThrow(global_offset, size);
            }
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
            if(binfElement.size_of_data <= 0) {
                specialInfoThrow(global_offset, size);
            }
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
                    specialInfoThrow(global_offset, size);
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

        int offset, size, the_rest_size, skip_size, count_error;
        boolean switcher_on;
        byte[] tmp_binary_data;
        final int MAX_ERROR = Protocol.TRANSLATION_TIMEOUT / 2;
        final int SIZE_BUFFER = 1<<15;
        byte[] binary_data1 = new byte[SIZE_BUFFER];
        byte[] binary_data2 = new byte[SIZE_BUFFER];
        
        switcher_on = true;
        tmp_binary_data = binary_data1;
        size = 0;
        count_error = 0;
        the_rest_size = 0;
        while(count_error < MAX_ERROR) {
            count_error++;
            offset = 0;
            size = dis.available() + the_rest_size;
            if(size > SIZE_BUFFER) {
                size = SIZE_BUFFER;
            }
            else if(size == 0) {
                Thread.sleep(500);
                continue;
            }
            dis.read(tmp_binary_data, the_rest_size, size - the_rest_size);
            while(true) {
                if((size - offset == 1) && (tmp_binary_data[offset] == END_SESSION)) {
                    // It mean that connection was finished successfully
                    return;
                }
                BinfElement binfElement = new BinfElement();
                skip_size = StreamReader.ReadMFreeItem(tmp_binary_data, offset, size, binfElement);
                if(skip_size > 0) {
                    binfArray.add(binfElement);
                    count_error = 0;
                } else if(skip_size == 0) {
                    break;
                } else {
                    skip_size = skipUnknownSequence(tmp_binary_data, offset, size);
                }
                offset += skip_size;
            }
            
            if(size - offset > 0) {
                if(switcher_on) {
                    System.arraycopy(binary_data1, offset, binary_data2, 0, size - offset);
                    tmp_binary_data = binary_data2;
                } else {
                    System.arraycopy(binary_data2, offset, binary_data1, 0, size - offset);
                    tmp_binary_data = binary_data1;
                }
                switcher_on = !switcher_on;
            }
            the_rest_size = size - offset;
            if(count_error > 0) {
                Thread.sleep(1000);
            }
        }
        throw new IOException("Unable to receive data! May be connection is slow.");
    }
    private static int skipUnknownSequence(byte[] content, int global_offset, int size)
            throws IOException {
        int num_skip_bytes, result_code;
        BinfElement binfElement = new BinfElement();
        num_skip_bytes = global_offset;
        while(num_skip_bytes < size) {
            result_code = StreamReader.ReadMFreeItem(content, num_skip_bytes, size, binfElement);
            if(result_code > 0) {
                break;
            }
            num_skip_bytes++;
        }
        return num_skip_bytes - global_offset;
    }
    private static void specialInfoThrow(int cur_position, int size) throws IOException {
        throw new IOException("Error data! Size of packet=" + String.valueOf(size)
            + ", position=" + String.valueOf(cur_position) + "!");
    }
    
    // Private variables
    // Codes to begin / end session
    private static final byte BEGIN_SESSION = (byte) 254;
    private static final byte END_SESSION = (byte) 255;
}
