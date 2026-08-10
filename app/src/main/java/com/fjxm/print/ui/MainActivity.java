package com.fjxm.print.ui;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.fjxm.print.R;
import com.fjxm.print.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyInsets(binding.root);

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_search) {
                show(new SearchFragment(), getString(R.string.search));
            } else if (item.getItemId() == R.id.nav_printer) {
                show(new PrinterFragment(), getString(R.string.printer));
            } else if (item.getItemId() == R.id.nav_settings) {
                show(new SettingsFragment(), getString(R.string.settings));
            } else {
                show(new TemplatesFragment(), getString(R.string.templates));
            }
            return true;
        });
        if (savedInstanceState == null) binding.bottomNavigation.setSelectedItemId(R.id.nav_templates);
    }

    private void show(Fragment fragment, String title) {
        binding.toolbar.setTitle(title);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void applyInsets(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }
}
