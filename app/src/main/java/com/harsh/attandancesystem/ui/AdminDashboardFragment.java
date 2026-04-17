package com.harsh.attandancesystem.ui;

import android.os.Bundle;
import android.text.Html;
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
import com.harsh.attandancesystem.databinding.FragmentAdminDashboardBinding;
import com.harsh.attandancesystem.viewmodel.AttendanceViewModel;
import com.harsh.attandancesystem.viewmodel.DashboardStats;

import java.util.List;

public class AdminDashboardFragment extends Fragment {

    private FragmentAdminDashboardBinding binding;
    private AttendanceViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(AttendanceViewModel.class);
        binding.greetingText.setText(getString(R.string.hello_named, ((MainActivity) requireActivity()).getDisplayName()));
        setupActions();
        
        viewModel.getDashboardStats().observe(getViewLifecycleOwner(), this::renderStats);
        viewModel.getLowAttendanceStudents().observe(getViewLifecycleOwner(), this::renderWarnings);
        viewModel.getSelectedDateLabel().observe(getViewLifecycleOwner(),
                date -> binding.selectedDateText.setText(getString(R.string.selected_date_label) + ": " + date));
        
        binding.viewAllAlertsButton.setOnClickListener(v -> ((MainActivity) requireActivity()).openSection(R.id.menu_students));
    }

    private void setupActions() {
        binding.manageStudentsCard.setOnClickListener(v -> ((MainActivity) requireActivity()).openSection(R.id.menu_students));
        binding.markAttendanceCard.setOnClickListener(v -> ((MainActivity) requireActivity()).openSection(R.id.menu_attendance));
        binding.viewReportsCard.setOnClickListener(v -> ((MainActivity) requireActivity()).openSection(R.id.menu_reports));
    }

    private void renderStats(DashboardStats stats) {
        binding.totalStudentsValue.setText(String.valueOf(stats.getTotalStudents()));
        binding.presentTodayValue.setText(stats.getAverageAttendance() + "%");
        binding.lowAttendanceValue.setText(String.valueOf(stats.getLowAttendanceCount()));
    }

    private void renderWarnings(List<StudentOverview> overviews) {
        if (overviews == null || overviews.isEmpty()) {
            binding.alertsText.setText(R.string.healthy_attendance_message);
            binding.viewAllAlertsButton.setVisibility(View.GONE);
            return;
        }

        StringBuilder html = new StringBuilder();
        int displayLimit = 5;
        int count = 0;

        for (StudentOverview overview : overviews) {
            if (count < displayLimit) {
                String color = overview.getAttendancePercentage() < 50 ? "#DC2626" : "#B45309";
                html.append("• <b>").append(overview.getName()).append("</b>")
                    .append(" (<font color='").append(color).append("'>")
                    .append(overview.getAttendancePercentage()).append("%</font>)<br/>");
            }
            count++;
        }

        if (overviews.size() > displayLimit) {
            html.append("<br/><i>+ ").append(overviews.size() - displayLimit).append(" more students...</i>");
            binding.viewAllAlertsButton.setVisibility(View.VISIBLE);
        } else {
            binding.viewAllAlertsButton.setVisibility(View.GONE);
        }

        binding.alertsText.setText(Html.fromHtml(html.toString(), Html.FROM_HTML_MODE_COMPACT));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
