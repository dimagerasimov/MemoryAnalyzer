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
import analyzer.ReaderThread.ReaderThreadCash;
import static bintypes.BinfElement.GetBacktracePointer;
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

    public static void MakeAnalyzeMFree(ReaderThreadCash cash) {
        if(cash.WasNewDataReceived()) {
            ArrayList<XY<Long>> allmem_mas = GetAllMemoryPreGraphic(cash);
            ArrayList<XY<Long>> unfreed_mas = GetUnfreedPreGraphic(cash);

            GluXYArrayLists(cash.GetAnalyzerResults().allMemoryUsedPoints, allmem_mas);
            GluXYArrayLists(cash.GetAnalyzerResults().unfreedMemoryPoints, unfreed_mas);

            allmem_mas.clear();
            unfreed_mas.clear();

            // Make equals last values
            if(!cash.GetAnalyzerResults().allMemoryUsedPoints.isEmpty() && !cash.GetAnalyzerResults().unfreedMemoryPoints.isEmpty()) {
                AlignmentXYArrayLists(cash.GetAnalyzerResults().allMemoryUsedPoints, cash.GetAnalyzerResults().unfreedMemoryPoints);
            }

            RemoveOldXYArrayList(cash.GetAnalyzerResults().allMemoryUsedPoints);
            RemoveOldXYArrayList(cash.GetAnalyzerResults().unfreedMemoryPoints);

            cash.GetAnalyzerResults().allMemoryUsedDescription = GenerateDescription(cash.GetAnalyzerResults().freed_sum_abs);
            cash.GetAnalyzerResults().unfreedMemoryDescription = GenerateDescription(cash.GetAnalyzerResults().unfreed_sum_level);
        }
    }

    public static HashMap<Long, Long> MakeGdbAddressesList(ReaderThreadCash cash) {
        HashMap<Long, Long> gdbAddressesList = null;
        HashMap<Long, BinfElement> handledData = cash.GetAlreadyHandledData();
        if(handledData != null) {
            gdbAddressesList = new HashMap(Help.WIN_MB);
            Long keyToPut, newValue, previousValue;
            Collection<BinfElement> allmemCollection = handledData.values();
            for(BinfElement tmpBinfElement : allmemCollection ) {
                keyToPut = GetBacktracePointer(tmpBinfElement).getValue();
                newValue = GetMFreeSize(tmpBinfElement).getValue();
                previousValue = gdbAddressesList.get(keyToPut);
                if(previousValue != null) {
                    newValue += previousValue;
                }
                gdbAddressesList.put(keyToPut, newValue);
            }
        }
        return gdbAddressesList;
    }

    public static class BinAnalyzerResults
    {
        public BinAnalyzerResults() {
            max_time = 0;
            freed_sum_level = 0;
            freed_sum_abs = 0;
            unfreed_sum_level = 0;
            allMemoryUsedDescription = "";
            allMemoryUsedPoints = new ArrayList<>();
            unfreedMemoryDescription = "";
            unfreedMemoryPoints = new ArrayList<>();
        }
        
        public long GetMaximumTime() {
            return max_time;
        }
        public long GetAllMemoryUsedValue() {
            return freed_sum_abs;
        }
        public long GetUnfreedMemoryValue() {
            return unfreed_sum_level;
        }
        public String GetAllMemoryUsedDescription() {
            return allMemoryUsedDescription;
        }
        public String GetUnfreedMemoryDescription() {
            return unfreedMemoryDescription;
        }
        public ArrayList<XY<Float>> GetAllMemoryUsedPoints() {
            return allMemoryUsedPoints;
        }
        public ArrayList<XY<Float>> GetUnfreedMemoryPoints() {
            return unfreedMemoryPoints;
        }
        
        //private variables
        private long max_time;
        private long freed_sum_abs;
        private long freed_sum_level;
        private long unfreed_sum_level;
        private String allMemoryUsedDescription;
        private ArrayList<XY<Float>> allMemoryUsedPoints;
        private String unfreedMemoryDescription;
        private ArrayList<XY<Float>> unfreedMemoryPoints;
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
    private static void GluXYArrayLists(ArrayList<XY<Float>> list1, ArrayList<XY<Long>> list2) {
        float coefficientXToNorm = Help.USEC_IN_SEC;
        float coefficientYToNorm = GetNumBytesInMB();
        if(list2.size() > 0)
        {
            for(XY<Long> xy : list2)
            {
                list1.add(new XY<>(
                    (float)(xy.x / coefficientXToNorm), (float)(xy.y / coefficientYToNorm)));

            }
        }
    }
    private static void RemoveOldXYArrayList(ArrayList<XY<Float>> list) {
        if(list.size() > 1)
        {
            ArrayList<XY<Float>> cutList = new ArrayList<>();
            float firstTime = list.get(list.size() - 1).x - GlobalVariables.g_TimelinePeriodMilisec / 1000.0f;
            for(XY<Float> xy : list)
            {
                if(xy.x >= firstTime) {
                    cutList.add(xy);
                }
            }
            list.clear();
            list.addAll(cutList);
        }
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
    private static ArrayList<XY<Long>> GetAllMemoryPreGraphic(ReaderThreadCash cash) {
        int tmp_count;
        long tmp_long_key, tmp_sum, value_sum;
        double current_time;
        // IMPORTANT!!!!!!!!!
        long freed_sum_level = cash.GetAnalyzerResults().freed_sum_level;
        long freed_sum_abs = cash.GetAnalyzerResults().freed_sum_abs;
        // IMPORTANT
        /////////////
        BinfElement retBinfElement;
        ArrayList<BinfElement> binfArray = cash.GetUnhandledData();
        HashMap<Long, BinfElement> allmemHashMap = cash.GetAlreadyHandledData();

        long startTime = GetMFreeTime(binfArray.get(0)).getValue();
        long lastTime = GetMFreeTime(binfArray.get(binfArray.size() - 1)).getValue();
        // Allocate memory
        ArrayList<XY<Long>> allmem_mas = new ArrayList((int)((lastTime - startTime) / Help.USEC_IN_SEC));
        if(cash.GetAnalyzerResults().allMemoryUsedPoints.isEmpty())
        {
            //First running
            allmem_mas.add(new XY((long)0, freed_sum_level));
        }
        if(lastTime < Help.USEC_IN_SEC) { //more detailed if time of the program is very short
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
                allmem_mas.add(new XY((long)current_time, freed_sum_level));
            }
        } else {
            double step_graphic_time = Help.USEC_IN_SEC;
            double tmp_time = (double)startTime;
            tmp_count = 0;
            tmp_sum = 0;
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
                if(current_time - tmp_time >= step_graphic_time) {
                    allmem_mas.add(new XY((long)((current_time + tmp_time) / 2.0), tmp_sum / tmp_count));
                    tmp_time = current_time;
                    tmp_sum = 0;
                    tmp_count = 0;
                }
            }
            allmem_mas.add(new XY(lastTime, freed_sum_level));
        }
        cash.GetAnalyzerResults().freed_sum_level = freed_sum_level;
        cash.GetAnalyzerResults().freed_sum_abs = freed_sum_abs;
        cash.GetAnalyzerResults().max_time = lastTime;
        
        return allmem_mas;
    }
    private static ArrayList<XY<Long>> GetUnfreedPreGraphic(ReaderThreadCash cash) {
        long value_sum;
        double current_time;
        // IMPORTANT
        long unfreed_sum = 0;
        // Extract usable info from AllmemPreGraphicInfo
        HashMap<Long, BinfElement> allmemHashMap = cash.GetAlreadyHandledData();
        // Get values of collection that put their in TreeMap
        Collection<BinfElement> allmemCollection = allmemHashMap.values();
        TreeMap<Long, BinfElement> unfreedMap = new TreeMap();
        for(BinfElement tmpBinfElement : allmemCollection ) {
            unfreedMap.put(GetMFreeTime(tmpBinfElement).getValue(), tmpBinfElement);
        }
        Collection<BinfElement> unfreedCollection = unfreedMap.values();
        
        long startTime = GetMFreeTime(cash.GetUnhandledData().get(0)).getValue();
        long lastTime = GetMFreeTime(cash.GetUnhandledData().get(cash.GetUnhandledData().size() - 1)).getValue();
        // Allocate memory
        ArrayList<XY<Long>> unfreed_mas = new ArrayList((int)((lastTime - startTime) / Help.USEC_IN_SEC));
        if(cash.GetAnalyzerResults().allMemoryUsedPoints.isEmpty())
        {
            //First running
            unfreed_mas.add(new XY((long)0, unfreed_sum));
        }
        if(lastTime < Help.USEC_IN_SEC) { //more detailed if time of the program is very short
            for( BinfElement tmpBinfElement : unfreedCollection ) {
                value_sum = GetMFreeSize(tmpBinfElement).getValue();
                unfreed_sum += value_sum;
                current_time = GetMFreeTime(tmpBinfElement).getValue();
                unfreed_mas.add(new XY((long)current_time, unfreed_sum));
            }
        }
        else {
            double step_graphic_time = Help.USEC_IN_SEC;
            double tmp_time = (double)startTime;
            for( BinfElement tmpBinfElement : unfreedCollection ) {
                value_sum = GetMFreeSize(tmpBinfElement).getValue();
                unfreed_sum += value_sum;
                current_time = GetMFreeTime(tmpBinfElement).getValue();
                if(current_time - tmp_time >= step_graphic_time) {
                    unfreed_mas.add(new XY((long)((current_time + tmp_time) / 2.0), unfreed_sum));
                    tmp_time = current_time;
                }
            }
            unfreed_mas.add(new XY(lastTime, unfreed_sum));
        }
        cash.GetAnalyzerResults().unfreed_sum_level = unfreed_sum;
        
        return unfreed_mas;
    }

}
