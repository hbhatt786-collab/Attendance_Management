package com.harsh.attandancesystem.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.harsh.attandancesystem.R;
import com.harsh.attandancesystem.data.local.ReportSummary;
import com.harsh.attandancesystem.databinding.ReportItemBinding;

import java.util.ArrayList;
import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    private final List<ReportSummary> items = new ArrayList<>();

    public void submitList(List<ReportSummary> reports) {
        items.clear();
        if (reports != null) {
            items.addAll(reports);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ReportItemBinding binding = ReportItemBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ReportViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        private final ReportItemBinding binding;

        ReportViewHolder(ReportItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ReportSummary summary) {
            binding.avatarText.setText(getInitials(summary.getName()));
            binding.studentNameText.setText(summary.getName());
            binding.classNameText.setText(summary.getClassName());
            binding.attendanceText.setText(binding.getRoot().getContext().getString(
                    R.string.attendance_percentage,
                    summary.getAttendancePercentage()
            ));
            binding.breakdownText.setText(binding.getRoot().getContext().getString(
                    R.string.attendance_breakdown,
                    summary.getPresentCount(),
                    summary.getTotalCount()
            ));
            binding.attendanceProgress.setProgress(summary.getAttendancePercentage());
            binding.warningText.setVisibility(summary.isWarning() ? View.VISIBLE : View.GONE);
        }

        private String getInitials(String name) {
            if (name == null || name.trim().isEmpty()) {
                return "RP";
            }
            String[] parts = name.trim().split("\\s+");
            if (parts.length == 1) {
                return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
            }
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
    }
}
