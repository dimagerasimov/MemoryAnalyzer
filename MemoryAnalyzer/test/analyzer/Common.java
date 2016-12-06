/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package analyzer;

import java.util.ArrayList;
import bintypes.BinfElement;
import bintypes.T_Long;
import bintypes.T_Ptr;
import bintypes.T_Size_t;

/**
 *
 * @author master
 */
public class Common {     
    public static void WriteToBufferFMalloc(byte[] buffer, int offset,
            T_Ptr arg1, T_Size_t arg2, T_Ptr arg3, T_Long arg4) {
        // Write code of function
        buffer[offset] = BinfElement.FCODE_MALLOC;
        offset += 1;
        // Write count of arguments
        buffer[offset] = BinfElement.FCOUNT_MALLOC;
        offset += 1;
        // Write types of arguments
        buffer[offset + 0] = BinfElement.TCODE_PTR;
        buffer[offset + 1] = BinfElement.TCODE_SIZE_T;
        buffer[offset + 2] = BinfElement.TCODE_PTR;
        buffer[offset + 3] = BinfElement.TCODE_LONG;
        offset += BinfElement.FCOUNT_MALLOC;
        // Write size of data
        buffer[offset] = (byte)BinfElement.FSIZE_OF_DATA_MALLOC;
        offset += 1;
        // Write data
        //arg1
        System.arraycopy(T_Ptr.ptrToBytes(arg1), 0, buffer, offset, T_Ptr.getSize());
        offset += T_Ptr.getSize();
        //arg2
        System.arraycopy(T_Size_t.size_tToBytes(arg2), 0, buffer, offset, T_Size_t.getSize());
        offset += T_Size_t.getSize();
        //arg3
        System.arraycopy(T_Ptr.ptrToBytes(arg3), 0, buffer, offset, T_Ptr.getSize());
        offset += T_Ptr.getSize();
        //arg4
        System.arraycopy(T_Long.longToBytes(arg4), 0, buffer, offset, T_Long.getSize());
        offset += T_Long.getSize();
    }
    public static void WriteToBufferFFree(byte[] buffer, int offset,
            T_Ptr arg1, T_Ptr arg2, T_Long arg3) {
        // Write code of function
        buffer[offset] = BinfElement.FCODE_FREE;
        offset += 1;
        // Write count of arguments
        buffer[offset] = BinfElement.FCOUNT_FREE;
        offset += 1;
        // Write types of arguments
        buffer[offset + 0] = BinfElement.TCODE_PTR;
        buffer[offset + 1] = BinfElement.TCODE_PTR;
        buffer[offset + 2] = BinfElement.TCODE_LONG;
        offset += BinfElement.FCOUNT_FREE;
        // Write size of data
        buffer[offset] = (byte)BinfElement.FSIZE_OF_DATA_FREE;
        offset += 1;
        // Write data
        //arg1
        System.arraycopy(T_Ptr.ptrToBytes(arg1), 0, buffer, offset, T_Ptr.getSize());
        offset += T_Ptr.getSize();
        //arg2
        System.arraycopy(T_Ptr.ptrToBytes(arg2), 0, buffer, offset, T_Ptr.getSize());
        offset += T_Ptr.getSize();
        //arg3
        System.arraycopy(T_Long.longToBytes(arg3), 0, buffer, offset, T_Long.getSize());
        offset += T_Long.getSize();
    }
    public static boolean isEquals(ArrayList<BinfElement> binfArray,
            byte[] binary, int offset) {
        BinfElement tmpBinfElement;
        for(int i = 0; i < binfArray.size(); i++) {
            tmpBinfElement = binfArray.get(i);
            if(tmpBinfElement.code_function != binary[offset]) {
                return false;
            }
            offset += 1;
            if(tmpBinfElement.count != binary[offset]) {
                return false;
            }
            offset += 1;
            for(int j = 0; j < tmpBinfElement.count; j++) {
                if(tmpBinfElement.types[j] != binary[offset]) {
                    return false;
                }
                offset += 1;
            }
            if(tmpBinfElement.size_of_data != binary[offset]) {
                return false;
            }
            offset += 1;
            for(int j = 0; j < tmpBinfElement.size_of_data; j++) {
                if(tmpBinfElement.data[j] != binary[offset]) {
                    return false;
                }
                offset += 1;
            }
        }
        return true;
    }
}
