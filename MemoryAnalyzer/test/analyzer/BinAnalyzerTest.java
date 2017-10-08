/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package analyzer;

import java.util.ArrayList;
import java.util.Collections;
import org.junit.Test;
import analyzer.BinAnalyzer.BinAnalyzerResults;
import analyzer.ReaderThread.ReaderThreadCash;
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
public class BinAnalyzerTest {
    private static final double EPS = 0.002;
    private static final int LEFT_BARRIER_NUMBER_ELEMENTS = 10000;
    private static final int RIGHT_BARRIER_NUMBER_ELEMENTS = 11000;
     
    public BinAnalyzerTest() {
    }

    private BinfElement newBinfMalloc(T_Ptr arg1, T_Size_t arg2, T_Ptr arg3, T_Long arg4) {
        BinfElement binfElement = new BinfElement();
        binfElement.code_function = BinfElement.FCODE_MALLOC;
        binfElement.count = BinfElement.FCOUNT_MALLOC;
            
        binfElement.types = new byte[BinfElement.FCOUNT_MALLOC];
        binfElement.types[0] = BinfElement.TCODE_PTR;
        binfElement.types[1] = BinfElement.TCODE_SIZE_T;
        binfElement.types[2] = BinfElement.TCODE_PTR;
        binfElement.types[3] = BinfElement.TCODE_LONG;
            
        binfElement.size_of_data = (byte)BinfElement.FSIZE_OF_DATA_MALLOC;
        binfElement.data = new byte[BinfElement.FSIZE_OF_DATA_MALLOC];
        
        int offset = 0;
        //arg1 (Memory pointer)
        System.arraycopy(T_Ptr.ptrToBytes(arg1), 0, binfElement.data, offset, T_Ptr.getSize());
        offset += T_Ptr.getSize();
        //arg2 (Memory size)
        System.arraycopy(T_Size_t.size_tToBytes(arg2), 0, binfElement.data, offset, T_Size_t.getSize());
        offset += T_Size_t.getSize();
        //arg3 (Malloc function pointer)
        System.arraycopy(T_Ptr.ptrToBytes(arg3), 0, binfElement.data, offset, T_Ptr.getSize());
        offset += T_Ptr.getSize();
        //arg4 (Time)
        System.arraycopy(T_Long.longToBytes(arg4), 0, binfElement.data, offset, T_Long.getSize());
        offset += T_Long.getSize();
        
        return binfElement;
    }
    
    private BinfElement newBinfFree(T_Ptr arg1, T_Ptr arg2, T_Long arg3) {
        BinfElement binfElement = new BinfElement();
        binfElement.code_function = BinfElement.FCODE_FREE;
        binfElement.count = BinfElement.FCOUNT_FREE;
            
        binfElement.types = new byte[BinfElement.FCOUNT_FREE];
        binfElement.types[0] = BinfElement.TCODE_PTR;
        binfElement.types[1] = BinfElement.TCODE_PTR;
        binfElement.types[2] = BinfElement.TCODE_LONG;
            
        binfElement.size_of_data = (byte)BinfElement.FSIZE_OF_DATA_FREE;
        binfElement.data = new byte[BinfElement.FSIZE_OF_DATA_FREE];

        int offset = 0;
        //arg1 (Memory pointer)
        System.arraycopy(T_Ptr.ptrToBytes(arg1), 0, binfElement.data, offset, T_Ptr.getSize());
        offset += T_Ptr.getSize();
        //arg2 (Malloc function pointer)
        System.arraycopy(T_Ptr.ptrToBytes(arg2), 0, binfElement.data, offset, T_Ptr.getSize());
        offset += T_Ptr.getSize();
        //arg3 (Time)
        System.arraycopy(T_Long.longToBytes(arg3), 0, binfElement.data, offset, T_Long.getSize());
        offset += T_Long.getSize();
        
        return binfElement;
    }
    
    private BinfElement setOtherTime(BinfElement binfElement, T_Long time) {
        int offset = 0;
        if(binfElement.code_function == BinfElement.FCODE_MALLOC) {
            //arg1 (Memory pointer)
            offset += T_Ptr.getSize();
            //arg2 (Memory size)
            offset += T_Size_t.getSize();
            //arg3 (Malloc function pointer)
            offset += T_Ptr.getSize();
            //arg4 (Time)
            System.arraycopy(T_Long.longToBytes(time), 0, binfElement.data, offset, T_Long.getSize());
            offset += T_Long.getSize();
        } else if(binfElement.code_function == BinfElement.FCODE_FREE) {
            //arg1 (Memory pointer)
            offset += T_Ptr.getSize();
            //arg2 (Malloc function pointer)
            offset += T_Ptr.getSize();
            //arg3 (Time)
            System.arraycopy(T_Long.longToBytes(time), 0, binfElement.data, offset, T_Long.getSize());
            offset += T_Long.getSize();
        }
        return binfElement;
    }
    
    private boolean isValidComputeValues(BinAnalyzerResults results,
            double allmemory_valid_value, double unfreed_memory_valid_value) {     
        String allmemory_series_key = results.GetAllMemoryUsedDescription();
        String unfreed_series_key = results.GetUnfreedMemoryDescription();

        double allmemory_value = Double.valueOf(allmemory_series_key.split(" ")[0]);
        double unfreed_memory_value = Double.valueOf(unfreed_series_key.split(" ")[0]);

        return Math.abs(allmemory_value - allmemory_valid_value) < EPS
                && Math.abs(unfreed_memory_value - unfreed_memory_valid_value) < EPS;
    }

    @Test
    public void testWithoutMemoryLeaksMakeAnalyzeMFree() {
        System.out.println("testWithoutMemoryLeaksMakeAnalyzeMFree");
        
        for(int num_exp = LEFT_BARRIER_NUMBER_ELEMENTS;
                num_exp < RIGHT_BARRIER_NUMBER_ELEMENTS; num_exp++) {
            
            final int size_of_first_partition = num_exp / 2;
            final int size_of_second_partition = num_exp - size_of_first_partition;

            // MAIN VARIABLES //
            double allmemory_value = 0, unfreed_memory_value = 0;

            // TMP VARIABLES //
            long random_size;

            ReaderThreadCash cash = new ReaderThreadCash();
            ArrayList<BinfElement> mainArray = cash.GetInputBinArray();
            ArrayList<BinfElement> tmpArray = new ArrayList(Help.WIN_MB);

            // FIRST PARTIION (MALLOC)
            for(int i = 0; i < size_of_first_partition; i++) {
                random_size = (long)(Math.random() * Help.WIN_MB) + 1;
                allmemory_value += random_size;
                tmpArray.add(newBinfMalloc(new T_Ptr(i), new T_Size_t(random_size),
                        new T_Ptr(-1), new T_Long(-1)));
            }
            Collections.shuffle(tmpArray);

            mainArray.addAll(tmpArray);
            tmpArray.clear();

            // FIRST PARTIION (FREE)
            for(int i = 0; i < size_of_first_partition; i++) {
                tmpArray.add(newBinfFree(new T_Ptr(i), new T_Ptr(-1),
                        new T_Long(-1)));
            }
            Collections.shuffle(tmpArray);

            mainArray.addAll(tmpArray);
            tmpArray.clear();

            // SECOND PARTIION (MALLOC)
            for(int i = 0; i < size_of_second_partition; i++) {
                random_size = (long)(Math.random() * Help.WIN_MB) + 1;
                allmemory_value += random_size;
                tmpArray.add(newBinfMalloc(new T_Ptr(i), new T_Size_t(random_size),
                        new T_Ptr(-1), new T_Long(-1)));
            }
            Collections.shuffle(tmpArray);

            mainArray.addAll(tmpArray);
            tmpArray.clear();

            // SECOND PARTIION (FREE)
            for(int i = 0; i < size_of_second_partition; i++) {
                tmpArray.add(newBinfFree(new T_Ptr(i), new T_Ptr(-1),
                        new T_Long(-1)));
            }
            Collections.shuffle(tmpArray);

            mainArray.addAll(tmpArray);
            tmpArray.clear();

            // SET VALID TIME
            for(int i = 0; i < num_exp * 2; i++) {
                setOtherTime(mainArray.get(i), new T_Long((i + 1) * 1000));
            }

            // CONVERT BYTES TO MEGABYTES
            allmemory_value /= Help.GetNumBytesInMB();
            unfreed_memory_value /= Help.GetNumBytesInMB();

            // TEST
            cash.UpdateUnhandledData();
            BinAnalyzer.MakeAnalyzeMFree(cash);
            BinAnalyzerResults results = cash.GetAnalyzerResults();
            assertTrue(results.GetAllMemoryUsedPoints().size() > 1);
            assertTrue(results.GetUnfreedMemoryPoints().size() > 1);
            assertTrue(isValidComputeValues(results, allmemory_value, unfreed_memory_value));
        }
    }

    @Test
    public void testWithMemoryLeaksMakeAnalyzeMFree() {
        System.out.println("testWithMemoryLeaksMakeAnalyzeMFree");
        
        for(int num_exp = LEFT_BARRIER_NUMBER_ELEMENTS;
                num_exp < RIGHT_BARRIER_NUMBER_ELEMENTS; num_exp++) {
            
            final int size_of_first_partition = (int)(num_exp * 0.75);
            final int size_of_second_partition = num_exp - size_of_first_partition;

            // MAIN VARIABLES //
            double allmemory_value = 0, unfreed_memory_value = 0;

            // TMP VARIABLES //
            long random_size;

            ReaderThreadCash cash = new ReaderThreadCash();
            ArrayList<BinfElement> mainArray = cash.GetInputBinArray();
            ArrayList<BinfElement> tmpArray = new ArrayList(Help.WIN_MB);

            // FIRST PARTIION (MALLOC)
            for(int i = 0; i < size_of_first_partition; i++) {
                random_size = (long)(Math.random() * Help.WIN_MB) + 1;
                allmemory_value += random_size;
                tmpArray.add(newBinfMalloc(new T_Ptr(i), new T_Size_t(random_size),
                        new T_Ptr(-1), new T_Long(-1)));
            }
            Collections.shuffle(tmpArray);

            mainArray.addAll(tmpArray);
            tmpArray.clear();

            // FIRST PARTIION (FREE)
            for(int i = 0; i < size_of_first_partition; i++) {
                tmpArray.add(newBinfFree(new T_Ptr(i), new T_Ptr(-1),
                        new T_Long(-1)));
            }
            Collections.shuffle(tmpArray);

            mainArray.addAll(tmpArray);
            tmpArray.clear();

            // SECOND PARTIION (MALLOC)
            for(int i = 0; i < size_of_second_partition * 2; i++) {
                random_size = (long)(Math.random() * Help.WIN_MB) + 1;
                allmemory_value += random_size;
                unfreed_memory_value += random_size;
                tmpArray.add(newBinfMalloc(new T_Ptr(i), new T_Size_t(random_size),
                        new T_Ptr(-1), new T_Long(-1)));
            }
            //Collections.shuffle(tmpArray);

            mainArray.addAll(tmpArray);
            tmpArray.clear();

            // SET VALID TIME
            for(int i = 0; i < num_exp * 2; i++) {
                setOtherTime(mainArray.get(i), new T_Long((i + 1) * 1000));
            }

            // CONVERT BYTES TO MEGABYTES
            allmemory_value /= Help.GetNumBytesInMB();
            unfreed_memory_value /= Help.GetNumBytesInMB();

            // TEST
            cash.UpdateUnhandledData();
            BinAnalyzer.MakeAnalyzeMFree(cash);
            BinAnalyzerResults results = cash.GetAnalyzerResults();
            assertTrue(results.GetAllMemoryUsedPoints().size() > 1);
            assertTrue(results.GetUnfreedMemoryPoints().size() > 1);
            assertTrue(isValidComputeValues(results, allmemory_value, unfreed_memory_value));
        }
    }
}
