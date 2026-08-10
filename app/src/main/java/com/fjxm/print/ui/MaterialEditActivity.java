package com.fjxm.print.ui;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.fjxm.print.ExpiryPrintApplication;
import com.fjxm.print.R;
import com.fjxm.print.data.MaterialEntity;
import com.fjxm.print.data.MaterialRepository;
import com.fjxm.print.databinding.ActivityMaterialEditBinding;
import com.fjxm.print.model.LabelConfig;
import com.fjxm.print.printer.BluetoothPrinterManager;
import com.fjxm.print.printer.LabelRenderer;
import com.fjxm.print.printer.TscCommandBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MaterialEditActivity extends AppCompatActivity {
    public static final String EXTRA_MATERIAL_ID = "material_id";

    private ActivityMaterialEditBinding binding;
    private MaterialEntity item;
    private int storageType = 1;
    private long startMillis;
    private boolean bindingData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMaterialEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);
        startMillis = System.currentTimeMillis();

        binding.toolbar.setNavigationOnClickListener(view -> finish());
        binding.storageToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || bindingData || item == null) return;
            storageType = checkedId == R.id.frozenButton ? 2 : checkedId == R.id.normalButton ? 3 : 1;
            bindingData = true;
            binding.durationInput.setText(getString(R.string.integer_value, item.durationFor(storageType)));
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
        binding.saveButton.setOnClickListener(view -> save(false));
        binding.printButton.setOnClickListener(view -> print());

        long id = getIntent().getLongExtra(EXTRA_MATERIAL_ID, -1L);
        MaterialRepository.get(this).getById(id, this::bindItem);
    }

    private void bindItem(MaterialEntity loaded) {
        if (loaded == null) {
            toast("模板不存在");
            finish();
            return;
        }
        item = loaded;
        storageType = loaded.storeType;
        bindingData = true;
        binding.productInput.setText(loaded.product);
        int checked = storageType == 2 ? R.id.frozenButton : storageType == 3 ? R.id.normalButton : R.id.refrigeratedButton;
        binding.storageToggle.check(checked);
        binding.durationInput.setText(getString(R.string.integer_value, loaded.durationFor(storageType)));
        binding.remarksText.setText(loaded.remarks.isEmpty() ? "" : getString(R.string.remarks_prefix, loaded.remarks));
        bindingData = false;
        updatePreview();
    }

    private void updatePreview() {
        if (item == null || binding == null) return;
        item.product = text(binding.productInput.getText());
        int hours = parseHours();
        LabelConfig config = LabelConfig.load(this);
        Bitmap bitmap = LabelRenderer.render(item, storageType, hours, startMillis, config);
        binding.previewImage.setImageBitmap(bitmap);
        long end = startMillis + Math.max(0, hours) * 60L * 60L * 1000L;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
        binding.timeSummary.setText(getString(R.string.time_summary,
                formatter.format(new Date(startMillis)), formatter.format(new Date(end))));
    }

    private void save(boolean beforePrint) {
        if (item == null) return;
        String product = text(binding.productInput.getText());
        if (product.isEmpty()) {
            toast("请输入食材名称");
            return;
        }
        item.product = product;
        item.setDurationFor(storageType, parseHours());
        MaterialRepository.get(this).update(item, () -> {
            if (!beforePrint) toast("模板已保存");
        });
    }

    private void print() {
        if (item == null) return;
        String product = text(binding.productInput.getText());
        if (product.isEmpty()) {
            toast("请输入食材名称");
            return;
        }
        item.product = product;
        int hours = parseHours();
        item.setDurationFor(storageType, hours);
        save(true);
        BluetoothPrinterManager manager = ((ExpiryPrintApplication) getApplication()).getPrinterManager();
        if (!manager.isConnected()) {
            toast("请先回到“打印机”页面连接 GP-M322");
            return;
        }
        LabelConfig config = LabelConfig.load(this);
        Bitmap bitmap = LabelRenderer.render(item, storageType, hours, startMillis, config);
        binding.printButton.setEnabled(false);
        binding.printButton.setText(R.string.sending_print_data);
        manager.print(TscCommandBuilder.build(bitmap, config), (success, message) -> {
            binding.printButton.setEnabled(true);
            binding.printButton.setText(R.string.print_label);
            toast(message);
        });
    }

    private int parseHours() {
        try { return Math.max(0, Integer.parseInt(text(binding.durationInput.getText()))); }
        catch (NumberFormatException error) { return 0; }
    }

    private String text(CharSequence value) { return value == null ? "" : value.toString().trim(); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }

    private void applyInsets(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }
}
