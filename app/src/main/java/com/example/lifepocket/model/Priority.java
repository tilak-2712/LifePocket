package com.example.lifepocket.model;

import com.example.lifepocket.R;

public enum Priority {
    LOW("Low", R.color.priority_low),
    MEDIUM("Medium", R.color.priority_medium),
    HIGH("High", R.color.priority_high);

    public final String label;
    public final int colorRes;

    Priority(String label, int colorRes) {
        this.label = label;
        this.colorRes = colorRes;
    }
}
