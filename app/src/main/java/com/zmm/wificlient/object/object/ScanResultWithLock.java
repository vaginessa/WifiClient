package com.zmm.wificlient.object.object;

import android.net.wifi.ScanResult;

public class ScanResultWithLock {

    ScanResult scanResult;
    boolean isLocked;

    public ScanResultWithLock(ScanResult scanResult, boolean isLocked) {
        this.scanResult = scanResult;
        this.isLocked = isLocked;
    }

    public ScanResult getScanResult() {
        return scanResult;
    }

    public void setScanResult(ScanResult scanResult) {
        this.scanResult = scanResult;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }
}
