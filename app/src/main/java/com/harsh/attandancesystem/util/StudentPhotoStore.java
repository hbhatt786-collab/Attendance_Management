package com.harsh.attandancesystem.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.harsh.attandancesystem.R;

import java.util.Locale;

public final class StudentPhotoStore {

    private static final String PREF_NAME = "student_photo_store";
    private static final String KEY_PREFIX = "student_photo_";

    private StudentPhotoStore() {
    }

    public static void savePhotoUri(Context context, String email, String uri) {
        if (context == null || TextUtils.isEmpty(normalizeEmail(email)) || TextUtils.isEmpty(uri)) {
            return;
        }

        preferences(context)
                .edit()
                .putString(KEY_PREFIX + normalizeEmail(email), uri)
                .apply();
    }

    public static void renamePhotoKey(Context context, String oldEmail, String newEmail) {
        String oldKey = normalizeEmail(oldEmail);
        String newKey = normalizeEmail(newEmail);
        if (context == null || TextUtils.isEmpty(oldKey) || TextUtils.isEmpty(newKey) || oldKey.equals(newKey)) {
            return;
        }

        SharedPreferences preferences = preferences(context);
        String value = preferences.getString(KEY_PREFIX + oldKey, "");
        if (TextUtils.isEmpty(value)) {
            return;
        }

        preferences.edit()
                .remove(KEY_PREFIX + oldKey)
                .putString(KEY_PREFIX + newKey, value)
                .apply();
    }

    public static String getPhotoUri(Context context, String email) {
        if (context == null || TextUtils.isEmpty(normalizeEmail(email))) {
            return "";
        }
        return preferences(context).getString(KEY_PREFIX + normalizeEmail(email), "");
    }

    public static void applyStudentPhoto(ImageView imageView, @Nullable TextView fallbackTextView,
                                         Context context, String email, String fallbackText) {
        if (imageView == null || context == null) {
            return;
        }

        String uri = getPhotoUri(context, email);
        if (TextUtils.isEmpty(uri)) {
            showFallback(imageView, fallbackTextView, fallbackText);
            return;
        }

        try {
            imageView.setImageURI(Uri.parse(uri));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setVisibility(android.view.View.VISIBLE);
            if (fallbackTextView != null) {
                fallbackTextView.setVisibility(android.view.View.GONE);
            }
        } catch (Exception ignored) {
            showFallback(imageView, fallbackTextView, fallbackText);
        }
    }

    public static void applyStudentPhotoOrDefault(ImageView imageView, Context context, String email) {
        if (imageView == null || context == null) {
            return;
        }

        String uri = getPhotoUri(context, email);
        if (TextUtils.isEmpty(uri)) {
            AvatarUtils.showDefaultAvatar(imageView);
            return;
        }

        try {
            imageView.setPadding(0, 0, 0, 0);
            imageView.setBackground(null);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setImageURI(Uri.parse(uri));
        } catch (Exception ignored) {
            AvatarUtils.showDefaultAvatar(imageView);
        }
    }

    private static void showFallback(ImageView imageView, @Nullable TextView fallbackTextView, String fallbackText) {
        imageView.setImageDrawable(null);
        imageView.setBackgroundResource(R.drawable.avatar_circle_dark);
        imageView.setVisibility(android.view.View.VISIBLE);
        if (fallbackTextView != null) {
            fallbackTextView.setText(fallbackText);
            fallbackTextView.setVisibility(android.view.View.VISIBLE);
        }
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
