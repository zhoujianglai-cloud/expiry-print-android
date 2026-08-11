package com.fjxm.print.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.fjxm.print.data.MaterialEntity;
import com.fjxm.print.databinding.ItemMaterialBinding;
import com.fjxm.print.R;

public final class MaterialAdapter extends ListAdapter<MaterialEntity, MaterialAdapter.Holder> {
    public interface OnItemClickListener { void onClick(MaterialEntity item); }

    private final OnItemClickListener listener;

    public MaterialAdapter(OnItemClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemMaterialBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final ItemMaterialBinding binding;
        Holder(ItemMaterialBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        void bind(MaterialEntity item, OnItemClickListener listener) {
            binding.productName.setText(item.product);
            binding.categoryName.setText(binding.getRoot().getContext().getString(
                    R.string.category_description, item.typeName, item.cateName));
            int hours = item.durationFor(item.storeType);
            String durationText = hours > 0 ? formatHours(binding, hours)
                    : binding.getRoot().getContext().getString(R.string.on_site_decision);
            binding.duration.setText(durationText);
            binding.getRoot().setContentDescription(binding.getRoot().getContext().getString(
                    R.string.material_accessibility, item.product, durationText));
            binding.getRoot().setOnClickListener(view -> listener.onClick(item));
        }
    }

    private static String formatHours(ItemMaterialBinding binding, int hours) {
        if (hours >= 24 && hours % 24 == 0) {
            return binding.getRoot().getContext().getString(R.string.duration_days_short, hours / 24);
        }
        return binding.getRoot().getContext().getString(R.string.duration_hours_short, hours);
    }

    private static final DiffUtil.ItemCallback<MaterialEntity> DIFF = new DiffUtil.ItemCallback<>() {
        @Override public boolean areItemsTheSame(@NonNull MaterialEntity oldItem, @NonNull MaterialEntity newItem) {
            return oldItem.id == newItem.id;
        }
        @Override public boolean areContentsTheSame(@NonNull MaterialEntity oldItem, @NonNull MaterialEntity newItem) {
            return oldItem.product.equals(newItem.product)
                    && oldItem.typeName.equals(newItem.typeName)
                    && oldItem.cateName.equals(newItem.cateName)
                    && oldItem.storeType == newItem.storeType
                    && oldItem.refrigerationHours == newItem.refrigerationHours
                    && oldItem.normalHours == newItem.normalHours
                    && oldItem.freezingHours == newItem.freezingHours
                    && oldItem.remarks.equals(newItem.remarks);
        }
    };
}
