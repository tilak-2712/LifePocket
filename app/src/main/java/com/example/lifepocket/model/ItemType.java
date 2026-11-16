package com.example.lifepocket.model;

import com.example.lifepocket.R;

/** Fixed item categories from the PRD (no custom category UI in the MVP). */
public enum ItemType {
    TASK("Task", R.drawable.ic_task, R.color.type_task, R.color.type_task_bg),
    REMINDER("Reminder", R.drawable.ic_bell, R.color.type_reminder, R.color.type_reminder_bg),
    APPOINTMENT("Appointment", R.drawable.ic_event, R.color.type_appointment, R.color.type_appointment_bg),
    SHOPPING("Shopping", R.drawable.ic_cart, R.color.type_shopping, R.color.type_shopping_bg),
    PAYMENT_DEADLINE("Payment/Deadline", R.drawable.ic_payment, R.color.type_payment, R.color.type_payment_bg);

    public final String label;
    public final int iconRes;
    public final int colorRes;
    public final int bgColorRes;

    ItemType(String label, int iconRes, int colorRes, int bgColorRes) {
        this.label = label;
        this.iconRes = iconRes;
        this.colorRes = colorRes;
        this.bgColorRes = bgColorRes;
    }
}
