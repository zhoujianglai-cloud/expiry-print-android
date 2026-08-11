package com.fjxm.print.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.fjxm.print.R;
import com.fjxm.print.data.MaterialEntity;
import com.fjxm.print.data.MaterialRepository;
import com.fjxm.print.databinding.DialogAddCategoryBinding;
import com.fjxm.print.databinding.FragmentTemplatesBinding;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class TemplatesFragment extends Fragment {
    private FragmentTemplatesBinding binding;
    private MaterialAdapter adapter;
    private List<MaterialEntity> allItems = new ArrayList<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTemplatesBinding.inflate(inflater, container, false);
        adapter = new MaterialAdapter(this::openItem);
        binding.materialList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.materialList.setAdapter(adapter);
        binding.addCategoryButton.setOnClickListener(view -> showAddCategoryDialog());
        binding.addMaterialButton.setOnClickListener(view -> openNewItem());
        return binding.getRoot();
    }

    @Override public void onResume() {
        super.onResume();
        if (binding != null) load();
    }

    private void load() {
        binding.loading.setVisibility(View.VISIBLE);
        MaterialRepository.get(requireContext()).loadAll(items -> {
            if (binding == null) return;
            allItems = items;
            binding.loading.setVisibility(View.GONE);
            buildChips(items);
            filter(null);
        });
    }

    private void buildChips(List<MaterialEntity> items) {
        binding.typeChips.removeAllViews();
        addChip(getString(R.string.all), null, true);
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (MaterialEntity item : items) names.add(item.typeName);
        for (String name : names) addChip(name, name, false);
    }

    private void addChip(String text, String filter, boolean checked) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setCheckable(true);
        chip.setChecked(checked);
        chip.setMinHeight(getResources().getDimensionPixelSize(R.dimen.touch_target));
        chip.setOnClickListener(view -> {
            for (int i = 0; i < binding.typeChips.getChildCount(); i++) {
                View child = binding.typeChips.getChildAt(i);
                if (child instanceof Chip && child != chip) ((Chip) child).setChecked(false);
            }
            chip.setChecked(true);
            filter(filter);
        });
        binding.typeChips.addView(chip);
    }

    private void filter(String typeName) {
        List<MaterialEntity> filtered = new ArrayList<>();
        for (MaterialEntity item : allItems) {
            if (typeName == null || typeName.equals(item.typeName)) filtered.add(item);
        }
        adapter.submitList(filtered);
        binding.emptyText.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openItem(MaterialEntity item) {
        Intent intent = new Intent(requireContext(), MaterialEditActivity.class);
        intent.putExtra(MaterialEditActivity.EXTRA_MATERIAL_ID, item.id);
        startActivity(intent);
    }

    private void openNewItem() {
        startActivity(new Intent(requireContext(), MaterialEditActivity.class));
    }

    private void showAddCategoryDialog() {
        DialogAddCategoryBinding dialogBinding = DialogAddCategoryBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_category)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(ignored -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setOnClickListener(view -> {
                String typeName = text(dialogBinding.typeNameInput.getText());
                String categoryName = text(dialogBinding.categoryNameInput.getText());
                dialogBinding.typeNameLayout.setError(typeName.isEmpty()
                        ? getString(R.string.group_required) : null);
                dialogBinding.categoryNameLayout.setError(categoryName.isEmpty()
                        ? getString(R.string.category_name_required) : null);
                if (typeName.isEmpty() || categoryName.isEmpty()) return;
                save.setEnabled(false);
                save.setText(R.string.saving);
                MaterialRepository.get(requireContext()).addCategory(typeName, categoryName, category -> {
                    if (binding == null || !isAdded()) return;
                    if (category == null) {
                        save.setEnabled(true);
                        save.setText(R.string.save);
                        dialogBinding.categoryNameLayout.setError(getString(R.string.category_exists));
                        return;
                    }
                    dialog.dismiss();
                    Toast.makeText(requireContext(), R.string.category_saved, Toast.LENGTH_SHORT).show();
                    load();
                });
            });
        });
        dialog.show();
    }

    private String text(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
