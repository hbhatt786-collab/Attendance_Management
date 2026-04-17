package com.harsh.attandancesystem.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.textfield.TextInputLayout;
import com.harsh.attandancesystem.R;
import com.harsh.attandancesystem.databinding.FragmentAttendanceBinding;
import com.harsh.attandancesystem.ui.adapter.AttendanceAdapter;
import com.harsh.attandancesystem.util.DateUtils;
import com.harsh.attandancesystem.viewmodel.AttendanceViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class AttendanceFragment extends Fragment implements AttendanceAdapter.AttendanceChangeListener {
    private static final Pattern ROLL_SPLIT_PATTERN = Pattern.compile("[,\\s]+");

    private FragmentAttendanceBinding binding;
    private AttendanceViewModel viewModel;
    private AttendanceAdapter adapter;
    private String selectedDivision = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAttendanceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(AttendanceViewModel.class);
        adapter = new AttendanceAdapter(this);
        binding.attendanceRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.attendanceRecyclerView.setAdapter(adapter);
        setupDivisionSelector();

        binding.selectDateButton.setOnClickListener(v -> openDatePicker());
        binding.markAllPresentButton.setOnClickListener(v -> adapter.markAll(AttendanceViewModel.STATUS_PRESENT));
        binding.markAllAbsentButton.setOnClickListener(v -> adapter.markAll(AttendanceViewModel.STATUS_ABSENT));
        binding.applyRollAbsentButton.setOnClickListener(v -> applyBulkAbsent());
        binding.rollAbsentEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                return applyBulkAbsent();
            }
            return false;
        });
        binding.rollAbsentEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateAbsentPreview();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        binding.saveAttendanceButton.setOnClickListener(v -> {
            if (TextUtils.isEmpty(selectedDivision)) {
                binding.classInputLayout.setError(getString(R.string.attendance_select_division_error));
                return;
            }
            viewModel.saveAttendanceRecords(adapter.getCurrentItems());
            Toast.makeText(requireContext(), R.string.status_saved, Toast.LENGTH_SHORT).show();
        });

        viewModel.getDivisions().observe(getViewLifecycleOwner(), this::updateDivisionOptions);
        viewModel.getSelectedDateLabel().observe(getViewLifecycleOwner(), date -> binding.selectedDateText.setText(date));
        viewModel.getAttendanceRecords().observe(getViewLifecycleOwner(), records -> {
            adapter.submitList(records);
            binding.emptyText.setVisibility(records == null || records.isEmpty() ? View.VISIBLE : View.GONE);
            updateAbsentPreview();
        });
    }

    @Override
    public void onAttendanceChanged(int totalCount, int presentCount, int absentCount) {
        if (binding == null) {
            return;
        }
        binding.totalCountValue.setText(String.valueOf(totalCount));
        binding.presentCountValue.setText(String.valueOf(presentCount));
        binding.absentCountValue.setText(String.valueOf(absentCount));
    }

    private void openDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) ->
                        viewModel.setSelectedDate(DateUtils.buildDate(year, month, dayOfMonth)),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void setupDivisionSelector() {
        binding.classAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            selectedDivision = String.valueOf(parent.getItemAtPosition(position));
            binding.classInputLayout.setError(null);
            binding.selectedDivisionChip.setVisibility(View.VISIBLE);
            binding.selectedDivisionChip.setText(getString(R.string.selected_division_value, selectedDivision));
            viewModel.setSelectedDivision(selectedDivision);
        });
    }

    private void updateDivisionOptions(List<String> divisions) {
        if (binding == null) {
            return;
        }

        Set<String> divisionSet = new LinkedHashSet<>();
        if (divisions != null) {
            divisionSet.addAll(divisions);
        }
        List<String> divisionList = new ArrayList<>(divisionSet);
        ArrayAdapter<String> divisionAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                divisionList
        );
        binding.classAutoComplete.setAdapter(divisionAdapter);

        if (!divisionList.contains(selectedDivision)) {
            selectedDivision = divisionList.isEmpty() ? "" : divisionList.get(0);
        }

        binding.classAutoComplete.setText(selectedDivision, false);
        binding.selectedDivisionChip.setVisibility(selectedDivision.isEmpty() ? View.GONE : View.VISIBLE);
        if (!selectedDivision.isEmpty()) {
            binding.classInputLayout.setError(null);
            binding.selectedDivisionChip.setText(getString(R.string.selected_division_value, selectedDivision));
        } else {
            binding.selectedDivisionChip.setText("");
        }
        if (!selectedDivision.isEmpty() && !selectedDivision.equals(viewModel.getSelectedDivision())) {
            viewModel.setSelectedDivision(selectedDivision);
        }
    }

    private boolean applyBulkAbsent() {
        if (binding == null) {
            return false;
        }
        if (TextUtils.isEmpty(selectedDivision)) {
            binding.classInputLayout.setError(getString(R.string.attendance_select_division_error));
            return false;
        }

        TextInputLayout inputLayout = binding.rollAbsentInputLayout;
        String input = String.valueOf(binding.rollAbsentEditText.getText()).trim();
        if (TextUtils.isEmpty(input)) {
            inputLayout.setError(getString(R.string.bulk_absent_error_empty));
            return false;
        }

        inputLayout.setError(null);
        List<String> rollNumbers = getParsedRollNumbers(input);
        int updatedCount = adapter.markAbsentByRollNumbers(rollNumbers);

        Toast.makeText(
                requireContext(),
                getString(R.string.bulk_absent_success, updatedCount),
                Toast.LENGTH_SHORT
        ).show();
        binding.rollAbsentEditText.setText("");
        return true;
    }

    private void updateAbsentPreview() {
        if (binding == null) {
            return;
        }

        List<String> rollNumbers = getParsedRollNumbers(String.valueOf(binding.rollAbsentEditText.getText()));
        if (rollNumbers.isEmpty()) {
            binding.absentPreviewCard.setVisibility(View.GONE);
            return;
        }

        binding.absentPreviewCard.setVisibility(View.VISIBLE);
        binding.absentPreviewCountText.setText(getString(R.string.absent_preview_count, rollNumbers.size()));
        binding.absentPreviewValueText.setText(TextUtils.join("  -  ", rollNumbers));
    }

    private List<String> getParsedRollNumbers(String rawInput) {
        Set<String> uniqueRollNumbers = new LinkedHashSet<>();
        if (rawInput == null) {
            return new ArrayList<>();
        }

        for (String part : ROLL_SPLIT_PATTERN.split(rawInput.trim())) {
            String value = part == null ? "" : part.trim();
            if (!value.isEmpty()) {
                uniqueRollNumbers.add(value);
            }
        }
        return new ArrayList<>(uniqueRollNumbers);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
