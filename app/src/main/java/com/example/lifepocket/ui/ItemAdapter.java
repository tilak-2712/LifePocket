package com.example.lifepocket.ui;

import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lifepocket.R;
import com.example.lifepocket.data.ItemStore;
import com.example.lifepocket.model.LifeItem;
import com.example.lifepocket.util.DateUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/** One adapter for both screens: summary card, section headers and item cards. */
public class ItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onItemClick(LifeItem item);
        void onToggleComplete(LifeItem item, boolean completed);
    }

    public static final int TYPE_SUMMARY = 0;
    public static final int TYPE_SECTION = 1;
    public static final int TYPE_ITEM = 2;

    public static class Row {
        final int type;
        final String text;
        final int count;
        final boolean danger;
        final LifeItem item;
        final int[] stats; // overdue, today, done

        private Row(int type, String text, int count, boolean danger, LifeItem item, int[] stats) {
            this.type = type; this.text = text; this.count = count; this.danger = danger;
            this.item = item; this.stats = stats;
        }

        public static Row summary(int overdue, int today, int done) {
            return new Row(TYPE_SUMMARY, null, 0, false, null, new int[]{overdue, today, done});
        }

        public static Row section(String title, int count, boolean danger) {
            return new Row(TYPE_SECTION, title, count, danger, null, null);
        }

        public static Row item(LifeItem item) {
            return new Row(TYPE_ITEM, null, 0, false, item, null);
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private final Listener listener;

    public ItemAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Row> newRows) {
        rows.clear();
        rows.addAll(newRows);
        notifyDataSetChanged();
    }

    public Row rowAt(int position) {
        return rows.get(position);
    }

    @Override public int getItemCount() { return rows.size(); }

    @Override public int getItemViewType(int position) { return rows.get(position).type; }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SUMMARY) return new SummaryVH(inf.inflate(R.layout.item_summary, parent, false));
        if (viewType == TYPE_SECTION) return new SectionVH(inf.inflate(R.layout.item_section, parent, false));
        return new ItemVH(inf.inflate(R.layout.item_life, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
        Row r = rows.get(position);
        if (h instanceof SummaryVH) ((SummaryVH) h).bind(r);
        else if (h instanceof SectionVH) ((SectionVH) h).bind(r);
        else ((ItemVH) h).bind(r.item);
    }

    static class SummaryVH extends RecyclerView.ViewHolder {
        final TextView title, sub, overdue, today, done;

        SummaryVH(View v) {
            super(v);
            title = v.findViewById(R.id.summary_title);
            sub = v.findViewById(R.id.summary_sub);
            overdue = v.findViewById(R.id.stat_overdue);
            today = v.findViewById(R.id.stat_today);
            done = v.findViewById(R.id.stat_done);
        }

        void bind(Row r) {
            int need = r.stats[0] + r.stats[1];
            title.setText(need == 0 ? "You're all caught up" :
                    need + (need == 1 ? " thing needs" : " things need") + " attention");
            sub.setText(r.stats[0] > 0 ? "Start with the overdue ones first."
                    : need == 0 ? "Nice work. Enjoy your day." : "You've got this.");
            overdue.setText(r.stats[0] + "\nOverdue");
            today.setText(r.stats[1] + "\nToday");
            done.setText(r.stats[2] + "\nDone");
        }
    }

    static class SectionVH extends RecyclerView.ViewHolder {
        final TextView title, count;
        final ImageView icon;

        SectionVH(View v) {
            super(v);
            title = v.findViewById(R.id.section_title);
            count = v.findViewById(R.id.section_count);
            icon = v.findViewById(R.id.section_icon);
        }

        void bind(Row r) {
            title.setText(r.text);
            count.setText(String.valueOf(r.count));
            int color = ContextCompat.getColor(itemView.getContext(),
                    r.danger ? R.color.overdue : R.color.text_secondary);
            title.setTextColor(color);
            count.setTextColor(color);
            icon.setVisibility(r.danger ? View.VISIBLE : View.GONE);
            if (r.danger) {
                icon.setImageResource(R.drawable.ic_warning);
                icon.setImageTintList(ColorStateList.valueOf(color));
            }
        }
    }

    class ItemVH extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final View iconBg;
        final ImageView icon;
        final TextView title, typeLabel, due, priority;
        final CheckBox check;

        ItemVH(View v) {
            super(v);
            card = v.findViewById(R.id.card);
            iconBg = v.findViewById(R.id.icon_bg);
            icon = v.findViewById(R.id.icon);
            title = v.findViewById(R.id.title);
            typeLabel = v.findViewById(R.id.type_label);
            due = v.findViewById(R.id.due);
            priority = v.findViewById(R.id.priority);
            check = v.findViewById(R.id.check);
        }

        void bind(LifeItem item) {
            android.content.Context ctx = itemView.getContext();
            int color = ContextCompat.getColor(ctx, item.itemType.colorRes);
            int bg = ContextCompat.getColor(ctx, item.itemType.bgColorRes);
            iconBg.setBackgroundTintList(ColorStateList.valueOf(bg));
            icon.setImageResource(item.itemType.iconRes);
            icon.setImageTintList(ColorStateList.valueOf(color));
            icon.setContentDescription(item.itemType.label);
            typeLabel.setText(item.itemType.label);
            typeLabel.setTextColor(color);
            typeLabel.setBackgroundTintList(ColorStateList.valueOf(bg));

            title.setText(item.title);
            boolean done = item.isCompleted();
            title.setPaintFlags(done ? title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                    : title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            itemView.setAlpha(done ? 0.6f : 1f);

            boolean overdue = "Overdue".equals(ItemStore.dateGroup(item));
            String dueText = DateUtils.dueLabel(item.dueAtMillis, item.hasTime);
            if (overdue) dueText = "Overdue · " + dueText;
            if (item.isReminderEnabled && !done) dueText = dueText + "  🔔";
            due.setText(dueText);
            due.setTextColor(ContextCompat.getColor(ctx, overdue ? R.color.overdue : R.color.text_secondary));
            card.setCardBackgroundColor(ContextCompat.getColor(ctx, overdue ? R.color.overdue_bg : R.color.surface));
            card.setStrokeColor(ContextCompat.getColor(ctx, overdue ? R.color.overdue_bg : R.color.divider));

            int pColor = ContextCompat.getColor(ctx, item.priority.colorRes);
            priority.setText("● " + item.priority.label + " priority");
            priority.setTextColor(pColor);

            check.setOnCheckedChangeListener(null);
            check.setChecked(done);
            check.setContentDescription((done ? "Mark active: " : "Mark complete: ") + item.title);
            check.setOnCheckedChangeListener((b, checked) -> listener.onToggleComplete(item, checked));
            itemView.setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}
