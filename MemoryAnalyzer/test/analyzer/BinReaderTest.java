/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package analyzer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import org.junit.Test;
import bintypes.BinfElement;
import bintypes.T_Long;
import bintypes.T_Ptr;
import bintypes.T_Size_t;
import crossplatform.Help;
import static org.junit.Assert.*;

/**
 *
 * @author master
 */
public class BinReaderTest {
    private static final int NUMBER_TEST_ELEMENTS = 10000;
    
    public BinReaderTest() {
    }

    @Test
    public void testValidDataReadMFreeBinFile() throws Exception {
        System.out.println("testValidDataReadMFreeBinFile");
                
        final int size_of_buffer = (BinfElement.BINFMALLOC_SIZE + BinfElement.BINFFREE_SIZE)
                * NUMBER_TEST_ELEMENTS;
        final byte[] buffer = new byte[Byte.BYTES + T_Size_t.getSize() + size_of_buffer];
        
        int offset = 0;
        buffer[offset] = BinfElement.MFREE_SECTION;
        offset += 1;
        System.arraycopy(T_Size_t.size_tToBytes(new T_Size_t(size_of_buffer)), 0,
                buffer, offset, T_Size_t.getSize());
        offset += T_Size_t.getSize();
        for(int i = 0; i < NUMBER_TEST_ELEMENTS; i++) {
            Common.WriteToBufferFMalloc(buffer, offset,
                    new T_Ptr(Math.round(Math.random() * Long.MAX_VALUE)),
                    new T_Size_t(Math.round(Math.random() * Long.MAX_VALUE)),
                    new T_Ptr(Math.round(Math.random() * Long.MAX_VALUE)),
                    new T_Long(Math.round(Math.random() * Long.MAX_VALUE)));
            offset += BinfElement.BINFMALLOC_SIZE;
            Common.WriteToBufferFFree(buffer, offset,
                    new T_Ptr(Math.round(Math.random() * Long.MAX_VALUE)),
                    new T_Ptr(Math.round(Math.random() * Long.MAX_VALUE)),
                    new T_Long(Math.round(Math.random() * Long.MAX_VALUE)));
            offset += BinfElement.BINFFREE_SIZE;
        }
        
        final String path_test_file = "./testValidDataReadMFreeBinFile.test";
        // Create file for the test
        FileOutputStream fos = new FileOutputStream(path_test_file);
        fos.write(buffer, 0, buffer.length);
        fos.close();
        
        ArrayList<BinfElement> binfArray = new ArrayList(Help.WIN_MB);
        BinReader.ReadMFreeBinFile(path_test_file, binfArray);

        // Delete file after the test
        Files.deleteIfExists(Paths.get(path_test_file));
        
        assertEquals(binfArray.size(), NUMBER_TEST_ELEMENTS * 2);
        assertEquals(Common.isEquals(binfArray, buffer, Byte.BYTES + T_Size_t.getSize()), true);
    }

    @Test
    public void testInvalidDataReadMFreeBinFile() throws Exception {
        System.out.println("testInvalidDataReadMFreeBinFile");
                
        final int size_of_buffer = (BinfElement.BINFMALLOC_SIZE + BinfElement.BINFFREE_SIZE)
                * NUMBER_TEST_ELEMENTS;
        final byte[] valid_buffer = new byte[Byte.BYTES + T_Size_t.getSize() + size_of_buffer];
        final byte[] invalid_buffer = new byte[valid_buffer.length];
        
        int offset = 0;
        valid_buffer[offset] = BinfElement.MFREE_SECTION;
        offset += 1;
        System.arraycopy(T_Size_t.size_tToBytes(new T_Size_t(size_of_buffer)), 0,
                valid_buffer, offset, T_Size_t.getSize());
        offset += T_Size_t.getSize();
        for(int i = 0; i < NUMBER_TEST_ELEMENTS; i++) {
            Common.WriteToBufferFMalloc(valid_buffer, offset,
                    new T_Ptr(Math.round(Math.random() * Long.MAX_VALUE)),
                    new T_Size_t(Math.round(Math.random() * Long.MAX_VALUE)),
                    new T_Ptr(Math.round(Math.random() * Long.MAX_VALUE)),
                    new T_Long(Math.round(Math.random() * Long.MAX_VALUE)));
            offset += BinfElement.BINFMALLOC_SIZE;
            Common.WriteToBufferFFree(valid_buffer, offset,
                    new T_Ptr(Math.round(Math.random() * Long.MAX_VALUE)),
                    new T_Ptr(Math.round(Math.random() * Long.MAX_VALUE)),
                    new T_Long(Math.round(Math.random() * Long.MAX_VALUE)));
            offset += BinfElement.BINFFREE_SIZE;
        }
        
        for(int k = 0; k < NUMBER_TEST_ELEMENTS; k++) {
            System.arraycopy(valid_buffer, 0, invalid_buffer, 0, valid_buffer.length);

            // Spoil the data //
            byte random_byte = (byte)(Byte.MIN_VALUE + (Math.abs(Byte.MIN_VALUE)
                    + Math.abs(Byte.MAX_VALUE)));
            int random_place = (int)(Math.random() * (invalid_buffer.length - 1));
            invalid_buffer[random_place] = random_byte;
            ////////////////////
            
            final String path_test_file = "./testInvalidDataReadMFreeBinFile.test";
            // Create file for the test
            FileOutputStream fos = new FileOutputStream(path_test_file);
            fos.write(invalid_buffer, 0, invalid_buffer.length);
            fos.close();

            ArrayList<BinfElement> binfArray = new ArrayList(Help.WIN_MB);
            try {
                BinReader.ReadMFreeBinFile(path_test_file, binfArray);
            } catch(IOException ex) {
                // Random byte spoiled metadata then must be IOException
                continue;
            } catch(Exception ex) {
                fail("Test failed");
            }
            // Delete file after the test
            Files.deleteIfExists(Paths.get(path_test_file));
            // Random byte spoiled data then sizes of arrays must be equals
            assertEquals(binfArray.size(), NUMBER_TEST_ELEMENTS * 2);
        }
    }
    
    @Test
    public void testSkipSectionReadMFreeBinFile() throws Exception {
        System.out.println("testSkipSectionReadMFreeBinFile");
                
        final int number_elements_other_section = 1000;
        final int size_of_buffer = (BinfElement.BINFMALLOC_SIZE + BinfElement.BINFFREE_SIZE)
                * (number_elements_other_section + NUMBER_TEST_ELEMENTS);
        final byte[] buffer = new byte[(Byte.BYTES + T_Size_t.getSize()) * 2 + size_of_buffer];
        
        int offset = 0;
        buffer[offset] = 96;// Section's code
        offset += 1;
        System.arraycopy(T_Size_t.size_tToBytes(new T_Size_t((BinfElement.BINFMALLOC_SIZE
                + BinfElement.BINFFREE_SIZE) * number_elements_other_section)), 0,
                buffer, offset, T_Size_t.getSize());
        offset += T_Size_t.getSize();
        for(int i = 0; i < number_elements_other_section; i++) {
            Common.WriteToBufferFMalloc(buffer, offset,
                    new T_Ptr(Math.round(Math.random() * Long.MAX_VALUE)),
                    new T_Size_t(Math.round(Math.random() * Long.MAX_VALUE)),
                    new T_Ptr(Math.round(Math.random() * Long.MAX_VALUE)),
                    new T_Long(Math.round(Math.random() * Long.MAX_VALUE)));
            offset += BinfElement.BINFMALLOC_SIZE;
            Common.WriteToBufferFFree(buffer, offset,
                    new T_Ptr(Math.round(Math.random() * Long.MAX_VALUE)),
                    new T_Ptr(Math.round(Math.random() * Long.MAX_VALUE)),
                    new T_Long(Math.round(Math.random() * Long.MAX_VALUE)));
            offset += BinfElement.BINFFREE_SIZE;
        }
        buffer[offset] = BinfElement.MFREE_SECTION;
        offset += 1;
        System.arraycopy(T_Size_t.size_tToBytes(new T_Size_t((BinfElement.BINFMALLOC_SIZE
                + BinfElement.BINFFREE_SIZE) * NUMBER_TEST_ELEMENTS)), 0,
                buffer, offset, T_Size_t.getSize());
        offset += T_Size_t.getSize();
        for(int i = 0; i < NUMBER_TEST_ELEMENTS; i++) {
            Common.WriteToBufferFMalloc(buffer, offset,
                    new T_Ptr(Math.round(Math.random() * Long.MAX_VALUE)),
                    new T_Size_t(Math.round(Math.random() * Long.MAX_VALUE)),
                    new T_Ptr(Math.round(Math.random() * Long.MAX_VALUE)),
                    new T_Long(Math.round(Math.random() * Long.MAX_VALUE)));
            offset += BinfElement.BINFMALLOC_SIZE;
            Common.WriteToBufferFFree(buffer, offset,
                    new T_Ptr(Math.round(Math.random() * Long.MAX_VALUE)),
                    new T_Ptr(Math.round(Math.random() * Long.MAX_VALUE)),
                    new T_Long(Math.round(Math.random() * Long.MAX_VALUE)));
            offset += BinfElement.BINFFREE_SIZE;
        }
        
        final String path_test_file = "./testSkipSectionReadMFreeBinFile.test";
        // Create file for the test
        FileOutputStream fos = new FileOutputStream(path_test_file);
        fos.write(buffer, 0, buffer.length);
        fos.close();
        
        ArrayList<BinfElement> binfArray = new ArrayList(Help.WIN_MB);
        BinReader.ReadMFreeBinFile(path_test_file, binfArray);

        // Delete file after the test
        Files.deleteIfExists(Paths.get(path_test_file));
        
        assertEquals(binfArray.size(), NUMBER_TEST_ELEMENTS * 2);
        assertEquals(Common.isEquals(binfArray, buffer, (Byte.BYTES + T_Size_t.getSize()) * 2
                + (BinfElement.BINFMALLOC_SIZE + BinfElement.BINFFREE_SIZE)
                * number_elements_other_section), true);
    }
}
