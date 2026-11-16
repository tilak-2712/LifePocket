package com.example.lifepocket.model;

/** Mirrors the PRD's LifeItem entity. Will become a Room @Entity in Phase 1. */
public class LifeItem {
    public static final String ACTIVE = "ACTIVE";
    public static final String COMPLETED = "COMPLETED";

    public long id;
    public String title = "";
    public String notes = "";
    public ItemType itemType = ItemType.TASK;
    public Priority priority = Priority.MEDIUM;
    public Long dueAtMillis;          // nullable
    public boolean hasTime;           // false = date only
    public boolean isReminderEnabled;
    public String status = ACTIVE;
    public long createdAtMillis;
    public long updatedAtMillis;
    public Long completedAtMillis;    // nullable

    public boolean isCompleted() {
        return COMPLETED.equals(status);
    }

    public LifeItem copy() {
        LifeItem c = new LifeItem();
        c.id = id; c.title = title; c.notes = notes; c.itemType = itemType; c.priority = priority;
        c.dueAtMillis = dueAtMillis; c.hasTime = hasTime; c.isReminderEnabled = isReminderEnabled;
        c.status = status; c.createdAtMillis = createdAtMillis; c.updatedAtMillis = updatedAtMillis;
        c.completedAtMillis = completedAtMillis;
        return c;
    }
}
