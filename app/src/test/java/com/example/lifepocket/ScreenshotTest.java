package com.example.lifepocket;

import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import com.example.lifepocket.data.ItemStore;
import com.example.lifepocket.model.LifeItem;
import com.example.lifepocket.ui.AddEditActivity;
import com.example.lifepocket.ui.DetailActivity;
import com.example.lifepocket.ui.MainActivity;
import com.example.lifepocket.ui.OnboardingActivity;
import com.example.lifepocket.util.Prefs;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Smoke test: launches every screen with sample data and renders it to a PNG in docs/screenshots.
 * Run with: ./gradlew testDebugUnitTest
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w393dp-h852dp-xhdpi")
public class ScreenshotTest {

    private Context ctx;

    @Before
    public void setUp() {
        ctx = ApplicationProvider.getApplicationContext();
        Prefs.setOnboarded(ctx, true);
        ItemStore store = ItemStore.get(ctx);
        store.clear();
        store.loadSamples();
    }

    @Test
    public void onboarding() throws Exception {
        Activity a = Robolectric.buildActivity(OnboardingActivity.class).setup().get();
        snap(a, "01_onboarding");
    }

    @Test
    public void todayAllItemsSettings() throws Exception {
        MainActivity a = Robolectric.buildActivity(MainActivity.class).setup().get();
        snap(a, "02_today");
        BottomNavigationView nav = a.findViewById(R.id.bottom_nav);
        nav.setSelectedItemId(R.id.nav_all);
        snap(a, "03_all_items");
        nav.setSelectedItemId(R.id.nav_settings);
        snap(a, "06_settings");
    }

    @Test
    public void addItem() throws Exception {
        Activity a = Robolectric.buildActivity(AddEditActivity.class).setup().get();
        snap(a, "04_add_item");
    }

    @Test
    public void detail() throws Exception {
        LifeItem item = ItemStore.get(ctx).dueToday().get(0);
        Intent i = new Intent(ctx, DetailActivity.class).putExtra(DetailActivity.EXTRA_ID, item.id);
        Activity a = Robolectric.buildActivity(DetailActivity.class, i).setup().get();
        snap(a, "05_detail");
    }

    @Test
    public void emptyToday() throws Exception {
        ItemStore.get(ctx).clear();
        MainActivity a = Robolectric.buildActivity(MainActivity.class).setup().get();
        snap(a, "07_today_empty");
    }

    private static void snap(Activity a, String name) throws Exception {
        shadowOf(Looper.getMainLooper()).idle();
        View root = a.getWindow().getDecorView();
        root.measure(View.MeasureSpec.makeMeasureSpec(root.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(root.getHeight(), View.MeasureSpec.EXACTLY));
        root.layout(0, 0, root.getWidth(), root.getHeight());
        Bitmap bmp = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bmp));
        assertNotNull(bmp);
        String dir = System.getProperty("screenshot.dir");
        if (dir == null) return;
        File out = new File(dir, name + ".png");
        out.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(out)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
    }
}
