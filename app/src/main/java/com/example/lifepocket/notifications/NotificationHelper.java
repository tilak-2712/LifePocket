package com.example.lifepocket.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.lifepocket.R;
import com.example.lifepocket.model.LifeItem;
import com.example.lifepocket.ui.DetailActivity;
import com.example.lifepocket.util.DateUtils;

/** Posts LifePocket reminder notifications. WorkManager scheduling arrives in Phase 3. */
public final class NotificationHelper {
    public static final String CHANNEL_ID = "reminders";

    private NotificationHelper() {}

    public static boolean canNotify(Context c) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(c,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(c).areNotificationsEnabled();
    }

    private static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                    c.getString(R.string.notif_channel), NotificationManager.IMPORTANCE_HIGH);
            c.getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    /** Returns false if notifications are not permitted. */
    public static boolean show(Context c, LifeItem item) {
        if (!canNotify(c)) return false;
        ensureChannel(c);
        Intent open = new Intent(c, DetailActivity.class)
                .putExtra(DetailActivity.EXTRA_ID, item.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, (int) item.id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bell)
                .setColor(ContextCompat.getColor(c, R.color.primary))
                .setContentTitle("Don't forget: " + item.title)
                .setContentText(item.itemType.label + " · " + DateUtils.dueLabel(item.dueAtMillis, item.hasTime))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);
        try {
            NotificationManagerCompat.from(c).notify((int) item.id, b.build());
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }
}
