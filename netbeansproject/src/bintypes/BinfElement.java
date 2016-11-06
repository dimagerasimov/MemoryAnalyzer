/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bintypes;

import static bintypes.T_Long.bytesToLong;
import static bintypes.T_Ptr.bytesToPtr;
import static bintypes.T_Size_t.bytesToSize_t;

/**
 *
 * @author master
 */
public class BinfElement {
    //SECTIONS
    public static final byte MFREE_SECTION = 0x01;
    
    //FUNCTION CODES
    public static final byte TCODE_PTR = 0;
    public static final byte TCODE_SIZE_T = 1;
    public static final byte TCODE_LONG = 2;
    
    ///FOR MALLOC FREE SECTION
    public static final byte FCODE_MALLOC = 0;
    public static final byte FCODE_FREE = 1;
    
    //Public content
    public byte code_function;
    public byte count;
    public byte types[];
    public byte size_of_data;
    public byte data[];
    
    public int GetSize() {
        return 3 * Byte.BYTES + count * Byte.BYTES + size_of_data;
    }
    public static T_Ptr GetMFreeAddress(BinfElement element)
    {
        int offset = 0;
        byte[] bytes_to_ptr = new byte[T_Ptr.getSize()];
        switch(element.code_function) {
            case BinfElement.FCODE_MALLOC:
                //arg0
                for(int i = 0; i < T_Ptr.getSize(); i++)
                    { bytes_to_ptr[i] = element.data[i + offset]; }
                break;
            case BinfElement.FCODE_FREE:
                //arg0
                for(int i = 0; i < T_Ptr.getSize(); i++)
                    { bytes_to_ptr[i] = element.data[i + offset]; }
                break;
            default:
                return new T_Ptr(-1);
        }
        return bytesToPtr(bytes_to_ptr);
    }
    public static T_Size_t GetMFreeSize(BinfElement element)
    {
        int offset = 0;
        byte[] bytes_to_size_t = new byte[T_Size_t.getSize()];
        switch(element.code_function) {
            case BinfElement.FCODE_MALLOC:
                //arg1
                switch(element.types[0]){
                    case BinfElement.TCODE_PTR:
                        offset += T_Ptr.getSize();
                        break;
                    case BinfElement.TCODE_SIZE_T:
                        offset += T_Size_t.getSize();
                        break;
                    default:
                        return new T_Size_t(-1);
                }
                for(int i = 0; i < T_Size_t.getSize(); i++)
                    { bytes_to_size_t[i] = element.data[i + offset]; }
                break;
            default:
                return new T_Size_t(-1);
        }
        return bytesToSize_t(bytes_to_size_t);
    }
    public static T_Long GetMFreeTime(BinfElement element)
    {
        int offset = 0;
        byte[] bytes_to_size_t = new byte[T_Long.getSize()];
        switch(element.code_function) {
            case BinfElement.FCODE_MALLOC:
                //arg3
                for(int i = 0; i < 3; i++) {
                    switch(element.types[0]){
                        case BinfElement.TCODE_PTR:
                            offset += T_Ptr.getSize();
                            break;
                        case BinfElement.TCODE_SIZE_T:
                            offset += T_Size_t.getSize();
                            break;
                        case BinfElement.TCODE_LONG:
                            offset += T_Long.getSize();
                            break;
                        default:
                            return new T_Long(-1);
                    }
                }
                for(int i = 0; i < T_Long.getSize(); i++)
                    { bytes_to_size_t[i] = element.data[i + offset]; }
                break;
            case BinfElement.FCODE_FREE:
                //arg2
                for(int i = 0; i < 2; i++) {
                    switch(element.types[0]){
                        case BinfElement.TCODE_PTR:
                            offset += T_Ptr.getSize();
                            break;
                        case BinfElement.TCODE_SIZE_T:
                            offset += T_Size_t.getSize();
                            break;
                        case BinfElement.TCODE_LONG:
                            offset += T_Long.getSize();
                            break;
                        default:
                            return new T_Long(-1);
                    }
                }
                for(int i = 0; i < T_Long.getSize(); i++)
                    { bytes_to_size_t[i] = element.data[i + offset]; }
                break;
                
            default:
                return new T_Long(-1);
        }
        return bytesToLong(bytes_to_size_t);
    }
}
