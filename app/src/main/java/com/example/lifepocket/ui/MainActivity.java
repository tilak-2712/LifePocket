package com.example.lifepocket.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.lifepocket.R;
import com.example.lifepocket.util.Prefs;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {

    private ExtendedFloatingActionButton fab;
    private BottomNavigationView nav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Prefs.isOnboarded(this)) {
            startActivity(new Intent(this, OnboardingActivity.class));
        }
        setContentView(R.layout.activity_main);
        fab = findViewById(R.id.fab);
        nav = findViewById(R.id.bottom_nav);

        fab.setOnClickListener(v -> openAdd());
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_today) show(new TodayFragment(), true);
            else if (id == R.id.nav_all) show(new AllItemsFragment(), true);
            else show(new SettingsFragment(), false);
            return true;
        });
        if (savedInstanceState == null) nav.setSelectedItemId(R.id.nav_today);
        else fab.setVisibility(nav.getSelectedItemId() == R.id.nav_settings ? View.GONE : View.VISIBLE);
    }

    private void show(Fragment f, boolean showFab) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, f)
                .commit();
        fab.setVisibility(showFab ? View.VISIBLE : View.GONE);
    }

    public void openAdd() {
        startActivity(new Intent(this, AddEditActivity.class));
    }

    public void selectTab(int id) {
        nav.setSelectedItemId(id);
    }

    /** Snackbar anchored above the FAB / bottom nav. */
    public Snackbar snack(String msg) {
        Snackbar s = Snackbar.make(findViewById(R.id.root), msg, Snackbar.LENGTH_LONG);
        s.setAnchorView(fab.getVisibility() == View.VISIBLE ? fab : nav);
        return s;
    }
}
