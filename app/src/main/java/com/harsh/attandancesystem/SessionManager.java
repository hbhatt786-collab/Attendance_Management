package com.harsh.attandancesystem;

import android.content.Context;
import android.content.SharedPreferences;

import com.harsh.attandancesystem.util.DateUtils;

public class SessionManager {

    public static final String ADMIN_EMAIL = "admin@attendance.com";
    public static final String ADMIN_PASSWORD = "Admin@123";

    private static final String PREF_NAME = "attendance_session";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_ROLE = "role";
    private static final String KEY_SESSION_NAME = "session_name";
    private static final String KEY_SESSION_EMAIL = "session_email";
    private static final String KEY_LAST_ATTENDANCE_DATE = "last_attendance_date";
    private static final String KEY_AVATAR_PREFIX = "avatar_uri_";
    
    // Persistent Registration Keys (Not cleared on logout or admin login)
    private static final String KEY_REG_NAME = "reg_name";
    private static final String KEY_REG_EMAIL = "reg_email";
    private static final String KEY_REG_PASSWORD = "reg_password";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_LOGGED_IN, false);
    }

    public void saveStudentRegistration(String name, String email, String password) {
        preferences.edit()
                .putString(KEY_REG_NAME, name)
                .putString(KEY_REG_EMAIL, email)
                .putString(KEY_REG_PASSWORD, password)
                .apply();
    }

    public boolean isValidStudentLogin(String email, String password) {
        String savedEmail = preferences.getString(KEY_REG_EMAIL, "");
        String savedPassword = preferences.getString(KEY_REG_PASSWORD, "");
        return savedEmail.equalsIgnoreCase(email) && savedPassword.equals(password);
    }

    public void createStudentSession(String name, String email) {
        preferences.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_ROLE, MainActivity.ROLE_STUDENT)
                .putString(KEY_SESSION_NAME, name)
                .putString(KEY_SESSION_EMAIL, email)
                .apply();
    }

    public void createStudentSession() {
        String name = preferences.getString(KEY_REG_NAME, "Student");
        String email = preferences.getString(KEY_REG_EMAIL, "");
        createStudentSession(name, email);
    }

    public void createAdminSession() {
        preferences.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_ROLE, MainActivity.ROLE_ADMIN)
                .putString(KEY_SESSION_NAME, "Admin")
                .putString(KEY_SESSION_EMAIL, ADMIN_EMAIL)
                .apply();
    }

    public String getRole() {
        return preferences.getString(KEY_ROLE, MainActivity.ROLE_ADMIN);
    }

    public String getDisplayName() {
        return preferences.getString(KEY_SESSION_NAME, "User");
    }

    public String getEmail() {
        return preferences.getString(KEY_SESSION_EMAIL, "");
    }

    public void saveLastAttendanceDate(String date) {
        preferences.edit()
                .putString(KEY_LAST_ATTENDANCE_DATE, date)
                .apply();
    }

    public String getLastAttendanceDate() {
        return preferences.getString(KEY_LAST_ATTENDANCE_DATE, DateUtils.getToday());
    }

    public void saveAvatarUri(String uri) {
        preferences.edit()
                .putString(getAvatarKey(), uri)
                .apply();
    }

    public String getAvatarUri() {
        return preferences.getString(getAvatarKey(), "");
    }

    private String getAvatarKey() {
        String identity = getEmail();
        if (identity == null || identity.trim().isEmpty()) {
            identity = getRole();
        }
        return KEY_AVATAR_PREFIX + identity.toLowerCase();
    }

    public void logout() {
        preferences.edit()
                .putBoolean(KEY_LOGGED_IN, false)
                .remove(KEY_ROLE)
                .remove(KEY_SESSION_NAME)
                .remove(KEY_SESSION_EMAIL)
                .apply();
    }
}
