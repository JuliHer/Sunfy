package com.artuok.appwork.widgets;

import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import com.artuok.appwork.InActivity;
import com.artuok.appwork.R;

import java.util.Random;

/**
 * Implementation of App Widget functionality.
 */
public class TodayTaskWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        // Construct the RemoteViews object
        int random = new Random().nextInt();
        Intent t = new Intent(context, RemoteTodayTaskWidget.class);
        t.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        t.putExtra("randomNumber", random);
        t.setData(Uri.parse(t.toUri(Intent.URI_INTENT_SCHEME)));

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.today_task_widget);
        views.setRemoteAdapter(R.id.recycler, t);
        Intent intent = new Intent(context, InActivity.class);
        intent.putExtra("task", 1);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        views.setOnClickPendingIntent(R.id.add_new_task, pendingIntent);
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, null);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if(ACTION_APPWIDGET_UPDATE.equals(action)){
            int[] ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            onUpdate(context, manager, ids);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}