package com.harsh.attandancesystem.util;

import android.net.Uri;
import android.widget.ImageView;

import com.harsh.attandancesystem.R;
import com.harsh.attandancesystem.SessionManager;

public final class AvatarUtils {

    private AvatarUtils() {
    }

    public static void applyAvatar(ImageView imageView, SessionManager sessionManager) {
        if (imageView == null || sessionManager == null) {
            return;
        }

        String avatarUri = sessionManager.getAvatarUri();
        if (avatarUri == null || avatarUri.trim().isEmpty()) {
            showDefaultAvatar(imageView);
            return;
        }

        try {
            imageView.setPadding(0, 0, 0, 0);
            imageView.setBackground(null);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setImageURI(Uri.parse(avatarUri));
        } catch (Exception ignored) {
            showDefaultAvatar(imageView);
        }
    }

    public static void showDefaultAvatar(ImageView imageView) {
        imageView.setBackgroundResource(R.drawable.avatar_circle_dark);
        imageView.setPadding(16, 16, 16, 16);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageView.setImageResource(R.drawable.ic_profile);
    }
}
