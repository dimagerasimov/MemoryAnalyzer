/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package memoryanalyzer;

/**
 *
 * @author master
 */
public class BinTypes {
    //SECTIONS
    public static final byte MFREE_SECTION = 0x01;
    
    //FUNCTION CODES
    public static final byte TCODE_PTR = 0;
    public static final byte TCODE_SIZE_T = 1;
    
    ///FOR MALLOC FREE SECTION
    public static final byte FCODE_MALLOC = 0;
    public static final byte FCODE_FREE = 1;
    
    public static class BinfElement{
	public byte code_function;
        public byte count;
        public byte types[];
        public byte size_of_data;
	public byte data[];
    }
}
