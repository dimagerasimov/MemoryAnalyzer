/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package memoryanalyzer;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.HashMap;
import org.jfree.data.xy.XYSeries;
import bintypes.BinfElement;
import bintypes.Ptr;
import bintypes.Size_t;
import static crossplatform.Help.GetNumBytesInMb;
import static bintypes.Ptr.bytesToPtr;
import static bintypes.Ptr.ptrToBytes;
import static bintypes.Ptr.readPtr;
import static bintypes.Size_t.bytesToSize_t;
import static bintypes.Size_t.readSize_t;
import static bintypes.Size_t.size_tToBytes;

/**
 *
 * @author master
 */
public class BinReader {
    private static void SkipSection(DataInputStream dis, boolean reverse) throws IOException
    {
        Size_t size;
        if(dis.available() >= Size_t.getSize())
        {
            size = readSize_t(dis, reverse);
            if(dis.available() >= size.getValue())
                { dis.skip(size.getValue()); }
        }
    }
    private static Ptr GetMFreeAddress(BinfElement element)
    {
        int offset = 0;
        byte[] bytes_to_ptr = new byte[Ptr.getSize()];
        switch(element.code_function) {
            case BinfElement.FCODE_MALLOC:
                //arg0
                for(int i = 0; i < Ptr.getSize(); i++)
                    { bytes_to_ptr[i] = element.data[i + offset]; }
                break;
            case BinfElement.FCODE_FREE:
                //arg0
                for(int i = 0; i < Ptr.getSize(); i++)
                    { bytes_to_ptr[i] = element.data[i + offset]; }
                break;
            default:
                return new Ptr(-1);
        }
        return bytesToPtr(bytes_to_ptr);
    }
    private static Size_t GetMFreeSize(BinfElement element)
    {
        int offset = 0;
        byte[] bytes_to_size_t = new byte[Size_t.getSize()];
        switch(element.code_function) {
            case BinfElement.FCODE_MALLOC:
                //arg1
                switch(element.types[0]){
                    case BinfElement.TCODE_PTR:
                        offset += Ptr.getSize();
                        break;
                    case BinfElement.TCODE_SIZE_T:
                        offset += Size_t.getSize();
                        break;
                    default:
                        return new Size_t(-1);
                }
                for(int i = 0; i < Size_t.getSize(); i++)
                    { bytes_to_size_t[i] = element.data[i + offset]; }
                break;
            default:
                return new Size_t(-1);
        }
        return bytesToSize_t(bytes_to_size_t);
    }
    private static BinfElement ReadMFreeItem(DataInputStream dis, boolean reverse) throws IOException
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
                    Ptr pointer = readPtr(dis, reverse);
                    bytes_buffer = ptrToBytes(pointer);
                    System.arraycopy(bytes_buffer, 0,
                            element.data, offset, bytes_buffer.length);
                    offset += bytes_buffer.length;
                    break;
                case BinfElement.TCODE_SIZE_T:
                    Size_t size = readSize_t(dis, reverse);
                    bytes_buffer = size_tToBytes(size);
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
    public static XYSeries ReadMFreeBinFile(String pathBinFile) throws IOException
    {  
        // If little endian then reverse byte order, because JRE works big endian
        boolean reverse;
        reverse = ByteOrder.nativeOrder() != ByteOrder.BIG_ENDIAN;
        
        byte rbyte;
        Size_t size;
        boolean isMFree = false;
        DataInputStream dis = new DataInputStream(new FileInputStream(pathBinFile));
        while(dis.available() > Byte.BYTES)
        {
            rbyte = dis.readByte();
            if(rbyte == BinfElement.MFREE_SECTION)
                { isMFree = true; break; }
            else
                { SkipSection(dis, reverse); }
        }
        if(!isMFree)
            { return null; }
        if(dis.available() >= Size_t.getSize()) {
            size = readSize_t(dis, reverse);
        }

        int count; //In future analog time (get from binf element)
        long tmp_long_key; 
        double sum, value_sum;
        BinfElement tmpBinfElement, retBinfElement;
        XYSeries curveOfMemory = new XYSeries("Capacity");
        HashMap<Long, BinfElement> map = new HashMap<>();

        //Add first value (start program)
        curveOfMemory.add(0, 0);
        count = 1; sum = 0;
        while(dis.available() > 0)
        {
            tmpBinfElement = ReadMFreeItem(dis, reverse);
            tmp_long_key = GetMFreeAddress(tmpBinfElement).getValue();
            if(tmpBinfElement == null || tmp_long_key == -1)
                { throw new IOException("Binary file was written wrong."); }
            retBinfElement = map.put(tmp_long_key, tmpBinfElement);
            value_sum = GetMFreeSize(tmpBinfElement).getValue();
            if(value_sum == -1)
                { value_sum = -GetMFreeSize(retBinfElement).getValue(); }
            sum += (double)value_sum / GetNumBytesInMb();
            curveOfMemory.add(count, sum);
            count++;
        }
        map.clear();
        dis.close();
                
        return curveOfMemory;
    }
}
