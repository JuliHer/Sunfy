package com.artuok.appwork.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmWorkManager extends BroadcastReceiver {
    public static final String ACTION_NOTIFY = "NOTIFY";
    public static final String ACTION_TIME_TO_DO_HOMEWORK = "HOMEWORK";
    public static final String ACTION_POSTPONE = "POSTPONE";
    public static final String ACTION_DISMISS = "DISMISS";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("say", "hi");
        Intent intent1 = new Intent(context, NotificationService.class);
        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_NOTIFY:
                    intent1.setAction(ACTION_NOTIFY);
                    intent1.putExtra("title", intent.getStringExtra("title"));
                    intent1.putExtra("desc", intent.getStringExtra("desc"));

                    context.startForegroundService(intent1);
                    break;
                case ACTION_TIME_TO_DO_HOMEWORK:
                    intent1.setAction(ACTION_TIME_TO_DO_HOMEWORK);
                    intent1.putExtra("time", intent.getLongExtra("time", 0));
                    intent1.putExtra("alarm", intent.getIntExtra("alarm", 0));
                    context.startForegroundService(intent1);
                    break;
                case ACTION_DISMISS:
                    intent1.setAction(ACTION_DISMISS);
                    context.startForegroundService(intent1);
                    break;
                case ACTION_POSTPONE:
                    intent1.setAction(ACTION_POSTPONE);
                    intent1.putExtra("time", intent.getLongExtra("time", 0));
                    intent1.putExtra("alarm", 1);
                    context.startForegroundService(intent1);
                    break;
            }
        }
    }
}
