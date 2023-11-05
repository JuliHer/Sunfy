package com.artuok.appwork;

import static com.artuok.appwork.services.NotificationService.TimeToDoHomework;
import static com.artuok.appwork.services.NotificationService.TomorrowEvent;
import static com.artuok.appwork.services.NotificationService.TomorrowSubjects;

import android.app.AlarmManager;
import android.app.Notification;
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
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.artuok.appwork.db.DbHelper;
import com.artuok.appwork.fragmets.SettingsFragment;
import com.artuok.appwork.library.MessageControler;
import com.artuok.appwork.services.AlarmWorkManager;
import com.artuok.appwork.services.MessageWorkManager;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class InActivity extends AppCompatActivity {

    public static final String CHANNEL_ID_1 = "CHANNEL_1";
    public static final String CHANNEL_ID_2 = "CHANNEL_2";
    public static final String CHANNEL_ID_3 = "CHANNEL_3";
    public static final String CHANNEL_ID_4 = "CHANNEL_4";
    public static final String CHANNEL_ID_5 = "CHANNEL_5";
    public static final String CHANNEL_ID_6 = "CHANNEL_6";
    public static final String GROUP_EVENTS = "com.artuok.appwork.EVENTS";
    public static final String GROUP_MESSAGES = "com.artuok.appwork.MESSAGES";
    public static final String GROUP_SUBJECTS = "com.artuok.appwork.SUBJECTS";

    private List<String> numberPhones = new ArrayList();

    private int contactsCount = 0;
    private int contactsDetailed = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_AppWork);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Preferences();

        MobileAds.initialize(this);
        createNotificationChannel();
        setAlarm();
        activateAlarms();
        setAlarmSchedule();
        setMainProject();
        if(SettingsFragment.isLogged(this)){
            MessageControler.restateUserChat(this, null);
            WorkManager workManager = WorkManager.getInstance(this);
            PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(MessageWorkManager.class, PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS).build();
            workManager.enqueueUniquePeriodicWork("messageWorker", ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, periodicWorkRequest);
        }
        new Handler().postDelayed(() -> loadMain(), 500);
    }

    void setMainProject(){
        DbHelper dbHelper = new DbHelper(this);
        SQLiteDatabase dbr = dbHelper.getReadableDatabase();
        SQLiteDatabase dbw = dbHelper.getWritableDatabase();
        String name = getString(R.string.personal);
        Cursor query = dbr.rawQuery("SELECT * FROM "+DbHelper.T_PROJECTS+" WHERE name = ?", new String[]{name});
        if(!query.moveToFirst()){
            ContentValues values = new ContentValues();
            values.put("id", 0);
            values.put("name", name);
            values.put("description", "User project");
            dbw.insert(DbHelper.T_PROJECTS, null, values);
        }
        query.close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    void loadMain() {
        Intent intent = new Intent(this, MainActivity.class);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.getInt("task", 0) == 1) {
                intent = new Intent(this, MainActivity.class);
                intent.putExtra("task", "new task");
            }
        }
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
        NotificationChannel notificationChannel6 = new NotificationChannel(CHANNEL_ID_6, "Chat Notifications", NotificationManager.IMPORTANCE_HIGH);
        notificationChannel6.setDescription("Chat Messages");


        notificationChannel1.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationChannel2.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationChannel3.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationChannel4.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationChannel5.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationChannel1.enableVibration(true);
        notificationChannel2.enableVibration(true);
        notificationChannel3.enableVibration(true);
        notificationChannel4.enableVibration(true);
        notificationChannel5.enableVibration(true);
        notificationChannel6.enableVibration(true);


        NotificationManager manager = getSystemService(NotificationManager.class);

        manager.createNotificationChannel(notificationChannel2);
        manager.createNotificationChannel(notificationChannel1);
        manager.createNotificationChannel(notificationChannel3);
        manager.createNotificationChannel(notificationChannel4);
        manager.createNotificationChannel(notificationChannel5);
        manager.createNotificationChannel(notificationChannel6);
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

    private void setAlarm() {
        DbHelper dbHelper =  new DbHelper(this);
        SQLiteDatabase dbr = dbHelper.getReadableDatabase();
        SQLiteDatabase dbw = dbHelper.getWritableDatabase();
        Cursor row = dbr.rawQuery(
                "SELECT * FROM "+DbHelper.t_alarm,
                null
        );

        List<String> alarms = new ArrayList();

        if(row.moveToFirst()){
            do {
                String alarm = row.getString(1);
                alarms.add(alarm);
            }while (row.moveToNext());
        }

        if(!alarms.contains(TimeToDoHomework)){
            ContentValues cv = new ContentValues();
            cv.put("title", TimeToDoHomework);
            cv.put("hour", 39600);
            cv.put("last_alarm", 9);
            cv.put("alarm", 0);

            dbw.insert(DbHelper.t_alarm, null, cv);
        }

        if(!alarms.contains(TomorrowSubjects)){
            ContentValues cv = new ContentValues();
            cv.put("title", TomorrowSubjects);
            cv.put("hour", 57600);
            cv.put("last_alarm", 9);
            cv.put("alarm", 0);

            dbw.insert(DbHelper.t_alarm, null, cv);
        }

        if(!alarms.contains(TomorrowEvent)){
            ContentValues cv = new ContentValues();
            cv.put("title", TomorrowEvent);
            cv.put("hour", 64800);
            cv.put("last_alarm", 9);
            cv.put("alarm", 0);

            dbw.insert(DbHelper.t_alarm, null, cv);
        }

        row.close();
    }
}