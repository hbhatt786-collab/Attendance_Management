package com.harsh.attandancesystem.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.harsh.attandancesystem.MainActivity;
import com.harsh.attandancesystem.R;
import com.harsh.attandancesystem.data.local.StudentOverview;
import com.harsh.attandancesystem.databinding.FragmentDashboardBinding;
import com.harsh.attandancesystem.viewmodel.AttendanceViewModel;
import com.harsh.attandancesystem.viewmodel.DashboardStats;

import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private AttendanceViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(AttendanceViewModel.class);
        setupActions();
        viewModel.getDashboardStats().observe(getViewLifecycleOwner(), this::renderStats);
        viewModel.getLowAttendanceStudents().observe(getViewLifecycleOwner(), this::renderWarnings);
        viewModel.getSelectedDateLabel().observe(getViewLifecycleOwner(),
                date -> binding.selectedDateText.setText(getString(R.string.selected_date_label) + ": " + date));
    }

    private void setupActions() {
        binding.manageStudentsCard.setOnClickListener(v -> ((MainActivity) requireActivity()).openSection(R.id.menu_students));
        binding.markAttendanceCard.setOnClickListener(v -> ((MainActivity) requireActivity()).openSection(R.id.menu_attendance));
        binding.viewReportsCard.setOnClickListener(v -> ((MainActivity) requireActivity()).openSection(R.id.menu_reports));
    }

    private void renderStats(DashboardStats stats) {
        binding.totalStudentsValue.setText(String.valueOf(stats.getTotalStudents()));
        binding.presentTodayValue.setText(String.valueOf(stats.getPresentToday()));
        binding.lowAttendanceValue.setText(String.valueOf(stats.getLowAttendanceCount()));
    }

    private void renderWarnings(List<StudentOverview> overviews) {
        if (overviews == null || overviews.isEmpty()) {
            binding.alertsText.setText(R.string.healthy_attendance_message);
            return;
        }

        List<String> labels = new ArrayList<>();
        for (StudentOverview overview : overviews) {
            labels.add(overview.getName() + " (" + overview.getAttendancePercentage() + "%)");
        }
        binding.alertsText.setText(getString(R.string.warning_list_prefix, TextUtils.join(", ", labels)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
