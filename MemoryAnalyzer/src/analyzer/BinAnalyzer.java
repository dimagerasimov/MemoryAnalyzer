/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package analyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collection;
import java.util.TreeMap;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.TableXYDataset;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import crossplatform.Help;
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
    // Maximum points for graphic
    public final static int AVERAGE_NUMBER_POINTS = 250;
    
    public static TableXYDataset MakeAnalyzeMFree(
            ArrayList<BinfElement> binfArray) {
        if(binfArray.isEmpty()) {
            return null;
        }
        AllmemPreGraphicInfo allmemPreGraphicInfo =
                new AllmemPreGraphicInfo();
        ArrayList<XY> allmem_mas = GetAllMemoryPreGraphic(binfArray, allmemPreGraphicInfo);
        UnfreedPreGraphicInfo unfreedPreGraphicInfo =
                new UnfreedPreGraphicInfo();
        ArrayList<XY> unfreed_mas = GetUnfreedPreGraphic(allmemPreGraphicInfo,
                unfreedPreGraphicInfo);
        
        // Make equals last values 
        AlignmentXYArrayList(allmem_mas, unfreed_mas);
        
        XYSeries allMemory = GetAllmemGraphic(allmem_mas, allmemPreGraphicInfo);
        XYSeries unfreedMemory = GetUnfreedGraphic(unfreed_mas, unfreedPreGraphicInfo);
        
        DefaultTableXYDataset xyDataset = new DefaultTableXYDataset();
        xyDataset.addSeries(unfreedMemory);
        xyDataset.addSeries(allMemory);
        return xyDataset;
    }
    // My xy for optimization
    private static class XY {
        public double x;
        public double y;
        public XY(double x, double y) {
            this.x = x; this.y = y;
        }
    };
    // My parameters for return
    private static class AllmemPreGraphicInfo {
        double freed_sum_level;
        double freed_sum_abs;
        double max_time;
        HashMap<Long, BinfElement> allmemHashMap;
    };
    // My parameters for return
    private static class UnfreedPreGraphicInfo {
        double unfreed_sum;
    };
    private static void AlignmentXYArrayList(ArrayList<XY> allmem_mas,
            ArrayList<XY> unfreed_mas) {
        if(allmem_mas.isEmpty() || unfreed_mas.isEmpty()) {
            return;
        }
        // Make equals last values
        if(allmem_mas.get(allmem_mas.size() - 1).x
                > unfreed_mas.get(unfreed_mas.size() - 1).x) {
            unfreed_mas.add(new XY(allmem_mas.get(allmem_mas.size() - 1).x + TIME_EPS,
                unfreed_mas.get(unfreed_mas.size() - 1).y));
        }
        allmem_mas.set(allmem_mas.size() - 1, new XY(unfreed_mas.get(unfreed_mas.size() - 1).x,
                unfreed_mas.get(unfreed_mas.size() - 1).y));
    }
    private static ArrayList<XY> GetAllMemoryPreGraphic(ArrayList<BinfElement> binfArray,
            AllmemPreGraphicInfo allmemPreGraphicInfo) {
        int tmp_count;
        long tmp_long_key;
        double tmp_sum, value_sum;
        double current_time, tmp_time, step_graphic_time;
        // IMPORTANT!!!!!!!!!
        double freed_sum_level = 0.0;
        double freed_sum_abs = 0.0;
        // IMPORTANT
        /////////////
        BinfElement retBinfElement;
        HashMap<Long, BinfElement> allmemHashMap = new HashMap(Help.WIN_MB);
        // Allocate memory
        ArrayList<XY> allmem_mas = new ArrayList(AVERAGE_NUMBER_POINTS);
        // First value
        allmem_mas.add(new XY(0.0, freed_sum_level));

        if(binfArray.size() < AVERAGE_NUMBER_POINTS) {
            for(BinfElement tmpBinfElement : binfArray)
            {
                tmp_long_key = GetMFreeAddress(tmpBinfElement).getValue();
                retBinfElement = allmemHashMap.put(tmp_long_key, tmpBinfElement);
                value_sum = GetMFreeSize(tmpBinfElement).getValue();
                if(value_sum != -1) {
                    freed_sum_abs += (double)value_sum / GetNumBytesInMb();
                }
                else {
                    if(retBinfElement == null) {
                        // otherwise, the error in the window application
                        continue;
                    }
                    value_sum = -GetMFreeSize(retBinfElement).getValue();
                    allmemHashMap.remove(tmp_long_key);
                }
                freed_sum_level += (double)value_sum / GetNumBytesInMb();
                current_time = (double)GetMFreeTime(tmpBinfElement).getValue() / Help.USEC_IN_SEC;
                allmem_mas.add(new XY(current_time, freed_sum_level));
            }
        } else {
            step_graphic_time = (double)GetMFreeTime(binfArray.get(
                    binfArray.size() - 1)).getValue() / (Help.USEC_IN_SEC * AVERAGE_NUMBER_POINTS);
            tmp_count = 0;
            tmp_time = 0.0;
            tmp_sum = 0.0;
            current_time = 0.0;
            for(BinfElement tmpBinfElement : binfArray)
            {
                tmp_long_key = GetMFreeAddress(tmpBinfElement).getValue();
                retBinfElement = allmemHashMap.put(tmp_long_key, tmpBinfElement);
                value_sum = GetMFreeSize(tmpBinfElement).getValue();
                if(value_sum != -1) {
                    freed_sum_abs += (double)value_sum / GetNumBytesInMb();
                }
                else {
                    if(retBinfElement == null) {
                        // otherwise, the error in the window application
                        continue;
                    }
                    value_sum = -GetMFreeSize(retBinfElement).getValue();
                    allmemHashMap.remove(tmp_long_key);
                }
                freed_sum_level += (double)value_sum / GetNumBytesInMb();
                tmp_sum += freed_sum_level;
                tmp_count += 1;
                current_time = (double)GetMFreeTime(tmpBinfElement).getValue() / Help.USEC_IN_SEC;
                if(current_time - tmp_time > step_graphic_time) {
                    tmp_time += step_graphic_time * (int)(current_time - tmp_time) / step_graphic_time;
                    allmem_mas.add(new XY((current_time + tmp_time) / 2.0,
                            tmp_sum / tmp_count));
                    tmp_time += step_graphic_time;
                    tmp_sum = 0.0;
                    tmp_count = 0;
                }
            }
        }
        allmemPreGraphicInfo.freed_sum_abs = freed_sum_abs;
        allmemPreGraphicInfo.freed_sum_level = freed_sum_level;
        allmemPreGraphicInfo.max_time = GetMFreeTime(binfArray.get(
                binfArray.size() - 1)).getValue();
        allmemPreGraphicInfo.allmemHashMap = allmemHashMap;
        
        return allmem_mas;
    }
    private static ArrayList<XY> GetUnfreedPreGraphic(AllmemPreGraphicInfo allmemPreGraphicInfo,
            UnfreedPreGraphicInfo unfreedPreGraphicInfo) {
        double value_sum;
        double current_time, tmp_time, step_graphic_time;
        // IMPORTANT
        double unfreed_sum = 0.0;
        // Extract usable info from AllmemPreGraphicInfo
        HashMap<Long, BinfElement> allmemHashMap = allmemPreGraphicInfo.allmemHashMap;
        // Allocate memory
        ArrayList<XY> unfreed_mas = new ArrayList(AVERAGE_NUMBER_POINTS);
        // First value
        unfreed_mas.add(new XY(0.0, unfreed_sum));
        // Get values of collection that put their in TreeMap
        Collection<BinfElement> allmemCollection = allmemHashMap.values();
        TreeMap<Long, BinfElement> unfreedMap = new TreeMap();
        for( BinfElement tmpBinfElement : allmemCollection ) {
            unfreedMap.put(GetMFreeTime(tmpBinfElement).getValue(), tmpBinfElement);
        }
        Collection<BinfElement> unfreedCollection = unfreedMap.values();
        if(unfreedCollection.size() < AVERAGE_NUMBER_POINTS) {
            for( BinfElement tmpBinfElement : unfreedCollection ) {
                value_sum = (double)GetMFreeSize(tmpBinfElement).getValue() / GetNumBytesInMb();
                unfreed_sum += value_sum;
                current_time = (double)GetMFreeTime(tmpBinfElement).getValue() / Help.USEC_IN_SEC;
                unfreed_mas.add(new XY(current_time, unfreed_sum));
            }
        }
        else {
            step_graphic_time = (double)allmemPreGraphicInfo.max_time / (Help.USEC_IN_SEC * AVERAGE_NUMBER_POINTS);
            tmp_time = 0.0;
            for( BinfElement tmpBinfElement : unfreedCollection ) {
                value_sum = (double)GetMFreeSize(tmpBinfElement).getValue() / GetNumBytesInMb();
                unfreed_sum += value_sum;
                current_time = (double)GetMFreeTime(tmpBinfElement).getValue() / Help.USEC_IN_SEC;

                if(current_time - tmp_time > step_graphic_time) {
                    tmp_time += step_graphic_time * (int)(current_time - tmp_time) / step_graphic_time;
                    unfreed_mas.add(new XY((current_time + tmp_time) / 2.0, unfreed_sum));
                    tmp_time += step_graphic_time;
                }
            }
        }
        unfreedPreGraphicInfo.unfreed_sum = unfreed_sum;
        
        return unfreed_mas;
    }
    private static XYSeries GetAllmemGraphic(ArrayList<XY> allmem_mas,
            AllmemPreGraphicInfo allmemPreGraphicInfo) {
        double freed_sum_abs = allmemPreGraphicInfo.freed_sum_abs;
        XYSeries allMemory = new XYSeries("All memory (" +
                String.format("%.3f", freed_sum_abs) +
                    " mb)", false, false);   
        
        // Default step
        for(XY xy : allmem_mas) {
            allMemory.add(new XYDataItem(xy.x, xy.y));
        }      
        return allMemory;
    }
    private static XYSeries GetUnfreedGraphic(ArrayList<XY> unfreed_mas,
            UnfreedPreGraphicInfo unfreedPreGraphicInfo) {
        double unfreed_sum = unfreedPreGraphicInfo.unfreed_sum;
        XYSeries unfreedMemory = new XYSeries("Unfreed memory (" +
                String.format("%.3f", unfreed_sum) + " mb)", false, false);

        // Default step
        for(XY xy : unfreed_mas) {
            unfreedMemory.add(new XYDataItem(xy.x, xy.y));
        }   
        return unfreedMemory;
    }
    
    // Private variables
    // My time offset for graphic
    private final static double TIME_EPS = (1.0 / Help.USEC_IN_SEC) / 8.0;
}
