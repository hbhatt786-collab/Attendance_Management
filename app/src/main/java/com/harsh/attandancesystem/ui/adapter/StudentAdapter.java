package com.harsh.attandancesystem.ui.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.harsh.attandancesystem.R;
import com.harsh.attandancesystem.data.local.StudentOverview;
import com.harsh.attandancesystem.databinding.StudentItemBinding;
import com.harsh.attandancesystem.util.StudentPhotoStore;

import java.util.ArrayList;
import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {

    public interface StudentActionListener {
        void onEdit(@NonNull StudentOverview overview);
        void onDelete(@NonNull StudentOverview overview);
    }

    private final List<StudentOverview> items = new ArrayList<>();
    private final StudentActionListener listener;

    public StudentAdapter(StudentActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<StudentOverview> students) {
        items.clear();
        if (students != null) {
            items.addAll(students);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        StudentItemBinding binding = StudentItemBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new StudentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        holder.bind(items.get(position), position + 1);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class StudentViewHolder extends RecyclerView.ViewHolder {
        private final StudentItemBinding binding;

        StudentViewHolder(StudentItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(StudentOverview overview, int serialNumber) {
            String rollNo = overview.getRollNo();
            if (rollNo != null && !rollNo.trim().isEmpty()) {
                binding.serialNumberText.setText("Roll: " + rollNo);
            } else {
                binding.serialNumberText.setText("#" + serialNumber);
            }

            binding.avatarText.setText(getInitials(overview.getName()));
            StudentPhotoStore.applyStudentPhoto(
                    binding.avatarImage,
                    binding.avatarText,
                    binding.getRoot().getContext(),
                    overview.getEmail(),
                    getInitials(overview.getName())
            );
            binding.studentNameText.setText(overview.getName());
            binding.classNameText.setText(overview.getClassName());
            binding.divisionText.setText(buildDivisionLine(overview));
            binding.enrollmentText.setText(buildEnrollmentLine(overview));

            int percentage = overview.getAttendancePercentage();
            binding.attendanceText.setText(percentage + "% Attendance");

            binding.attendanceProgressBar.setProgress(percentage);

            int color;
            if (percentage >= 75) {
                color = ContextCompat.getColor(binding.getRoot().getContext(), R.color.green_600);
                binding.warningText.setVisibility(View.GONE);
            } else if (percentage >= 50) {
                color = ContextCompat.getColor(binding.getRoot().getContext(), R.color.amber_600);
                binding.warningText.setVisibility(View.VISIBLE);
                binding.warningText.setText("Low Attendance");
                binding.warningText.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(binding.getRoot().getContext(), R.color.amber_100)));
                binding.warningText.setTextColor(color);
            } else {
                color = ContextCompat.getColor(binding.getRoot().getContext(), R.color.red_600);
                binding.warningText.setVisibility(View.VISIBLE);
                binding.warningText.setText("Very Low Attendance");
                binding.warningText.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(binding.getRoot().getContext(), R.color.red_100)));
                binding.warningText.setTextColor(color);
            }
            
            binding.attendanceProgressBar.setIndicatorColor(color);
            
            binding.editButton.setOnClickListener(v -> listener.onEdit(overview));
            binding.deleteButton.setOnClickListener(v -> listener.onDelete(overview));
        }
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "ST";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        String first = parts[0].substring(0, 1);
        String second = parts[1].substring(0, 1);
        return (first + second).toUpperCase();
    }

    private String buildDivisionLine(StudentOverview overview) {
        String division = overview.getDivision() == null ? "" : overview.getDivision().trim();
        String branch = overview.getBranch() == null ? "" : overview.getBranch().trim();

        if (!division.isEmpty() && !branch.isEmpty()) {
            return "Division: " + division + "  |  Branch: " + branch;
        }
        if (!division.isEmpty()) {
            return "Division: " + division;
        }
        if (!branch.isEmpty()) {
            return "Branch: " + branch;
        }
        return "Division: -";
    }

    private String buildEnrollmentLine(StudentOverview overview) {
        String enrollment = overview.getEnrollmentNo() == null ? "" : overview.getEnrollmentNo().trim();
        String mobile = overview.getMobileNo() == null ? "" : overview.getMobileNo().trim();

        if (!enrollment.isEmpty() && !mobile.isEmpty()) {
            return "Enrollment: " + enrollment + "  |  Mobile: " + mobile;
        }
        if (!enrollment.isEmpty()) {
            return "Enrollment: " + enrollment;
        }
        if (!mobile.isEmpty()) {
            return "Mobile: " + mobile;
        }
        return "Enrollment: -";
    }
}
