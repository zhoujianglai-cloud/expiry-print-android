package com.fjxm.print.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.fjxm.print.data.MaterialEntity;
import com.fjxm.print.data.MaterialRepository;
import com.fjxm.print.databinding.FragmentSearchBinding;
import com.fjxm.print.R;

import java.util.Collections;

public class SearchFragment extends Fragment {
    private FragmentSearchBinding binding;
    private MaterialAdapter adapter;
    private int requestVersion;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        adapter = new MaterialAdapter(this::openItem);
        binding.searchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.searchResults.setAdapter(adapter);
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { performSearch(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
        binding.searchInput.requestFocus();
        return binding.getRoot();
    }

    private void performSearch(String query) {
        int version = ++requestVersion;
        String keyword = query.trim();
        if (keyword.isEmpty()) {
            adapter.submitList(Collections.emptyList());
            binding.resultSummary.setText(R.string.search_prompt);
            return;
        }
        MaterialRepository.get(requireContext()).search(keyword, items -> {
            if (binding == null || version != requestVersion) return;
            adapter.submitList(items);
            binding.resultSummary.setText(getString(R.string.search_result_count, items.size()));
        });
    }

    private void openItem(MaterialEntity item) {
        Intent intent = new Intent(requireContext(), MaterialEditActivity.class);
        intent.putExtra(MaterialEditActivity.EXTRA_MATERIAL_ID, item.id);
        startActivity(intent);
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
