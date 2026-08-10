package com.fjxm.print.ui;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.fjxm.print.ExpiryPrintApplication;
import com.fjxm.print.R;
import com.fjxm.print.data.MaterialEntity;
import com.fjxm.print.databinding.FragmentPrinterBinding;
import com.fjxm.print.model.LabelConfig;
import com.fjxm.print.printer.BluetoothPrinterManager;
import com.fjxm.print.printer.LabelRenderer;
import com.fjxm.print.printer.TscCommandBuilder;

import java.util.Map;

public class PrinterFragment extends Fragment {
    private FragmentPrinterBinding binding;
    private BluetoothPrinterManager manager;
    private DeviceAdapter deviceAdapter;
    private boolean scanAfterPermission;

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), this::onPermissionsResult);
    private final ActivityResultLauncher<Intent> enableLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (manager != null && manager.isEnabled()) startScan();
                else toast("需要开启蓝牙才能连接打印机");
            });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPrinterBinding.inflate(inflater, container, false);
        manager = ((ExpiryPrintApplication) requireActivity().getApplication()).getPrinterManager();
        deviceAdapter = new DeviceAdapter(manager, this::connect);
        binding.deviceList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.deviceList.setAdapter(deviceAdapter);
        binding.scanButton.setOnClickListener(view -> requestAndScan());
        binding.testButton.setOnClickListener(view -> testPrint());
        updateStatus();
        if (hasPermissions()) loadBonded();
        return binding.getRoot();
    }

    private void requestAndScan() {
        if (!manager.isSupported()) {
            toast("这台手机不支持蓝牙");
            return;
        }
        if (!hasPermissions()) {
            scanAfterPermission = true;
            permissionLauncher.launch(requiredPermissions());
            return;
        }
        startScan();
    }

    private void startScan() {
        if (!manager.isEnabled()) {
            enableLauncher.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            return;
        }
        loadBonded();
        binding.scanButton.setEnabled(false);
        binding.scanButton.setText(R.string.scanning);
        manager.discover(new BluetoothPrinterManager.DiscoveryCallback() {
            @Override public void onDevice(BluetoothDevice device) {
                if (binding != null) deviceAdapter.add(device);
            }
            @Override public void onFinished() {
                if (binding != null) {
                    binding.scanButton.setEnabled(true);
                    binding.scanButton.setText(R.string.scan_printer);
                }
            }
        });
    }

    private void loadBonded() {
        deviceAdapter.replace(manager.bondedDevices());
    }

    private void connect(BluetoothDevice device) {
        deviceAdapter.setEnabled(false);
        binding.connectionStatus.setText(getString(R.string.connecting_device, manager.safeName(device)));
        manager.connect(device, (success, message) -> {
            if (binding == null) return;
            deviceAdapter.setEnabled(true);
            binding.connectionStatus.setText(message);
            toast(message);
        });
    }

    private void testPrint() {
        if (!manager.isConnected()) {
            toast("请先选择并连接 GP-M322");
            return;
        }
        MaterialEntity item = new MaterialEntity();
        item.product = "茉莉茶叶";
        LabelConfig config = LabelConfig.load(requireContext());
        Bitmap bitmap = LabelRenderer.render(item, 1, 168, System.currentTimeMillis(), config);
        binding.testButton.setEnabled(false);
        binding.testButton.setText(R.string.sending_print_data);
        manager.print(TscCommandBuilder.build(bitmap, config), (success, message) -> {
            if (binding != null) {
                binding.testButton.setEnabled(true);
                binding.testButton.setText(R.string.test_print);
            }
            toast(message);
        });
    }

    private void updateStatus() {
        if (manager.isConnected() && manager.getConnectedDevice() != null) {
            binding.connectionStatus.setText(getString(R.string.connected_device, manager.safeName(manager.getConnectedDevice())));
        } else {
            binding.connectionStatus.setText(R.string.printer_not_connected);
        }
    }

    private boolean hasPermissions() {
        for (String permission : requiredPermissions()) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    private String[] requiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
        }
        return new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
    }

    private void onPermissionsResult(Map<String, Boolean> result) {
        boolean granted = true;
        for (Boolean value : result.values()) granted &= Boolean.TRUE.equals(value);
        if (granted) {
            loadBonded();
            if (scanAfterPermission) startScan();
        } else {
            toast("未获得蓝牙权限，无法扫描打印机");
        }
        scanAfterPermission = false;
    }

    private void toast(String message) {
        if (isAdded()) Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (manager != null) manager.cancelDiscovery();
        binding = null;
    }
}
