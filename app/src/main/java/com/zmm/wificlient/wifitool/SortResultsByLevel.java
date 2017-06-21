package com.zmm.wificlient.wifitool;

import android.net.wifi.ScanResult;

import java.util.Comparator;

public class SortResultsByLevel implements Comparator {

    @Override
    public int compare(Object lhs, Object rhs) {
        ScanResult s1 = (ScanResult) lhs;
        ScanResult s2 = (ScanResult) rhs;
        if (s1.level < s2.level) {
            return 1;
        } else if (s1.level == s2.level) {
            return 0;
        } else {
            return -1;
        }
    }
}
