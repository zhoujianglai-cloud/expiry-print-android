package com.fjxm.print.ui;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.fjxm.print.ExpiryPrintApplication;
import com.fjxm.print.R;
import com.fjxm.print.data.CategoryEntity;
import com.fjxm.print.data.MaterialEntity;
import com.fjxm.print.data.MaterialRepository;
import com.fjxm.print.databinding.ActivityMaterialEditBinding;
import com.fjxm.print.model.LabelConfig;
import com.fjxm.print.printer.BluetoothPrinterManager;
import com.fjxm.print.printer.LabelRenderer;
import com.fjxm.print.printer.TscCommandBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MaterialEditActivity extends AppCompatActivity {
    public static final String EXTRA_MATERIAL_ID = "material_id";

    private ActivityMaterialEditBinding binding;
    private MaterialRepository repository;
    private final List<CategoryEntity> categories = new ArrayList<>();
    private MaterialEntity item;
    private CategoryEntity selectedCategory;
    private int storageType = 1;
    private long startMillis;
    private boolean bindingData;
    private boolean isNew;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMaterialEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);
        repository = MaterialRepository.get(this);
        startMillis = System.currentTimeMillis();

        binding.toolbar.setNavigationOnClickListener(view -> finish());
        binding.categoryInput.setOnClickListener(view -> binding.categoryInput.showDropDown());
        binding.categoryInput.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= categories.size()) return;
            selectedCategory = categories.get(position);
            binding.categoryInputLayout.setError(null);
        });
        binding.storageToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || bindingData || item == null) return;
            storageType = checkedId == R.id.frozenButton ? 2
                    : checkedId == R.id.normalButton ? 3 : 1;
            bindingData = true;
            binding.durationInput.setText(item.durationFor(storageType) > 0
                    ? getString(R.string.integer_value, item.durationFor(storageType)) : "");
            bindingData = false;
            updatePreview();
        });
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!bindingData) updatePreview();
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        binding.productInput.addTextChangedListener(watcher);
        binding.durationInput.addTextChangedListener(watcher);
        binding.saveButton.setOnClickListener(view -> persist(false, null));
        binding.printButton.setOnClickListener(view -> persist(true, this::sendPrint));
        setActionsEnabled(false);

        long id = getIntent().getLongExtra(EXTRA_MATERIAL_ID, -1L);
        repository.loadCategories(loaded -> {
            categories.clear();
            categories.addAll(loaded);
            List<String> names = new ArrayList<>(categories.size());
            for (CategoryEntity category : categories) names.add(category.displayName());
            binding.categoryInput.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, names));
            if (id < 0) {
                bindNewItem();
            } else {
                repository.getById(id, this::bindItem);
            }
        });
    }

    private void bindNewItem() {
        isNew = true;
        item = new MaterialEntity();
        binding.toolbar.setTitle(R.string.add_ingredient);
        bindingData = true;
        if (!categories.isEmpty()) selectCategory(categories.get(0));
        binding.productInput.setText("");
        binding.storageToggle.check(R.id.refrigeratedButton);
        binding.durationInput.setText("");
        binding.remarksInput.setText("");
        bindingData = false;
        setActionsEnabled(true);
        updatePreview();
        binding.productInput.requestFocus();
    }

    private void bindItem(MaterialEntity loaded) {
        if (loaded == null) {
            toast("模板不存在");
            finish();
            return;
        }
        isNew = false;
        item = loaded;
        storageType = loaded.storeType;
        binding.toolbar.setTitle(R.string.edit_and_print);
        bindingData = true;
        for (CategoryEntity category : categories) {
            if (category.typeName.equals(loaded.typeName)
                    && category.cateName.equals(loaded.cateName)) {
                selectCategory(category);
                break;
            }
        }
        binding.productInput.setText(loaded.product);
        int checked = storageType == 2 ? R.id.frozenButton
                : storageType == 3 ? R.id.normalButton : R.id.refrigeratedButton;
        binding.storageToggle.check(checked);
        int duration = loaded.durationFor(storageType);
        binding.durationInput.setText(duration > 0
                ? getString(R.string.integer_value, duration) : "0");
        binding.remarksInput.setText(loaded.remarks);
        bindingData = false;
        setActionsEnabled(true);
        updatePreview();
    }

    private void selectCategory(CategoryEntity category) {
        selectedCategory = category;
        binding.categoryInput.setText(category.displayName(), false);
        binding.categoryInputLayout.setError(null);
    }

    private void updatePreview() {
        if (item == null || binding == null) return;
        String product = text(binding.productInput.getText());
        item.product = product.isEmpty() ? getString(R.string.ingredient_name) : product;
        int hours = parseHours();
        LabelConfig config = LabelConfig.load(this);
        Bitmap bitmap = LabelRenderer.render(item, storageType, hours, startMillis, config);
        binding.previewImage.setImageBitmap(bitmap);
        long end = startMillis + Math.max(0, hours) * 60L * 60L * 1000L;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
        binding.timeSummary.setText(getString(R.string.time_summary,
                formatter.format(new Date(startMillis)), formatter.format(new Date(end))));
    }

    private void persist(boolean beforePrint, Runnable afterSave) {
        if (item == null || !readForm()) return;
        setActionsEnabled(false);
        binding.saveButton.setText(R.string.saving);
        Runnable completed = () -> {
            isNew = false;
            binding.toolbar.setTitle(R.string.edit_and_print);
            binding.saveButton.setText(R.string.save);
            setActionsEnabled(true);
            if (!beforePrint) toast(getString(R.string.ingredient_saved));
            if (afterSave != null) afterSave.run();
        };
        if (isNew) repository.insert(item, completed);
        else repository.update(item, completed);
    }

    private boolean readForm() {
        String product = text(binding.productInput.getText());
        String durationText = text(binding.durationInput.getText());
        binding.categoryInputLayout.setError(selectedCategory == null
                ? getString(R.string.category_required) : null);
        binding.productInputLayout.setError(product.isEmpty()
                ? getString(R.string.ingredient_required) : null);
        binding.durationInputLayout.setError(durationText.isEmpty()
                ? getString(R.string.duration_required) : null);
        if (selectedCategory == null || product.isEmpty() || durationText.isEmpty()) return false;
        int hours;
        try {
            hours = Math.max(0, Integer.parseInt(durationText));
        } catch (NumberFormatException error) {
            binding.durationInputLayout.setError(getString(R.string.duration_required));
            return false;
        }
        item.applyCategory(selectedCategory);
        item.product = product;
        item.remarks = text(binding.remarksInput.getText());
        item.setDurationFor(storageType, hours);
        return true;
    }

    private void sendPrint() {
        BluetoothPrinterManager manager = ((ExpiryPrintApplication) getApplication()).getPrinterManager();
        if (!manager.isConnected()) {
            toast("请先回到“打印机”页面连接 GP-M322");
            return;
        }
        LabelConfig config = LabelConfig.load(this);
        Bitmap bitmap = LabelRenderer.render(item, storageType,
                item.durationFor(storageType), startMillis, config);
        binding.printButton.setEnabled(false);
        binding.printButton.setText(R.string.sending_print_data);
        manager.print(TscCommandBuilder.build(bitmap, config), (success, message) -> {
            binding.printButton.setText(R.string.print_label);
            setActionsEnabled(true);
            toast(message);
        });
    }

    private int parseHours() {
        try { return Math.max(0, Integer.parseInt(text(binding.durationInput.getText()))); }
        catch (NumberFormatException error) { return 0; }
    }

    private void setActionsEnabled(boolean enabled) {
        binding.saveButton.setEnabled(enabled);
        binding.printButton.setEnabled(enabled);
    }

    private String text(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void applyInsets(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }
}
