package com.harsh.attandancesystem.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.harsh.attandancesystem.MainActivity;
import com.harsh.attandancesystem.R;
import com.harsh.attandancesystem.SessionManager;
import com.harsh.attandancesystem.data.local.StudentOverview;
import com.harsh.attandancesystem.data.local.StudentStatusRecord;
import com.harsh.attandancesystem.databinding.FragmentUserDashboardBinding;
import com.harsh.attandancesystem.util.AvatarUtils;
import com.harsh.attandancesystem.util.DateUtils;
import com.harsh.attandancesystem.viewmodel.AttendanceViewModel;

import java.util.Calendar;
import java.util.List;

public class UserDashboardFragment extends Fragment {

    private FragmentUserDashboardBinding binding;
    private AttendanceViewModel viewModel;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentUserDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(AttendanceViewModel.class);
        sessionManager = new SessionManager(requireContext());
        
        binding.greetingText.setText(getString(R.string.hello_named, sessionManager.getDisplayName()));
        AvatarUtils.applyAvatar(binding.profileIcon, sessionManager);
        binding.openReportsCard.setOnClickListener(v -> ((MainActivity) requireActivity()).openSection(R.id.menu_reports));

        // Selected Date selection for student - making the card or text clickable
        binding.selectedDateText.setOnClickListener(v -> openDatePicker());
        binding.statusCard.setOnClickListener(v -> openDatePicker());

        // Overall statistics observer (Calculates real-time when admin saves)
        viewModel.getStudentOverviews().observe(getViewLifecycleOwner(), this::updateOverallStats);

        viewModel.getStudentStatusForToday(sessionManager.getEmail())
                .observe(getViewLifecycleOwner(), this::updateTodayStatus);

        viewModel.getSelectedDateLabel().observe(getViewLifecycleOwner(),
                date -> binding.selectedDateText.setText(getString(R.string.selected_date_label) + ": " + date));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && sessionManager != null) {
            AvatarUtils.applyAvatar(binding.profileIcon, sessionManager);
        }
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

    private void updateOverallStats(List<StudentOverview> overviews) {
        if (overviews == null || binding == null) return;

        String currentUserEmail = sessionManager.getEmail();
        StudentOverview myOverview = null;
        
        for (StudentOverview overview : overviews) {
            if (overview.getEmail() != null && overview.getEmail().equalsIgnoreCase(currentUserEmail)) {
                myOverview = overview;
                break;
            }
        }

        if (myOverview != null) {
            int percentage = myOverview.getAttendancePercentage();
            binding.attendanceValue.setText(percentage + "%");
            
            String countSummary = myOverview.getPresentCount() + " / " + myOverview.getTotalCount() + " days present";
            binding.attendanceCountText.setText(countSummary);
            
            if (myOverview.isLowAttendance()) {
                binding.alertsValue.setText("1");
                binding.personalSummaryText.setText(getString(R.string.low_attendance_message, "Your attendance", percentage));
                binding.alertsValue.setTextColor(getResources().getColor(R.color.red_600));
            } else {
                binding.alertsValue.setText("0");
                binding.personalSummaryText.setText(R.string.healthy_attendance_message);
                binding.alertsValue.setTextColor(getResources().getColor(R.color.green_600));
            }
        } else {
            binding.attendanceValue.setText("--");
            binding.attendanceCountText.setText("No attendance data");
            binding.alertsValue.setText("0");
        }
    }

    private void updateTodayStatus(StudentStatusRecord record) {
        if (binding == null) return;

        String status = "Not Marked";
        int statusColor = getResources().getColor(R.color.divider);

        if (record != null) {
            if (AttendanceViewModel.STATUS_PRESENT.equals(record.getStatus())) {
                status = "Present";
                statusColor = getResources().getColor(R.color.green_600);
            } else if (AttendanceViewModel.STATUS_ABSENT.equals(record.getStatus())) {
                status = "Absent";
                statusColor = getResources().getColor(R.color.red_600);
            }
        }

        binding.todayStatusValue.setText(status);
        binding.todayStatusValue.setTextColor(statusColor);
        binding.statusIndicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(statusColor));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
