package com.artuok.appwork.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmWorkManager extends BroadcastReceiver {
    public static final String ACTION_NOTIFY = "com.artuok.appwork.services.NOTIFY";
    public static final String ACTION_TIME_TO_DO_HOMEWORK = "com.artuok.appwork.services.HOMEWORK";
    public static final String ACTION_POSTPONE = "com.artuok.appwork.services.POSTPONE";
    public static final String ACTION_DISMISS = "com.artuok.appwork.services.DISMISS";
    public static final String ACTION_EVENT = "com.artuok.appwork.services.EVENT";
    public static final String ACTION_TOMORROW_EVENTS = "com.artuok.appwork.services.TEVENTS";
    public static final String ACTION_TOMORROW_SUBJECTS = "com.artuok.appwork.services.TSUBJECTS";
    public static final String ACTION_MESSAGES = "com.artuok.appwork.services.MESSAGES";
    public static final String ACTION_SET_BACKUP = "com.artuok.appwork.services.SETBACKUP";
    public static final String ACTION_RESTORE_BACKUP = "com.artuok.appwork.services.RESTOREBACKUP";

    @Override
    public void onReceive(Context context, Intent intent) {

        Intent intent1 = new Intent(context, NotificationService.class);
        Intent intent2 = new Intent(context, ServiceManager.class);
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
                case ACTION_EVENT:
                    intent1.setAction(ACTION_EVENT);
                    intent1.putExtra("name", intent.getStringExtra("name"));
                    intent1.putExtra("time", intent.getLongExtra("time", 0));
                    intent1.putExtra("duration", intent.getLongExtra("duration", 0));
                    context.startForegroundService(intent1);
                    break;
                case ACTION_TOMORROW_EVENTS:
                    intent1.setAction(ACTION_TOMORROW_EVENTS);
                    context.startForegroundService(intent1);
                    break;
                case ACTION_TOMORROW_SUBJECTS:
                    intent1.setAction(ACTION_TOMORROW_SUBJECTS);
                    context.startForegroundService(intent1);
                    break;
                case ACTION_MESSAGES:
                    intent2.setAction(ACTION_MESSAGES);
                    context.startForegroundService(intent2);
                    break;
                case ACTION_SET_BACKUP:
                    intent2.setAction(ACTION_SET_BACKUP);
                    context.startForegroundService(intent2);
                    break;
                case ACTION_RESTORE_BACKUP:
                    intent2.setAction(ACTION_RESTORE_BACKUP);
                    context.startForegroundService(intent2);
                    break;

            }
        }
    }
}
