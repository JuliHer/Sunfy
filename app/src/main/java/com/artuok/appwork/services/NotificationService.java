package com.artuok.appwork.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import java.util.Calendar;

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

        if (alarm) {
            displayAlarmDH();
        } else {
            showNotifyDH();
        }


        SharedPreferences sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean as = sharedPreferences.getBoolean("AlarmSet", false);
        int h = Integer.parseInt(sharedPreferences.getString("timeTDH", "11:00").split(":")[0]);
        int m = Integer.parseInt(sharedPreferences.getString("timeTDH", "11:00").split(":")[1]);
        setAlarm(h, m, as);
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

    private void setAlarm(int hour, int minute, boolean alarm) {
        final Calendar c = Calendar.getInstance();
        long rest = 0;
        int hr = 0;
        if (c.get(Calendar.HOUR_OF_DAY) >= hour) {
            hr = 24 + hour - c.get(Calendar.HOUR_OF_DAY);
        } else {
            hr = hour - c.get(Calendar.HOUR_OF_DAY);
        }

        int mr = minute - c.get(Calendar.MINUTE);

        rest = (hr * 60L * 60L * 1000L) + (mr * 60L * 1000L);

        Calendar a = Calendar.getInstance();
        rest += a.getTimeInMillis();

        a.setTimeInMillis(rest);
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent notify = new Intent(this, AlarmWorkManager.class)
                .setAction(AlarmWorkManager.ACTION_TIME_TO_DO_HOMEWORK);
        notify.putExtra("time", rest);
        if (alarm) {
            notify.putExtra("alarm", 1);
        }
        PendingIntent pendingNotify = PendingIntent.getBroadcast(
                this,
                0, notify,
                PendingIntent.FLAG_UPDATE_CURRENT);

        manager.cancel(pendingNotify);
        manager.setExact(AlarmManager.RTC_WAKEUP, rest, pendingNotify);
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
}
