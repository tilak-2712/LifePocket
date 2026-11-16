package com.example.lifepocket.ui;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.lifepocket.BuildConfig;
import com.example.lifepocket.R;
import com.example.lifepocket.data.ItemStore;
import com.example.lifepocket.model.LifeItem;
import com.example.lifepocket.notifications.NotificationHelper;
import com.example.lifepocket.util.Prefs;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class SettingsFragment extends Fragment {

    private View root;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                bind();
                snack(granted ? "Reminders enabled" : "Notifications are off — you can enable them in system settings");
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_settings, container, false);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        bind();
    }

    private void bind() {
        boolean on = NotificationHelper.canNotify(requireContext());
        row(R.id.s_notif, R.drawable.ic_bell, "Notifications", on ? "Enabled" : "Off · tap to enable reminders",
                v -> enableNotifications());
        row(R.id.s_test, R.drawable.ic_clock, "Send a test reminder", "Shows how a LifePocket reminder looks",
                v -> sendTest());
        row(R.id.s_theme, R.drawable.ic_tune, "Theme", "Light · dark theme coming soon", null);
        row(R.id.s_samples, R.drawable.ic_list, "Load sample data", "Adds 8 demo items", v -> {
            ItemStore.get(requireContext()).loadSamples();
            snack("Sample items added");
        });
        row(R.id.s_clear, R.drawable.ic_delete, "Clear all data", "Removes every item from this device", v -> confirmClear());
        row(R.id.s_onboarding, R.drawable.ic_info, "Show onboarding again", "Replay the welcome screens", v -> {
            Prefs.setOnboarded(requireContext(), false);
            startActivity(new Intent(requireContext(), OnboardingActivity.class));
        });
        row(R.id.s_about, R.drawable.ic_check_circle, "LifePocket " + BuildConfig.VERSION_NAME,
                "Mobile Application Development Lab mini-project · all data stays on your device", null);
    }

    private void row(int id, int icon, String label, String value, View.OnClickListener click) {
        View r = root.findViewById(id);
        ((ImageView) r.findViewById(R.id.row_icon)).setImageResource(icon);
        TextView l = r.findViewById(R.id.row_label);
        TextView v = r.findViewById(R.id.row_value);
        // In settings the top line is the title (large) and the second line a subtitle (small).
        l.setText(label);
        l.setTextSize(16);
        l.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        v.setText(value);
        v.setTextSize(13);
        v.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        r.setOnClickListener(click);
        r.setClickable(click != null);
    }

    private void enableNotifications() {
        if (NotificationHelper.canNotify(requireContext())) {
            snack("Reminders are already enabled");
        } else if (Build.VERSION.SDK_INT >= 33) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName()));
        }
    }

    private void sendTest() {
        List<LifeItem> candidates = ItemStore.get(requireContext()).dueToday();
        if (candidates.isEmpty()) candidates = ItemStore.get(requireContext()).upcoming(1);
        if (candidates.isEmpty()) candidates = ItemStore.get(requireContext()).overdue();
        if (candidates.isEmpty()) {
            snack("Add an item first, then send a test reminder");
            return;
        }
        if (!NotificationHelper.show(requireContext(), candidates.get(0))) {
            enableNotifications();
        } else {
            snack("Test reminder sent — check your notifications");
        }
    }

    private void confirmClear() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear all data?")
                .setMessage("Every item will be permanently removed from this device.")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton("Clear all", (d, w) -> {
                    ItemStore.get(requireContext()).clear();
                    snack("All data cleared");
                })
                .show();
    }

    private void snack(String msg) {
        ((MainActivity) requireActivity()).snack(msg).show();
    }
}
