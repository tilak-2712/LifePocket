package com.example.lifepocket.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lifepocket.R;
import com.example.lifepocket.data.ItemStore;
import com.example.lifepocket.model.ItemType;
import com.example.lifepocket.model.LifeItem;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AllItemsFragment extends Fragment implements ItemAdapter.Listener {

    private static final String[] STATUSES = {"Active", "Completed", "All"};
    private static final String[] DATES = {"All", "Today", "Upcoming", "Overdue"};

    private ItemAdapter adapter;
    private RecyclerView list;
    private View empty, filterDot;
    private TextView resultCount;
    private EditText search;
    private ChipGroup typeChips;

    private String query = "";
    private ItemType typeFilter = null;
    private String statusFilter = "Active";
    private String dateFilter = "All";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_all, container, false);
        list = v.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ItemAdapter(this);
        list.setAdapter(adapter);
        empty = v.findViewById(R.id.empty);
        filterDot = v.findViewById(R.id.filter_dot);
        resultCount = v.findViewById(R.id.result_count);
        search = v.findViewById(R.id.search);
        typeChips = v.findViewById(R.id.type_chips);

        ((TextView) empty.findViewById(R.id.empty_title)).setText(R.string.no_match_title);
        ((TextView) empty.findViewById(R.id.empty_body)).setText(R.string.no_match_body);
        ((android.widget.ImageView) empty.findViewById(R.id.empty_icon)).setImageResource(R.drawable.ic_search);
        ((TextView) empty.findViewById(R.id.empty_action)).setText(R.string.clear_filters);
        empty.findViewById(R.id.empty_action).setOnClickListener(b -> clearFilters());

        buildTypeChips();
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                query = s.toString().trim().toLowerCase(Locale.getDefault());
                refresh();
            }
        });
        v.findViewById(R.id.filter_btn).setOnClickListener(b -> showFilterSheet());
        attachSwipeToDelete();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void buildTypeChips() {
        typeChips.removeAllViews();
        addTypeChip("All types", null);
        for (ItemType t : ItemType.values()) addTypeChip(t.label, t);
        typeChips.setOnCheckedStateChangeListener((g, ids) -> {
            if (ids.isEmpty()) return;
            Chip c = g.findViewById(ids.get(0));
            typeFilter = (ItemType) c.getTag();
            refresh();
        });
    }

    private void addTypeChip(String label, ItemType type) {
        Chip c = new Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle);
        c.setId(View.generateViewId());
        c.setText(label);
        c.setTag(type);
        c.setCheckable(true);
        if (type != null) c.setChipIconResource(type.iconRes);
        c.setChecked(type == typeFilter);
        typeChips.addView(c);
    }

    private void refresh() {
        List<LifeItem> all = ItemStore.get(requireContext()).all();
        List<LifeItem> out = new ArrayList<>();
        for (LifeItem i : all) {
            if (!query.isEmpty() && !i.title.toLowerCase(Locale.getDefault()).contains(query)
                    && !i.notes.toLowerCase(Locale.getDefault()).contains(query)) continue;
            if (typeFilter != null && i.itemType != typeFilter) continue;
            if ("Active".equals(statusFilter) && i.isCompleted()) continue;
            if ("Completed".equals(statusFilter) && !i.isCompleted()) continue;
            if (!"All".equals(dateFilter) && !dateFilter.equals(ItemStore.dateGroup(i))) continue;
            out.add(i);
        }
        ItemStore.sort(out);
        List<ItemAdapter.Row> rows = new ArrayList<>();
        for (LifeItem i : out) rows.add(ItemAdapter.Row.item(i));
        adapter.submit(rows);

        resultCount.setText(out.size() + (out.size() == 1 ? " item" : " items") + " · " + statusFilter
                + ("All".equals(dateFilter) ? "" : " · " + dateFilter));
        empty.setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
        list.setVisibility(out.isEmpty() ? View.GONE : View.VISIBLE);
        boolean filtered = !"Active".equals(statusFilter) || !"All".equals(dateFilter);
        filterDot.setVisibility(filtered ? View.VISIBLE : View.GONE);
    }

    private void clearFilters() {
        query = "";
        search.setText("");
        typeFilter = null;
        statusFilter = "Active";
        dateFilter = "All";
        buildTypeChips();
        refresh();
    }

    private void showFilterSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        View v = getLayoutInflater().inflate(R.layout.sheet_filter, null);
        ChipGroup statusGroup = v.findViewById(R.id.status_group);
        ChipGroup dateGroup = v.findViewById(R.id.date_group);
        fillChoice(statusGroup, STATUSES, statusFilter);
        fillChoice(dateGroup, DATES, dateFilter);
        v.findViewById(R.id.sheet_apply).setOnClickListener(b -> {
            statusFilter = selected(statusGroup, statusFilter);
            dateFilter = selected(dateGroup, dateFilter);
            refresh();
            sheet.dismiss();
        });
        v.findViewById(R.id.sheet_clear).setOnClickListener(b -> {
            clearFilters();
            sheet.dismiss();
        });
        sheet.setContentView(v);
        sheet.show();
    }

    private void fillChoice(ChipGroup group, String[] options, String current) {
        for (String o : options) {
            Chip c = new Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle);
            c.setId(View.generateViewId());
            c.setText(o);
            c.setCheckable(true);
            c.setChecked(o.equals(current));
            group.addView(c);
        }
    }

    private static String selected(ChipGroup g, String fallback) {
        int id = g.getCheckedChipId();
        if (id == View.NO_ID) return fallback;
        return ((Chip) g.findViewById(id)).getText().toString();
    }

    private void attachSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder t) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {
                LifeItem item = adapter.rowAt(vh.getBindingAdapterPosition()).item;
                ItemActions.confirmDelete(requireActivity(), item, () -> {
                    refresh();
                    ((MainActivity) requireActivity()).snack(getString(R.string.deleted_msg)).show();
                }, AllItemsFragment.this::refresh);
            }
        }).attachToRecyclerView(list);
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
