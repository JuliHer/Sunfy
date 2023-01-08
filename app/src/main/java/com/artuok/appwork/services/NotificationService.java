package com.artuok.appwork.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.artuok.appwork.AlarmActivity;
import com.artuok.appwork.InActivity;
import com.artuok.appwork.R;
import com.artuok.appwork.db.DbHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import kotlin.jvm.internal.Intrinsics;

public class NotificationService extends Service {

    private final Binder mBinder = new NotificationServiceBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        foregroundNotify();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle extras = intent.getExtras();

        if (intent.getAction() != null) {

            switch (intent.getAction()) {
                case AlarmWorkManager.ACTION_NOTIFY:
                    notifyAction(extras.getString("title"), extras.getString("desc"));
                    destroy();
                    break;
                case AlarmWorkManager.ACTION_TIME_TO_DO_HOMEWORK:
                    notifyDoHomework(extras.getInt("alarm", 0) > 0);
                    destroy();
                    break;
                case AlarmWorkManager.ACTION_POSTPONE:
                    startRepeat(extras.getInt("time", 0));
                    destroy();
                    break;
                case AlarmWorkManager.ACTION_DISMISS:
                    cancelDoHomework();
                    destroy();
                    break;
                case AlarmWorkManager.ACTION_EVENT:
                    eventNotification(extras.getString("name"), extras.getLong("time"), extras.getLong("duration"));
                    destroy();
                    break;
                case AlarmWorkManager.ACTION_TOMORROW_EVENTS:
                    notifyTEvents();
                    destroy();
                    break;
                case AlarmWorkManager.ACTION_TOMORROW_SUBJECTS:
                    notifyTSubjects();
                    destroy();
                    break;
            }
        }

        return START_STICKY;
    }

    private void startRepeat(int time) {
        time++;
        Intent alarmRinging = new Intent(this, AlarmActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        alarmRinging.putExtra("time", time);
        startActivity(alarmRinging);
    }

    private void notifyAction(String name, String description) {
        Notification notification = new NotificationCompat.Builder(this, InActivity.CHANNEL_ID_1)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(name)
                .setContentText(description)
                .build();


        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        assert manager != null;
        manager.notify(1, notification);
    }

    private void cancelDoHomework() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(2);
        }
    }

    private void notifyTSubjects() {
        DbHelper dbHelper = new DbHelper(this);
        SQLiteDatabase dbr = dbHelper.getReadableDatabase();
        Calendar calendar = Calendar.getInstance();

        int dow = calendar.get(Calendar.DAY_OF_WEEK) % 7;


        Cursor row = dbr.rawQuery("SELECT * FROM " + DbHelper.t_event + " WHERE day_of_week = '" + dow + "' ORDER BY time DESC", null);

        if (row.moveToFirst()) {
            Notification notification = new NotificationCompat.Builder(this, InActivity.CHANNEL_ID_5)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setColor(Color.parseColor("#1982C4"))
                    .setOnlyAlertOnce(true)
                    .setGroup(InActivity.GROUP_SUBJECTS)
                    .setGroupSummary(true)
                    .build();
            int i = 200000;
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            assert manager != null;
            manager.notify(i, notification);
            do {

                long start = row.getLong(3);
                long end = start + row.getLong(4);

                int h = (int) (start / 60 / 60);

                int m = (int) (start / 60 % 60);

                String min = m < 10 ? "0" + m : "" + m;
                int hr = h > 12 ? h - 12 : h;
                String hour = hr < 10 ? "0" + hr : "" + hr;
                String tm = h > 11 ? "pm" : "am";
                String time = hour + ":" + min + " " + tm + " -> ";

                h = (int) (end / 60 / 60);

                m = (int) (end / 60 % 60);

                min = m < 10 ? "0" + m : "" + m;
                hr = h > 12 ? h - 12 : h;
                hour = hr < 10 ? "0" + hr : "" + hr;
                tm = h > 11 ? "pm" : "am";
                time += hour + ":" + min + " " + tm;

                notifyTSubject(row.getInt(0), row.getString(1), time);
            } while (row.moveToNext());
        }
        if (row.getCount() < 1) {
            Notification notification = new NotificationCompat.Builder(this, InActivity.CHANNEL_ID_5)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setColor(Color.parseColor("#1982C4"))
                    .setContentTitle(getString(R.string.congratulations))
                    .setContentText("You Haven't Subjects Tomorrow")
                    .setShowWhen(true)
                    .setOnlyAlertOnce(true)
                    .build();

            int i = 200001;
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            assert manager != null;
            manager.notify(i, notification);
        }

        row.close();

        SQLiteDatabase dbw = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        int dow1 = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        values.put("last_alarm", dow1);
        dbw.update(DbHelper.t_alarm, values, "title = 'NDS'", null);

        setAlarms();
    }

    private void notifyTSubject(int id, String title, String time) {
        Notification notification = new NotificationCompat.Builder(this, InActivity.CHANNEL_ID_5)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setColor(Color.parseColor("#1982C4"))
                .setContentTitle(title)
                .setContentText(time)
                .setShowWhen(true)
                .setOnlyAlertOnce(true)
                .setGroup(InActivity.GROUP_SUBJECTS)
                .build();

        int i = 200001 + id;
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        assert manager != null;
        manager.notify(i, notification);

    }

    private void notifyTEvents() {
        DbHelper dbHelper = new DbHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Calendar calendar = Calendar.getInstance();
        long day = 86400000;

        long tday = calendar.getTimeInMillis() + day;
        calendar.setTimeInMillis(tday);

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
        String tomorrows = format.format(calendar.getTime());
        format.applyPattern("yyyy-MM-dd 23:59:59");
        String tomorrowe = format.format(calendar.getTime());

        Cursor row = db.rawQuery("SELECT * FROM " + DbHelper.t_task + " WHERE end_date BETWEEN '" + tomorrows + "' AND '" + tomorrowe + "' ORDER BY end_date ASC", null);


        if (row.moveToFirst()) {
            Notification notification = new NotificationCompat.Builder(this, InActivity.CHANNEL_ID_4)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setColor(Color.parseColor("#1982C4"))
                    .setOnlyAlertOnce(true)
                    .setGroup(InActivity.GROUP_EVENTS)
                    .setGroupSummary(true)
                    .build();
            int i = 100000;
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            assert manager != null;
            manager.notify(i, notification);
            do {
                SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                Calendar c = Calendar.getInstance();
                try {
                    c.setTime(format1.parse(row.getString(3)));
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                int m = c.get(Calendar.MINUTE);
                String min = m < 10 ? "0" + m : m + "";
                String tm = c.get(Calendar.AM_PM) == Calendar.AM ? "am" : "pm";
                String hr = c.get(Calendar.HOUR) == 0 ? "12" : c.get(Calendar.HOUR) + "";
                String hour = hr + ":" + min + " " + tm;
                String time = getString(R.string.will_end) + " " + hour;
                notifyTEvent(row.getInt(0), row.getString(5), time);
            } while (row.moveToNext());
        }

        if (row.getCount() < 1) {
            Notification notification = new NotificationCompat.Builder(this, InActivity.CHANNEL_ID_4)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setColor(Color.parseColor("#1982C4"))
                    .setContentTitle(getString(R.string.congratulations))
                    .setContentText(getString(R.string.yht_for_tomorrow))
                    .setShowWhen(true)
                    .setOnlyAlertOnce(true)
                    .build();

            int i = 100001;
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            assert manager != null;
            manager.notify(i, notification);
        }

        row.close();

        SQLiteDatabase dbw = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        int dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        values.put("last_alarm", dow);
        dbw.update(DbHelper.t_alarm, values, "title = 'NDE'", null);

        setAlarms();
    }

    private void notifyTEvent(int id, String title, String time) {
        Notification notification = new NotificationCompat.Builder(this, InActivity.CHANNEL_ID_4)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setColor(Color.parseColor("#1982C4"))
                .setContentTitle(title)
                .setContentText(time)
                .setShowWhen(true)
                .setOnlyAlertOnce(true)
                .setGroup(InActivity.GROUP_EVENTS)
                .build();

        int i = 100001 + id;
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        assert manager != null;
        manager.notify(i, notification);

    }

    private void showNotifyDH() {
        String g;
        String t;
        int count = awaitingActivities();

        if (count > 0) {
            t = getString(R.string.its_time_to_do_homework);
        } else {
            t = getString(R.string.congratulations);
        }

        if (count > 1) {
            g = getString(R.string.you_have) + " " + count + " " + getString(R.string.pending_activities);
        } else if (count == 1) {
            g = getString(R.string.you_have) + " " + count + " " + getString(R.string.pending_activity);
        } else {
            g = getString(R.string.you_havent_tasks);
        }
        Notification foreground = new NotificationCompat.Builder(this, InActivity.CHANNEL_ID_2)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setColor(Color.parseColor("#1982C4"))
                .setContentTitle(t)
                .setContentText(g)
                .setShowWhen(true)
                .build();
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        assert manager != null;
        manager.notify(2, foreground);
    }

    private void displayAlarmDH() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent cancelAlarm = new Intent(this, AlarmWorkManager.class);
            cancelAlarm.setAction(AlarmWorkManager.ACTION_DISMISS);
            PendingIntent cancelPendingIntent =
                    PendingIntent.getBroadcast(this, 0, cancelAlarm, 0);

            Intent postponeIntent = new Intent(this, AlarmWorkManager.class);
            postponeIntent.setAction(AlarmWorkManager.ACTION_DISMISS);
            PendingIntent postponePendingIntent =
                    PendingIntent.getBroadcast(this, 0, postponeIntent, 0);


            Intent fullScreenIntent = new Intent(this, AlarmActivity.class);
            PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this,
                    0,
                    fullScreenIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);


            Notification foreground = new NotificationCompat.Builder(this, InActivity.CHANNEL_ID_3)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setContentTitle(getString(R.string.its_time_to_do_homework))
                    .setContentText("Playing Alarm Ringtone")
                    .setSilent(true)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .addAction(R.drawable.ic_stat_name, getString(R.string.Cancel_M), cancelPendingIntent)
                    .setFullScreenIntent(fullScreenPendingIntent, true)
                    .build();

            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            assert manager != null;
            manager.notify(2, foreground);
        } else {
            Intent intent = new Intent(this, AlarmActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private void notifyDoHomework(boolean alarm) {
        int count = awaitingActivities();
        if (alarm && count > 0) {
            displayAlarmDH();
        } else {
            showNotifyDH();
        }

        DbHelper dbHelper = new DbHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        int dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        values.put("last_alarm", dow);
        db.update(DbHelper.t_alarm, values, "title = 'TTDH'", null);

        setAlarms();
    }

    public void eventNotification(String name, long time, long duration) {
        String t = getString(R.string.your_subject) + " " + name + " " + getString(R.string.is_going_to_start);

        Calendar v = Calendar.getInstance();
        v.setTimeInMillis(time);
        int min = v.get(Calendar.MINUTE);
        String m = min < 10 ? "0" + min : min + "";
        String tm = v.get(Calendar.AM_PM) == Calendar.AM ? "am" : "pm";
        String g = v.get(Calendar.HOUR) + ":" + m + " " + tm + " -> ";
        long end = time + duration;
        v.setTimeInMillis(end);
        min = v.get(Calendar.MINUTE);
        m = min < 10 ? "0" + min : min + "";
        tm = v.get(Calendar.AM_PM) == Calendar.AM ? "am" : "pm";
        g += v.get(Calendar.HOUR) + ":" + m + " " + tm;
        Notification foreground = new NotificationCompat.Builder(this, InActivity.CHANNEL_ID_2)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(t)
                .setColor(Color.parseColor("#1982C4"))
                .setContentText(g)
                .setShowWhen(true)
                .build();
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        assert manager != null;
        setAlarmSchedule();
        manager.notify(2, foreground);
    }

    public void foregroundNotify() {
        Notification foreground = new NotificationCompat.Builder(this, InActivity.CHANNEL_ID_2)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle("Activating")
                .setSilent(true)
                .setAutoCancel(true)
                .build();
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        assert manager != null;
        manager.notify(3, foreground);
        startForeground(1, foreground);
    }

    public class NotificationServiceBinder extends Binder {
        public NotificationService getServices() {
            return NotificationService.this;
        }
    }

    public void destroy() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (manager != null) {
            stopForeground(true);
            manager.cancel(3);
        }
        stopSelf();
    }

    public int awaitingActivities() {
        DbHelper dbHelper = new DbHelper(NotificationService.this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        long a = Calendar.getInstance().getTimeInMillis();

        Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.t_task + " WHERE status = '0'", null);

        int b = cursor.getCount();

        cursor.close();

        return b;
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
}
