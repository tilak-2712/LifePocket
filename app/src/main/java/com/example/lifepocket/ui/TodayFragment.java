package com.example.lifepocket.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lifepocket.R;
import com.example.lifepocket.data.ItemStore;
import com.example.lifepocket.model.LifeItem;
import com.example.lifepocket.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class TodayFragment extends Fragment implements ItemAdapter.Listener {

    private ItemAdapter adapter;
    private View empty;
    private RecyclerView list;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_today, container, false);
        ((TextView) v.findViewById(R.id.greeting)).setText(DateUtils.greeting());
        ((TextView) v.findViewById(R.id.date)).setText(DateUtils.fullDate(System.currentTimeMillis()));
        list = v.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ItemAdapter(this);
        list.setAdapter(adapter);
        empty = v.findViewById(R.id.empty);
        empty.findViewById(R.id.empty_action).setOnClickListener(b -> ((MainActivity) requireActivity()).openAdd());
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        ItemStore store = ItemStore.get(requireContext());
        List<LifeItem> overdue = store.overdue();
        List<LifeItem> today = store.dueToday();
        List<LifeItem> upcoming = store.upcoming(5);

        List<ItemAdapter.Row> rows = new ArrayList<>();
        rows.add(ItemAdapter.Row.summary(overdue.size(), today.size(), store.completedCount()));
        addSection(rows, getString(R.string.section_overdue), overdue, true);
        addSection(rows, getString(R.string.section_today), today, false);
        addSection(rows, getString(R.string.section_upcoming), upcoming, false);
        adapter.submit(rows);

        boolean nothing = store.activeCount() == 0;
        empty.setVisibility(nothing ? View.VISIBLE : View.GONE);
        list.setVisibility(nothing ? View.GONE : View.VISIBLE);
    }

    private static void addSection(List<ItemAdapter.Row> rows, String title, List<LifeItem> items, boolean danger) {
        if (items.isEmpty()) return;
        rows.add(ItemAdapter.Row.section(title, items.size(), danger));
        for (LifeItem i : items) rows.add(ItemAdapter.Row.item(i));
    }

    @Override
    public void onItemClick(LifeItem item) {
        ItemActions.openDetail(requireActivity(), item);
    }

    @Override
    public void onToggleComplete(LifeItem item, boolean completed) {
        ItemActions.toggleComplete((MainActivity) requireActivity(), item, completed, this::refresh);
    }
}
