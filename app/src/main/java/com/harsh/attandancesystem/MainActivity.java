package com.harsh.attandancesystem;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.harsh.attandancesystem.databinding.ActivityMainBinding;
import com.harsh.attandancesystem.ui.AdminDashboardFragment;
import com.harsh.attandancesystem.ui.AttendanceFragment;
import com.harsh.attandancesystem.ui.ProfileFragment;
import com.harsh.attandancesystem.ui.ReportsFragment;
import com.harsh.attandancesystem.ui.StudentListFragment;
import com.harsh.attandancesystem.ui.UserDashboardFragment;
import com.harsh.attandancesystem.util.AvatarUtils;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    public static final String EXTRA_USER_ROLE = "user_role";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_STUDENT = "student";

    private ActivityMainBinding binding;
    private String activeRole;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            openAuthScreen();
            return;
        }

        activeRole = resolveRole();
        setSupportActionBar(binding.toolbar);
        setupDrawer();
        configureRoleBasedNavigation();
        requestNotificationPermissionIfNeeded();

        if (savedInstanceState == null) {
            binding.navigationView.setCheckedItem(R.id.menu_dashboard);
            openDashboard();
        }
    }

    private String resolveRole() {
        String role = getIntent().getStringExtra(EXTRA_USER_ROLE);
        if (ROLE_STUDENT.equalsIgnoreCase(role)) {
            return ROLE_STUDENT;
        }
        return sessionManager.getRole();
    }

    private void setupDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                binding.drawerLayout,
                binding.toolbar,
                R.string.nav_dashboard,
                R.string.nav_reports
        );
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.getDrawerArrowDrawable().setColor(getResources().getColor(android.R.color.white));
        toggle.syncState();
        binding.navigationView.setNavigationItemSelectedListener(this);
    }

    private void configureRoleBasedNavigation() {
        binding.navigationView.getMenu().clear();
        if (isAdmin()) {
            binding.navigationView.inflateMenu(R.menu.drawer_menu_admin);
        } else {
            binding.navigationView.inflateMenu(R.menu.drawer_menu_user);
        }
        updateDrawerHeader();
    }

    private void updateDrawerHeader() {
        NavigationView navigationView = binding.navigationView;
        android.view.View headerView = navigationView.getHeaderView(0);
        if (headerView == null) {
            return;
        }

        TextView panelLabel = headerView.findViewById(R.id.headerPanelLabel);
        TextView title = headerView.findViewById(R.id.headerTitle);
        TextView subtitle = headerView.findViewById(R.id.headerSubtitle);
        android.widget.ImageView avatar = headerView.findViewById(R.id.headerAvatar);

        if (isAdmin()) {
            panelLabel.setText(R.string.admin_panel);
            title.setText(sessionManager.getDisplayName());
            subtitle.setText(R.string.admin_drawer_subtitle);
            headerView.setBackgroundResource(R.drawable.header_gradient_admin);
        } else {
            panelLabel.setText(R.string.student_panel);
            title.setText(sessionManager.getDisplayName());
            subtitle.setText(R.string.student_drawer_subtitle);
            headerView.setBackgroundResource(R.drawable.header_gradient_admin);
        }
        AvatarUtils.applyAvatar(avatar, sessionManager);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null && sessionManager != null) {
            updateDrawerHeader();
        }
    }

    private boolean isAdmin() {
        return ROLE_ADMIN.equals(activeRole);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    1001
            );
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_dashboard) {
            openDashboard();
        } else if (itemId == R.id.menu_profile) {
            navigateTo(new ProfileFragment(), "Personal Information", "Your official profile details");
        } else if (itemId == R.id.menu_students && isAdmin()) {
            navigateTo(new StudentListFragment(), getString(R.string.nav_students), getString(R.string.toolbar_subtitle_students));
        } else if (itemId == R.id.menu_attendance && isAdmin()) {
            navigateTo(new AttendanceFragment(), getString(R.string.nav_attendance), getString(R.string.toolbar_subtitle_attendance));
        } else if (itemId == R.id.menu_reports) {
            navigateTo(new ReportsFragment(), getString(R.string.nav_reports), getString(R.string.toolbar_subtitle_reports));
        } else if (itemId == R.id.menu_logout) {
            logout();
        }

        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void openDashboard() {
        if (isAdmin()) {
            navigateTo(new AdminDashboardFragment(), getString(R.string.nav_dashboard), getString(R.string.toolbar_subtitle_dashboard));
        } else {
            navigateTo(new UserDashboardFragment(), getString(R.string.nav_dashboard), getString(R.string.toolbar_subtitle_student_dashboard));
        }
    }

    private void navigateTo(Fragment fragment, String title, String subtitle) {
        binding.toolbar.setTitle(title);
        binding.toolbar.setSubtitle(subtitle);
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    public void openSection(int menuId) {
        Menu menu = binding.navigationView.getMenu();
        if (menu.findItem(menuId) == null) {
            return;
        }
        binding.navigationView.setCheckedItem(menuId);
        onNavigationItemSelected(menu.findItem(menuId));
    }

    public boolean isAdminRole() {
        return isAdmin();
    }

    public String getDisplayName() {
        return sessionManager.getDisplayName();
    }

    private void logout() {
        sessionManager.logout();
        Toast.makeText(this, R.string.logout_message, Toast.LENGTH_SHORT).show();
        openAuthScreen();
    }

    private void openAuthScreen() {
        Intent intent = new Intent(this, AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
