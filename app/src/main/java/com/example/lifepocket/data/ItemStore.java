package com.example.lifepocket.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.lifepocket.model.ItemType;
import com.example.lifepocket.model.LifeItem;
import com.example.lifepocket.model.Priority;
import com.example.lifepocket.util.DateUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * UI-prototype data layer: a simple local store persisted as JSON in SharedPreferences.
 * The public API matches the PRD's repository boundary so it can be swapped for Room later.
 */
public final class ItemStore {
    private static final String PREFS = "lifepocket_store";
    private static final String KEY_ITEMS = "items";
    private static final String KEY_NEXT_ID = "next_id";

    private static ItemStore instance;

    private final SharedPreferences prefs;
    private final List<LifeItem> items = new ArrayList<>();
    private long nextId = 1;

    private ItemStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        load();
    }

    public static synchronized ItemStore get(Context context) {
        if (instance == null) instance = new ItemStore(context);
        return instance;
    }

    // ---- Queries -------------------------------------------------------------------------

    public List<LifeItem> all() {
        return new ArrayList<>(items);
    }

    public LifeItem byId(long id) {
        for (LifeItem i : items) if (i.id == id) return i;
        return null;
    }

    private static boolean isOverdue(LifeItem i, long now) {
        return !i.isCompleted() && i.dueAtMillis != null
                && DateUtils.effectiveDue(i.dueAtMillis, i.hasTime) < now;
    }

    public List<LifeItem> overdue() {
        long now = System.currentTimeMillis();
        List<LifeItem> out = new ArrayList<>();
        for (LifeItem i : items) if (isOverdue(i, now)) out.add(i);
        sort(out);
        return out;
    }

    public List<LifeItem> dueToday() {
        long now = System.currentTimeMillis();
        long start = DateUtils.startOfDay(now), end = DateUtils.endOfDay(now);
        List<LifeItem> out = new ArrayList<>();
        for (LifeItem i : items) {
            if (!i.isCompleted() && i.dueAtMillis != null && i.dueAtMillis >= start
                    && i.dueAtMillis <= end && !isOverdue(i, now)) {
                out.add(i);
            }
        }
        sort(out);
        return out;
    }

    /** Date-group filter used by All Items: "Overdue", "Today", "Upcoming". */
    public static String dateGroup(LifeItem i) {
        long now = System.currentTimeMillis();
        if (i.dueAtMillis == null) return "None";
        if (isOverdue(i, now)) return "Overdue";
        if (i.dueAtMillis < DateUtils.startOfDay(now)) return "Past";
        if (i.dueAtMillis <= DateUtils.endOfDay(now)) return "Today";
        return "Upcoming";
    }

    public List<LifeItem> upcoming(int limit) {
        long end = DateUtils.endOfDay(System.currentTimeMillis());
        List<LifeItem> out = new ArrayList<>();
        for (LifeItem i : items) {
            if (!i.isCompleted() && i.dueAtMillis != null && i.dueAtMillis > end) out.add(i);
        }
        sort(out);
        return out.size() > limit ? new ArrayList<>(out.subList(0, limit)) : out;
    }

    public int completedCount() {
        int n = 0;
        for (LifeItem i : items) if (i.isCompleted()) n++;
        return n;
    }

    public int activeCount() {
        return items.size() - completedCount();
    }

    /** Sort by due time ascending (undated last), then priority high→low. */
    public static void sort(List<LifeItem> list) {
        Collections.sort(list, Comparator
                .comparing((LifeItem i) -> i.dueAtMillis == null ? Long.MAX_VALUE : i.dueAtMillis)
                .thenComparing(i -> -i.priority.ordinal()));
    }

    // ---- Mutations -----------------------------------------------------------------------

    public LifeItem save(LifeItem item) {
        long now = System.currentTimeMillis();
        item.updatedAtMillis = now;
        LifeItem existing = item.id == 0 ? null : byId(item.id);
        if (existing == null) {
            item.id = nextId++;
            item.createdAtMillis = now;
            items.add(item);
        } else {
            items.set(items.indexOf(existing), item);
        }
        persist();
        return item;
    }

    public void setCompleted(long id, boolean completed) {
        LifeItem i = byId(id);
        if (i == null) return;
        i.status = completed ? LifeItem.COMPLETED : LifeItem.ACTIVE;
        i.completedAtMillis = completed ? System.currentTimeMillis() : null;
        i.updatedAtMillis = System.currentTimeMillis();
        persist();
    }

    public void delete(long id) {
        LifeItem i = byId(id);
        if (i != null) {
            items.remove(i);
            persist();
        }
    }

    public void clear() {
        items.clear();
        persist();
    }

    public void loadSamples() {
        Calendar c = Calendar.getInstance();
        add("Submit DBMS assignment", "Upload PDF on the college portal", ItemType.PAYMENT_DEADLINE,
                Priority.HIGH, at(c, -1, 23, 59), true, false, false);
        add("Pay electricity bill", "₹1,240 · pay via UPI", ItemType.PAYMENT_DEADLINE,
                Priority.HIGH, at(c, 0, 18, 0), true, true, false);
        add("Buy groceries", "Milk, bread, soap, eggs", ItemType.SHOPPING,
                Priority.MEDIUM, at(c, 0, 19, 30), true, false, false);
        add("Carry lab record", "Mobile App Dev lab — get it signed", ItemType.REMINDER,
                Priority.MEDIUM, at(c, 1, 8, 30), true, true, false);
        add("Dentist appointment", "City Dental Clinic, 2nd floor", ItemType.APPOINTMENT,
                Priority.MEDIUM, at(c, 3, 16, 0), true, true, false);
        add("Pay hostel fee", "Semester fee — keep the receipt", ItemType.PAYMENT_DEADLINE,
                Priority.HIGH, at(c, 4, 18, 0), true, true, false);
        add("Call mom", "", ItemType.TASK, Priority.LOW, null, false, false, false);
        add("Return library book", "Operating System Concepts", ItemType.TASK,
                Priority.LOW, at(c, -2, 17, 0), true, false, true);
    }

    private static Long at(Calendar base, int dayOffset, int hour, int minute) {
        Calendar c = (Calendar) base.clone();
        c.add(Calendar.DAY_OF_YEAR, dayOffset);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private void add(String title, String notes, ItemType type, Priority p, Long due,
                     boolean hasTime, boolean reminder, boolean completed) {
        LifeItem i = new LifeItem();
        i.title = title;
        i.notes = notes;
        i.itemType = type;
        i.priority = p;
        i.dueAtMillis = due;
        i.hasTime = hasTime;
        i.isReminderEnabled = reminder && due != null && due > System.currentTimeMillis();
        if (completed) {
            i.status = LifeItem.COMPLETED;
            i.completedAtMillis = System.currentTimeMillis();
        }
        save(i);
    }

    // ---- Persistence ---------------------------------------------------------------------

    private void persist() {
        try {
            JSONArray arr = new JSONArray();
            for (LifeItem i : items) {
                JSONObject o = new JSONObject();
                o.put("id", i.id);
                o.put("title", i.title);
                o.put("notes", i.notes);
                o.put("itemType", i.itemType.name());
                o.put("priority", i.priority.name());
                if (i.dueAtMillis != null) o.put("dueAtMillis", i.dueAtMillis);
                o.put("hasTime", i.hasTime);
                o.put("isReminderEnabled", i.isReminderEnabled);
                o.put("status", i.status);
                o.put("createdAtMillis", i.createdAtMillis);
                o.put("updatedAtMillis", i.updatedAtMillis);
                if (i.completedAtMillis != null) o.put("completedAtMillis", i.completedAtMillis);
                arr.put(o);
            }
            prefs.edit().putString(KEY_ITEMS, arr.toString()).putLong(KEY_NEXT_ID, nextId).apply();
        } catch (Exception ignored) {
            // Prototype: persistence failures should never crash the UI.
        }
    }

    private void load() {
        items.clear();
        nextId = prefs.getLong(KEY_NEXT_ID, 1);
        try {
            JSONArray arr = new JSONArray(prefs.getString(KEY_ITEMS, "[]"));
            for (int k = 0; k < arr.length(); k++) {
                JSONObject o = arr.getJSONObject(k);
                LifeItem i = new LifeItem();
                i.id = o.getLong("id");
                i.title = o.optString("title");
                i.notes = o.optString("notes");
                i.itemType = ItemType.valueOf(o.optString("itemType", ItemType.TASK.name()));
                i.priority = Priority.valueOf(o.optString("priority", Priority.MEDIUM.name()));
                i.dueAtMillis = o.has("dueAtMillis") ? o.getLong("dueAtMillis") : null;
                i.hasTime = o.optBoolean("hasTime");
                i.isReminderEnabled = o.optBoolean("isReminderEnabled");
                i.status = o.optString("status", LifeItem.ACTIVE);
                i.createdAtMillis = o.optLong("createdAtMillis");
                i.updatedAtMillis = o.optLong("updatedAtMillis");
                i.completedAtMillis = o.has("completedAtMillis") ? o.getLong("completedAtMillis") : null;
                items.add(i);
                nextId = Math.max(nextId, i.id + 1);
            }
        } catch (Exception e) {
            items.clear();
        }
    }
}
