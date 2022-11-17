package com.artuok.appwork.fragmets;

import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.CreateActivity;
import com.artuok.appwork.MainActivity;
import com.artuok.appwork.R;
import com.artuok.appwork.adapters.BottomEventAdapter;
import com.artuok.appwork.db.DbHelper;
import com.artuok.appwork.library.CalendarWeekView;
import com.artuok.appwork.objects.TaskEvent;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CalendarFragment extends Fragment {

    private List<CalendarWeekView.EventsTask> elements;
    CalendarWeekView weekView;
    com.artuok.appwork.library.Calendar calendarV;

    TextView schedule, calendar;
    List<TaskEvent> element;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_calendar, container, false);

        elements = new ArrayList<>();
        element = new ArrayList<>();
        weekView = root.findViewById(R.id.weekly);
        calendarV = root.findViewById(R.id.n_calendar);
        schedule = root.findViewById(R.id.schedule);
        calendar = root.findViewById(R.id.calendar);

        PushDownAnim.setPushDownAnimTo(schedule)
                .setDurationPush(100)
                .setScale(0.98f)
                .setOnClickListener(view -> {
                    weekView.setVisibility(View.VISIBLE);
                    calendarV.setVisibility(View.GONE);
                });

        PushDownAnim.setPushDownAnimTo(calendar)
                .setDurationPush(100)
                .setScale(0.98f)
                .setOnClickListener(view -> {
                    weekView.setVisibility(View.GONE);
                    calendarV.setVisibility(View.VISIBLE);
                });

        setEvents();
        calendarEvents();

        Calendar c = Calendar.getInstance();
        long time = (c.get(Calendar.HOUR_OF_DAY) * 60 * 60) + (c.get(Calendar.MINUTE) * 60);
        weekView.setViewRegisterListener(() -> weekView.scrollAt(time));

        calendarV.addOnDateClickListener(this::showEvents);
        weekView.setDateListener(() -> {
            ((MainActivity) requireActivity()).navigateTo(3);
        });
        weekView.setSelectListener(this::startCreateActivity);

        return root;
    }

    public void startCreateActivity(CalendarWeekView.EventsTask e) {
        Intent intent = new Intent(requireActivity(), CreateActivity.class);
        if (e != null) {
            intent.putExtra("day", e.getDay());
            intent.putExtra("hour", e.getHour());
            intent.putExtra("duration", e.getDuration());
        }

        startActivity(intent);
    }

    public void NotifyChanged() {
        elements = new ArrayList<>();
        setEvents();
    }

    public void setEvents() {
        DbHelper dbHelper = new DbHelper(requireActivity());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.t_event, null);
        if (cursor.moveToFirst()) {
            do {
                String title = cursor.getString(1);
                int day = cursor.getInt(2);
                long time = cursor.getLong(3);
                long duration = cursor.getLong(4);
                int type = cursor.getInt(5);
                int subject = cursor.getInt(6);

                elements.add(new CalendarWeekView.EventsTask(day, time, duration, type, title));
            } while (cursor.moveToNext());
        }

        weekView.setEvents(elements);
        cursor.close();
    }

    public void calendarEvents() {
        DbHelper dbHelper = new DbHelper(requireActivity());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<TaskEvent> e = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT * FROM " + DbHelper.t_task, null);
        if (c.moveToFirst()) {
            do {
                String ti = c.getString(2);
                String[] t = c.getString(3).split(" ");

                String[] dat = t[0].split("-");
                int year = Integer.parseInt(dat[0]);
                int month = Integer.parseInt(dat[1]) - 1;
                int day = Integer.parseInt(dat[2]);
                String[] timed = t[1].split(":");
                int hour = Integer.parseInt(timed[0]);
                int minute = Integer.parseInt(timed[1]);

                Calendar a = Calendar.getInstance();

                a.set(year, month, day, hour, minute);

                long tim = a.getTimeInMillis();
                String sub = c.getString(4);

                Cursor b = db.rawQuery("SELECT color FROM " + DbHelper.t_subjects + " WHERE name = '" + sub + "'", null);

                int color = 0;
                if (b.moveToFirst()) {
                    color = b.getInt(0);
                }
                e.add(new TaskEvent(ti, "", tim, color));

                b.close();
            } while (c.moveToNext());
        }

        calendarV.setEvents(e);

        c.close();
    }

    public void showEvents(int dd, int mm, int yyyy) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireActivity());
        BottomSheetBehavior<View> behavior;
        View bottomSheet = LayoutInflater.from(requireActivity()).inflate(R.layout.event_bottom_sheet_layout, null);
        dialog.setContentView(bottomSheet);


        behavior = BottomSheetBehavior.from((View) bottomSheet.getParent());

        behavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        LinearLayoutManager manager = new LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false);
        BottomEventAdapter adapter = new BottomEventAdapter(requireActivity(), element);

        LinearLayout layout = dialog.findViewById(R.id.bottom_sheet_layout);
        RecyclerView recyclerView = dialog.findViewById(R.id.recycler);

        recyclerView.setLayoutManager(manager);

        element.clear();
        loadEvents(dd, mm, yyyy);


        if (element.size() != 0) {
            dialog.show();
            recyclerView.setAdapter(adapter);

            assert layout != null;
            layout.setMinimumHeight(Resources.getSystem().getDisplayMetrics().heightPixels / 2);
        }

    }

    public void loadEvents(int dd, int mm, int yyyy) {
        DbHelper dbHelper = new DbHelper(requireActivity());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String m = mm < 10 ? "0" + (mm + 1) : "" + (mm + 1);
        String d = dd < 10 ? "0" + dd : "" + dd;

        String date = yyyy + "-" + m + "-" + d + " 00:00:00";
        String dated = yyyy + "-" + m + "-" + d + " 33:59:59";
        Cursor c = db.rawQuery("SELECT * FROM " + DbHelper.t_task + " WHERE end_date BETWEEN '" + date + "' AND '" + dated + "'", null);
        if (c.moveToFirst()) {
            do {
                String ti = c.getString(2);
                String[] t = c.getString(3).split(" ");

                String[] dat = t[0].split("-");
                int year = Integer.parseInt(dat[0]);
                int month = Integer.parseInt(dat[1]);
                int day = Integer.parseInt(dat[2]);
                String[] timed = t[1].split(":");
                int hour = Integer.parseInt(timed[0]);
                int minute = Integer.parseInt(timed[1]);

                Calendar a = Calendar.getInstance();

                a.set(year, month, day, hour, minute);

                long tim = a.getTimeInMillis();
                String sub = c.getString(4);

                Cursor b = db.rawQuery("SELECT color FROM " + DbHelper.t_subjects + " WHERE name = '" + sub + "'", null);

                int color = 0;
                if (b.moveToFirst()) {
                    color = b.getInt(0);
                }
                element.add(new TaskEvent(ti, "", tim, color));

                b.close();
            } while (c.moveToNext());
        }
        c.close();
    }
}