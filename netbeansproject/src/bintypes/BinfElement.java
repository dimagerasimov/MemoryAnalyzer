/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bintypes;

import static bintypes.Ptr.bytesToPtr;
import static bintypes.Size_t.bytesToSize_t;

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
    public static Ptr GetMFreeAddress(BinfElement element)
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
    public static Size_t GetMFreeSize(BinfElement element)
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
}
