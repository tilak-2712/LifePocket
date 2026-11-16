package com.example.lifepocket.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.lifepocket.R;
import com.example.lifepocket.data.ItemStore;
import com.example.lifepocket.model.LifeItem;
import com.example.lifepocket.util.DateUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

/** Item detail — also the destination when a reminder notification is tapped. */
public class DetailActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "item_id";

    private long itemId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        itemId = getIntent().getLongExtra(EXTRA_ID, 0);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.inflateMenu(R.menu.menu_detail);
        toolbar.setOnMenuItemClickListener(m -> {
            LifeItem item = ItemStore.get(this).byId(itemId);
            if (item == null) return true;
            if (m.getItemId() == R.id.action_edit) {
                startActivity(new Intent(this, AddEditActivity.class).putExtra(AddEditActivity.EXTRA_ID, itemId));
            } else if (m.getItemId() == R.id.action_delete) {
                ItemActions.confirmDelete(this, item, () -> {
                    Toast.makeText(this, R.string.deleted_msg, Toast.LENGTH_SHORT).show();
                    finish();
                }, null);
            }
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        bind();
    }

    private void bind() {
        LifeItem item = ItemStore.get(this).byId(itemId);
        if (item == null) {
            Toast.makeText(this, "This item no longer exists", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        int color = ContextCompat.getColor(this, item.itemType.colorRes);
        int bg = ContextCompat.getColor(this, item.itemType.bgColorRes);
        findViewById(R.id.icon_bg).setBackgroundTintList(ColorStateList.valueOf(bg));
        ImageView icon = findViewById(R.id.icon);
        icon.setImageResource(item.itemType.iconRes);
        icon.setImageTintList(ColorStateList.valueOf(color));
        TextView type = findViewById(R.id.type_label);
        type.setText(item.itemType.label);
        type.setTextColor(color);
        type.setBackgroundTintList(ColorStateList.valueOf(bg));

        String group = ItemStore.dateGroup(item);
        TextView status = findViewById(R.id.status_label);
        status.setText(item.isCompleted() ? "✓ Completed" : "Overdue".equals(group) ? "⚠ Active · Overdue" : "Active");
        status.setTextColor(ContextCompat.getColor(this, item.isCompleted() ? R.color.success
                : "Overdue".equals(group) ? R.color.overdue : R.color.text_secondary));

        ((TextView) findViewById(R.id.title)).setText(item.title);

        String due = item.dueAtMillis == null ? "No due date"
                : DateUtils.fullDate(item.dueAtMillis) + (item.hasTime ? " · " + DateUtils.time(item.dueAtMillis) : "");
        row(R.id.d_due, R.drawable.ic_event, getString(R.string.due_date), due);
        row(R.id.d_priority, R.drawable.ic_flag, getString(R.string.priority), item.priority.label);
        row(R.id.d_reminder, R.drawable.ic_bell, getString(R.string.reminder),
                item.isReminderEnabled ? "On · notifies at " + DateUtils.time(item.dueAtMillis) : "Off");
        row(R.id.d_notes, R.drawable.ic_notes, "Notes", item.notes.isEmpty() ? "—" : item.notes);

        MaterialButton toggle = findViewById(R.id.toggle_btn);
        toggle.setText(item.isCompleted() ? R.string.mark_active : R.string.mark_complete);
        toggle.setOnClickListener(v -> {
            ItemStore.get(this).setCompleted(itemId, !item.isCompleted());
            bind();
        });
    }

    private void row(int includeId, int icon, String label, String value) {
        View row = findViewById(includeId);
        ((ImageView) row.findViewById(R.id.dr_icon)).setImageResource(icon);
        ((TextView) row.findViewById(R.id.dr_label)).setText(label);
        ((TextView) row.findViewById(R.id.dr_value)).setText(value);
    }
}
