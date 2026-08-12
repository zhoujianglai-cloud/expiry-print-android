package com.fjxm.print.ui;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fjxm.print.R;
import com.fjxm.print.data.CategoryEntity;
import com.fjxm.print.data.MaterialEntity;
import com.fjxm.print.data.MaterialRepository;
import com.fjxm.print.databinding.DialogAddCategoryBinding;
import com.fjxm.print.databinding.FragmentTemplatesBinding;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TemplatesFragment extends Fragment {
    private FragmentTemplatesBinding binding;
    private MaterialAdapter adapter;
    private List<MaterialEntity> allItems = new ArrayList<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTemplatesBinding.inflate(inflater, container, false);
        adapter = new MaterialAdapter(this::openItem, this::confirmDeleteMaterial);
        binding.materialList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.materialList.setAdapter(adapter);
        attachSwipeToDelete();
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
        MaterialRepository repository = MaterialRepository.get(requireContext());
        repository.loadAll(items -> repository.loadCategories(categories -> {
            if (binding == null) return;
            allItems = items;
            binding.loading.setVisibility(View.GONE);
            buildChips(categories);
            filter(null);
        }));
    }

    private void buildChips(List<CategoryEntity> categories) {
        binding.typeChips.removeAllViews();
        addChip(getString(R.string.all), null, true, false);
        Map<String, Boolean> userCreatedTypes = new LinkedHashMap<>();
        for (CategoryEntity category : categories) {
            userCreatedTypes.merge(category.typeName, category.userCreated, (left, right) -> left && right);
        }
        for (Map.Entry<String, Boolean> entry : userCreatedTypes.entrySet()) {
            addChip(CategoryLabelFormatter.format(entry.getKey()), entry.getKey(), false, entry.getValue());
        }
    }

    private void addChip(String text, String filter, boolean checked, boolean userCreated) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setCheckable(true);
        chip.setChecked(checked);
        chip.setEnsureMinTouchTargetSize(true);
        chip.setMinHeight(getResources().getDimensionPixelSize(R.dimen.touch_target));
        chip.setOnClickListener(view -> {
            chip.setChecked(true);
            filter(filter);
        });
        if (userCreated) {
            chip.setContentDescription(getString(R.string.custom_category_accessibility, text));
            chip.setOnLongClickListener(view -> {
                confirmDeleteCategory(filter, text);
                return true;
            });
        }
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

    private void confirmDeleteCategory(String typeName, String displayName) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_category_title)
                .setMessage(getString(R.string.delete_category_message, displayName))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) ->
                        MaterialRepository.get(requireContext()).deleteCategory(typeName, result -> {
                            if (binding == null) return;
                            if (result == MaterialRepository.DeleteCategoryResult.DELETED) {
                                Toast.makeText(requireContext(), R.string.category_deleted,
                                        Toast.LENGTH_SHORT).show();
                                load();
                            } else if (result == MaterialRepository.DeleteCategoryResult.IN_USE) {
                                Toast.makeText(requireContext(), R.string.category_in_use,
                                        Toast.LENGTH_LONG).show();
                            } else if (result == MaterialRepository.DeleteCategoryResult.PROTECTED) {
                                Toast.makeText(requireContext(), R.string.built_in_category_protected,
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(requireContext(), R.string.category_not_found,
                                        Toast.LENGTH_SHORT).show();
                                load();
                            }
                        }))
                .show();
    }

    private void confirmDeleteMaterial(MaterialEntity item) {
        confirmDeleteMaterial(item, null);
    }

    private void confirmDeleteMaterial(MaterialEntity item, Runnable restoreSwipe) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_ingredient_title)
                .setMessage(getString(R.string.delete_ingredient_message, item.product))
                .setNegativeButton(R.string.cancel, (ignored, which) -> restore(restoreSwipe))
                .setPositiveButton(R.string.delete, (ignored, which) ->
                        MaterialRepository.get(requireContext()).deleteMaterial(item.id, deleted -> {
                            if (binding == null) return;
                            if (deleted) {
                                Toast.makeText(requireContext(), R.string.ingredient_deleted,
                                        Toast.LENGTH_SHORT).show();
                                load();
                            } else {
                                restore(restoreSwipe);
                                Toast.makeText(requireContext(), R.string.built_in_ingredient_protected,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }))
                .create();
        dialog.setOnCancelListener(ignored -> restore(restoreSwipe));
        dialog.show();
    }

    private void restore(Runnable action) {
        if (action != null) action.run();
    }

    private void attachSwipeToDelete() {
        Paint background = new Paint(Paint.ANTI_ALIAS_FLAG);
        background.setColor(ContextCompat.getColor(requireContext(), R.color.primary));
        Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
        label.setColor(ContextCompat.getColor(requireContext(), R.color.on_primary));
        label.setTextAlign(Paint.Align.CENTER);
        label.setTextSize(16f * getResources().getDisplayMetrics().scaledDensity);

        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT) {
            @Override public int getSwipeDirs(@NonNull RecyclerView recyclerView,
                                              @NonNull RecyclerView.ViewHolder viewHolder) {
                MaterialEntity item = adapter.itemAt(viewHolder.getBindingAdapterPosition());
                return item != null && item.userCreated
                        ? super.getSwipeDirs(recyclerView, viewHolder) : 0;
            }

            @Override public boolean onMove(@NonNull RecyclerView recyclerView,
                                            @NonNull RecyclerView.ViewHolder viewHolder,
                                            @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                MaterialEntity item = adapter.itemAt(position);
                if (item == null) {
                    load();
                    return;
                }
                Runnable restore = position >= 0
                        ? () -> adapter.notifyItemChanged(position) : TemplatesFragment.this::load;
                confirmDeleteMaterial(item, restore);
            }

            @Override public void onChildDraw(@NonNull Canvas canvas,
                                              @NonNull RecyclerView recyclerView,
                                              @NonNull RecyclerView.ViewHolder viewHolder,
                                              float dX, float dY, int actionState,
                                              boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;
                if (dX < 0) {
                    canvas.drawRect(itemView.getRight() + dX, itemView.getTop(),
                            itemView.getRight(), itemView.getBottom(), background);
                    Paint.FontMetrics metrics = label.getFontMetrics();
                    float centerY = itemView.getTop() + itemView.getHeight() / 2f
                            - (metrics.ascent + metrics.descent) / 2f;
                    float centerX = itemView.getRight() + dX / 2f;
                    canvas.drawText(getString(R.string.delete), centerX, centerY, label);
                }
                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY,
                        actionState, isCurrentlyActive);
            }
        });
        helper.attachToRecyclerView(binding.materialList);
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
