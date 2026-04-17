package com.harsh.attandancesystem.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.harsh.attandancesystem.MainActivity;
import com.harsh.attandancesystem.R;
import com.harsh.attandancesystem.SessionManager;
import com.harsh.attandancesystem.data.local.StudentOverview;
import com.harsh.attandancesystem.databinding.FragmentProfileBinding;
import com.harsh.attandancesystem.util.AvatarUtils;
import com.harsh.attandancesystem.util.StudentPhotoStore;
import com.harsh.attandancesystem.viewmodel.AttendanceViewModel;

import java.util.List;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private AttendanceViewModel viewModel;
    private SessionManager sessionManager;
    private final ActivityResultLauncher<String[]> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handleImagePicked);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(AttendanceViewModel.class);
        sessionManager = new SessionManager(requireContext());
        applyCurrentProfilePhoto();
        binding.profileImage.setOnClickListener(v -> imagePickerLauncher.launch(new String[]{"image/*"}));
        binding.changePhotoText.setOnClickListener(v -> imagePickerLauncher.launch(new String[]{"image/*"}));

        viewModel.getStudentOverviews().observe(getViewLifecycleOwner(), this::loadProfileData);
    }

    private void handleImagePicked(@Nullable Uri uri) {
        if (uri == null || getContext() == null) {
            return;
        }

        try {
            requireContext().getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (SecurityException ignored) {
            // Some providers do not offer persistable permissions.
        }

        sessionManager.saveAvatarUri(uri.toString());
        applyCurrentProfilePhoto();
        if (requireActivity() instanceof MainActivity) {
            requireActivity().recreate();
        }
    }

    private void loadProfileData(List<StudentOverview> overviews) {
        if (overviews == null) return;

        String currentUserName = sessionManager.getDisplayName();
        StudentOverview myInfo = null;

        for (StudentOverview overview : overviews) {
            if (overview.getName().equalsIgnoreCase(currentUserName)) {
                myInfo = overview;
                break;
            }
        }

        if (myInfo != null) {
            StudentPhotoStore.applyStudentPhotoOrDefault(binding.profileImage, requireContext(), myInfo.getEmail());
            binding.profileNameText.setText(myInfo.getName());
            binding.profileClassText.setText(myInfo.getClassName());
            binding.rollNoValue.setText(myInfo.getRollNo().isEmpty() ? "Not Assigned" : myInfo.getRollNo());
            binding.divisionValue.setText(myInfo.getDivision().isEmpty() ? "Not Assigned" : myInfo.getDivision());
            binding.branchValue.setText(myInfo.getBranch().isEmpty() ? "Not Assigned" : myInfo.getBranch());
            binding.enrollmentValue.setText(myInfo.getEnrollmentNo().isEmpty() ? "Not Assigned" : myInfo.getEnrollmentNo());
            binding.mobileValue.setText(myInfo.getMobileNo().isEmpty() ? "Not Provided" : myInfo.getMobileNo());
            binding.abcIdValue.setText(myInfo.getAbcId().isEmpty() ? "Not Provided" : myInfo.getAbcId());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void applyCurrentProfilePhoto() {
        if (binding == null) {
            return;
        }
        String email = sessionManager.getEmail();
        if (MainActivity.ROLE_STUDENT.equals(sessionManager.getRole())
                && email != null
                && !email.trim().isEmpty()) {
            StudentPhotoStore.applyStudentPhotoOrDefault(binding.profileImage, requireContext(), email);
        } else {
            AvatarUtils.applyAvatar(binding.profileImage, sessionManager);
        }
    }
}
