/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package analyzer;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
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
public class StreamReaderTest {
    private static final int NUMBER_TEST_ELEMENTS = 10000;
        
    public StreamReaderTest() {
    }

    @Test
    public void testValidDataReadInputStream() throws Exception {
        System.out.println("testValidDataReadInputStream");
                
        final int size_of_buffer = (BinfElement.BINFMALLOC_SIZE + BinfElement.BINFFREE_SIZE)
                * NUMBER_TEST_ELEMENTS;
        final byte[] buffer = new byte[1 + size_of_buffer + 1];
        
        int offset = 0;
        buffer[offset] = StreamReader.BEGIN_SESSION;
        offset += 1;
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
        buffer[buffer.length - 1] = StreamReader.END_SESSION;
        
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        DataInputStream dis = new DataInputStream(bais);
        
        ArrayList<BinfElement> binfArray = new ArrayList(Help.WIN_MB);
        StreamReader.ReadInputStream(dis, binfArray);
        
        dis.close();
        bais.close();

        assertEquals(binfArray.size(), NUMBER_TEST_ELEMENTS * 2);
        assertEquals(Common.isEquals(binfArray, buffer, 1), true);
    }

    @Test
    public void testInvalidDataReadInputStream() throws Exception {
        System.out.println("testInvalidDataReadInputStream");
                
        final int size_of_buffer = (BinfElement.BINFMALLOC_SIZE + BinfElement.BINFFREE_SIZE)
                * NUMBER_TEST_ELEMENTS;
        final byte[] valid_buffer = new byte[1 + size_of_buffer + 1];
        final byte[] invalid_buffer = new byte[valid_buffer.length];
       
        int offset = 0;
        valid_buffer[offset] = StreamReader.BEGIN_SESSION;
        offset += 1;
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
        valid_buffer[valid_buffer.length - 1] = StreamReader.END_SESSION;
            
        for(int k = 0; k < NUMBER_TEST_ELEMENTS; k++) {
            System.arraycopy(valid_buffer, 0, invalid_buffer, 0, valid_buffer.length);

            // Spoil the data //
            byte random_byte = (byte)(Byte.MIN_VALUE + (Math.abs(Byte.MIN_VALUE)
                    + Math.abs(Byte.MAX_VALUE)));
            int random_place = (int)(Math.random() * (invalid_buffer.length - 1));
            invalid_buffer[random_place] = random_byte;
            ////////////////////
            
            ByteArrayInputStream bais = new ByteArrayInputStream(invalid_buffer);
            DataInputStream dis = new DataInputStream(bais);

            ArrayList<BinfElement> binfArray = new ArrayList(Help.WIN_MB);
            try {
                StreamReader.ReadInputStream(dis, binfArray);
            } catch(IOException ex) {
                // Random byte spoiled metadata then must be IOException
                continue;
            } catch(InterruptedException ex) {
                fail("Test failed");
            }
            dis.close();
            bais.close();
            // Random byte spoiled data then sizes of arrays must be equals
            assertEquals(binfArray.size(), NUMBER_TEST_ELEMENTS * 2);
        }
    }
}
