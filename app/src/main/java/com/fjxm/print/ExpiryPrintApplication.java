package com.fjxm.print;

import android.app.Application;

import com.fjxm.print.printer.BluetoothPrinterManager;

public class ExpiryPrintApplication extends Application {
    private BluetoothPrinterManager printerManager;

    @Override
    public void onCreate() {
        super.onCreate();
        printerManager = new BluetoothPrinterManager(this);
    }

    public BluetoothPrinterManager getPrinterManager() {
        return printerManager;
    }
}
