package com.artuok.appwork;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.artuok.appwork.db.DbHelper;
import com.artuok.appwork.services.AlarmWorkManager;
import com.thekhaeng.pushdownanim.PushDownAnim;

import java.util.Calendar;

public class AlarmActivity extends AppCompatActivity {

    BroadcastReceiver receiver;
    private TextView fhour;
    private TextView postTimer;
    Ringtone ringtoneAlarm;
    boolean isCancelP = false;
    Handler postpone;
    LinearLayout linearLayout1;

    int height = 0;

    BroadcastReceiver receiverS = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == AlarmWorkManager.ACTION_ACTIVITY_DISMISS) {
                cancelVibration(true);
                finish();
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ringing);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            keyguardManager.requestDismissKeyguard(this, null);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }


        linearLayout1 = findViewById(R.id.time_context);

        postTimer = findViewById(R.id.tm_clock);
        fhour = findViewById(R.id.time_clock);
        TextView info = findViewById(R.id.info);
        setPendents(info);
        LinearLayout akc = findViewById(R.id.akc);

        ViewGroup.MarginLayoutParams a = (ViewGroup.MarginLayoutParams) akc.getLayoutParams();
        a.setMargins(a.leftMargin, statusBarHeight() + a.topMargin, a.rightMargin, a.bottomMargin);
        akc.setLayoutParams(a);
        akc.requestLayout();

        Calendar c = Calendar.getInstance();
        String fh = "";

        final boolean isHourFormat = DateFormat.is24HourFormat(this);

        int h = c.get(Calendar.HOUR_OF_DAY);

        if (!isHourFormat && h > 12) {
            h = h - 12;
        }

        fh += h < 10 ? "0" + h + ":" : h + ":";

        fh += c.get(Calendar.MINUTE) < 10 ? "0" + c.get(Calendar.MINUTE) : c.get(Calendar.MINUTE);

        if (!isHourFormat) {
            postTimer.setVisibility(View.VISIBLE);
            String tm = c.get(Calendar.AM_PM) == Calendar.AM ? "a. m." : "p. m.";
            postTimer.setText(tm);
        } else {
            postTimer.setVisibility(View.GONE);
        }

        fhour.setText(fh);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().compareTo(Intent.ACTION_TIME_TICK) == 0) {
                    Calendar c = Calendar.getInstance();
                    String fh = "";

                    int h = c.get(Calendar.HOUR_OF_DAY);

                    if (!isHourFormat && h > 12) {
                        h = h - 12;
                    }

                    fh += h < 10 ? "0" + h + ":" : h + ":";

                    fh += c.get(Calendar.MINUTE) < 10 ? "0" + c.get(Calendar.MINUTE) : c.get(Calendar.MINUTE);

                    if (!isHourFormat) {
                        postTimer.setVisibility(View.VISIBLE);
                        String tm = c.get(Calendar.AM_PM) == Calendar.AM ? "a. m." : "p. m.";
                        postTimer.setText(tm);
                    } else {
                        postTimer.setVisibility(View.GONE);
                    }
                    fhour.setText(fh);
                }
            }
        };



        ImageView button = findViewById(R.id.put_off);
        PushDownAnim.setPushDownAnimTo(button)
                .setScale(PushDownAnim.MODE_SCALE, 0.98f)
                .setDurationPush(100).setOnClickListener(view -> {
                    cancelVibration(false);
                    finish();
                });

        postpone = new Handler();
        postpone.postDelayed(() -> {
            if (!isCancelP) {
                cancelVibration(false);
                Log.d("CattoNotifications", "SendingBroadcast");
                Intent i = new Intent(this, AlarmWorkManager.class)
                        .setAction(AlarmWorkManager.ACTION_TIME_TO_DO_HOMEWORK)
                        .putExtra("notify", true);
                sendBroadcast(i);
                finish();
            }
        }, 300000);


        registerReceiver(receiverS, new IntentFilter(AlarmWorkManager.ACTION_ACTIVITY_DISMISS));
        registerReceiver(receiver, new IntentFilter(Intent.ACTION_TIME_TICK));

        vibrate();
    }


    @Override
    protected void onResume() {
        super.onResume();
        View decorView = getWindow().getDecorView();

        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        decorView.setOnSystemUiVisibilityChangeListener(i -> {
            int uiOptions1 = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(uiOptions1);
        });
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    protected void onStop() {
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
        if(receiverS != null){
            unregisterReceiver(receiverS);
        }
        cancelVibration(false);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        long[] a = {1000, 1000, 1000, 1000};
        vibrator.vibrate(VibrationEffect.createWaveform(a, 0));
        Uri alarmTone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        ringtoneAlarm = RingtoneManager.getRingtone(getApplicationContext(), alarmTone);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ringtoneAlarm.setLooping(true);
        }
        ringtoneAlarm.play();
    }

    void cancelVibration(boolean fromNotification) {
        if(!fromNotification) {
            Intent intent = new Intent(this, AlarmWorkManager.class)
                    .setAction(AlarmWorkManager.ACTION_DISMISS);
            sendBroadcast(intent);
        }
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrator.cancel();
        postpone.removeCallbacksAndMessages(null);
        ringtoneAlarm.stop();
        isCancelP = true;
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

    int statusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    void MoreFiveMinutes() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MINUTE, 8);

        Intent i = getIntent();
        int times = i.getIntExtra("time", 0);
        setPAlarm(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), times);
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

    void setPendents(TextView a) {
        DbHelper dbHelper = new DbHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor p = db.rawQuery("SELECT * FROM " + DbHelper.T_TASK + " WHERE status = '0'", null);

        int count = p.getCount();
        String g = "";
        if (count > 1) {
            g = getResources().getString(R.string.you_have) + " " + count + " " + getResources().getString(R.string.pending_activities);
        } else {
            g = getResources().getString(R.string.you_have) + " " + count + " " + getResources().getString(R.string.pending_activity);
        }


        a.setText(g);

        p.close();
    }

    void doMyHomework(final View view) {
        Intent intent = new Intent(this, MainActivity.class).putExtra("task", "do tasks");
        startActivity(intent);
        finish();
    }
}