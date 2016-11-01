/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package memoryanalyzer;

import memoryanalyzer.BinTypes.BinfElement;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import org.jfree.data.xy.XYSeries;

/**
 *
 * @author master
 */
public class BinReader {
    private static byte[] LongToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(x);
        return buffer.array();
    }
    private static long BytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        buffer.flip();
        return buffer.getLong();
    }
    private static void SkipSection(DataInputStream dis) throws IOException
    {
        Long rlong;
        if(dis.available() >= BinTypes.SIZE_OF_SIZE_T)
        {
            rlong = dis.readLong();
            if(dis.available() >= rlong)
                { dis.skip(rlong); }
        }
    }
    private static long GetMFreeAddress(BinfElement element)
    {
        int offset = 0;
        byte[] bin_to_long = new byte[BinTypes.SIZE_OF_PTR];
        switch(element.code_function) {
            case BinTypes.FCODE_MALLOC:
                //arg0
                for(int i = 0; i < BinTypes.SIZE_OF_PTR; i++)
                    { bin_to_long[i] = element.data[i + offset]; }
                break;
            case BinTypes.FCODE_FREE:
                //arg0
                for(int i = 0; i < BinTypes.SIZE_OF_PTR; i++)
                    { bin_to_long[i] = element.data[i + offset]; }
                break;
            default:
                return -1;
        }
        return BytesToLong(bin_to_long);
    }
    private static double GetMFreeSizeMB(BinfElement element)
    {
        int offset = 0;
        byte[] bin_to_long = new byte[BinTypes.SIZE_OF_SIZE_T];
        switch(element.code_function) {
            case BinTypes.FCODE_MALLOC:
                //arg1
                switch(element.types[0]){
                    case BinTypes.TCODE_PTR:
                        offset += BinTypes.SIZE_OF_PTR;
                        break;
                    case BinTypes.TCODE_SIZE_T:
                        offset += BinTypes.SIZE_OF_SIZE_T;
                        break;
                    default:
                        return -1;
                }
                for(int i = 0; i < BinTypes.SIZE_OF_SIZE_T; i++)
                    { bin_to_long[i] = element.data[i + offset]; }
                break;
            default:
                return -1;
        }
        return (double)BytesToLong(bin_to_long)
                / CrossPlatform.GetNumBytesInMb();// In megabytes
    }
    private static BinfElement ReadMFreeItem(DataInputStream dis, boolean reverse) throws IOException
    {
        BinfElement element = new BinfElement();
        //READ FUNCTION CODE
        if(dis.available() < BinTypes.SIZE_OF_BYTE)
            { return null; }
        else
            { element.code_function = dis.readByte(); }
        //READ COUNT
        if(dis.available() < BinTypes.SIZE_OF_BYTE)
            { return null; }
        else
            { element.count = dis.readByte(); }
        //READ TYPES
        if(dis.available() < element.count * BinTypes.SIZE_OF_BYTE)
            { return null; }
        else
        {
            element.types = new byte[element.count];
            dis.read(element.types, 0, element.count);
        }
        //READ SIZE OF DATA
        if(dis.available() < BinTypes.SIZE_OF_BYTE)
            { return null; }
        else
            { element.size_of_data = dis.readByte(); }
        //READ DATA
        long rlong;
        byte[] long_to_bin;
        element.data = new byte[element.size_of_data];
        for(int i = 0, offset = 0; i < element.count; i++)
        {
            switch(element.types[i]) {
                case BinTypes.TCODE_PTR:
                    rlong = dis.readLong();
                    if(reverse) {
                        rlong = Long.reverseBytes(rlong);
                    }
                    long_to_bin = LongToBytes(rlong);
                    System.arraycopy(long_to_bin, 0,
                            element.data, offset, long_to_bin.length);
                    offset += long_to_bin.length;
                    break;
                case BinTypes.TCODE_SIZE_T:
                    rlong = dis.readLong();
                    if(reverse) {
                        rlong = Long.reverseBytes(rlong);
                    }
                    long_to_bin = LongToBytes(rlong);
                    System.arraycopy(long_to_bin, 0,
                            element.data, offset, long_to_bin.length);
                    offset += long_to_bin.length;
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
        long rlong;
        boolean isMFree = false;
        DataInputStream dis = new DataInputStream(new FileInputStream(pathBinFile));
        while(dis.available() > BinTypes.SIZE_OF_BYTE)
        {
            rbyte = dis.readByte();
            if(rbyte == BinTypes.MFREE_SECTION)
                { isMFree = true; break; }
            else
                { SkipSection(dis); }
        }
        if(!isMFree)
            { return null; }
        if(dis.available() >= BinTypes.SIZE_OF_SIZE_T)
        {
            rlong = dis.readLong();
            if(reverse)
                { rlong = Long.reverseBytes(rlong); }
            if(dis.available() < rlong)
                { throw new IOException("Binary file was written wrong"); }
        }

        int count; //In future analog time (get from binf element)
        long tmp_key; 
        double sum, value_sum;
        BinfElement tmp_element, ret_element;
        XYSeries curveOfMemory = new XYSeries("Capacity");
        HashMap<Long, BinfElement> map = new HashMap<>();

        //Add first value (start program)
        curveOfMemory.add(0, 0);
        count = 1; sum = 0;
        while(dis.available() > 0)
        {
            tmp_element = ReadMFreeItem(dis, reverse);
            tmp_key = GetMFreeAddress(tmp_element);
            if(tmp_element == null || tmp_key == -1)
                { throw new IOException("Binary file was written wrong"); }
            ret_element = map.put(tmp_key, tmp_element);
            value_sum = GetMFreeSizeMB(tmp_element);
            if(value_sum == -1)
                { value_sum = -GetMFreeSizeMB(ret_element); }
            sum += value_sum;
            curveOfMemory.add(count, sum);
            count++;
        }
        map.clear();
        dis.close();
                
        return curveOfMemory;
    }
}
