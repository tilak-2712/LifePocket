package com.example.lifepocket.ui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.lifepocket.R;
import com.example.lifepocket.data.ItemStore;
import com.example.lifepocket.util.Prefs;
import com.google.android.material.button.MaterialButton;

public class OnboardingActivity extends AppCompatActivity {

    private int page = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        if (savedInstanceState != null) page = savedInstanceState.getInt("page");

        MaterialButton primary = findViewById(R.id.primary_btn);
        primary.setOnClickListener(v -> {
            if (page == 0) { page = 1; render(); }
            else finishOnboarding(false);
        });
        findViewById(R.id.samples_btn).setOnClickListener(v -> finishOnboarding(true));
        findViewById(R.id.skip).setOnClickListener(v -> finishOnboarding(false));
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (page == 1) { page = 0; render(); } else finishAffinity();
            }
        });
        render();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("page", page);
    }

    private void render() {
        ((ImageView) findViewById(R.id.onb_icon)).setImageResource(page == 0 ? R.drawable.ic_bell : R.drawable.ic_today);
        ((TextView) findViewById(R.id.onb_title)).setText(page == 0 ? R.string.onb_title_1 : R.string.onb_title_2);
        ((TextView) findViewById(R.id.onb_body)).setText(page == 0 ? R.string.onb_body_1 : R.string.onb_body_2);
        ((MaterialButton) findViewById(R.id.primary_btn)).setText(page == 0 ? R.string.next : R.string.get_started);
        findViewById(R.id.samples_btn).setVisibility(page == 1 ? View.VISIBLE : View.INVISIBLE);
        findViewById(R.id.skip).setVisibility(page == 0 ? View.VISIBLE : View.INVISIBLE);
        dot(findViewById(R.id.dot1), page == 0);
        dot(findViewById(R.id.dot2), page == 1);
    }

    private void dot(View d, boolean active) {
        float density = getResources().getDisplayMetrics().density;
        ViewGroup.LayoutParams lp = d.getLayoutParams();
        lp.width = (int) ((active ? 24 : 8) * density);
        d.setLayoutParams(lp);
        d.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, active ? R.color.primary : R.color.divider)));
    }

    private void finishOnboarding(boolean samples) {
        if (samples && ItemStore.get(this).all().isEmpty()) ItemStore.get(this).loadSamples();
        Prefs.setOnboarded(this, true);
        finish();
    }
}
