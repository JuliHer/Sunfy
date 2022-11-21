package com.artuok.appwork;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artuok.appwork.adapters.ScheduleAdapter;
import com.artuok.appwork.adapters.SubjectAdapter;
import com.artuok.appwork.db.DbHelper;
import com.artuok.appwork.fragmets.homeFragment;
import com.artuok.appwork.library.CalendarWeekView;
import com.artuok.appwork.objects.ItemSubjectElement;
import com.artuok.appwork.objects.SubjectElement;
import com.artuok.appwork.services.AlarmWorkManager;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class CreateActivity extends AppCompatActivity {


    private RecyclerView recyclerView;
    private List<CalendarWeekView.EventsTask> elements;
    private ScheduleAdapter adapter;
    private ScheduleAdapter.OnClickListener listener;
    private ScheduleAdapter.OnClickListener removeListener;


    int posModify = 0;
    int dayModify = 0;
    long startModify = 0;
    long endModify = 0;

    int color = 0;

    TextView textDay, subject;
    String subject_txt = "";
    ImageView colorD;

    TextView day, hours;


    CardView today;


    private int sDay;
    private long sHour;
    private long sDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);

        elements = new ArrayList<>();
        setListeners();
        adapter = new ScheduleAdapter(this, elements, listener, removeListener);
        LinearLayoutManager manager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView = findViewById(R.id.times_recycler);
        subject = findViewById(R.id.subject_text);
        day = findViewById(R.id.day_date);
        hours = findViewById(R.id.time_date);
        today = findViewById(R.id.today);

        recyclerView.setHasFixedSize(false);
        recyclerView.setLayoutManager(manager);

        ImageView backStack = findViewById(R.id.create_back);


        backStack.setOnClickListener(view -> finish());

        LinearLayout add = findViewById(R.id.add_recurrence);
        PushDownAnim.setPushDownAnimTo(add)
                .setDurationPush(100)
                .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                .setOnClickListener(view -> addElement());

        LinearLayout subjectb = findViewById(R.id.subject_button);

        PushDownAnim.setPushDownAnimTo(subjectb)
                .setDurationPush(100)
                .setScale(PushDownAnim.MODE_SCALE, 0.99f)
                .setOnClickListener(view -> setSelectSubject(subject));

        TextView act = findViewById(R.id.addevent);
        PushDownAnim.setPushDownAnimTo(today)
                .setDurationPush(100)
                .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                .setOnClickListener(view -> {
                    showDialog(null, true);
                });

        PushDownAnim.setPushDownAnimTo(act)
                .setDurationPush(100)
                .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                .setOnClickListener(view -> setEvents());


        setElements();
    }

    private void setEvents() {
        if (!Objects.equals(subject_txt, "")) {
            Bundle extras = getIntent().getExtras();
            insert(subject_txt, sDay, sHour, sDuration, color);
            for (CalendarWeekView.EventsTask e : elements) {
                insert(subject_txt, e.getDay(), e.getHour(), e.getDuration(), color);
            }
            Intent i = new Intent();
            i.putExtra("requestCode", 1);
            setResult(RESULT_OK, i);

            finish();
        }
    }

    private void insert(String title, int dow, long time, long dur, int color) {
        DbHelper dbHelper = new DbHelper(this);
        SQLiteDatabase dbw = dbHelper.getWritableDatabase();
        SQLiteDatabase dbr = dbHelper.getReadableDatabase();

        ContentValues values = new ContentValues();


        values.put("title", title);
        values.put("day_of_week", dow);
        values.put("time", time);
        values.put("duration", dur);
        values.put("type", color);

        title = DatabaseUtils.sqlEscapeString(title);
        Cursor c = dbr.rawQuery("SELECT id FROM " + DbHelper.t_subjects + " WHERE name = " + title + "", null);
        int idSubject = -1;
        if (c.moveToFirst()) {
            idSubject = c.getInt(0);
        }
        values.put("subject", idSubject);

        setAlarmSchedule();
        dbw.insert(DbHelper.t_event, null, values);
        c.close();
    }

    private void setListeners() {
        listener = (view, pos) -> {
            posModify = pos;
            showDialog(elements.get(pos), false);
        };

        removeListener = (view, pos) -> {
            elements.remove(pos);
            adapter.notifyItemRemoved(pos);
        };
    }

    private void setElements() {
        if (getIntent().getExtras() != null) {
            Bundle extras = getIntent().getExtras();
            sHour = extras.getLong("hour", 0);
            sDuration = extras.getLong("duration", 0);
            sDay = extras.getInt("day", 0);

            updateMainDate();
        }

        recyclerView.setAdapter(adapter);
    }

    private void addElement() {
        if (elements.size() < 7) {
            int day;
            long hour;
            long duration;
            if (elements.size() <= 0) {
                day = (sDay + 1) % 7;
                hour = sHour;
                duration = sDuration;
            } else {
                day = (elements.get(elements.size() - 1).getDay() + 1) % 7;
                hour = elements.get(elements.size() - 1).getHour();
                duration = elements.get(elements.size() - 1).getDuration();
            }
            CalendarWeekView.EventsTask e = new CalendarWeekView.EventsTask(day, hour, duration, 1, "");
            elements.add(e);
            elements.get(0).setColor(0);
            adapter.notifyDataSetChanged();
            Log.d("eSize", elements.size() + "");
        }
    }

    void showDialog(CalendarWeekView.EventsTask e, boolean f) {
        Dialog edit = new Dialog(this);
        edit.requestWindowFeature(Window.FEATURE_NO_TITLE);
        edit.setContentView(R.layout.bottom_recurrence_layout);
        edit.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        edit.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        edit.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        edit.getWindow().setGravity(Gravity.BOTTOM);

        if (f) {
            startModify = sHour;
            endModify = sDuration + sHour;
            dayModify = sDay;
        } else {
            startModify = elements.get(posModify).getHour();
            endModify = elements.get(posModify).getDuration() + elements.get(posModify).getHour();
            dayModify = elements.get(posModify).getDay();
        }

        LinearLayout edi = edit.findViewById(R.id.edit_create);
        edi.setVisibility(View.VISIBLE);

        textDay = edit.findViewById(R.id.day_of_recurrence);
        TextView start = edit.findViewById(R.id.start_hour);
        TextView end = edit.findViewById(R.id.end_hour);

        LinearLayout days = edit.findViewById(R.id.day_of_frecuency);

        PushDownAnim.setPushDownAnimTo(days)
                .setDurationPush(100)
                .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                .setOnClickListener(view -> showDaySelector());
        start.setOnClickListener(view -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (timePicker, i, i1) -> {
                startModify = (i * 60L * 60L) + (i1 * 60L);
                if (startModify > 85500) {
                    startModify = 85500;
                }
                if ((endModify - startModify) < 900) {
                    endModify = startModify + 900;
                    if ((startModify + 900) >= 86400) {
                        endModify = 0;
                    }
                    String mod = convertMillisInTime(endModify);
                    end.setText(mod);
                }
                String desc = convertMillisInTime(startModify);
                start.setText(desc);

            }, 0, 0, false);
            timePickerDialog.show();
        });

        end.setOnClickListener(view -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(this, (timePicker, i, i1) -> {
                endModify = (i * 60L * 60L) + (i1 * 60L);
                if (endModify < 900) {
                    endModify = 900;
                }
                if ((endModify - startModify) < 900) {
                    startModify = endModify - 900;
                    String mod = convertMillisInTime(startModify);
                    start.setText(mod);
                }
                String desc = convertMillisInTime(endModify);
                end.setText(desc);
            }, 0, 0, false);
            timePickerDialog.show();
        });

        if (f) {
            String dayDate = homeFragment.getDayOfWeek(this, sDay + 1);
            textDay.setText(dayDate);

            String desc = convertMillisInTime(sHour);


            start.setText(desc);

            long hourEndMillis = sDuration + sHour;
            desc = convertMillisInTime(hourEndMillis);

            end.setText(desc);
        } else {
            String dayDate = homeFragment.getDayOfWeek(this, e.getDay() + 1);
            textDay.setText(dayDate);

            String desc = convertMillisInTime(e.getHour());


            start.setText(desc);

            long hourEndMillis = e.getDuration() + e.getHour();
            desc = convertMillisInTime(hourEndMillis);

            end.setText(desc);
        }


        Button accept = edit.findViewById(R.id.accept);
        PushDownAnim
                .setPushDownAnimTo(accept)
                .setDurationPush(100)
                .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                .setOnClickListener(view -> {
                    if (f) {
                        sDay = dayModify;
                        sHour = startModify;
                        sDuration = endModify - startModify;
                        updateMainDate();
                    } else {
                        elements.get(posModify).setDay(dayModify);
                        elements.get(posModify).setHour(startModify);
                        elements.get(posModify).setDuration(endModify - startModify);
                        adapter.notifyItemChanged(posModify);
                    }

                    edit.dismiss();
                });

        edit.show();
    }

    private void updateMainDate() {
        long hourStartMillis = sHour;
        int hour = (int) (hourStartMillis / 3600);
        int minute = (int) (hourStartMillis / 60) % 60;
        hour = hour % 24;
        String tm = hour > 11 ? "PM" : "AM";
        hour = hour > 12 ? hour - 12 : hour;
        if (hour == 0) {
            hour = 12;
        }
        String min = minute < 10 ? "0" + minute : minute + "";

        String desc = hour + ":" + min + " " + tm + " -> ";

        long hourEndMillis = sDuration + sHour;
        hour = (int) (hourEndMillis / 3600);
        minute = (int) (hourEndMillis / 60) % 60;
        hour = hour % 24;
        tm = hour > 11 ? "PM" : "AM";
        hour = hour > 12 ? hour - 12 : hour;
        if (hour == 0) {
            hour = 12;
        }

        min = minute < 10 ? "0" + minute : minute + "";

        desc += hour + ":" + min + " " + tm;

        String mon = homeFragment.getDayOfWeek(this, sDay + 1);
        day.setText(mon);
        hours.setText(desc);
    }

    String convertMillisInTime(long timeInMillis) {
        int hour = (int) (timeInMillis / 3600);
        int minute = (int) (timeInMillis / 60) % 60;
        hour = hour % 24;
        String tm = hour > 11 ? "PM" : "AM";
        hour = hour > 12 ? hour - 12 : hour;
        if (hour == 0) {
            hour = 12;
        }
        String min = minute < 10 ? "0" + minute : minute + "";

        return hour + ":" + min + " " + tm;
    }

    void showDaySelector() {
        Dialog daySelector = new Dialog(this);
        daySelector.requestWindowFeature(Window.FEATURE_NO_TITLE);
        daySelector.setContentView(R.layout.bottom_recurrence_layout);
        daySelector.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        daySelector.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        daySelector.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        daySelector.getWindow().setGravity(Gravity.BOTTOM);

        LinearLayout edi = daySelector.findViewById(R.id.days);
        edi.setVisibility(View.VISIBLE);

        TextView sunday = daySelector.findViewById(R.id.sunday);
        sunday.setOnClickListener(view -> {
            dayModify = 0;
            if (textDay != null) {
                textDay.setText(homeFragment.getDayOfWeek(this, dayModify + 1));
            }
            daySelector.dismiss();
        });
        TextView monday = daySelector.findViewById(R.id.monday);
        monday.setOnClickListener(view -> {
            dayModify = 1;
            if (textDay != null) {
                textDay.setText(homeFragment.getDayOfWeek(this, dayModify + 1));
            }
            daySelector.dismiss();
        });
        TextView tuesday = daySelector.findViewById(R.id.tuesday);
        tuesday.setOnClickListener(view -> {
            dayModify = 2;
            if (textDay != null) {
                textDay.setText(homeFragment.getDayOfWeek(this, dayModify + 1));
            }
            daySelector.dismiss();
        });
        TextView wednesday = daySelector.findViewById(R.id.wednesday);
        wednesday.setOnClickListener(view -> {
            dayModify = 3;
            if (textDay != null) {
                textDay.setText(homeFragment.getDayOfWeek(this, dayModify + 1));
            }
            daySelector.dismiss();
        });
        TextView thursday = daySelector.findViewById(R.id.thursday);
        thursday.setOnClickListener(view -> {
            dayModify = 4;
            if (textDay != null) {
                textDay.setText(homeFragment.getDayOfWeek(this, dayModify + 1));
            }
            daySelector.dismiss();
        });
        TextView friday = daySelector.findViewById(R.id.friday);
        friday.setOnClickListener(view -> {
            dayModify = 5;
            if (textDay != null) {
                textDay.setText(homeFragment.getDayOfWeek(this, dayModify + 1));
            }
            daySelector.dismiss();
        });
        TextView saturday = daySelector.findViewById(R.id.saturday);
        saturday.setOnClickListener(view -> {
            dayModify = 6;
            if (textDay != null) {
                textDay.setText(homeFragment.getDayOfWeek(this, dayModify + 1));
            }
            daySelector.dismiss();

        });


        daySelector.show();
    }

    private void setSelectSubject(TextView a) {
        Dialog subjectDialog = new Dialog(this);
        subjectDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        subjectDialog.setContentView(R.layout.bottom_sheet_layout);
        subjectDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subjectDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        subjectDialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        subjectDialog.getWindow().setGravity(Gravity.BOTTOM);

        RecyclerView recyclerView = subjectDialog.findViewById(R.id.subjects_recycler);
        LinearLayoutManager manager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        final List<ItemSubjectElement> elements = getSubjects();
        SubjectAdapter adapter = new SubjectAdapter(this, elements, (view, position) -> {
            subject_txt = ((SubjectElement) elements.get(position).getObject()).getName();
            color = ((SubjectElement) elements.get(position).getObject()).getColor();
            subjectDialog.dismiss();
            a.setText(subject_txt);
        });

        LinearLayout add = subjectDialog.findViewById(R.id.add_subject);
        add.setVisibility(View.GONE);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        subjectDialog.show();
    }

    private List<ItemSubjectElement> getSubjects() {
        DbHelper dbHelper = new DbHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        List<ItemSubjectElement> elements = new ArrayList<>();

        Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.t_subjects + " ORDER BY name DESC", null);
        if (cursor.moveToFirst()) {
            do {
                elements.add(new ItemSubjectElement(new SubjectElement(cursor.getString(1), cursor.getInt(2)), 2));
            } while (cursor.moveToNext());
        }

        cursor.close();

        return elements;
    }

    void setAlarmSchedule() {
        DbHelper dbHelper = new DbHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor v = db.rawQuery("SELECT * FROM " + DbHelper.t_event + " ORDER BY day_of_week ASC, time ASC", null);
        Calendar c = Calendar.getInstance();
        long time = 0;
        long duration = 0;
        int day = -1;
        String name = "";
        long hour = (60 * 60 * c.get(Calendar.HOUR_OF_DAY)) + (60 * c.get(Calendar.MINUTE));
        int dow = c.get(Calendar.DAY_OF_WEEK) - 1;
        if (v.moveToFirst()) {
            do {
                if (v.getLong(3) > (hour + (60 * 60)) && dow == v.getInt(2)) {
                    time = v.getLong(3) * 1000;
                    day = v.getInt(2);
                    time = time - (hour * 1000);
                    duration = v.getLong(4) * 1000;
                    name = v.getString(1);
                    break;
                } else if (dow < v.getInt(2)) {
                    time = v.getLong(3) * 1000;
                    day = v.getInt(2);
                    int r = (day + 1) - (dow + 1);
                    time = (r * 86400000L) + (time) - (hour * 1000);
                    duration = v.getLong(4) * 1000;
                    name = v.getString(1);
                    break;
                }
            } while (v.moveToNext());
            if (!(!name.equals("") && time != 0 && duration != 0)) {
                v.moveToFirst();
                do {
                    time = v.getLong(3) * 1000;
                    day = v.getInt(2);
                    int r = 7 - (dow + 1) + (day + 1);
                    time = (r * 86400000L) + (time) - (hour * 1000);
                    duration = v.getLong(4) * 1000;
                    name = v.getString(1);
                    if (duration != 0) {
                        break;
                    }
                } while (v.moveToNext());
            }
        }
        if (!name.equals("") && time != 0 && duration != 0) {
            setNotify(name, time, duration);
        }
        v.close();
    }

    void setNotify(String name, long diff, long duration) {

        long start = Calendar.getInstance().getTimeInMillis() + diff;

        Intent notify = new Intent(this, AlarmWorkManager.class)
                .setAction(AlarmWorkManager.ACTION_EVENT);
        int days = (int) (diff / 1000 / 60 / 60 / 24);
        int hour = (int) (diff / 1000 / 60 / 60 % 24);
        int min = (int) (diff / 1000 / 60 % 60);
        Log.d("faltan", days + "d " + hour + " h" + min + " m");

        notify.putExtra("name", name);
        notify.putExtra("time", start);
        notify.putExtra("duration", duration);
        PendingIntent pendingNotify = PendingIntent.getBroadcast(
                this,
                1, notify,
                PendingIntent.FLAG_UPDATE_CURRENT);


        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingNotify);
        manager.setExact(AlarmManager.RTC_WAKEUP, start - (60 * 60 * 1000), pendingNotify);
    }
}