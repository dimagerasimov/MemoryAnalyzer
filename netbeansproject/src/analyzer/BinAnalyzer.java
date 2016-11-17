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
    // My time offset for graphic
    private final static double TIME_EPS = (1.0 / Help.MSEC_IN_SEC) / 8.0;
    // Maximum points for graphic
    private final static int MAX_POINTS = 500;
    
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
        double max_allmem_derivative;
        double scalar_coefficient;
        HashMap<Long, BinfElement> allmemHashMap;
    };
    // My parameters for return
    private static class UnfreedPreGraphicInfo {
        double unfreed_sum;
    };
    
    private static void AlignmentXYArrayList(XY[] firstArray,
            XY[] secondArray) {
        if(firstArray.length == 0 || secondArray.length == 0) {
            return;
        }
        // Make equals last values
        if(firstArray[firstArray.length - 2].x
                < secondArray[secondArray.length - 2].x) {
            secondArray[secondArray.length - 1] = new XY(
                    secondArray[secondArray.length - 2].x + TIME_EPS,
                    secondArray[secondArray.length - 2].y);
            firstArray[firstArray.length - 1] = new XY(
                    secondArray[secondArray.length - 1].x,
                    secondArray[secondArray.length - 1].y);
        }
        else {
            firstArray[firstArray.length - 1] = new XY(
                    firstArray[firstArray.length - 2].x + TIME_EPS,
                    firstArray[firstArray.length - 2].y);
            secondArray[secondArray.length - 1] = new XY(
                    firstArray[firstArray.length - 1].x,
                    firstArray[firstArray.length - 1].y);
        }
    }
    private static XY[] GetAllMemoryPreGraphic(ArrayList<BinfElement> binfArray,
            AllmemPreGraphicInfo allmemPreGraphicInfo) {
        int count = 0;
        long tmp_long_key;
        double time, value_sum, tmp_value_time;
        // IMPORTANT!!!!!!!!!
        double freed_sum_level = 0.0;
        double freed_sum_abs = 0.0;
        // IMPORTANT
        double tmp_derivative;
        double max_allmem_derivative = 0.0;
        /////////////
        BinfElement retBinfElement;
        HashMap<Long, BinfElement> allmemHashMap = new HashMap(Help.WIN_MB);
        // Allocate memory
        XY[] allmem_mas = new XY[binfArray.size() + 2];
        // First value
        allmem_mas[count] = new XY(0.0, freed_sum_level);
        for(BinfElement tmpBinfElement : binfArray)
        {
            tmp_long_key = GetMFreeAddress(tmpBinfElement).getValue();
            retBinfElement = allmemHashMap.put(tmp_long_key, tmpBinfElement);
            value_sum = GetMFreeSize(tmpBinfElement).getValue();
            if(value_sum != -1) {
                freed_sum_abs += (double)value_sum / GetNumBytesInMb();
            }
            else {
                value_sum = -GetMFreeSize(retBinfElement).getValue();
                allmemHashMap.remove(tmp_long_key);
            }
            freed_sum_level += (double)value_sum / GetNumBytesInMb();
            
            time = GetMFreeTime(tmpBinfElement).getValue();
            tmp_value_time = time / 1000.0;
            tmp_derivative = Math.abs(allmem_mas[count].y - freed_sum_level) /
                    (tmp_value_time - allmem_mas[count].x);
            if(tmp_derivative > max_allmem_derivative) {
                max_allmem_derivative = tmp_derivative;
            }
            allmem_mas[++count] = new XY(tmp_value_time, freed_sum_level);
        }
        allmemPreGraphicInfo.freed_sum_abs = freed_sum_abs;
        allmemPreGraphicInfo.freed_sum_level = freed_sum_level;
        allmemPreGraphicInfo.max_allmem_derivative = max_allmem_derivative;
        allmemPreGraphicInfo.scalar_coefficient = allmem_mas.length;
        allmemPreGraphicInfo.allmemHashMap = allmemHashMap;
        
        return allmem_mas;
    }
    private static XY[] GetUnfreedPreGraphic(AllmemPreGraphicInfo allmemPreGraphicInfo,
            UnfreedPreGraphicInfo unfreedPreGraphicInfo) {
        int count = 0;
        double time, value_sum;
        // IMPORTANT
        double unfreed_sum = 0.0;
        // Extract usable info from AllmemPreGraphicInfo
        HashMap<Long, BinfElement> allmemHashMap = allmemPreGraphicInfo.allmemHashMap;
        // Allocate memory
        XY[] unfreed_mas = new XY[allmemHashMap.size() + 2];
        // First value
        unfreed_mas[count] = new XY(0.0, unfreed_sum);
        // Get values of collection that put their in TreeMap
        Collection<BinfElement> allmemCollection = allmemHashMap.values();
        TreeMap<Long, BinfElement> unfreedMap = new TreeMap();
        for( BinfElement tmpBinfElement : allmemCollection ) {
            unfreedMap.put(GetMFreeTime(tmpBinfElement).getValue(), tmpBinfElement);
        }
        Collection<BinfElement> unfreedCollection = unfreedMap.values();
        for( BinfElement tmpBinfElement : unfreedCollection ) {
            value_sum = GetMFreeSize(tmpBinfElement).getValue();
            unfreed_sum += (double)value_sum / GetNumBytesInMb();
            time = GetMFreeTime(tmpBinfElement).getValue();
            unfreed_mas[++count] = new XY(time / 1000.0, unfreed_sum);   
        }
        unfreedPreGraphicInfo.unfreed_sum = unfreed_sum;
        
        return unfreed_mas;
    }
    private static XYSeries GetAllmemGraphic(XY[] allmem_mas,
            AllmemPreGraphicInfo allmemPreGraphicInfo) {
        double value, value4Accept, tmp_derivative;
        double freed_sum_abs = allmemPreGraphicInfo.freed_sum_abs;
        double max_allmem_derivative = allmemPreGraphicInfo.max_allmem_derivative;
        double scalar_coefficient = allmemPreGraphicInfo.scalar_coefficient;
        XYSeries allMemory = new XYSeries("All memory (" +
                String.format("%.3f", freed_sum_abs) +
                    " mb)", false, false);   
        value = Math.log(MAX_POINTS) / Math.log(scalar_coefficient);
        if(value >= 1.0) {
            value4Accept = 0.0;
        }
        else {
            value4Accept = max_allmem_derivative * (1.0 - value);
        }
        
        //Allocate a new array to select random points
        ArrayList<XY> filter_allmem_mas = new ArrayList(MAX_POINTS);
        
        filter_allmem_mas.add(new XY(allmem_mas[0].x, allmem_mas[0].y));
        for(int i = 1; i < allmem_mas.length - 1; i++) {
            tmp_derivative = Math.abs(allmem_mas[i].y - allmem_mas[i - 1].y) /
                    (allmem_mas[i].x - allmem_mas[i - 1].x);
            if(tmp_derivative > value4Accept) {
                filter_allmem_mas.add(new XY(allmem_mas[i].x - TIME_EPS,
                        allmem_mas[i - 1].y));
                filter_allmem_mas.add(new XY(allmem_mas[i].x, allmem_mas[i].y));
            }
        }
        filter_allmem_mas.add(new XY(allmem_mas[allmem_mas.length - 1].x - TIME_EPS / 2.0,
                filter_allmem_mas.get(filter_allmem_mas.size() - 1).y));
        filter_allmem_mas.add(new XY(allmem_mas[allmem_mas.length - 1].x,
                allmem_mas[allmem_mas.length - 1].y));
        
        // Default step
        double step = 1.0, cur_index = 1.0;
        if(filter_allmem_mas.size() > MAX_POINTS) {
            step = (double)filter_allmem_mas.size() / MAX_POINTS;
        }
        allMemory.add(new XYDataItem(filter_allmem_mas.get(0).x,
                filter_allmem_mas.get(0).y));
        while((int)cur_index < filter_allmem_mas.size() - 1) {
            allMemory.add(new XYDataItem(filter_allmem_mas.get((int)cur_index).x,
                    filter_allmem_mas.get((int)cur_index).y));
            cur_index += step;
        }
        allMemory.add(new XYDataItem(filter_allmem_mas.get(filter_allmem_mas.size() - 1).x,
                filter_allmem_mas.get(filter_allmem_mas.size() - 1).y));
        
        filter_allmem_mas.clear();
        
        return allMemory;
    }
    private static XYSeries GetUnfreedGraphic(XY[] unfreed_mas,
            AllmemPreGraphicInfo allmemPreGraphicInfo,
            UnfreedPreGraphicInfo unfreedPreGraphicInfo) {
        double value, value4Accept;
        double tmp_derivative;
        double max_allmem_derivative = allmemPreGraphicInfo.max_allmem_derivative;
        double unfreed_sum = unfreedPreGraphicInfo.unfreed_sum;
        double scalar_coefficient = allmemPreGraphicInfo.scalar_coefficient;
        XYSeries unfreedMemory = new XYSeries("Unfreed memory (" +
                String.format("%.3f", unfreed_sum) + " mb)", false, false);

        value = Math.log(MAX_POINTS) / Math.log(scalar_coefficient);
        if(value >= 1.0) {
            value4Accept = 0.0;
        }
        else {
            value4Accept = max_allmem_derivative * (1.0 - value);
        }
        
        //Allocate a new array to select random points
        ArrayList<XY> filter_unfreed_mas = new ArrayList(MAX_POINTS);
        
        filter_unfreed_mas.add(new XY(unfreed_mas[0].x, unfreed_mas[0].y));
        for(int i = 1; i < unfreed_mas.length - 1; i++) {
            tmp_derivative = Math.abs(unfreed_mas[i].y - unfreed_mas[i - 1].y) /
                    (unfreed_mas[i].x - unfreed_mas[i - 1].x);
            if(tmp_derivative > value4Accept) {
                filter_unfreed_mas.add(new XY(unfreed_mas[i].x - TIME_EPS,
                    filter_unfreed_mas.get(filter_unfreed_mas.size() - 1).y));
                filter_unfreed_mas.add(new XY(unfreed_mas[i].x, unfreed_mas[i].y));
            }
        }
        filter_unfreed_mas.add(new XY(unfreed_mas[unfreed_mas.length - 1].x - TIME_EPS / 2.0,
                filter_unfreed_mas.get(filter_unfreed_mas.size() - 1).y));
        filter_unfreed_mas.add(new XY(unfreed_mas[unfreed_mas.length - 1].x,
                unfreed_mas[unfreed_mas.length - 1].y));
        
        // Default step
        double step = 1.0, cur_index = 1.0;
        if(filter_unfreed_mas.size() > MAX_POINTS) {
            step = (double)filter_unfreed_mas.size() / MAX_POINTS;
        }
        unfreedMemory.add(new XYDataItem(filter_unfreed_mas.get(0).x,
                filter_unfreed_mas.get(0).y));
        while((int)cur_index < filter_unfreed_mas.size() - 1) {
            unfreedMemory.add(new XYDataItem(filter_unfreed_mas.get((int)cur_index).x,
                    filter_unfreed_mas.get((int)cur_index).y));
            cur_index += step;
        }
        unfreedMemory.add(new XYDataItem(filter_unfreed_mas.get(filter_unfreed_mas.size() - 1).x,
                filter_unfreed_mas.get(filter_unfreed_mas.size() - 1).y));
        
        filter_unfreed_mas.clear();

        return unfreedMemory;
    }
    public static TableXYDataset MakeAnalyzeMFree(
            ArrayList<BinfElement> binfArray) {
        AllmemPreGraphicInfo allmemPreGraphicInfo =
                new AllmemPreGraphicInfo();
        XY[] allmem_mas = GetAllMemoryPreGraphic(binfArray, allmemPreGraphicInfo);
        UnfreedPreGraphicInfo unfreedPreGraphicInfo =
                new UnfreedPreGraphicInfo();
        XY[] unfreed_mas = GetUnfreedPreGraphic(allmemPreGraphicInfo,
                unfreedPreGraphicInfo);
        
        // Make equals last values 
        AlignmentXYArrayList(allmem_mas, unfreed_mas);
        
        XYSeries allMemory = GetAllmemGraphic(allmem_mas, allmemPreGraphicInfo);
        XYSeries unfreedMemory = GetUnfreedGraphic(unfreed_mas, allmemPreGraphicInfo,
                unfreedPreGraphicInfo);
        
        DefaultTableXYDataset xyDataset = new DefaultTableXYDataset();
        xyDataset.addSeries(unfreedMemory);
        xyDataset.addSeries(allMemory);
        return xyDataset;
    }
}
