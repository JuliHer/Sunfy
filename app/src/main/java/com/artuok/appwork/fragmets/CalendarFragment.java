package com.artuok.appwork.fragmets;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.CreateActivity;
import com.artuok.appwork.MainActivity;
import com.artuok.appwork.R;
import com.artuok.appwork.adapters.BottomEventAdapter;
import com.artuok.appwork.adapters.ScheduleAdapter;
import com.artuok.appwork.db.DbHelper;
import com.artuok.appwork.library.CalendarWeekView;
import com.artuok.appwork.objects.Item;
import com.artuok.appwork.objects.TaskEvent;
import com.artuok.appwork.objects.TextElement;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CalendarFragment extends Fragment {

    private List<CalendarWeekView.EventsTask> elements;
    CalendarWeekView weekView;
    com.artuok.appwork.library.Calendar calendarV;

    TextView schedule, calendar;
    List<Item> element;

    RecyclerView recyclerView;
    BottomEventAdapter adapter;
    private boolean scheduleVisible = false;

    int defaultColor = 0;
    int selectColor = 0;

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
        recyclerView = root.findViewById(R.id.recyclerdate);
        defaultColor = schedule.getTextColors().getDefaultColor();
        TypedArray pa = requireActivity().obtainStyledAttributes(R.styleable.AppCustomAttrs);
        selectColor = pa.getColor(R.styleable.AppCustomAttrs_backgroundDialog, Color.WHITE);
        pa.recycle();
        registLaunchers();
        LinearLayoutManager manager = new LinearLayoutManager(requireActivity(), RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(manager);
        recyclerView.setHasFixedSize(true);
        Calendar t = Calendar.getInstance();
        adapter = new BottomEventAdapter(requireActivity(), element);

        loadEvents(t.get(Calendar.DAY_OF_MONTH), t.get(Calendar.MONTH), t.get(Calendar.YEAR));
        recyclerView.setAdapter(adapter);

        PushDownAnim.setPushDownAnimTo(schedule)
                .setDurationPush(100)
                .setScale(0.98f)
                .setOnClickListener(view -> {
                    weekView.setVisibility(View.VISIBLE);
                    calendarV.setVisibility(View.GONE);
                    selectPopup(1);
                    scheduleVisible = true;
                });

        PushDownAnim.setPushDownAnimTo(calendar)
                .setDurationPush(100)
                .setScale(0.98f)
                .setOnClickListener(view -> {
                    weekView.setVisibility(View.GONE);
                    calendarV.setVisibility(View.VISIBLE);
                    selectPopup(0);
                    scheduleVisible = false;
                });

        setEvents();
        calendarEvents();

        Calendar c = Calendar.getInstance();
        long time = (c.get(Calendar.HOUR_OF_DAY) * 60 * 60) + (c.get(Calendar.MINUTE) * 60);
        weekView.setViewRegisterListener(() -> weekView.scrollAt(time));

        calendarV.addOnDateClickListener((d, m, y) -> {
            loadEvents(d, m, y);
            adapter.notifyDataSetChanged();
        });
        weekView.setDateListener(c1 -> showSubjectInfo(c1.getEvent().getTitle()));
        weekView.setSelectListener(this::startCreateActivity);

        return root;
    }

    public boolean isScheduleVisible() {
        return scheduleVisible;
    }

    public void setScheduleVisible(boolean scheduleVisible) {
        this.scheduleVisible = scheduleVisible;
    }

    private void selectPopup(int i){
        calendar.setBackground(requireActivity().getDrawable(R.drawable.popup_buttons));
        schedule.setBackground(requireActivity().getDrawable(R.drawable.popup_buttons));


        calendar.setTextColor(defaultColor);
        schedule.setTextColor(defaultColor);

        if(i == 0){
            calendar.setBackground(requireActivity().getDrawable(R.drawable.popup_buttons_active));
            calendar.setTextColor(selectColor);
        }else{
            schedule.setBackground(requireActivity().getDrawable(R.drawable.popup_buttons_active));
            schedule.setTextColor(selectColor);
        }
    }



    public void startCreateActivity(CalendarWeekView.EventsTask e) {
        Intent intent = new Intent(requireActivity(), CreateActivity.class);
        if (e != null) {
            intent.putExtra("day", e.getDay());
            intent.putExtra("hour", e.getHour());
            intent.putExtra("duration", e.getDuration());
        }

        resultLauncher.launch(intent);
    }

    private void registLaunchers() {
        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data.getIntExtra("requestCode", 0) == 1) {
                            NotifyChanged();
                        }
                    }
                }
        );
    }

    ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data.getIntExtra("requestCode", 0) == 1) {
                        NotifyChanged();
                    }
                }
            }
    );

    public void NotifyChanged() {
        elements = new ArrayList<>();
        setEvents();
        calendarEvents();
    }

    public void setEvents() {
        DbHelper dbHelper = new DbHelper(requireActivity());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.t_event + " ORDER BY duration DESC", null);
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
        DbHelper dbHelper = new DbHelper(requireActivity().getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<TaskEvent> e = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT * FROM " + DbHelper.T_TASK, null);
        if (c.moveToFirst()) {
            do {
                String ti = c.getString(4);
                long tim = c.getLong(2);
                int sub = c.getInt(3);

                Cursor b = db.rawQuery("SELECT color FROM " + DbHelper.t_subjects + " WHERE id = " + sub, null);


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

    public void loadEvents(int dd, int mm, int yyyy) {
        DbHelper dbHelper = new DbHelper(requireActivity().getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Calendar ca = Calendar.getInstance();
        ca.set(yyyy, mm, dd, 0, 0, 0);
        element.clear();
        SimpleDateFormat format = new SimpleDateFormat("MMMM dd, yyyy");
        format.applyPattern("MMMM dd, yyyy");
        String dt = format.format(ca.getTime());
        long dated = ca.getTimeInMillis();
        ca.set(yyyy, mm, dd, 23, 59, 59);
        long dating = ca.getTimeInMillis();

        element.add(new Item(new TextElement(dt), 1));

        Cursor c = db.rawQuery("SELECT * FROM " + DbHelper.T_TASK + " WHERE end_date BETWEEN '" + dated + "' AND '" + dating + "' ORDER BY end_date ASC", null);
        if (c.moveToFirst()) {
            do {
                String ti = c.getString(4);
                long tim = c.getLong(2);
                int sub = c.getInt(3);

                Cursor b = db.rawQuery("SELECT color FROM " + DbHelper.t_subjects + " WHERE id = " + sub, null);
                int color = 0;
                if (b.moveToFirst()) {
                    color = b.getInt(0);
                }
                element.add(new Item(new TaskEvent(ti, "", tim, color), 0));
                b.close();
            } while (c.moveToNext());
        }
        c.close();
    }

    ScheduleAdapter adapterS;
    ScheduleAdapter.OnClickListener n;
    List<CalendarWeekView.EventsTask> elementS;

    private void setListener() {
        n = (view, pos) -> {
            removeEvent(elementS.get(pos).getId());
            elementS.remove(pos);
            adapterS.notifyItemRemoved(pos);
            ((MainActivity) requireActivity()).notifyAllChanged();
        };
    }

    private void removeEvent(int id) {
        DbHelper dbHelper = new DbHelper(requireActivity());
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.delete(DbHelper.t_event, "id = '" + id + "'", null);
    }


    private void showSubjectInfo(String subject) {
        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottom_task_layout);


        TextView title = dialog.findViewById(R.id.title_subject);
        title.setText(subject);
        LinearLayout delete = dialog.findViewById(R.id.deleteSubject);

        delete.setVisibility(View.GONE);

        PushDownAnim.setPushDownAnimTo(delete)
                .setDurationPush(100)
                .setScale(PushDownAnim.MODE_SCALE, 0.95f)
                .setOnClickListener(view -> {
                    dialog.dismiss();

                });

        RecyclerView r = dialog.findViewById(R.id.recycler);
        setListener();
        elementS = new ArrayList<>();

        elementS = getSSchedule(subject);
        adapterS = new ScheduleAdapter(
                requireActivity(),
                elementS,
                (view, pos) -> {

                },
                n);

        LinearLayoutManager m = new LinearLayoutManager(requireActivity().getApplicationContext(), RecyclerView.VERTICAL, false);
        r.setLayoutManager(m);
        r.setHasFixedSize(false);

        if (elementS != null)
            r.setAdapter(adapterS);

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }


    private List<CalendarWeekView.EventsTask> getSSchedule(String n) {
        List<CalendarWeekView.EventsTask> a = new ArrayList<>();
        DbHelper dbHelper = new DbHelper(requireActivity());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        n = DatabaseUtils.sqlEscapeString(n);
        Cursor c = db.rawQuery("SELECT * FROM " + DbHelper.t_subjects + " WHERE name = " + n + "", null);
        if (c.moveToFirst()) {
            int id = c.getInt(0);
            Cursor s = db.rawQuery("SELECT * FROM " + DbHelper.t_event + " WHERE subject = '" + id + "' ORDER BY day_of_week ASC, time ASC", null);

            if (s.moveToFirst()) {
                do {
                    int ids = s.getInt(0);
                    int dd = s.getInt(2);
                    long h = s.getLong(3);
                    long d = s.getLong(4);
                    int t = s.getInt(5);
                    String tt = s.getString(1);

                    a.add(new CalendarWeekView.EventsTask(ids, dd, h, d, t, tt));
                } while (s.moveToNext());
            }
            s.close();
        }

        c.close();

        return a;
    }
}