package com.harsh.attandancesystem.ui.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.harsh.attandancesystem.R;
import com.harsh.attandancesystem.data.local.StudentAttendanceRecord;
import com.harsh.attandancesystem.databinding.AttendanceItemBinding;
import com.harsh.attandancesystem.viewmodel.AttendanceViewModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder> {

    public interface AttendanceChangeListener {
        void onAttendanceChanged(int totalCount, int presentCount, int absentCount);
    }

    private final List<StudentAttendanceRecord> items = new ArrayList<>();
    private final AttendanceChangeListener listener;

    public AttendanceAdapter(AttendanceChangeListener listener) {
        this.listener = listener;
    }

    public void submitList(List<StudentAttendanceRecord> records) {
        items.clear();
        if (records != null) {
            for (StudentAttendanceRecord record : records) {
                // Fixed: Added record.getEmail() to the constructor call
                items.add(new StudentAttendanceRecord(
                        record.getId(),
                        record.getName(),
                        record.getClassName(),
                        record.getRollNo(),
                        record.getEmail(),
                        record.getDivision(),
                        record.getStatus()
                ));
            }
        }
        notifyDataSetChanged();
        notifyAttendanceChanged();
    }

    public void markAll(String status) {
        for (StudentAttendanceRecord item : items) {
            item.setStatus(status);
        }
        notifyDataSetChanged();
        notifyAttendanceChanged();
    }

    public List<StudentAttendanceRecord> getCurrentItems() {
        return new ArrayList<>(items);
    }

    public int markAbsentByRollNumbers(List<String> rollNumbers) {
        Set<String> normalizedRollNumbers = new HashSet<>();
        for (String rollNumber : rollNumbers) {
            String normalized = normalizeRollNumber(rollNumber);
            if (!normalized.isEmpty()) {
                normalizedRollNumbers.add(normalized);
            }
        }

        if (normalizedRollNumbers.isEmpty()) {
            return 0;
        }

        int updatedCount = 0;
        for (StudentAttendanceRecord item : items) {
            if (normalizedRollNumbers.contains(normalizeRollNumber(item.getRollNo()))
                    && !AttendanceViewModel.STATUS_ABSENT.equals(item.getStatus())) {
                item.setStatus(AttendanceViewModel.STATUS_ABSENT);
                updatedCount++;
            }
        }

        if (updatedCount > 0) {
            notifyDataSetChanged();
            notifyAttendanceChanged();
        }
        return updatedCount;
    }

    @NonNull
    @Override
    public AttendanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AttendanceItemBinding binding = AttendanceItemBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new AttendanceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendanceViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class AttendanceViewHolder extends RecyclerView.ViewHolder {
        private final AttendanceItemBinding binding;

        AttendanceViewHolder(AttendanceItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(StudentAttendanceRecord record) {
            binding.avatarText.setText(getInitials(record.getName()));
            binding.studentNameText.setText(record.getName());
            String rollNo = record.getRollNo() == null ? "" : record.getRollNo().trim();
            if (rollNo.isEmpty()) {
                binding.rollNoText.setText("Roll: -");
            } else {
                binding.rollNoText.setText("Roll: " + rollNo);
            }
            binding.classNameText.setText(record.getClassName());
            updateStatusUi(record.getStatus());

            binding.presentButton.setOnClickListener(v -> {
                record.setStatus(AttendanceViewModel.STATUS_PRESENT);
                updateStatusUi(record.getStatus());
                notifyAttendanceChanged();
            });
            binding.absentButton.setOnClickListener(v -> {
                record.setStatus(AttendanceViewModel.STATUS_ABSENT);
                updateStatusUi(record.getStatus());
                notifyAttendanceChanged();
            });
        }

        private void updateStatusUi(String status) {
            boolean isPresent = AttendanceViewModel.STATUS_PRESENT.equals(status);
            int activeColor = ContextCompat.getColor(binding.getRoot().getContext(), R.color.green_600);
            int inactiveColor = ContextCompat.getColor(binding.getRoot().getContext(), R.color.surface_soft);
            int activeText = ContextCompat.getColor(binding.getRoot().getContext(), R.color.white);
            int inactiveText = ContextCompat.getColor(binding.getRoot().getContext(), R.color.text_primary);
            int absentColor = ContextCompat.getColor(binding.getRoot().getContext(), R.color.red_600);

            binding.presentButton.setBackgroundTintList(ColorStateList.valueOf(isPresent ? activeColor : inactiveColor));
            binding.absentButton.setBackgroundTintList(ColorStateList.valueOf(isPresent ? inactiveColor : absentColor));
            binding.presentButton.setTextColor(isPresent ? activeText : inactiveText);
            binding.absentButton.setTextColor(isPresent ? inactiveText : activeText);
        }
    }

    private void notifyAttendanceChanged() {
        if (listener == null) {
            return;
        }
        int presentCount = 0;
        int absentCount = 0;
        for (StudentAttendanceRecord item : items) {
            if (AttendanceViewModel.STATUS_PRESENT.equals(item.getStatus())) {
                presentCount++;
            } else if (AttendanceViewModel.STATUS_ABSENT.equals(item.getStatus())) {
                absentCount++;
            }
        }
        listener.onAttendanceChanged(items.size(), presentCount, absentCount);
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "AA";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }

    private String normalizeRollNumber(String rollNumber) {
        if (rollNumber == null) {
            return "";
        }

        String normalized = rollNumber.trim().toLowerCase(Locale.US).replaceAll("[^a-z0-9]", "");
        if (normalized.matches("\\d+")) {
            normalized = normalized.replaceFirst("^0+", "");
        }
        return normalized == null ? "" : normalized;
    }
}
