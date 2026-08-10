package com.fjxm.print.printer;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BluetoothPrinterManager {
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public interface ResultCallback { void onResult(boolean success, String message); }
    public interface DiscoveryCallback {
        void onDevice(BluetoothDevice device);
        void onFinished();
    }

    private final Context context;
    private final BluetoothAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private BluetoothSocket socket;
    private BluetoothDevice connectedDevice;
    private BroadcastReceiver discoveryReceiver;

    public BluetoothPrinterManager(Context context) {
        this.context = context.getApplicationContext();
        BluetoothManager manager = this.context.getSystemService(BluetoothManager.class);
        adapter = manager == null ? null : manager.getAdapter();
    }

    public boolean isSupported() { return adapter != null; }
    public boolean isEnabled() { return adapter != null && adapter.isEnabled(); }
    public boolean isConnected() { return socket != null && socket.isConnected(); }
    public BluetoothDevice getConnectedDevice() { return connectedDevice; }

    @SuppressLint("MissingPermission")
    public List<BluetoothDevice> bondedDevices() {
        if (adapter == null) return Collections.emptyList();
        try {
            Set<BluetoothDevice> bonded = adapter.getBondedDevices();
            return bonded == null ? Collections.emptyList() : new ArrayList<>(bonded);
        } catch (SecurityException error) {
            return Collections.emptyList();
        }
    }

    @SuppressLint("MissingPermission")
    public void discover(DiscoveryCallback callback) {
        if (adapter == null) {
            callback.onFinished();
            return;
        }
        stopDiscovery();
        discoveryReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context receiverContext, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null) callback.onDevice(device);
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    stopDiscovery();
                    callback.onFinished();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        ContextCompat.registerReceiver(context, discoveryReceiver, filter, ContextCompat.RECEIVER_EXPORTED);
        try {
            if (!adapter.startDiscovery()) callback.onFinished();
        } catch (SecurityException error) {
            stopDiscovery();
            callback.onFinished();
        }
    }

    @SuppressLint("MissingPermission")
    public void connect(BluetoothDevice device, ResultCallback callback) {
        executor.execute(() -> {
            closeSocket();
            try {
                if (adapter != null) adapter.cancelDiscovery();
                BluetoothSocket newSocket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                newSocket.connect();
                socket = newSocket;
                connectedDevice = device;
                post(callback, true, "已连接 " + safeName(device));
            } catch (Exception error) {
                closeSocket();
                post(callback, false, "连接失败：" + readable(error));
            }
        });
    }

    public void print(byte[] command, ResultCallback callback) {
        executor.execute(() -> {
            BluetoothSocket current = socket;
            if (current == null || !current.isConnected()) {
                post(callback, false, "请先连接打印机");
                return;
            }
            try {
                OutputStream output = current.getOutputStream();
                output.write(command);
                output.flush();
                post(callback, true, "打印数据已发送");
            } catch (Exception error) {
                closeSocket();
                post(callback, false, "发送失败：" + readable(error));
            }
        });
    }

    public void disconnect() {
        executor.execute(this::closeSocket);
    }

    public void cancelDiscovery() {
        stopDiscovery();
    }

    @SuppressLint("MissingPermission")
    public String safeName(BluetoothDevice device) {
        try {
            String name = device.getName();
            return name == null || name.trim().isEmpty() ? "蓝牙设备" : name;
        } catch (SecurityException error) {
            return "蓝牙设备";
        }
    }

    @SuppressLint("MissingPermission")
    private void stopDiscovery() {
        if (adapter != null) {
            try { adapter.cancelDiscovery(); } catch (SecurityException ignored) {}
        }
        if (discoveryReceiver != null) {
            try { context.unregisterReceiver(discoveryReceiver); } catch (Exception ignored) {}
            discoveryReceiver = null;
        }
    }

    private void closeSocket() {
        if (socket != null) {
            try { socket.close(); } catch (Exception ignored) {}
        }
        socket = null;
        connectedDevice = null;
    }

    private void post(ResultCallback callback, boolean success, String message) {
        main.post(() -> callback.onResult(success, message));
    }

    private String readable(Exception error) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty() ? error.getClass().getSimpleName() : message;
    }
}
