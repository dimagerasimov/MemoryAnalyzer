/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package analyzer;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import bintypes.BinfElement;
import bintypes.T_Long;
import bintypes.T_Ptr;
import bintypes.T_Size_t;
import static bintypes.T_Ptr.ptrToBytes;
import static bintypes.T_Long.longToBytes;
import static bintypes.T_Size_t.size_tToBytes;
import static bintypes.T_Ptr.readPtr;
import static bintypes.T_Size_t.readSize_t;
import static bintypes.T_Long.readLong;

/**
 *
 * @author master
 */
public class BinReader {
    public static void ReadMFreeBinFile(String pathBinFile,
            ArrayList<BinfElement> binfArray) throws IOException
    {  
        if(Files.notExists(Paths.get(pathBinFile))) {
            throw new IOException("Output file don't exists");
        }
        
        byte rbyte;
        DataInputStream dis = new DataInputStream(new FileInputStream(pathBinFile));
        while(dis.available() > Byte.BYTES)
        {
            rbyte = dis.readByte();
            if(rbyte != BinfElement.MFREE_SECTION) {
                SkipSection(dis);
            } else {
                break;
            }
        }
        if(dis.available() < T_Size_t.getSize()) {
            throw new IOException("Length of section dont't exist!");
        }
        //Read length of section
        T_Size_t sizeOfSection = readSize_t(dis);        
        if(dis.available() < sizeOfSection.getValue()) {
            throw new IOException("Invalid length of section!");
        }
        //Read file content
        byte[] content = new byte[(int)sizeOfSection.getValue()];
        dis.readFully(content);
        dis.close();
        
        int file_offset = 0;
        BinfElement tmpBinfElement;
        while(file_offset < content.length) {
            tmpBinfElement = ReadMFreeItem(content, file_offset, content.length);
            file_offset += tmpBinfElement.GetSize();
            binfArray.add(tmpBinfElement);
        }
    }
    private static void SkipSection(DataInputStream dis) throws IOException
    {
        T_Size_t size;
        if(dis.available() >= T_Size_t.getSize())
        {
            size = readSize_t(dis);
            if(dis.available() >= size.getValue()) {
                dis.skip(size.getValue());
                return;
            }
        }
        throw new IOException("Section was written wrong!");
    }
    private static BinfElement ReadMFreeItem(byte[] content,
            final int file_offset, final int size) throws IOException
    {
        int internal_offset = file_offset;
        BinfElement binfElement = new BinfElement();
        //READ MALLOC
        if(size - internal_offset < Byte.BYTES)
            { specialInfoThrow(file_offset, size); }
        binfElement.code_function = content[internal_offset];
        if(binfElement.code_function != BinfElement.FCODE_MALLOC
                && binfElement.code_function != BinfElement.FCODE_FREE) {
                specialInfoThrow(file_offset, size);
        }
        internal_offset += Byte.BYTES;
        if(binfElement.code_function == (byte)BinfElement.FCODE_MALLOC) {
            //READ COUNT
            if(size - internal_offset < Byte.BYTES)
                { specialInfoThrow(file_offset, size); }
            binfElement.count = content[internal_offset];
            if(binfElement.count != BinfElement.FCOUNT_MALLOC) {
                specialInfoThrow(file_offset, size);
            }
            internal_offset += Byte.BYTES;
            //READ TYPES
            if(size - internal_offset < binfElement.count * Byte.BYTES)
                { specialInfoThrow(file_offset, size); }
            binfElement.types = new byte[binfElement.count];
            System.arraycopy(content, internal_offset,
                    binfElement.types, 0, binfElement.count * Byte.BYTES);
            if(binfElement.types[0] != BinfElement.TCODE_PTR
                    || binfElement.types[1] != BinfElement.TCODE_SIZE_T
                    || binfElement.types[2] != BinfElement.TCODE_PTR
                    || binfElement.types[3] != BinfElement.TCODE_LONG) {
                specialInfoThrow(file_offset, size);
            }
            internal_offset += binfElement.count * Byte.BYTES;
            //READ SIZE OF DATA
            if(size - internal_offset < Byte.BYTES)
                { specialInfoThrow(file_offset, size); }
            binfElement.size_of_data = content[internal_offset];
            if(binfElement.size_of_data != BinfElement.FSIZE_OF_DATA_MALLOC) {
                specialInfoThrow(file_offset, size);
            }
            internal_offset += Byte.BYTES;
            //READ DATA
            if(size - internal_offset < binfElement.size_of_data * Byte.BYTES)
                { specialInfoThrow(file_offset, size); }
            int offset_in_data = 0;
            byte[] bytes_buffer;
            binfElement.data = new byte[binfElement.size_of_data];
            //arg1
            T_Ptr arg1 = readPtr(content, internal_offset);
            bytes_buffer = ptrToBytes(arg1);
            System.arraycopy(bytes_buffer, 0,
                    binfElement.data, offset_in_data, bytes_buffer.length);
            internal_offset += T_Ptr.getSize();
            offset_in_data += T_Ptr.getSize();
            //arg2
            T_Size_t arg2 = readSize_t(content, internal_offset);
            bytes_buffer = size_tToBytes(arg2);
            System.arraycopy(bytes_buffer, 0,
                    binfElement.data, offset_in_data, bytes_buffer.length);
            internal_offset += T_Size_t.getSize();
            offset_in_data += T_Size_t.getSize();
            //arg3
            T_Ptr arg3 = readPtr(content, internal_offset);
            bytes_buffer = ptrToBytes(arg3);
            System.arraycopy(bytes_buffer, 0,
                    binfElement.data, offset_in_data, bytes_buffer.length);
            internal_offset += T_Ptr.getSize();
            offset_in_data += T_Ptr.getSize();
            //arg4
            T_Long arg4 = readLong(content, internal_offset);
            bytes_buffer = longToBytes(arg4);
            System.arraycopy(bytes_buffer, 0,
                    binfElement.data, offset_in_data, bytes_buffer.length);
            internal_offset += T_Long.getSize();
            offset_in_data += T_Long.getSize();
       } else if(binfElement.code_function == (byte)BinfElement.FCODE_FREE) {
            //READ COUNT
            if(size - internal_offset < Byte.BYTES)
                { specialInfoThrow(file_offset, size); }
            binfElement.count = content[internal_offset];
            if(binfElement.count != BinfElement.FCOUNT_FREE) {
                specialInfoThrow(file_offset, size);
            }
            internal_offset += Byte.BYTES;
            //READ TYPES
            if(size - internal_offset < binfElement.count * Byte.BYTES)
                { specialInfoThrow(file_offset, size); }
            binfElement.types = new byte[binfElement.count];
            System.arraycopy(content, internal_offset,
                    binfElement.types, 0, binfElement.count * Byte.BYTES);
            if(binfElement.types[0] != BinfElement.TCODE_PTR
                    || binfElement.types[1] != BinfElement.TCODE_PTR
                    || binfElement.types[2] != BinfElement.TCODE_LONG) {
                specialInfoThrow(file_offset, size);
            }
            internal_offset += binfElement.count * Byte.BYTES;
            //READ SIZE OF DATA
            if(size - internal_offset < Byte.BYTES)
                { specialInfoThrow(file_offset, size); }
            binfElement.size_of_data = content[internal_offset];
            if(binfElement.size_of_data != BinfElement.FSIZE_OF_DATA_FREE) {
                specialInfoThrow(file_offset, size);
            }
            internal_offset += Byte.BYTES;
            //READ DATA
            if(size - internal_offset < binfElement.size_of_data * Byte.BYTES)
                { specialInfoThrow(file_offset, size); }
            int offset_in_data = 0;
            byte[] bytes_buffer;
            binfElement.data = new byte[binfElement.size_of_data];
            //arg1
            T_Ptr arg1 = readPtr(content, internal_offset);
            bytes_buffer = ptrToBytes(arg1);
            System.arraycopy(bytes_buffer, 0,
                    binfElement.data, offset_in_data, bytes_buffer.length);
            internal_offset += T_Ptr.getSize();
            offset_in_data += T_Ptr.getSize();
            //arg2
            T_Ptr arg2 = readPtr(content, internal_offset);
            bytes_buffer = ptrToBytes(arg2);
            System.arraycopy(bytes_buffer, 0,
                    binfElement.data, offset_in_data, bytes_buffer.length);
            internal_offset += T_Ptr.getSize();
            offset_in_data += T_Ptr.getSize();
            //arg3
            T_Long arg3 = readLong(content, internal_offset);
            bytes_buffer = longToBytes(arg3);
            System.arraycopy(bytes_buffer, 0,
                    binfElement.data, offset_in_data, bytes_buffer.length);
            internal_offset += T_Long.getSize();
            offset_in_data += T_Long.getSize();
        }
        return binfElement;
    }
    private static void specialInfoThrow(int cur_position, int size) throws IOException {
        throw new IOException("Error data! Size of packet=" + String.valueOf(size)
            + ", position=" + String.valueOf(cur_position) + "!");
    }
}
