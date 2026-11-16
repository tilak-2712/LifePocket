package com.example.lifepocket.util;

import android.content.Context;

public final class Prefs {
    private static final String NAME = "lifepocket_prefs";
    private static final String KEY_ONBOARDED = "onboarded";

    private Prefs() {}

    public static boolean isOnboarded(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE).getBoolean(KEY_ONBOARDED, false);
    }

    public static void setOnboarded(Context c, boolean value) {
        c.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_ONBOARDED, value).apply();
    }
}
