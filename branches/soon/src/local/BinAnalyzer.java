/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

import java.util.ArrayList;
import java.util.HashMap;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.TableXYDataset;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import bintypes.BinfElement;
import static bintypes.BinfElement.GetMFreeAddress;
import static bintypes.BinfElement.GetMFreeSize;
import static bintypes.BinfElement.GetMFreeTime;
import static crossplatform.Help.GetNumBytesInMb;

/**
 *
 * @author master
 */
public class BinAnalyzer {
    private static void AlignmentXYArrayList(ArrayList<XYDataItem> firstArray,
            ArrayList<XYDataItem> secondArray) {
        if(firstArray.isEmpty() || secondArray.isEmpty()) {
            return;
        }
        // Make equals last values
        XYDataItem last_first_array = firstArray.get(firstArray.size() - 1);
        XYDataItem last_second_array = secondArray.get(secondArray.size() - 1);
        if(last_first_array.getXValue() < last_second_array.getXValue()) {
            firstArray.add(new XYDataItem(
                    last_second_array.getXValue(), last_first_array.getYValue()));
        }
        else if (last_first_array.getXValue() > last_second_array.getXValue()) {
            secondArray.add(new XYDataItem(
                    last_first_array.getXValue(), last_second_array.getYValue()));
        }
    }
    public static TableXYDataset GetAnalyzeMFree(
            ArrayList<BinfElement> binfArray) {
        long tmp_long_key, time; 
        double value_sum;
        BinfElement retBinfElement;
        HashMap<Long, BinfElement> memoryMap = new HashMap<>();

        ArrayList<XYDataItem> allmem_mas = new ArrayList();
        // First value
        allmem_mas.add(new XYDataItem(0.0, 0.0));
        double freed_sum_level = 0.0;
        double freed_sum_abs = 0.0;
        for(BinfElement tmpBinfElement : binfArray)
        {
            tmp_long_key = GetMFreeAddress(tmpBinfElement).getValue();
            retBinfElement = memoryMap.put(tmp_long_key, tmpBinfElement);
            value_sum = GetMFreeSize(tmpBinfElement).getValue();
            if(value_sum == -1) {
                value_sum = -GetMFreeSize(retBinfElement).getValue();
            }
            else {
                freed_sum_abs += (double)value_sum / GetNumBytesInMb();
            }
            freed_sum_level += (double)value_sum / GetNumBytesInMb();
            
            time = GetMFreeTime(tmpBinfElement).getValue();
            allmem_mas.add(new XYDataItem(time / 1000.0, freed_sum_level));//Add time in ms
        } 

        ArrayList<XYDataItem> unfreed_mas = new ArrayList();
        // First value
        unfreed_mas.add(new XYDataItem(0.0, 0.0));             
        double unfreed_sum = 0.0;
        for(BinfElement tmpBinfElement : binfArray)
        {
            tmp_long_key = GetMFreeAddress(tmpBinfElement).getValue();
            retBinfElement = memoryMap.get(tmp_long_key);
            if(retBinfElement != null) {
                value_sum = GetMFreeSize(retBinfElement).getValue();
                if(value_sum != -1) {
                    unfreed_sum += (double)value_sum / GetNumBytesInMb();
                    time = GetMFreeTime(retBinfElement).getValue();
                    unfreed_mas.add(new XYDataItem(time / 1000.0, unfreed_sum));//Add time in ms
                }
                memoryMap.remove(tmp_long_key);
            }
        }
        // Make equals last values 
        AlignmentXYArrayList(unfreed_mas, allmem_mas);
        
        XYSeries unfreedMemory = new XYSeries("Unfreed memory (" +
                String.format("%.3f", unfreed_sum)
                    + " mb)", false, false);
        for (XYDataItem iter : unfreed_mas) {
            unfreedMemory.add(iter);
        }     
        XYSeries allMemory = new XYSeries("All memory (" +
                String.format("%.3f", freed_sum_abs + unfreed_sum) +
                    " mb)",false, false);
        for (XYDataItem iter : allmem_mas) {
            allMemory.add(iter);
        }  
        
        // Clean all
        memoryMap.clear();
        unfreed_mas.clear();
        allmem_mas.clear();
        
        DefaultTableXYDataset xyDataset = new DefaultTableXYDataset();
        xyDataset.addSeries(unfreedMemory);
        xyDataset.addSeries(allMemory);
        return xyDataset;
    }
}
