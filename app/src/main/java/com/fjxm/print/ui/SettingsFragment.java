package com.fjxm.print.ui;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fjxm.print.data.MaterialEntity;
import com.fjxm.print.databinding.FragmentSettingsBinding;
import com.fjxm.print.model.LabelConfig;
import com.fjxm.print.printer.LabelRenderer;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        showConfig(LabelConfig.load(requireContext()));
        binding.saveSettingsButton.setOnClickListener(view -> save());
        binding.resetSettingsButton.setOnClickListener(view -> {
            LabelConfig.reset(requireContext());
            showConfig(new LabelConfig());
            toast("已恢复 GP-M322 默认参数");
        });
        return binding.getRoot();
    }

    private void showConfig(LabelConfig config) {
        binding.widthInput.setText(trim(config.widthMm));
        binding.heightInput.setText(trim(config.heightMm));
        binding.gapInput.setText(trim(config.gapMm));
        binding.marginInput.setText(trim(config.horizontalMarginMm));
        binding.operatorInput.setText(config.operator);
        updatePreview(config);
    }

    private void save() {
        try {
            LabelConfig config = new LabelConfig();
            config.widthMm = clamp(parse(binding.widthInput.getText()), 30f, 72f);
            config.heightMm = clamp(parse(binding.heightInput.getText()), 20f, 60f);
            config.gapMm = clamp(parse(binding.gapInput.getText()), 0f, 5f);
            config.horizontalMarginMm = clamp(parse(binding.marginInput.getText()), 0.5f, 5f);
            config.operator = text(binding.operatorInput.getText());
            config.save(requireContext());
            showConfig(config);
            toast("标签参数已保存");
        } catch (NumberFormatException error) {
            toast("请填写正确的标签尺寸");
        }
    }

    private void updatePreview(LabelConfig config) {
        MaterialEntity item = new MaterialEntity();
        item.product = "茉莉茶叶";
        Bitmap bitmap = LabelRenderer.render(item, 1, 168, System.currentTimeMillis(), config);
        binding.previewImage.setImageBitmap(bitmap);
    }

    private static float parse(CharSequence value) { return Float.parseFloat(text(value)); }
    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
    private static String text(CharSequence value) { return value == null ? "" : value.toString().trim(); }
    private static String trim(float value) { return value == (int) value ? Integer.toString((int) value) : Float.toString(value); }

    private void toast(String message) { Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show(); }

    @Override public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
