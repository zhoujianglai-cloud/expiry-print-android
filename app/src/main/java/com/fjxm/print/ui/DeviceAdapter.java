package com.fjxm.print.ui;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fjxm.print.databinding.ItemDeviceBinding;
import com.fjxm.print.printer.BluetoothPrinterManager;

import java.util.ArrayList;
import java.util.List;

public final class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.Holder> {
    public interface OnDeviceClickListener { void onClick(BluetoothDevice device); }

    private final List<BluetoothDevice> devices = new ArrayList<>();
    private final BluetoothPrinterManager manager;
    private final OnDeviceClickListener listener;
    private boolean enabled = true;

    public DeviceAdapter(BluetoothPrinterManager manager, OnDeviceClickListener listener) {
        this.manager = manager;
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void replace(List<BluetoothDevice> source) {
        devices.clear();
        for (BluetoothDevice device : source) add(device);
        notifyDataSetChanged();
    }

    public void add(BluetoothDevice device) {
        String address = safeAddress(device);
        for (BluetoothDevice existing : devices) {
            if (safeAddress(existing).equals(address)) return;
        }
        devices.add(device);
        notifyItemInserted(devices.size() - 1);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemDeviceBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        BluetoothDevice device = devices.get(position);
        holder.binding.deviceName.setText(manager.safeName(device));
        holder.binding.deviceAddress.setText(safeAddress(device));
        holder.binding.getRoot().setEnabled(enabled);
        holder.binding.getRoot().setAlpha(enabled ? 1f : 0.5f);
        holder.binding.getRoot().setOnClickListener(view -> {
            if (enabled) listener.onClick(device);
        });
    }

    @Override public int getItemCount() { return devices.size(); }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemDeviceBinding binding;
        Holder(ItemDeviceBinding binding) { super(binding.getRoot()); this.binding = binding; }
    }

    @SuppressLint("MissingPermission")
    private String safeAddress(BluetoothDevice device) {
        try { return device.getAddress(); } catch (SecurityException error) { return "地址不可见"; }
    }
}
