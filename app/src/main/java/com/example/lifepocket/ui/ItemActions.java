package com.example.lifepocket.ui;

import android.app.Activity;
import android.content.Intent;

import com.example.lifepocket.R;
import com.example.lifepocket.data.ItemStore;
import com.example.lifepocket.model.LifeItem;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Shared list actions: open detail, complete with undo, confirm delete. */
final class ItemActions {
    private ItemActions() {}

    static void openDetail(Activity a, LifeItem item) {
        a.startActivity(new Intent(a, DetailActivity.class).putExtra(DetailActivity.EXTRA_ID, item.id));
    }

    static void toggleComplete(MainActivity a, LifeItem item, boolean completed, Runnable refresh) {
        ItemStore store = ItemStore.get(a);
        store.setCompleted(item.id, completed);
        refresh.run();
        a.snack(completed ? a.getString(R.string.completed_msg) : "Moved back to active")
                .setAction(R.string.undo, v -> {
                    store.setCompleted(item.id, !completed);
                    refresh.run();
                })
                .show();
    }

    static void confirmDelete(Activity a, LifeItem item, Runnable onDeleted, Runnable onCancel) {
        new MaterialAlertDialogBuilder(a)
                .setTitle(R.string.delete_title)
                .setMessage(R.string.delete_body)
                .setNegativeButton(R.string.cancel, (d, w) -> { if (onCancel != null) onCancel.run(); })
                .setPositiveButton(R.string.delete, (d, w) -> {
                    ItemStore.get(a).delete(item.id);
                    onDeleted.run();
                })
                .setOnCancelListener(d -> { if (onCancel != null) onCancel.run(); })
                .show();
    }
}
