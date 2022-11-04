package com.artuok.appwork.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

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
                    notifyDoHomework(extras.getLong("time", 0), extras.getInt("alarm", 0) > 0);
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

    private void notifyDoHomework(long timeInMillis, boolean alarm) {

        if (alarm) {
            /*KeyguardManager myKM = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if( myKM.inKeyguardRestrictedInputMode()) {

            } else {

            }*/
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent cancelIntent = new Intent(this, AlarmWorkManager.class);
                cancelIntent.setAction(AlarmWorkManager.ACTION_DISMISS);
                PendingIntent cancelPendingIntent =
                        PendingIntent.getBroadcast(this, 0, cancelIntent, 0);

                Intent postponeIntent = new Intent(this, AlarmWorkManager.class);
                postponeIntent.setAction(AlarmWorkManager.ACTION_DISMISS);
                PendingIntent postponePendingIntent =
                        PendingIntent.getBroadcast(this, 0, postponeIntent, 0);
                Log.d("say", "hi " + alarm);
                Intent fullScreenIntent = new Intent(this, AlarmActivity.class);
                PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this,
                        0,
                        fullScreenIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                Notification foreground = new NotificationCompat.Builder(this, InActivity.CHANNEL_ID_2)
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setContentTitle(getString(R.string.its_time_to_do_homework))
                        .setContentText("Playing Alarm Ringtone")
                        .setSilent(true)
                        .setOngoing(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .addAction(R.drawable.ic_stat_name, "Cancel", cancelPendingIntent)
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
        } else {

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
                    .setContentTitle(t)
                    .setContentText(g)
                    .setShowWhen(true)
                    .build();
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            assert manager != null;
            manager.notify(2, foreground);
        }


        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timeInMillis);
        setAlarm(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), alarm);
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

        hour = hour + (minute / 60);

        minute = minute % 60;

        hour = hour % 24;

        Calendar c = Calendar.getInstance();
        int day = 1000 * 60 * 60 * 24;
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        long whe = c.getTimeInMillis() <= Calendar.getInstance().getTimeInMillis() ? c.getTimeInMillis() + day : c.getTimeInMillis();
        Intent notify = new Intent(this, AlarmWorkManager.class)
                .setAction(AlarmWorkManager.ACTION_TIME_TO_DO_HOMEWORK);
        if (alarm) {
            notify.putExtra("alarm", 1);
            Log.d("say", "hello");
        }
        notify.putExtra("time", whe);
        PendingIntent pendingNotify = PendingIntent.getBroadcast(
                this,
                0, notify,
                PendingIntent.FLAG_UPDATE_CURRENT);

        manager.cancel(pendingNotify);
        manager.setExact(AlarmManager.RTC_WAKEUP, whe, pendingNotify);
    }

    private void setPAlarm(int hour, int minute, int time) {

        hour = hour + (minute / 60);

        minute = minute % 60;

        hour = hour % 24;

        Calendar c = Calendar.getInstance();
        int day = 1000 * 60 * 60 * 24;
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        long whe = c.getTimeInMillis() <= Calendar.getInstance().getTimeInMillis() ? c.getTimeInMillis() + day : c.getTimeInMillis();
        Intent notify = new Intent(this, AlarmWorkManager.class)
                .setAction(AlarmWorkManager.ACTION_POSTPONE);

        notify.putExtra("time", time);
        PendingIntent pendingNotify = PendingIntent.getBroadcast(
                this,
                1, notify,
                PendingIntent.FLAG_UPDATE_CURRENT);

        manager.cancel(pendingNotify);
        manager.setExact(AlarmManager.RTC_WAKEUP, whe, pendingNotify);
    }
}
