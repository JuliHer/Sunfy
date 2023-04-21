package com.artuok.appwork.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
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
import android.text.SpannableString;
import android.text.style.ImageSpan;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.artuok.appwork.AlarmActivity;
import com.artuok.appwork.InActivity;
import com.artuok.appwork.R;
import com.artuok.appwork.db.DbHelper;

import java.util.Calendar;
import java.util.Random;

public class NotificationService extends Service {

    private final Binder mBinder = new NotificationServiceBinder();
    public static final String TimeToDoHomework = "TTDH";
    public static final String TomorrowEvent = "TE";
    public static final String TomorrowSubjects = "TS";


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
                    notifyDoHomework();
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
                    .setContentTitle(TomorrowSubjects)
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

                int startS = time.length()-1;
                int endS = time.length();

                h = (int) (end / 60 / 60);

                m = (int) (end / 60 % 60);

                min = m < 10 ? "0" + m : "" + m;
                hr = h > 12 ? h - 12 : h;
                hour = hr < 10 ? "0" + hr : "" + hr;
                tm = h > 11 ? "pm" : "am";
                time += hour + ":" + min + " " + tm;

                ImageSpan imageSpan = new ImageSpan(this, R.drawable.arrow_right);
                SpannableString s = new SpannableString(time);
                s.setSpan(imageSpan, startS, endS, 0);

                notifyTSubject(row.getInt(0), row.getString(1), s);
            } while (row.moveToNext());
        }
        if (row.getCount() < 1) {
            Notification notification = new NotificationCompat.Builder(this, InActivity.CHANNEL_ID_5)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setColor(Color.parseColor("#1982C4"))
                    .setContentTitle(getString(R.string.congratulations))
                    .setContentText(getString(R.string.no_task_tomorrow))
                    .setShowWhen(true)
                    .setOnlyAlertOnce(false)
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
        dbw.update(DbHelper.t_alarm, values, "title = '"+TomorrowSubjects+"'", null);

        activateAlarms();
    }

    private void notifyTSubject(int id, String title, SpannableString time) {
        Notification notification = new NotificationCompat.Builder(this, InActivity.CHANNEL_ID_5)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setColor(Color.parseColor("#1982C4"))
                .setContentTitle(title)
                .setContentText(time)
                .setShowWhen(true)
                .setOnlyAlertOnce(false)
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

        int dd = calendar.get(Calendar.DAY_OF_MONTH);
        int dm = calendar.get(Calendar.MONTH);
        int dy = calendar.get(Calendar.YEAR);

        calendar.set(dy, dm, dd, 0, 0, 0);
        long tomorrows = calendar.getTimeInMillis();

        calendar.set(dy, dm, dd, 23, 59, 59);
        long tomorrowe = calendar.getTimeInMillis();

        Cursor row = db.rawQuery("SELECT * FROM " + DbHelper.T_TASK + " WHERE end_date > '" + tomorrows + "' AND end_date < '" + tomorrowe + "' ORDER BY end_date ASC", null);


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
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(row.getLong(2));

                int m = c.get(Calendar.MINUTE);
                String min = m < 10 ? "0" + m : m + "";
                String tm = c.get(Calendar.AM_PM) == Calendar.AM ? "am" : "pm";
                String hr = c.get(Calendar.HOUR) == 0 ? "12" : c.get(Calendar.HOUR) + "";
                String hour = hr + ":" + min + " " + tm;
                String time = getString(R.string.will_end) + " " + hour;
                notifyTEvent(row.getInt(0), row.getString(4), time);
            } while (row.moveToNext());
        }

        if (row.getCount() < 1) {
            Notification notification = new NotificationCompat.Builder(this, InActivity.CHANNEL_ID_4)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setColor(Color.parseColor("#1982C4"))
                    .setContentTitle(getString(R.string.congratulations))
                    .setContentText(getString(R.string.yht_for_tomorrow))
                    .setShowWhen(true)
                    .setOnlyAlertOnce(false)
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
        dbw.update(DbHelper.t_alarm, values, "title = '"+TomorrowEvent+"'", null);
        activateAlarms();
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

    private void showNotifyDH(int count) {
        String g;
        String t;

        int r = new Random().nextInt() % 3;
        if (count > 0) {
            if (r == 0) {
                t = getString(R.string.its_time_to_do_homework);
            } else if (r == 1) {
                t = getString(R.string.dont_forgot);
            } else {
                t = getString(R.string.didnt_you_do);
            }
        } else {
            if (r == 0) {
                t = getString(R.string.congratulations);
            } else if (r == 1) {
                t = getString(R.string.free_day);
            } else {
                t = getString(R.string.nothing_to_do);
            }
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

    private boolean getAlarmStatus() {
        SharedPreferences s = getSharedPreferences("settings", Context.MODE_PRIVATE);

        return s.getBoolean("alarm", false);
    }

    private void notifyDoHomework() {
        int count = awaitingActivities();

        if (getAlarmStatus() && count > 0) {
            showAlarmNotification(count);
        } else {
            showNotifyDH(count);
        }

        DbHelper dbHelper = new DbHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        int dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
        values.put("last_alarm", dow);
        db.update(DbHelper.t_alarm, values, "title = '" + TimeToDoHomework + "'", null);
        activateAlarms();
    }

    public void showAlarmNotification(int count) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            String g = "";
            if (count > 1) {
                g = getString(R.string.you_have) + " " + count + " " + getString(R.string.pending_activities);
            } else if (count == 1) {
                g = getString(R.string.you_have) + " " + count + " " + getString(R.string.pending_activity);
            }
            Intent cancelAlarm = new Intent(this, AlarmWorkManager.class);
            cancelAlarm.setAction(AlarmWorkManager.ACTION_DISMISS);
            PendingIntent cancelPendingIntent =
                    PendingIntent.getBroadcast(this, 0, cancelAlarm, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Intent fullScreenIntent = new Intent(this, AlarmActivity.class);
            PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this,
                    0,
                    fullScreenIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Notification foreground = new NotificationCompat.Builder(this, InActivity.CHANNEL_ID_3)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setContentTitle(getString(R.string.its_time_to_do_homework))
                    .setContentText(g)
                    .setColor(Color.parseColor("#1982C4"))
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

    public void eventNotification(String name, long time, long duration) {
        String t = getString(R.string.your_subject) + " " + name + " " + getString(R.string.is_going_to_start);

        Calendar v = Calendar.getInstance();
        v.setTimeInMillis(time);
        int min = v.get(Calendar.MINUTE);
        String m = min < 10 ? "0" + min : min + "";
        String tm = v.get(Calendar.AM_PM) == Calendar.AM ? "am" : "pm";
        String g = v.get(Calendar.HOUR) + ":" + m + " " + tm + " -> ";

        int startS = g.length()-1;
        int endS = g.length();


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

        Cursor cursor = db.rawQuery("SELECT * FROM " + DbHelper.T_TASK + " WHERE status = '0'", null);

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
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);


        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingNotify);
        manager.setExact(AlarmManager.RTC_WAKEUP, start - (60 * 5 * 1000), pendingNotify);
    }


    private int nextAlarm() {
        DbHelper dbHelper = new DbHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor row = db.rawQuery("SELECT * FROM " + DbHelper.t_alarm + " ORDER BY hour ASC", null);

        Calendar today = Calendar.getInstance();

        long hour = (today.get(Calendar.HOUR_OF_DAY) * 60 * 60) + (today.get(Calendar.MINUTE) * 60) + (today.get(Calendar.SECOND));

        int id = -1;
        int idt = 0;

        if(row.moveToFirst()){
            idt = row.getInt(0);
            do {
                long l = row.getLong(2);

                if(l > hour){
                    id = row.getInt(0);
                    return id;
                }
            }while (row.moveToNext());
        }

        row.close();
        return idt;
    }

    private void activateAlarms(){
        int id = nextAlarm();
        DbHelper dbHelper = new DbHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor row = db.rawQuery("SELECT * FROM "+DbHelper.t_alarm+" WHERE id = '"+id+"'", null);

        if(row.moveToFirst()){
            long hour = row.getLong(2) * 1000;
            Calendar calendar = Calendar.getInstance();
            int hourd = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            int second = calendar.get(Calendar.SECOND);
            long thour = (((60L * 60L * hourd) + (60L * minute) + second)*1000) + calendar.get(Calendar.MILLISECOND);

            long time = hour <= thour ?
                hour + 86400000L - thour
            :
                hour - thour
            ;

            setTimeOut(row.getString(1), time);
        }
        row.close();
    }



    private void setTimeOut(String type, Long diff){
        long start = Calendar.getInstance().getTimeInMillis()+ diff;

        Calendar t = Calendar.getInstance();
        t.setTimeInMillis(start);

        Intent notify = new Intent(this, AlarmWorkManager.class);

        switch (type) {
            case TomorrowEvent:
                notify.setAction(AlarmWorkManager.ACTION_TOMORROW_EVENTS);
                break;
            case TomorrowSubjects:
                notify.setAction(AlarmWorkManager.ACTION_TOMORROW_SUBJECTS);
                break;
            default:
                notify.setAction(AlarmWorkManager.ACTION_TIME_TO_DO_HOMEWORK);
                break;
        }

        PendingIntent pendingNotify = PendingIntent.getBroadcast(this, 0, notify, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        AlarmManager manager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingNotify);
        manager.setExact(AlarmManager.RTC_WAKEUP, start, pendingNotify);

    }
}
