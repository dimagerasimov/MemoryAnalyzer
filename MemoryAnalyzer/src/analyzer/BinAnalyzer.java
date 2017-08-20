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
import common.GlobalVariables;
import crossplatform.Help;
import bintypes.BinfElement;
import fast_chart.XY;
import static bintypes.BinfElement.GetMFreeAddress;
import static bintypes.BinfElement.GetMFreeSize;
import static bintypes.BinfElement.GetMFreeTime;
import static crossplatform.Help.GetNumBytesInKB;
import static crossplatform.Help.GetNumBytesInMB;

/**
 *
 * @author master
 */
public class BinAnalyzer {
    // Maximum points for graphic
    public final static int AVERAGE_NUMBER_POINTS = 250;
    
    public static BinAnalyzerResults MakeAnalyzeMFree(
            ArrayList<BinfElement> binfArray) {
        if(binfArray.isEmpty()) {
            return null;
        }
        AllmemPreGraphicInfo allmemPreGraphicInfo =
                new AllmemPreGraphicInfo();
        ArrayList<XY<Long>> allmem_mas = GetAllMemoryPreGraphic(binfArray, allmemPreGraphicInfo);
        UnfreedPreGraphicInfo unfreedPreGraphicInfo =
                new UnfreedPreGraphicInfo();
        ArrayList<XY<Long>> unfreed_mas = GetUnfreedPreGraphic(allmemPreGraphicInfo,
                unfreedPreGraphicInfo);
        
        // Make equals last values
        ArrayList<XY<Float>> repack_allmem_mas = RepackAndNormXYArrayList(allmem_mas);
        ArrayList<XY<Float>> repack_unfreed_mas = RepackAndNormXYArrayList(unfreed_mas);
        if(!repack_allmem_mas.isEmpty() && !repack_unfreed_mas.isEmpty()) {
            AlignmentXYArrayLists(repack_allmem_mas, repack_unfreed_mas);
        }

        allmem_mas.clear();
        unfreed_mas.clear();

        BinAnalyzerResults binAnalyzerResult = new BinAnalyzerResults();
        binAnalyzerResult.allMemoryUsedDescription = GenerateDescription(allmemPreGraphicInfo.freed_sum_abs);
        binAnalyzerResult.allMemoryUsedPoints = repack_allmem_mas;
        binAnalyzerResult.unfreedMemoryDescription = GenerateDescription(unfreedPreGraphicInfo.unfreed_sum);
        binAnalyzerResult.unfreedMemoryPoints = repack_unfreed_mas; 
        return binAnalyzerResult;
    }

    public static class BinAnalyzerResults
    {
        public String allMemoryUsedDescription;
        public ArrayList<XY<Float>> allMemoryUsedPoints;
        public String unfreedMemoryDescription;
        public ArrayList<XY<Float>> unfreedMemoryPoints;
    };
    // My parameters for return
    private static class AllmemPreGraphicInfo {
        long freed_sum_level;
        long freed_sum_abs;
        long max_time;
        HashMap<Long, BinfElement> allmemHashMap;
    };
    // My parameters for return
    private static class UnfreedPreGraphicInfo {
        long unfreed_sum;
    };
    
    private static String GenerateDescription(long value) {
        String result;
        if(value < GetNumBytesInKB()) {
            result = String.format("%.0f", (float)value) + " bytes";
        }
        else if(value < GetNumBytesInMB()) {
            result = String.format("%.0f", (float)(value) / GetNumBytesInKB()) + " KB";
        }
        else {
            result = String.format("%.3f", (float)(value) / GetNumBytesInMB()) + " MB";
        }
        return result;
    }
    private static ArrayList<XY<Float>> RepackAndNormXYArrayList(ArrayList<XY<Long>> list) {
        float coefficientXToNorm = Help.USEC_IN_SEC;
        float coefficientYToNorm = GetNumBytesInMB();
        ArrayList<XY<Float>> repack_list = new ArrayList<>(list.size() + 1);
        if(list.size() > 1)
        {
            for(XY<Long> xy : list)
            {
                repack_list.add(new XY<>(
                    (float)(xy.x / coefficientXToNorm), (float)(xy.y / coefficientYToNorm)));

            }
        }
        return repack_list;
    }
    private static void AlignmentXYArrayLists(ArrayList<XY<Float>> allmem_mas,
            ArrayList<XY<Float>> unfreed_mas) {
        if(allmem_mas.isEmpty() || unfreed_mas.isEmpty()) {
            return;
        }
        // Make equals last values
        if(allmem_mas.get(allmem_mas.size() - 1).x
                > unfreed_mas.get(unfreed_mas.size() - 1).x) {
            unfreed_mas.add(new XY(allmem_mas.get(allmem_mas.size() - 1).x,
                unfreed_mas.get(unfreed_mas.size() - 1).y));
        }
        allmem_mas.set(allmem_mas.size() - 1, new XY(unfreed_mas.get(unfreed_mas.size() - 1).x,
                unfreed_mas.get(unfreed_mas.size() - 1).y));
    }
    private static ArrayList<XY<Long>> GetAllMemoryPreGraphic(ArrayList<BinfElement> binfArray,
            AllmemPreGraphicInfo allmemPreGraphicInfo) {
        int tmp_count;
        long tmp_long_key, tmp_sum, value_sum;
        long current_time, tmp_time, step_graphic_time;
        // IMPORTANT!!!!!!!!!
        long freed_sum_level = 0;
        long freed_sum_abs = 0;
        // IMPORTANT
        /////////////
        BinfElement retBinfElement;
        HashMap<Long, BinfElement> allmemHashMap = new HashMap(Help.WIN_MB);
        // Allocate memory
        ArrayList<XY<Long>> allmem_mas = new ArrayList(AVERAGE_NUMBER_POINTS);

        long startTimeToAdd = GetMFreeTime(
                binfArray.get(binfArray.size() - 1)).getValue()
                - (long)(GlobalVariables.g_TimelinePeriodMilisec) * 1000;
        if(startTimeToAdd <= 0)
        {
            startTimeToAdd = 0;
            //First value
            allmem_mas.add(new XY(startTimeToAdd, freed_sum_level));
        }
        if(binfArray.size() < AVERAGE_NUMBER_POINTS) {
            for(BinfElement tmpBinfElement : binfArray)
            {
                tmp_long_key = GetMFreeAddress(tmpBinfElement).getValue();
                retBinfElement = allmemHashMap.put(tmp_long_key, tmpBinfElement);
                value_sum = GetMFreeSize(tmpBinfElement).getValue();
                if(value_sum != -1) {
                    freed_sum_abs += value_sum;
                }
                else {
                    if(retBinfElement == null) {
                        // otherwise, the error in the window application
                        continue;
                    }
                    value_sum = -GetMFreeSize(retBinfElement).getValue();
                    allmemHashMap.remove(tmp_long_key);
                }
                freed_sum_level += value_sum;
                current_time = GetMFreeTime(tmpBinfElement).getValue();
                if(current_time > startTimeToAdd)
                {
                    allmem_mas.add(new XY(current_time, freed_sum_level));
                }
            }
        } else {
            long timeToAverage = GlobalVariables.g_TimelinePeriodMilisec * 1000;
            if(startTimeToAdd == 0)
            {
                timeToAverage = GetMFreeTime(binfArray.get(binfArray.size() - 1)).getValue();
            }
            step_graphic_time = (long)(timeToAverage / AVERAGE_NUMBER_POINTS);
            tmp_count = 0;
            tmp_sum = 0;
            tmp_time = current_time = 0;
            for(BinfElement tmpBinfElement : binfArray)
            {
                tmp_long_key = GetMFreeAddress(tmpBinfElement).getValue();
                retBinfElement = allmemHashMap.put(tmp_long_key, tmpBinfElement);
                value_sum = GetMFreeSize(tmpBinfElement).getValue();
                if(value_sum != -1) {
                    freed_sum_abs += value_sum;
                }
                else {
                    if(retBinfElement == null) {
                        // otherwise, the error in the window application
                        continue;
                    }
                    value_sum = -GetMFreeSize(retBinfElement).getValue();
                    allmemHashMap.remove(tmp_long_key);
                }
                freed_sum_level += value_sum;
                tmp_sum += freed_sum_level;
                tmp_count += 1;
                current_time = GetMFreeTime(tmpBinfElement).getValue();
                if((current_time - tmp_time > step_graphic_time) && (current_time > startTimeToAdd)) {
                    tmp_time += step_graphic_time * (int)(current_time - tmp_time) / step_graphic_time;
                    allmem_mas.add(new XY((current_time + tmp_time) / 2,
                            tmp_sum / tmp_count));
                    tmp_time += step_graphic_time;
                    tmp_sum = 0;
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
    private static ArrayList<XY<Long>> GetUnfreedPreGraphic(AllmemPreGraphicInfo allmemPreGraphicInfo,
            UnfreedPreGraphicInfo unfreedPreGraphicInfo) {
        long value_sum;
        long current_time, tmp_time, step_graphic_time;
        // IMPORTANT
        long unfreed_sum = 0;
        // Extract usable info from AllmemPreGraphicInfo
        HashMap<Long, BinfElement> allmemHashMap = allmemPreGraphicInfo.allmemHashMap;
        // Allocate memory
        ArrayList<XY<Long>> unfreed_mas = new ArrayList(AVERAGE_NUMBER_POINTS);
        // Get values of collection that put their in TreeMap
        Collection<BinfElement> allmemCollection = allmemHashMap.values();
        TreeMap<Long, BinfElement> unfreedMap = new TreeMap();
        for( BinfElement tmpBinfElement : allmemCollection ) {
            unfreedMap.put(GetMFreeTime(tmpBinfElement).getValue(), tmpBinfElement);
        }
        Collection<BinfElement> unfreedCollection = unfreedMap.values();
        
        long startTimeToAdd = allmemPreGraphicInfo.max_time
                - (long)(GlobalVariables.g_TimelinePeriodMilisec) * 1000;
        if(startTimeToAdd <= 0)
        {
            startTimeToAdd = 0;
            // First value
            unfreed_mas.add(new XY(startTimeToAdd, unfreed_sum));
        }
        if(unfreedCollection.size() < AVERAGE_NUMBER_POINTS) {
            for( BinfElement tmpBinfElement : unfreedCollection ) {
                value_sum = GetMFreeSize(tmpBinfElement).getValue();
                unfreed_sum += value_sum;
                current_time = GetMFreeTime(tmpBinfElement).getValue();
                if(current_time > startTimeToAdd)
                {
                    unfreed_mas.add(new XY(current_time, unfreed_sum));
                }
            }
        }
        else {
            long timeToAverage = GlobalVariables.g_TimelinePeriodMilisec * 1000;
            if(startTimeToAdd == 0)
            {
                timeToAverage = allmemPreGraphicInfo.max_time;
            }
            step_graphic_time = timeToAverage / AVERAGE_NUMBER_POINTS;
            tmp_time = 0;
            for( BinfElement tmpBinfElement : unfreedCollection ) {
                value_sum = GetMFreeSize(tmpBinfElement).getValue();
                unfreed_sum += value_sum;
                current_time = GetMFreeTime(tmpBinfElement).getValue();

                if((current_time - tmp_time > step_graphic_time) && (current_time > startTimeToAdd)) {
                    tmp_time += step_graphic_time * (int)(current_time - tmp_time) / step_graphic_time;
                    unfreed_mas.add(new XY((current_time + tmp_time) / 2, unfreed_sum));
                    tmp_time += step_graphic_time;
                }
            }
        }
        unfreedPreGraphicInfo.unfreed_sum = unfreed_sum;
        
        return unfreed_mas;
    }

}
