package com.example.lifepocket.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lifepocket.R;
import com.example.lifepocket.data.ItemStore;
import com.example.lifepocket.model.ItemType;
import com.example.lifepocket.model.LifeItem;
import com.example.lifepocket.model.Priority;
import com.example.lifepocket.util.DateUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.Calendar;
import java.util.TimeZone;

public class AddEditActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "item_id";

    private LifeItem item;
    private boolean isEdit;

    private TextInputLayout titleLayout;
    private TextInputEditText titleInput, notesInput;
    private ChipGroup typeGroup;
    private MaterialButtonToggleGroup priorityGroup;
    private MaterialSwitch reminderSwitch;
    private TextView reminderHint, dateValue, timeValue;
    private View dateClear, timeClear;

    // Picked values (date = local midnight millis, time = minutes of day)
    private Long pickedDate;
    private Integer pickedMinutes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        long id = getIntent().getLongExtra(EXTRA_ID, 0);
        LifeItem existing = id == 0 ? null : ItemStore.get(this).byId(id);
        isEdit = existing != null;
        item = isEdit ? existing.copy() : new LifeItem();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(isEdit ? R.string.edit_item : R.string.add_item);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.inflateMenu(R.menu.menu_form);
        toolbar.setOnMenuItemClickListener(m -> { save(); return true; });

        titleLayout = findViewById(R.id.title_layout);
        titleInput = findViewById(R.id.title_input);
        notesInput = findViewById(R.id.notes_input);
        typeGroup = findViewById(R.id.type_group);
        priorityGroup = findViewById(R.id.priority_group);
        reminderSwitch = findViewById(R.id.reminder_switch);
        reminderHint = findViewById(R.id.reminder_hint);
        findViewById(R.id.save_btn).setOnClickListener(v -> save());

        buildTypeChips();
        setupRow(findViewById(R.id.row_date), R.drawable.ic_event, getString(R.string.due_date), this::pickDate);
        setupRow(findViewById(R.id.row_time), R.drawable.ic_clock, getString(R.string.due_time), this::pickTime);
        View rowDate = findViewById(R.id.row_date), rowTime = findViewById(R.id.row_time);
        dateValue = rowDate.findViewById(R.id.row_value);
        timeValue = rowTime.findViewById(R.id.row_value);
        dateClear = rowDate.findViewById(R.id.row_clear);
        timeClear = rowTime.findViewById(R.id.row_clear);
        dateClear.setOnClickListener(v -> { pickedDate = null; pickedMinutes = null; updateDateTime(); });
        timeClear.setOnClickListener(v -> { pickedMinutes = null; updateDateTime(); });

        titleInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { titleLayout.setError(null); }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Populate from item
        titleInput.setText(item.title);
        notesInput.setText(item.notes);
        priorityGroup.check(item.priority == Priority.LOW ? R.id.prio_low
                : item.priority == Priority.HIGH ? R.id.prio_high : R.id.prio_medium);
        if (item.dueAtMillis != null) {
            pickedDate = DateUtils.startOfDay(item.dueAtMillis);
            if (item.hasTime) {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(item.dueAtMillis);
                pickedMinutes = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
            }
        }
        updateDateTime();
        reminderSwitch.setChecked(item.isReminderEnabled && reminderSwitch.isEnabled());

        if (!isEdit) {
            titleInput.requestFocus();
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }

    private void buildTypeChips() {
        for (ItemType t : ItemType.values()) {
            Chip c = new Chip(this, null, com.google.android.material.R.attr.chipStyle);
            c.setId(View.generateViewId());
            c.setText(t.label);
            c.setTag(t);
            c.setCheckable(true);
            c.setChipIconResource(t.iconRes);
            c.setChipIconVisible(true);
            c.setCheckedIconVisible(false);
            c.setChecked(t == item.itemType);
            typeGroup.addView(c);
        }
    }

    private void setupRow(View row, int icon, String label, Runnable onClick) {
        ((ImageView) row.findViewById(R.id.row_icon)).setImageResource(icon);
        ((TextView) row.findViewById(R.id.row_label)).setText(label);
        row.setOnClickListener(v -> onClick.run());
    }

    private void pickDate() {
        long selection;
        if (pickedDate != null) {
            Calendar local = Calendar.getInstance();
            local.setTimeInMillis(pickedDate);
            Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utc.clear();
            utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH));
            selection = utc.getTimeInMillis();
        } else {
            selection = MaterialDatePicker.todayInUtcMilliseconds();
        }
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.due_date)
                .setSelection(selection)
                .build();
        picker.addOnPositiveButtonClickListener(utcMillis -> {
            // Picker returns UTC midnight; convert to local midnight of the same calendar day.
            Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utc.setTimeInMillis(utcMillis);
            Calendar local = Calendar.getInstance();
            local.clear();
            local.set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH));
            pickedDate = local.getTimeInMillis();
            updateDateTime();
        });
        picker.show(getSupportFragmentManager(), "date");
    }

    private void pickTime() {
        int minutes = pickedMinutes != null ? pickedMinutes : 18 * 60;
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(DateFormat.is24HourFormat(this) ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
                .setHour(minutes / 60)
                .setMinute(minutes % 60)
                .setTitleText(R.string.due_time)
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            pickedMinutes = picker.getHour() * 60 + picker.getMinute();
            if (pickedDate == null) pickedDate = DateUtils.startOfDay(System.currentTimeMillis());
            updateDateTime();
        });
        picker.show(getSupportFragmentManager(), "time");
    }

    private Long combinedDue() {
        if (pickedDate == null) return null;
        return pickedDate + (pickedMinutes == null ? 0 : pickedMinutes * 60_000L);
    }

    private void updateDateTime() {
        dateValue.setText(pickedDate == null ? getString(R.string.not_set) : DateUtils.relativeDay(pickedDate)
                + " · " + DateUtils.shortDate(pickedDate));
        timeValue.setText(pickedMinutes == null ? getString(R.string.not_set) : DateUtils.time(combinedDue()));
        dateClear.setVisibility(pickedDate == null ? View.GONE : View.VISIBLE);
        timeClear.setVisibility(pickedMinutes == null ? View.GONE : View.VISIBLE);

        Long due = combinedDue();
        boolean canRemind = due != null && pickedMinutes != null && due > System.currentTimeMillis();
        reminderSwitch.setEnabled(canRemind);
        if (!canRemind) reminderSwitch.setChecked(false);
        reminderHint.setText(canRemind ? R.string.reminder_enabled_hint : R.string.reminder_disabled_hint);
    }

    private void save() {
        String title = titleInput.getText() == null ? "" : titleInput.getText().toString().trim();
        if (title.isEmpty()) {
            titleLayout.setError(getString(R.string.title_required));
            titleInput.requestFocus();
            return;
        }
        int typeId = typeGroup.getCheckedChipId();
        if (typeId == View.NO_ID) {
            Toast.makeText(this, R.string.type_required, Toast.LENGTH_SHORT).show();
            return;
        }
        item.title = title;
        item.notes = notesInput.getText() == null ? "" : notesInput.getText().toString().trim();
        item.itemType = (ItemType) typeGroup.findViewById(typeId).getTag();
        int p = priorityGroup.getCheckedButtonId();
        item.priority = p == R.id.prio_low ? Priority.LOW : p == R.id.prio_high ? Priority.HIGH : Priority.MEDIUM;
        item.dueAtMillis = combinedDue();
        item.hasTime = pickedMinutes != null;
        item.isReminderEnabled = reminderSwitch.isEnabled() && reminderSwitch.isChecked();
        ItemStore.get(this).save(item);
        Toast.makeText(this, R.string.saved_msg, Toast.LENGTH_SHORT).show();
        finish();
    }
}
