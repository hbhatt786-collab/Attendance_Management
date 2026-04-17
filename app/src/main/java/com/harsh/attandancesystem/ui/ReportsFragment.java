package com.harsh.attandancesystem.ui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.harsh.attandancesystem.MainActivity;
import com.harsh.attandancesystem.R;
import com.harsh.attandancesystem.SessionManager;
import com.harsh.attandancesystem.data.local.ReportSummary;
import com.harsh.attandancesystem.databinding.FragmentReportsBinding;
import com.harsh.attandancesystem.ui.adapter.ReportAdapter;
import com.harsh.attandancesystem.viewmodel.AttendanceViewModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportsFragment extends Fragment {

    private FragmentReportsBinding binding;
    private AttendanceViewModel viewModel;
    private ReportAdapter adapter;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentReportsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(AttendanceViewModel.class);
        sessionManager = new SessionManager(requireContext());
        adapter = new ReportAdapter();
        binding.reportRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.reportRecyclerView.setAdapter(adapter);

        binding.weeklyButton.setOnClickListener(v -> {
            viewModel.setReportMode(AttendanceViewModel.REPORT_MODE_WEEKLY);
            updateModeButtons(AttendanceViewModel.REPORT_MODE_WEEKLY);
        });
        binding.monthlyButton.setOnClickListener(v -> {
            viewModel.setReportMode(AttendanceViewModel.REPORT_MODE_MONTHLY);
            updateModeButtons(AttendanceViewModel.REPORT_MODE_MONTHLY);
        });
        binding.sixMonthButton.setOnClickListener(v -> {
            viewModel.setReportMode(AttendanceViewModel.REPORT_MODE_SIX_MONTHS);
            updateModeButtons(AttendanceViewModel.REPORT_MODE_SIX_MONTHS);
        });

        viewModel.getReportRangeLabel().observe(getViewLifecycleOwner(),
                label -> binding.rangeText.setText(getString(R.string.report_period_label, label)));
        
        viewModel.getReportSummaries().observe(getViewLifecycleOwner(), reports -> {
            List<ReportSummary> filteredReports = filterReportsForUser(reports);
            adapter.submitList(filteredReports);
            binding.emptyText.setVisibility(filteredReports == null || filteredReports.isEmpty() ? View.VISIBLE : View.GONE);
            renderSummaryCards(filteredReports);
            renderClassWiseSummary(filteredReports);
        });

        updateModeButtons(viewModel.getReportMode());
    }

    private List<ReportSummary> filterReportsForUser(List<ReportSummary> allReports) {
        if (allReports == null) return null;
        
        // Admin gets all reports
        if (MainActivity.ROLE_ADMIN.equals(sessionManager.getRole())) {
            return allReports;
        }

        // Student only gets their own report
        String currentUserName = sessionManager.getDisplayName();
        List<ReportSummary> studentReports = new ArrayList<>();
        for (ReportSummary report : allReports) {
            if (report.getName() != null && report.getName().equalsIgnoreCase(currentUserName)) {
                studentReports.add(report);
            }
        }
        return studentReports;
    }

    private void updateModeButtons(String selectedMode) {
        int activeColor = ContextCompat.getColor(requireContext(), R.color.slate_800);
        int inactiveColor = ContextCompat.getColor(requireContext(), R.color.surface_soft);
        int activeText = ContextCompat.getColor(requireContext(), R.color.white);
        int inactiveText = ContextCompat.getColor(requireContext(), R.color.text_primary);

        boolean weeklySelected = AttendanceViewModel.REPORT_MODE_WEEKLY.equals(selectedMode);
        boolean monthlySelected = AttendanceViewModel.REPORT_MODE_MONTHLY.equals(selectedMode);
        boolean sixMonthSelected = AttendanceViewModel.REPORT_MODE_SIX_MONTHS.equals(selectedMode);

        binding.weeklyButton.setBackgroundTintList(ColorStateList.valueOf(weeklySelected ? activeColor : inactiveColor));
        binding.monthlyButton.setBackgroundTintList(ColorStateList.valueOf(monthlySelected ? activeColor : inactiveColor));
        binding.sixMonthButton.setBackgroundTintList(ColorStateList.valueOf(sixMonthSelected ? activeColor : inactiveColor));

        binding.weeklyButton.setTextColor(weeklySelected ? activeText : inactiveText);
        binding.monthlyButton.setTextColor(monthlySelected ? activeText : inactiveText);
        binding.sixMonthButton.setTextColor(sixMonthSelected ? activeText : inactiveText);
    }

    private void renderSummaryCards(List<ReportSummary> reports) {
        int average = 0;
        int studentCount = reports != null ? reports.size() : 0;
        if (reports != null && !reports.isEmpty()) {
            int sum = 0;
            for (ReportSummary summary : reports) {
                sum += summary.getAttendancePercentage();
            }
            average = sum / reports.size();
        }

        String mode = viewModel.getReportMode();
        if (AttendanceViewModel.REPORT_MODE_WEEKLY.equals(mode)) {
            binding.weeklySummaryTitle.setText(R.string.weekly_summary);
        } else if (AttendanceViewModel.REPORT_MODE_MONTHLY.equals(mode)) {
            binding.weeklySummaryTitle.setText(R.string.monthly_summary);
        } else {
            binding.weeklySummaryTitle.setText(R.string.six_month_summary);
        }
        binding.weeklySummaryValue.setText(average + "%");
        binding.monthlySummaryTitle.setText(R.string.students_count_label);
        binding.monthlySummaryValue.setText(String.valueOf(studentCount));
    }

    private void renderClassWiseSummary(List<ReportSummary> reports) {
        if (reports == null || reports.isEmpty()) {
            binding.classWiseSummaryCard.setVisibility(View.GONE);
            return;
        }

        Map<String, int[]> classTotals = new LinkedHashMap<>();
        for (ReportSummary summary : reports) {
            String className = summary.getClassName() == null || summary.getClassName().trim().isEmpty()
                    ? getString(R.string.unknown_class)
                    : summary.getClassName().trim();
            int[] totals = classTotals.get(className);
            if (totals == null) {
                totals = new int[]{0, 0, 0};
                classTotals.put(className, totals);
            }
            totals[0] += summary.getPresentCount();
            totals[1] += summary.getTotalCount();
            totals[2] += 1;
        }

        String mode = viewModel.getReportMode();
        if (AttendanceViewModel.REPORT_MODE_SIX_MONTHS.equals(mode)) {
            binding.classWiseSummaryTitle.setText(R.string.class_wise_six_month_title);
        } else if (AttendanceViewModel.REPORT_MODE_MONTHLY.equals(mode)) {
            binding.classWiseSummaryTitle.setText(R.string.class_wise_monthly_title);
        } else {
            binding.classWiseSummaryTitle.setText(R.string.class_wise_weekly_title);
        }

        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : classTotals.entrySet()) {
            int[] totals = entry.getValue();
            int percentage = totals[1] == 0 ? 0 : (totals[0] * 100) / totals[1];
            lines.add(getString(
                    R.string.class_wise_summary_line,
                    entry.getKey(),
                    percentage,
                    totals[2]
            ));
        }

        binding.classWiseSummaryValue.setText(android.text.TextUtils.join("\n", lines));
        binding.classWiseSummaryCard.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
