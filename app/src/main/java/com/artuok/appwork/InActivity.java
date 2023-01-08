package com.artuok.appwork;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.artuok.appwork.db.DbHelper;
import com.artuok.appwork.services.AlarmWorkManager;

import java.util.Calendar;

import kotlin.jvm.internal.Intrinsics;

public class InActivity extends AppCompatActivity {

    public static final String CHANNEL_ID_1 = "CHANNEL_1";
    public static final String CHANNEL_ID_2 = "CHANNEL_2";
    public static final String CHANNEL_ID_3 = "CHANNEL_3";
    public static final String CHANNEL_ID_4 = "CHANNEL_4";
    public static final String CHANNEL_ID_5 = "CHANNEL_5";
    public static final String GROUP_EVENTS = "com.artuok.appwork.EVENTS";
    public static final String GROUP_SUBJECTS = "com.artuok.appwork.SUBJECTS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_AppWork);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Preferences();

        createNotificationChannel();
        setAlarm();
        setAlarms();
        setAlarmSchedule();

        new Handler().postDelayed(this::loadMain, 500);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    void loadMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    void Preferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
        boolean b = sharedPreferences.getBoolean("DarkMode", false);

        if (b) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    void createNotificationChannel() {
        NotificationChannel notificationChannel1 = new NotificationChannel(CHANNEL_ID_1, "NOTES", NotificationManager.IMPORTANCE_HIGH);
        notificationChannel1.setDescription("Channel for remembers");
        NotificationChannel notificationChannel2 = new NotificationChannel(CHANNEL_ID_2, "Homework", NotificationManager.IMPORTANCE_HIGH);
        notificationChannel2.setDescription("Channel for remember when you need to do homework");
        NotificationChannel notificationChannel3 = new NotificationChannel(CHANNEL_ID_3, "Alarm", NotificationManager.IMPORTANCE_HIGH);
        notificationChannel3.setDescription("Alarm to do homework");
        NotificationChannel notificationChannel4 = new NotificationChannel(CHANNEL_ID_4, "Tomorrow Events", NotificationManager.IMPORTANCE_HIGH);
        notificationChannel4.setDescription("Events to do tomorrow");
        NotificationChannel notificationChannel5 = new NotificationChannel(CHANNEL_ID_5, "Tomorrow SUBJECTS", NotificationManager.IMPORTANCE_HIGH);
        notificationChannel5.setDescription("Subjects tomorrow");

        NotificationManager manager = getSystemService(NotificationManager.class);

        manager.createNotificationChannel(notificationChannel2);
        manager.createNotificationChannel(notificationChannel1);
        manager.createNotificationChannel(notificationChannel3);
        manager.createNotificationChannel(notificationChannel4);
        manager.createNotificationChannel(notificationChannel5);
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
                if (v.getLong(3) > (hour + (60 * 5)) && dow == v.getInt(2)) {
                    time = v.getLong(3) * 1000;
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

        notify.putExtra("name", name);
        notify.putExtra("time", start);
        notify.putExtra("duration", duration);
        PendingIntent pendingNotify = PendingIntent.getBroadcast(
                this,
                1, notify,
                PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingNotify);
        manager.setExact(AlarmManager.RTC_WAKEUP, start - (60 * 5 * 1000), pendingNotify);
    }

    public void setAlarms() {
        int dow = 0;

        int id;
        for (id = -1; dow < 7; ++dow) {
            int b = getAlarm(dow);
            if (b >= 0) {
                id = b;
                break;
            }
        }

        if (id >= 0) {
            DbHelper dbHelper = new DbHelper(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor row = db.rawQuery("SELECT * FROM alarm WHERE id = '" + id + '\'', null);
            if (row.moveToFirst()) {
                Intrinsics.checkNotNullExpressionValue(row, "row");
                if (row.getCount() == 1) {
                    long rest = row.getLong(2) * (long) 1000;
                    Calendar calendar = Calendar.getInstance();
                    int hour = calendar.get(11);
                    int minute = calendar.get(12);
                    long thour = 3600L * (long) hour + 60L * (long) minute;
                    int tdow = calendar.get(7) - 1;
                    dow += tdow;
                    int r = tdow > dow ? 7 - (tdow + 1) + dow + 1 : dow + 1 - (tdow + 1);
                    long time = (long) r * 86400000L + rest - thour * (long) 1000;
                    setNotify(row.getInt(4), time);
                }
            }
        }

    }

    private void setNotify(int type, long diff) {
        long start = Calendar.getInstance().getTimeInMillis() + diff;

        Intent notify = new Intent(this, AlarmWorkManager.class);
        if (type == 0) {
            notify.setAction(AlarmWorkManager.ACTION_TIME_TO_DO_HOMEWORK);
        } else if (type == 1) {
            notify.setAction(AlarmWorkManager.ACTION_TIME_TO_DO_HOMEWORK);
            notify.putExtra("alarm", 1);
        } else if (type == 2) {
            notify.setAction(AlarmWorkManager.ACTION_TOMORROW_EVENTS);
        }

        PendingIntent pendingNotify = PendingIntent.getBroadcast(
                this,
                0, notify,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingNotify);
        manager.setExact(AlarmManager.RTC_WAKEUP, start, pendingNotify);
    }

    private int getAlarm(int i) {
        DbHelper dbHelper = new DbHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Calendar calendar = Calendar.getInstance();
        String query = "";
        int dow = (calendar.get(7) - 1 + i) % 7 + 1;
        if (dow == 1) {
            query = "AND sunday = '1'";
        } else if (dow == 2) {
            query = "AND monday = '1'";
        } else if (dow == 3) {
            query = "AND tuesday = '1'";
        } else if (dow == 4) {
            query = "AND wednesday = '1'";
        } else if (dow == 5) {
            query = "AND thursday = '1'";
        } else if (dow == 6) {
            query = "AND friday = '1'";
        } else if (dow == 7) {
            query = "AND saturday = '1'";
        }

        Cursor row = db.rawQuery("SELECT * FROM alarm WHERE last_alarm != '" + dow + "' " + query + " ORDER BY hour ASC", (String[]) null);
        int hour = calendar.get(11);
        int minute = calendar.get(12);
        long time = 3600L * (long) hour + 60L * (long) minute;
        int id = -1;
        if (row.moveToFirst()) {
            do {
                if (calendar.get(7) != dow) {
                    id = row.getInt(0);
                    break;
                }

                if (row.getLong(2) >= time) {
                    id = row.getInt(0);
                    break;
                }
            } while (row.moveToNext());
        }

        row.close();
        return id;
    }


    void setAlarm() {
        DbHelper dbHelper = new DbHelper(this);
        SQLiteDatabase dbr = dbHelper.getReadableDatabase();
        SQLiteDatabase dbw = dbHelper.getWritableDatabase();
        Cursor row = dbr.rawQuery(
                "SELECT * FROM " + DbHelper.t_alarm + " WHERE title = 'TTDH' AND (alarm = '0' OR alarm = '1')",
                null
        );

        if (row.getCount() < 1) {
            ContentValues values = new ContentValues();
            values.put("title", "TTDH");
            values.put("hour", 39600L);
            values.put("last_alarm", 1);
            values.put("alarm", 0);
            values.put("sunday", 1);
            values.put("monday", 1);
            values.put("tuesday", 1);
            values.put("wednesday", 1);
            values.put("thursday", 1);
            values.put("friday", 1);
            values.put("saturday", 1);

            dbw.insert(DbHelper.t_alarm, null, values);
        }

        row.close();

        row = dbr.rawQuery("SELECT * FROM " + DbHelper.t_alarm + " WHERE title = 'NDE' ", null);

        if (row.getCount() < 1) {
            ContentValues values = new ContentValues();
            values.put("title", "NDE");
            values.put("hour", 79200L);
            values.put("last_alarm", 1);
            values.put("alarm", 2);
            values.put("sunday", 1);
            values.put("monday", 1);
            values.put("tuesday", 1);
            values.put("wednesday", 1);
            values.put("thursday", 1);
            values.put("friday", 1);
            values.put("saturday", 1);

            dbw.insert(DbHelper.t_alarm, null, values);
        }

        row.close();
    }
}