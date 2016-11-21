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
import java.nio.file.Paths;

/**
 *
 * @author master
 */
public class BinReader {
    private static void SkipSection(DataInputStream dis) throws IOException
    {
        T_Size_t size;
        if(dis.available() >= T_Size_t.getSize())
        {
            size = readSize_t(dis);
            if(dis.available() >= size.getValue())
                { dis.skip(size.getValue()); }
        }
    }
    private static BinfElement ReadMFreeItem(byte[] content,
            final int file_offset) throws IOException
    {
        int internal_offset = file_offset;
        BinfElement element = new BinfElement();
        //READ FUNCTION CODE
        if(content.length - internal_offset < Byte.BYTES)
            { return null; }
        else
        {
            element.code_function = content[internal_offset]; 
            internal_offset += Byte.BYTES;
        }
        //READ COUNT
        if(content.length - internal_offset < Byte.BYTES)
            { return null; }
        else
        {
            element.count = content[internal_offset];
            internal_offset += Byte.BYTES;
        }
        //READ TYPES
        if(content.length - internal_offset < element.count * Byte.BYTES)
            { return null; }
        else
        {
            element.types = new byte[element.count];
            System.arraycopy(content, internal_offset,
                    element.types, 0, element.count * Byte.BYTES);
            internal_offset += element.count * Byte.BYTES;
        }
        //READ SIZE OF DATA
        if(content.length - internal_offset < Byte.BYTES)
            { return null; }
        else
        {
            element.size_of_data = content[internal_offset];
            internal_offset += Byte.BYTES;
        }
        //READ DATA
        byte[] bytes_buffer;
        element.data = new byte[element.size_of_data];
        for(int i = 0, offset = 0; i < element.count; i++)
        {
            switch(element.types[i]) {
                case BinfElement.TCODE_PTR:
                    T_Ptr pointer = readPtr(content, offset + internal_offset);
                    bytes_buffer = ptrToBytes(pointer);
                    System.arraycopy(bytes_buffer, 0,
                            element.data, offset, bytes_buffer.length);
                    offset += bytes_buffer.length;
                    break;
                case BinfElement.TCODE_SIZE_T:
                    T_Size_t size = readSize_t(content, offset + internal_offset);
                    bytes_buffer = size_tToBytes(size);
                    System.arraycopy(bytes_buffer, 0,
                            element.data, offset, bytes_buffer.length);
                    offset += bytes_buffer.length;
                    break;
                case BinfElement.TCODE_LONG:
                    T_Long long_value = readLong(content, offset + internal_offset);
                    bytes_buffer = longToBytes(long_value);
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
    public static ArrayList<BinfElement> ReadMFreeBinFile(String pathBinFile,
            ArrayList<BinfElement> binfArray) throws IOException
    {  
        byte rbyte;
        boolean isMFree = false;
        //Open binary file
        if(Files.notExists(Paths.get(pathBinFile))) {
            throw new IOException("Output file don't exists.\n" +
                    "Used not special pin-tool!");
        }
        DataInputStream dis = new DataInputStream(new FileInputStream(pathBinFile));
        while(dis.available() > Byte.BYTES)
        {
            rbyte = dis.readByte();
            if(rbyte == BinfElement.MFREE_SECTION)
                { isMFree = true; break; }
            else
                { SkipSection(dis); }
        }
        if(!isMFree || dis.available() < T_Size_t.getSize())
            { throw new IOException("File content can't be showed.\n"
                    + "May be used not special pin-tool."); }
        //Read size (length) of section
        T_Size_t sizeOfSection = readSize_t(dis);        
        if(dis.available() < sizeOfSection.getValue()) {
            throw new IOException("The binary file was written wrong!");
        }
        //Read file content
        byte[] content = new byte[(int)sizeOfSection.getValue()];
        dis.readFully(content);
        //Close binary file
        dis.close();
        
        int file_offset = 0;
        BinfElement tmpBinfElement;
        while(file_offset < content.length) {
            tmpBinfElement = ReadMFreeItem(content, file_offset);
            if(tmpBinfElement == null) {
                return null;
            }
            file_offset += tmpBinfElement.GetSize();
            if(tmpBinfElement.GetSize() != 39 && tmpBinfElement.GetSize() != 30) {
                continue;
            }
            binfArray.add(tmpBinfElement);
        }
        return binfArray;
    }
}
