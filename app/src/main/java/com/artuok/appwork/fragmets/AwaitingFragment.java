package com.artuok.appwork.fragmets;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.MainActivity;
import com.artuok.appwork.R;
import com.artuok.appwork.ViewActivity;
import com.artuok.appwork.adapters.AwaitingAdapter;
import com.artuok.appwork.db.DbHelper;
import com.artuok.appwork.objects.AwaitingElement;
import com.artuok.appwork.objects.Item;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class AwaitingFragment extends Fragment {

    //recyclerView
    RecyclerView recyclerView;
    List<Item> elements;
    AwaitingAdapter adapter;
    LinearLayoutManager manager;
    TextView done, onHold, lose;
    AwaitingAdapter.OnClickListener listener;

    ItemTouchHelper.SimpleCallback touchHelper = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

            int position = viewHolder.getLayoutPosition();
            switch (direction) {
                case ItemTouchHelper.LEFT:
                    removeTask(position);
                    break;
                case ItemTouchHelper.RIGHT:
                    checkTask(position);
                    break;
            }
            ((MainActivity) requireActivity()).updateWidget();
            statistics();
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            new RecyclerViewSwipeDecorator.Builder(requireActivity(), c, recyclerView, viewHolder, dX, dY, actionState,
                    isCurrentlyActive)
                    .addSwipeLeftBackgroundColor(requireActivity().getColor(R.color.red_500))
                    .addSwipeLeftLabel(requireActivity().getString(R.string.delete))
                    .addSwipeLeftActionIcon(R.drawable.ic_trash)
                    .setSwipeLeftActionIconTint(requireActivity().getColor(R.color.white))
                    .setSwipeLeftLabelColor(requireActivity().getColor(R.color.white))

                    .addSwipeRightBackgroundColor(requireActivity().getColor(R.color.blue_400))
                    .addSwipeRightLabel(requireActivity().getString(R.string.check))
                    .addSwipeRightActionIcon(R.drawable.ic_hexagon)
                    .setSwipeRightActionIconTint(requireActivity().getColor(R.color.white))
                    .setSwipeRightLabelColor(requireActivity().getColor(R.color.white))
                    .create()
                    .decorate();
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.awaiting_fragment, container, false);

        setListener();
        elements = new ArrayList<>();
        adapter = new AwaitingAdapter(requireActivity(), elements);
        adapter.setOnClickListener(listener);
        manager = new LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false);
        recyclerView = root.findViewById(R.id.awaiting_recycler);
        done = root.findViewById(R.id.done_txt);
        onHold = root.findViewById(R.id.onHold_txt);
        lose = root.findViewById(R.id.losed_txt);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(manager);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(touchHelper);
        itemTouchHelper.attachToRecyclerView(recyclerView);


        statistics();
        loadAwaitings(true);

        return root;
    }

    void setListener() {
        listener = (view, p) -> {
            if (elements.get(p).getType() == 0) {
                AwaitingElement e = ((AwaitingElement) elements.get(p).getObject());
                Intent i = new Intent(requireActivity(), ViewActivity.class);
                i.putExtra("id", e.getId());
                startActivity(i);
            }

        };
    }

    void statistics() {
        DbHelper dbHelper = new DbHelper(requireActivity().getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.t_task + " WHERE status = '1'", null);

        Calendar c = Calendar.getInstance();

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dat = format.format(c.getTime());

        Cursor hold = db.rawQuery("SELECT * FROM " + DbHelper.t_task + " WHERE status = '0' AND end_date >= '" + dat + "'", null);
        Cursor lose = db.rawQuery("SELECT * FROM " + DbHelper.t_task + " WHERE status = '0' AND end_date < '" + dat + "'", null);


        String d = cursor.getCount() + "";
        String l = lose.getCount() + "";
        String h = requireActivity().getString(R.string.on_hold_string) + ": " + hold.getCount() + "";
        done.setText(d);
        onHold.setText(h);
        this.lose.setText(l);
        //elements.add(new Item(new StatisticsElement(cursor.getCount(), hold.getCount(), lose.getCount()), 1));
        cursor.close();
        hold.close();
        lose.close();
    }

    public void NotifyChanged() {
        if (elements != null) {
            elements.clear();
            statistics();
            loadAwaitings(false);
        }
    }

    void loadAwaitings(boolean first) {
        DbHelper dbHelper = new DbHelper(requireActivity().getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Calendar ti = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String min = format.format(ti.getTime());

        Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.t_task + " WHERE status = '0' AND end_date > '" + min + "' ORDER BY end_date ASC", null);

        if (cursor.moveToFirst()) {
            do {
                Calendar c = Calendar.getInstance();
                boolean e = true;
                String[] t = cursor.getString(3).split(" ");

                String[] date = t[0].split("-");
                int year = Integer.parseInt(date[0]);
                int month = Integer.parseInt(date[1]);
                int day = Integer.parseInt(date[2]);

                String[] time = t[1].split(":");
                int hour = Integer.parseInt(time[0]);
                int minute = Integer.parseInt(time[1]);

                c.set(year, (month - 1), day, hour, minute);

                String mm = month < 10 ? "0" + month : "" + month;
                String dd = day < 10 ? "0" + day : "" + day;
                String datetime = dd + " " + homeFragment.getMonthMinor(requireActivity(), (month - 1)) + " " + year + " ";

                String mn = minute < 10 ? "0" + minute : "" + minute;
                if (hour > 12) {
                    hour = hour - 12;
                    datetime += hour + ":" + mn + " PM";
                } else {
                    datetime += hour + ":" + mn;

                    if (hour == 12) {
                        datetime += " PM";
                    } else {
                        datetime += " AM";
                    }
                }

                String status = "";
                if (c.getTimeInMillis() < Calendar.getInstance().getTimeInMillis()) {
                    e = false;
                } else {
                    status = ((c.getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) / 86400000) + "";
                }

                boolean done = cursor.getInt(6) == 1;

                elements.add(new Item(new AwaitingElement(cursor.getInt(0), cursor.getString(2), cursor.getString(4), datetime, status, cursor.getString(5), done, e), 0));
            } while (cursor.moveToNext());
        }
        loadLate();
        loadDone();
        cursor.close();
        if (first) {
            recyclerView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    void loadLate() {
        DbHelper dbHelper = new DbHelper(requireActivity().getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Calendar ti = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String min = format.format(ti.getTime());
        Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.t_task + " WHERE status = '0' AND end_date <= '" + min + "' ORDER BY end_date ASC", null);

        if (cursor.moveToFirst()) {
            do {
                Calendar c = Calendar.getInstance();
                boolean e = true;
                String[] t = cursor.getString(3).split(" ");

                String[] date = t[0].split("-");
                int year = Integer.parseInt(date[0]);
                int month = Integer.parseInt(date[1]);
                int day = Integer.parseInt(date[2]);

                String[] time = t[1].split(":");
                int hour = Integer.parseInt(time[0]);
                int minute = Integer.parseInt(time[1]);

                c.set(year, (month - 1), day, hour, minute);

                String dd = day < 10 ? "0" + day : "" + day;
                String datetime = dd + " " + homeFragment.getMonthMinor(requireActivity(), (month - 1)) + " " + year + " ";

                String mn = minute < 10 ? "0" + minute : "" + minute;
                if (hour > 12) {
                    hour = hour - 12;
                    datetime += hour + ":" + mn + " PM";
                } else {
                    datetime += hour + ":" + mn;

                    if (hour == 12) {
                        datetime += " PM";
                    } else {
                        datetime += " AM";
                    }
                }

                String status = "";
                if (c.getTimeInMillis() < Calendar.getInstance().getTimeInMillis()) {
                    e = false;
                } else {
                    status = ((c.getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) / 86400000) + "";
                }

                boolean done = cursor.getInt(6) == 1;

                elements.add(new Item(new AwaitingElement(cursor.getInt(0), cursor.getString(2), cursor.getString(4), datetime, status, cursor.getString(5), done, e), 0));
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    void loadDone() {
        DbHelper dbHelper = new DbHelper(requireActivity().getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Calendar ti = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.t_task + " WHERE status = '1' ORDER BY end_date ASC", null);

        if (cursor.moveToFirst()) {
            do {
                Calendar c = Calendar.getInstance();
                boolean e = true;
                String[] t = cursor.getString(3).split(" ");

                String[] date = t[0].split("-");
                int year = Integer.parseInt(date[0]);
                int month = Integer.parseInt(date[1]);
                int day = Integer.parseInt(date[2]);

                String[] time = t[1].split(":");
                int hour = Integer.parseInt(time[0]);
                int minute = Integer.parseInt(time[1]);

                c.set(year, (month - 1), day, hour, minute);

                String mm = month < 10 ? "0" + month : "" + month;
                String dd = day < 10 ? "0" + day : "" + day;
                String datetime = dd + " " + homeFragment.getMonthMinor(requireActivity(), (month - 1)) + " " + year + " ";

                String mn = minute < 10 ? "0" + minute : "" + minute;
                if (hour > 12) {
                    hour = hour - 12;
                    datetime += hour + ":" + mn + " PM";
                } else {
                    datetime += hour + ":" + mn;

                    if (hour == 12) {
                        datetime += " PM";
                    } else {
                        datetime += " AM";
                    }
                }

                String status = "";
                if (c.getTimeInMillis() < Calendar.getInstance().getTimeInMillis()) {
                    e = false;
                } else {
                    status = ((c.getTimeInMillis() - Calendar.getInstance().getTimeInMillis()) / 86400000) + "";
                }

                boolean done = cursor.getInt(6) == 1;

                elements.add(new Item(new AwaitingElement(cursor.getInt(0), cursor.getString(2), cursor.getString(4), datetime, status, cursor.getString(5), done, e), 0));
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    void removeTask(int position) {
        DbHelper dbHelper = new DbHelper(requireActivity());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int id = ((AwaitingElement) elements.get(position).getObject()).getId();

        Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.t_task + " WHERE id = '" + id + "'", null);
        if (cursor.moveToFirst()) {
            SQLiteDatabase db2 = dbHelper.getWritableDatabase();
            db2.delete(DbHelper.t_task, " id = '" + id + "'", null);
        }

        elements.remove(position);
        cursor.close();
        adapter.notifyItemRemoved(position);
        ((MainActivity) requireActivity()).notifyAllChanged();
    }

    void checkTask(int position) {
        DbHelper dbHelper = new DbHelper(requireActivity());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int id = ((AwaitingElement) elements.get(position).getObject()).getId();

        Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.t_task + " WHERE id = '" + id + "'", null);
        if (cursor.moveToFirst() && cursor.getCount() == 1) {
            boolean s = cursor.getInt(6) > 0;
            ContentValues values = new ContentValues();
            ((AwaitingElement) elements.get(position).getObject()).setStatusB(!s);

            values.put("status", !s);
            SQLiteDatabase db2 = dbHelper.getWritableDatabase();
            db2.update(DbHelper.t_task, values, " id = '" + id + "'", null);
        }

        cursor.close();
        adapter.notifyItemChanged(position);
    }
}