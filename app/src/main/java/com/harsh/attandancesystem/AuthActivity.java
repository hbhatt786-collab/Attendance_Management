package com.harsh.attandancesystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.harsh.attandancesystem.data.local.AttendanceDatabase;
import com.harsh.attandancesystem.data.local.Student;
import com.harsh.attandancesystem.databinding.ActivityAuthBinding;

public class AuthActivity extends AppCompatActivity {

    private ActivityAuthBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            openMainScreen();
            return;
        }

        setupRoleDropdown();
        binding.noAccountText.setVisibility(android.view.View.GONE);
        binding.registerText.setVisibility(android.view.View.GONE);
        binding.loginButton.setOnClickListener(v -> handleLogin());
        binding.registerText.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void setupRoleDropdown() {
        String[] roles = new String[]{getString(R.string.student_role), getString(R.string.admin_role)};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, roles);
        binding.roleAutoComplete.setAdapter(adapter);
        binding.roleAutoComplete.setText(roles[0], false);
    }

    private void handleLogin() {
        String email = safeText(binding.emailEditText.getText());
        String password = safeText(binding.passwordEditText.getText());
        String role = safeText(binding.roleAutoComplete.getText());

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(role)) {
            Toast.makeText(this, R.string.login_error_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        if (getString(R.string.admin_role).equals(role)) {
            if (SessionManager.ADMIN_EMAIL.equalsIgnoreCase(email) && SessionManager.ADMIN_PASSWORD.equals(password)) {
                sessionManager.createAdminSession();
                openMainScreen();
            } else {
                Toast.makeText(this, R.string.login_error_invalid_admin, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        binding.loginButton.setEnabled(false);
        new Thread(() -> {
            Student student = AttendanceDatabase.getInstance(getApplicationContext())
                    .studentDao()
                    .getStudentByCredentials(email, password);

            runOnUiThread(() -> {
                binding.loginButton.setEnabled(true);
                if (student != null) {
                    sessionManager.createStudentSession(student.getName(), student.getEmail());
                    openMainScreen();
                } else {
                    Toast.makeText(this, R.string.login_error_invalid_student, Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void openMainScreen() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_USER_ROLE, sessionManager.getRole());
        startActivity(intent);
        finish();
    }

    private String safeText(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }
}
