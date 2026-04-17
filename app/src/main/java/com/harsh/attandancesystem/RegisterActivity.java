package com.harsh.attandancesystem;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.harsh.attandancesystem.databinding.ActivityRegisterBinding;
import com.harsh.attandancesystem.viewmodel.AttendanceViewModel;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private SessionManager sessionManager;
    private AttendanceViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState) ;
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(AttendanceViewModel.class);

        binding.createAccountButton.setOnClickListener(v -> handleRegistration());
        binding.signInText.setOnClickListener(v -> finish());
    }

    private void handleRegistration() {
        String name = safeText(binding.nameEditText.getText());
        String email = safeText(binding.registerEmailEditText.getText());
        String password = safeText(binding.registerPasswordEditText.getText());
        String confirm = safeText(binding.confirmPasswordEditText.getText());

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || 
            TextUtils.isEmpty(password) || TextUtils.isEmpty(confirm)) {
            Toast.makeText(this, R.string.register_error_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirm)) {
            Toast.makeText(this, R.string.register_error_password_mismatch, Toast.LENGTH_SHORT).show();
            return;
        }

        // Save registration for login
        sessionManager.saveStudentRegistration(name, email, password);
        
        // Keep the Room student record aligned with login credentials used elsewhere in the app.
        viewModel.addStudent(name, "Class-A", email, password); // Default class, can be updated by admin

        Toast.makeText(this, R.string.register_success, Toast.LENGTH_SHORT).show();
        finish();
    }

    private String safeText(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }
}
